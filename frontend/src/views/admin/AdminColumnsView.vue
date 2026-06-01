<script setup>
import {reactive, ref, watch} from 'vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
    addAdminColumnArticleApi,
    createAdminColumnApi,
    deleteAdminColumnApi,
    getAdminArticlesApi,
    getAdminColumnsApi,
    removeAdminColumnArticleApi,
    updateAdminColumnApi
} from '@/api/admin';
import {getColumnArticlesApi} from '@/api/columns';
import {uploadImage} from '@/api/uploads';
import {
    createPagedState,
    readPositiveInt,
    readQueryText,
    resolveAdminOverflowPage,
    syncAdminQuery,
    useAdminRefresh
} from '@/views/admin/adminShared';
import {useRoute, useRouter} from 'vue-router';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const previewUrl = ref('');
const openPreview = (url) => { if (url) previewUrl.value = url; };
const closePreview = () => { previewUrl.value = ''; };
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const COLUMN_STATUS_OPTIONS = [
    {value: 'PUBLISHED', label: '已发布'},
    {value: 'DRAFT', label: '草稿'}
];

const form = reactive({
    authorId: '',
    title: '',
    summary: '',
    coverUrl: '',
    sortOrder: 0
});

const state = reactive({
    ...createPagedState(10),
    keyword: '',
    status: '',
    submitting: false,
    createCoverUploading: false,
    editCoverUploading: false,
    editingId: null,
    editForm: {
        title: '',
        summary: '',
        coverUrl: '',
        sortOrder: 0,
        status: 'PUBLISHED'
    },
    // 文章管理
    managingColumn: null,
    columnArticles: [],
    columnArticlesLoading: false,
    searchKeyword: '',
    searchResults: [],
    searching: false,
    articleActionLoadingId: null
});

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.keyword = readQueryText(route, 'keyword');
    state.status = readQueryText(route, 'status');
    state.jumpPage = String(state.page);
};

const loadColumns = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminColumnsApi(
            state.page,
            state.pageSize,
            state.keyword || null,
            state.status || null
        );
        const overflowPage = resolveAdminOverflowPage(state, result);
        if (overflowPage) {
            state.page = overflowPage;
            state.jumpPage = String(overflowPage);
            await syncQuery({ page: overflowPage > 1 ? String(overflowPage) : undefined });
            return;
        }
        state.items = result.items || [];
        state.total = result.total || 0;
        state.page = result.page || state.page;
        state.jumpPage = String(state.page);
    } catch (error) {
        state.error = error.message || '专栏列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        keyword: patch.keyword ?? (state.keyword || undefined),
        status: patch.status ?? (state.status || undefined)
    });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({ page: targetPage > 1 ? String(targetPage) : undefined });
};

const changeFilter = async () => {
    state.page = 1;
    await syncQuery({
        page: undefined,
        keyword: state.keyword || undefined,
        status: state.status || undefined
    });
};

const resetFilter = async () => {
    state.keyword = '';
    state.status = '';
    state.page = 1;
    await syncQuery({ page: undefined, keyword: undefined, status: undefined });
};

const uploadColumnCover = async (event, targetForm, uploadingKey) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
        toast.error('请选择图片文件');
        event.target.value = '';
        return;
    }
    state[uploadingKey] = true;
    try {
        const result = await uploadImage(file, 'cover');
        targetForm.coverUrl = result.mediumUrl || result.url || result.originalUrl || '';
        toast.success('封面上传成功');
    } catch (error) {
        toast.error(error.message || '封面上传失败');
    } finally {
        state[uploadingKey] = false;
        event.target.value = '';
    }
};

const submitColumn = async () => {
    if (!form.authorId) {
        toast.error('请填写作者 ID');
        return;
    }
    if (state.createCoverUploading) {
        toast.error('封面图片还在上传中');
        return;
    }
    state.submitting = true;
    try {
        await createAdminColumnApi({
            authorId: Number(form.authorId),
            title: form.title,
            summary: form.summary,
            coverUrl: form.coverUrl,
            sortOrder: Number(form.sortOrder || 0)
        });
        form.authorId = '';
        form.title = '';
        form.summary = '';
        form.coverUrl = '';
        form.sortOrder = 0;
        await loadColumns();
    } catch (error) {
        toast.error(error.message || '专栏创建失败');
    } finally {
        state.submitting = false;
    }
};

const startEdit = (column) => {
    state.editingId = column.id;
    state.editForm.title = column.title || '';
    state.editForm.summary = column.summary || '';
    state.editForm.coverUrl = column.coverUrl || '';
    state.editForm.sortOrder = column.sortOrder ?? 0;
    state.editForm.status = column.status || 'PUBLISHED';
};

const cancelEdit = () => {
    state.editingId = null;
};

const saveEdit = async (columnId) => {
    if (state.editCoverUploading) {
        toast.error('封面图片还在上传中');
        return;
    }
    state.submitting = true;
    try {
        await updateAdminColumnApi(columnId, {
            title: state.editForm.title,
            summary: state.editForm.summary,
            coverUrl: state.editForm.coverUrl,
            sortOrder: Number(state.editForm.sortOrder || 0),
            status: state.editForm.status
        });
        state.editingId = null;
        await loadColumns();
    } catch (error) {
        toast.error(error.message || '专栏更新失败');
    } finally {
        state.submitting = false;
    }
};

// ===== 专栏文章管理 =====

const startManageArticles = async (column) => {
    state.managingColumn = column;
    state.columnArticles = [];
    state.searchKeyword = '';
    state.searchResults = [];
    state.articleActionLoadingId = null;
    await loadColumnArticles();
};

const stopManageArticles = async () => {
    state.managingColumn = null;
    state.columnArticles = [];
    state.searchKeyword = '';
    state.searchResults = [];
    await loadColumns();
};

const loadColumnArticles = async () => {
    if (!state.managingColumn) return;
    state.columnArticlesLoading = true;
    try {
        const result = await getColumnArticlesApi(state.managingColumn.id, { page: 1, pageSize: 100 });
        state.columnArticles = result.items || [];
    } catch (error) {
        state.columnArticles = [];
        toast.error('加载专栏文章失败');
    } finally {
        state.columnArticlesLoading = false;
    }
};

const handleSearchArticles = async () => {
    if (!state.searchKeyword.trim()) {
        state.searchResults = [];
        return;
    }
    state.searching = true;
    try {
        const result = await getAdminArticlesApi(1, 20, 'PUBLISHED', state.searchKeyword.trim(), null);
        state.searchResults = (result.items || []).filter(
            (a) => !state.columnArticles.some((ca) => ca.id === a.id)
        );
    } catch (error) {
        toast.error('搜索文章失败');
        state.searchResults = [];
    } finally {
        state.searching = false;
    }
};

const handleAddArticle = async (articleId) => {
    if (!state.managingColumn) return;
    state.articleActionLoadingId = articleId;
    try {
        await addAdminColumnArticleApi(state.managingColumn.id, { articleId, sortOrder: 0 });
        await loadColumnArticles();
        state.searchResults = state.searchResults.filter((a) => a.id !== articleId);
    } catch (error) {
        toast.error(error.message || '添加失败');
    } finally {
        state.articleActionLoadingId = null;
    }
};

const handleRemoveArticle = async (articleId) => {
    if (!state.managingColumn) return;
    state.articleActionLoadingId = articleId;
    try {
        await removeAdminColumnArticleApi(state.managingColumn.id, articleId);
        state.columnArticles = state.columnArticles.filter((a) => a.id !== articleId);
    } catch (error) {
        toast.error(error.message || '移除失败');
    } finally {
        state.articleActionLoadingId = null;
    }
};

const removeColumn = async (column) => {
    openConfirmDialog({
        eyebrow: '专栏治理确认',
        title: '删除专栏',
        message: `确定删除专栏「${column.title}」吗？删除后该专栏将不再对外展示。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            state.submitting = true;
            try {
                await deleteAdminColumnApi(column.id);
                await loadColumns();
            } catch (error) {
                toast.error(error.message || '专栏删除失败');
            } finally {
                state.submitting = false;
            }
        }
    });
};

useAdminRefresh(loadColumns);

watch(
    () => [route.query.page, route.query.keyword, route.query.status],
    () => {
        applyRouteState();
        loadColumns();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel admin-resource-page">
        <div class="admin-toolbar">
            <!-- 新增专栏表单 -->
            <form class="admin-filter-toolbar admin-create-toolbar" @submit.prevent="submitColumn">
                <label>
                    <span>作者 ID</span>
                    <input v-model.trim="form.authorId" type="number" placeholder="用户 ID" required>
                </label>
                <label>
                    <span>专栏标题</span>
                    <input v-model.trim="form.title" type="text" placeholder="专栏标题" required>
                </label>
                <label>
                    <span>简介</span>
                    <input v-model.trim="form.summary" type="text" placeholder="专栏简介">
                </label>
                <div class="cover-field">
                    <span>封面 URL</span>
                    <div class="cover-input-row">
                        <input v-model.trim="form.coverUrl" type="text" placeholder="封面图片 URL 或上传本地图片">
                        <label
                            class="cover-upload-button"
                            :class="{disabled: state.submitting || state.createCoverUploading}"
                        >
                            <input
                                type="file"
                                accept="image/*"
                                :disabled="state.submitting || state.createCoverUploading"
                                @change="uploadColumnCover($event, form, 'createCoverUploading')"
                            >
                            {{ state.createCoverUploading ? '上传中' : '本地上传' }}
                        </label>
                    </div>
                    <img v-if="form.coverUrl" class="cover-preview" :src="form.coverUrl" alt="专栏封面预览" @click="openPreview(form.coverUrl)">
                </div>
                <label>
                    <span>排序值</span>
                    <input v-model.number="form.sortOrder" type="number" placeholder="排序值">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit" :disabled="state.submitting">
                        新增专栏
                    </button>
                </div>
            </form>

            <!-- 关键字搜索 -->
            <div class="admin-filter-toolbar">
                <label>
                    <span>状态</span>
                    <select v-model="state.status" @change="changeFilter">
                        <option value="">全部</option>
                        <option v-for="option in COLUMN_STATUS_OPTIONS" :key="option.value" :value="option.value">
                            {{ option.label }}
                        </option>
                    </select>
                </label>
                <label>
                    <span>关键字搜索</span>
                    <input v-model.trim="state.keyword" type="text" placeholder="标题/简介关键字"
                           @keyup.enter="changeFilter">
                </label>
                <div class="admin-filter-actions">
                    <button type="button" @click="changeFilter">搜索</button>
                    <button type="button" @click="resetFilter">重置</button>
                </div>
            </div>
        </div>

        <div class="admin-table-shell">
            <p v-if="state.error && state.items.length" class="backend-state-text error-text subtle">
                {{ state.error }}
            </p>
            <p v-else-if="state.error && !state.items.length" class="backend-state-text error-text">
                {{ state.error }}
            </p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-columns-table">
                    <table class="admin-table">
                        <colgroup>
                            <col style="width: 8%">
                            <col style="width: 12%">
                            <col style="width: 16%">
                            <col style="width: 12%">
                            <col style="width: 8%">
                            <col style="width: 7%">
                            <col style="width: 8%">
                            <col style="width: 8%">
                            <col style="width: 8%">
                            <col style="width: 13%">
                        </colgroup>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>标题</th>
                                <th>简介</th>
                                <th>封面</th>
                                <th>作者ID</th>
                                <th>排序</th>
                                <th>订阅数</th>
                                <th>文章数</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="column in state.items" :key="column.id">
                                <td>#{{ column.id }}</td>

                                <!-- 标题 -->
                                <td v-if="state.editingId === column.id" class="admin-edit-cell">
                                    <input v-model.trim="state.editForm.title" class="admin-edit-input" type="text">
                                </td>
                                <td v-else><span class="admin-cell-text">{{ column.title }}</span></td>

                                <!-- 简介 -->
                                <td v-if="state.editingId === column.id" class="admin-edit-cell admin-edit-cell-wide">
                                    <input v-model.trim="state.editForm.summary" class="admin-edit-input" type="text">
                                </td>
                                <td v-else class="summary-cell">{{ column.summary || '-' }}</td>

                                <!-- 封面 -->
                                <td v-if="state.editingId === column.id" class="admin-edit-cell cover-edit-cell">
                                    <input
                                        v-model.trim="state.editForm.coverUrl"
                                        class="admin-edit-input"
                                        type="text"
                                        placeholder="封面 URL"
                                    >
                                    <div class="cover-cell-actions">
                                        <label
                                            class="cover-upload-button compact"
                                            :class="{disabled: state.submitting || state.editCoverUploading}"
                                        >
                                            <input
                                                type="file"
                                                accept="image/*"
                                                :disabled="state.submitting || state.editCoverUploading"
                                                @change="
                                                    uploadColumnCover($event, state.editForm, 'editCoverUploading')
                                                "
                                            >
                                            {{ state.editCoverUploading ? '上传中' : '上传' }}
                                        </label>
                                        <img
                                            v-if="state.editForm.coverUrl"
                                            class="cover-thumb"
                                            :src="state.editForm.coverUrl"
                                            alt="专栏封面预览"
                                            @click="openPreview(state.editForm.coverUrl)"
                                        >
                                    </div>
                                </td>
                                <td v-else>
                                    <img v-if="column.coverUrl" class="cover-thumb" :src="column.coverUrl" alt="专栏封面" @click="openPreview(column.coverUrl)">
                                    <span v-else class="admin-subtext">-</span>
                                </td>

                                <!-- 作者 ID -->
                                <td>{{ column.authorId }}</td>

                                <!-- 排序 -->
                                <td v-if="state.editingId === column.id" class="admin-edit-cell admin-edit-cell-narrow">
                                    <input v-model.number="state.editForm.sortOrder" class="admin-edit-input" type="number">
                                </td>
                                <td v-else>{{ column.sortOrder ?? 0 }}</td>

                                <td>{{ column.subscriberCount }}</td>
                                <td>{{ column.articleCount }}</td>

                                <!-- 状态 -->
                                <td v-if="state.editingId === column.id">
                                    <select v-model="state.editForm.status" class="admin-edit-select">
                                        <option
                                            v-for="option in COLUMN_STATUS_OPTIONS"
                                            :key="option.value"
                                            :value="option.value"
                                        >
                                            {{ option.label }}
                                        </option>
                                    </select>
                                </td>
                                <td v-else>
                                    <span class="status-pill" :class="{ danger: column.status !== 'PUBLISHED' }">
                                        {{ column.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                                    </span>
                                </td>

                                <!-- 操作 -->
                                <td class="table-actions">
                                    <template v-if="state.editingId === column.id">
                                        <button type="button" class="admin-edit-btn primary" :disabled="state.submitting"
                                                @click="saveEdit(column.id)">
                                            保存
                                        </button>
                                        <button type="button" class="admin-edit-btn secondary" :disabled="state.submitting"
                                                @click="cancelEdit">取消</button>
                                    </template>
                                    <template v-else>
                                        <button type="button" :disabled="state.submitting"
                                                @click="startEdit(column)">编辑</button>
                                        <button type="button" :disabled="state.submitting || state.managingColumn"
                                                @click="startManageArticles(column)">管理文章</button>
                                        <button type="button" class="danger-link" :disabled="state.submitting"
                                                @click="removeColumn(column)">删除</button>
                                    </template>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有专栏</p>
            </template>
        </div>

        <AdminPagination :state="state" label="专栏分页" @page-change="changePage" />
        <ConfirmDialog
            :visible="confirmDialog.visible"
            :eyebrow="confirmDialog.eyebrow"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :confirm-text="confirmDialog.confirmText"
            :tone="confirmDialog.tone"
            :loading="confirmDialog.loading"
            @close="closeConfirmDialog"
            @confirm="executeConfirmDialog"
        />

        <!-- 专栏文章管理弹窗 -->
        <Teleport to="body">
            <div v-if="state.managingColumn" class="modal-overlay" @click.self="stopManageArticles">
                <div class="modal-container">
                    <div class="modal-header">
                        <h2>管理专栏文章</h2>
                        <p class="modal-subtitle">专栏：{{ state.managingColumn.title }}</p>
                        <button type="button" class="modal-close-btn" @click="stopManageArticles">&times;</button>
                    </div>

                    <!-- 搜索文章 -->
                    <div class="modal-search">
                        <input v-model.trim="state.searchKeyword" type="text"
                               placeholder="搜索已发布文章标题，输入后按回车搜索"
                               @keyup.enter="handleSearchArticles">
                        <button type="button" :disabled="state.searching" @click="handleSearchArticles">
                            搜索
                        </button>
                    </div>

                    <!-- 搜索结果 -->
                    <div v-if="state.searchResults.length" class="modal-section">
                        <p class="modal-section-title">搜索结果（点击添加）</p>
                        <div class="modal-article-list">
                            <div v-for="article in state.searchResults" :key="'s-' + article.id" class="modal-article-row">
                                <div class="modal-article-info">
                                    <span class="modal-article-id">#{{ article.id }}</span>
                                    <span class="modal-article-title">{{ article.title }}</span>
                                    <span class="modal-article-category">{{ article.category || '-' }}</span>
                                </div>
                                <button type="button" :disabled="state.articleActionLoadingId !== null"
                                        @click="handleAddArticle(article.id)">
                                    {{ state.articleActionLoadingId === article.id ? '添加中' : '添加' }}
                                </button>
                            </div>
                        </div>
                    </div>
                    <p v-else-if="state.searchKeyword && !state.searching" class="modal-hint">
                        没有搜索到符合条件的已发布文章
                    </p>

                    <!-- 当前专栏文章 -->
                    <div class="modal-section" style="flex:1; min-height:0; display:flex; flex-direction:column;">
                        <p class="modal-section-title">
                            已有文章（{{ state.columnArticles.length }} 篇）
                        </p>
                        <div v-if="state.columnArticlesLoading" class="modal-hint">加载中...</div>
                        <div v-else-if="state.columnArticles.length" class="modal-article-list" style="flex:1; overflow-y:auto;">
                            <div v-for="article in state.columnArticles" :key="'c-' + article.id" class="modal-article-row">
                                <div class="modal-article-info">
                                    <span class="modal-article-id">#{{ article.id }}</span>
                                    <span class="modal-article-title">{{ article.title }}</span>
                                    <span class="modal-article-category">{{ article.category || '-' }}</span>
                                </div>
                                <button type="button" class="danger-link"
                                        :disabled="state.articleActionLoadingId !== null"
                                        @click="handleRemoveArticle(article.id)">
                                    {{ state.articleActionLoadingId === article.id ? '移除中' : '移除' }}
                                </button>
                            </div>
                        </div>
                        <p v-else class="modal-hint">该专栏暂无文章</p>
                    </div>
                </div>
            </div>
        </Teleport>
    </section>

    <Teleport to="body">
        <div v-if="previewUrl" class="image-preview-overlay" @click.self="closePreview">
            <button type="button" class="image-preview-close" @click="closePreview">&times;</button>
            <img :src="previewUrl" alt="封面预览" class="image-preview-full">
        </div>
    </Teleport>
</template>

<style scoped src="@/styles/views/admin/AdminColumnsView.part-1.css"></style>
<style scoped src="@/styles/views/admin/AdminColumnsView.part-2.css"></style>
