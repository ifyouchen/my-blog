<script setup>
import {reactive, ref, watch} from 'vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
    addAdminTopicArticleApi,
    createAdminTopicApi,
    deleteAdminTopicApi,
    getAdminArticlesApi,
    getAdminTopicsApi,
    removeAdminTopicArticleApi,
    updateAdminTopicApi
} from '@/api/admin';
import {getTopicArticlesApi} from '@/api/topic';
import {uploadImage} from '@/api/uploads';
import {
    createPagedState,
    readPositiveInt,
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
const getDifficultyLabel = (d) => d === 'BEGINNER' ? '入门' : d === 'ADVANCED' ? '深入' : '进阶';
const previewUrl = ref('');
const openPreview = (url) => { if (url) previewUrl.value = url; };
const closePreview = () => { previewUrl.value = ''; };
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const form = reactive({
    title: '',
    summary: '',
    coverUrl: '',
    sortOrder: 0,
    difficulty: 'INTERMEDIATE'
});

const state = reactive({
    ...createPagedState(10),
    keyword: '',
    submitting: false,
    createCoverUploading: false,
    editCoverUploading: false,
    editingId: null,
    editForm: {
        title: '',
        summary: '',
        coverUrl: '',
        sortOrder: 0,
        status: 'PUBLISHED',
        difficulty: 'INTERMEDIATE'
    },
    // 文章管理
    managingTopic: null,
    topicArticles: [],
    topicArticlesLoading: false,
    searchKeyword: '',
    searchResults: [],
    searching: false,
    articleActionLoadingId: null
});

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.keyword = route.query.keyword || '';
    state.jumpPage = String(state.page);
};

const loadTopics = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminTopicsApi(
            state.page,
            state.pageSize,
            state.keyword || null
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
        state.error = error.message || '专题列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        keyword: patch.keyword ?? (state.keyword || undefined)
    });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({ page: targetPage > 1 ? String(targetPage) : undefined });
};

const changeFilter = async () => {
    state.page = 1;
    await syncQuery({ page: undefined, keyword: state.keyword || undefined });
};

const uploadTopicCover = async (event, targetForm, uploadingKey) => {
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

const submitTopic = async () => {
    if (!form.title) {
        toast.error('请填写专题标题');
        return;
    }
    if (state.createCoverUploading) {
        toast.error('封面图片还在上传中');
        return;
    }
    state.submitting = true;
    try {
        await createAdminTopicApi({
            title: form.title,
            summary: form.summary,
            coverUrl: form.coverUrl,
            sortOrder: Number(form.sortOrder || 0),
            difficulty: form.difficulty
        });
        form.title = '';
        form.summary = '';
        form.coverUrl = '';
        form.sortOrder = 0;
        form.difficulty = 'INTERMEDIATE';
        await loadTopics();
    } catch (error) {
        toast.error(error.message || '专题创建失败');
    } finally {
        state.submitting = false;
    }
};

const startEdit = (topic) => {
    state.editingId = topic.id;
    state.editForm.title = topic.title || '';
    state.editForm.summary = topic.summary || '';
    state.editForm.coverUrl = topic.coverUrl || '';
    state.editForm.sortOrder = topic.sortOrder ?? 0;
    state.editForm.status = topic.status || 'PUBLISHED';
    state.editForm.difficulty = topic.difficulty || 'INTERMEDIATE';
};

const cancelEdit = () => {
    state.editingId = null;
};

const saveEdit = async (topicId) => {
    if (state.editCoverUploading) {
        toast.error('封面图片还在上传中');
        return;
    }
    state.submitting = true;
    try {
        await updateAdminTopicApi(topicId, {
            title: state.editForm.title,
            summary: state.editForm.summary,
            coverUrl: state.editForm.coverUrl,
            sortOrder: Number(state.editForm.sortOrder || 0),
            status: state.editForm.status,
            difficulty: state.editForm.difficulty
        });
        state.editingId = null;
        await loadTopics();
    } catch (error) {
        toast.error(error.message || '专题更新失败');
    } finally {
        state.submitting = false;
    }
};

// ===== 专题文章管理 =====

const startManageArticles = async (topic) => {
    state.managingTopic = topic;
    state.topicArticles = [];
    state.searchKeyword = '';
    state.searchResults = [];
    state.articleActionLoadingId = null;
    await loadTopicArticles();
};

const stopManageArticles = async () => {
    state.managingTopic = null;
    state.topicArticles = [];
    state.searchKeyword = '';
    state.searchResults = [];
    await loadTopics();
};

const loadTopicArticles = async () => {
    if (!state.managingTopic) return;
    state.topicArticlesLoading = true;
    try {
        const result = await getTopicArticlesApi(state.managingTopic.id, { page: 1, pageSize: 100 });
        state.topicArticles = result.items || [];
    } catch (error) {
        state.topicArticles = [];
        toast.error('加载专题文章失败');
    } finally {
        state.topicArticlesLoading = false;
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
            (a) => !state.topicArticles.some((ta) => ta.id === a.id)
        );
    } catch (error) {
        toast.error('搜索文章失败');
        state.searchResults = [];
    } finally {
        state.searching = false;
    }
};

const handleAddArticle = async (articleId) => {
    if (!state.managingTopic) return;
    state.articleActionLoadingId = articleId;
    try {
        await addAdminTopicArticleApi(state.managingTopic.id, { articleId, sortOrder: 0 });
        await loadTopicArticles();
        state.searchResults = state.searchResults.filter((a) => a.id !== articleId);
    } catch (error) {
        toast.error(error.message || '添加失败');
    } finally {
        state.articleActionLoadingId = null;
    }
};

const handleRemoveArticle = async (articleId) => {
    if (!state.managingTopic) return;
    state.articleActionLoadingId = articleId;
    try {
        await removeAdminTopicArticleApi(state.managingTopic.id, articleId);
        state.topicArticles = state.topicArticles.filter((a) => a.id !== articleId);
    } catch (error) {
        toast.error(error.message || '移除失败');
    } finally {
        state.articleActionLoadingId = null;
    }
};

const removeTopic = async (topic) => {
    openConfirmDialog({
        eyebrow: '专题治理确认',
        title: '删除专题',
        message: `确定删除专题「${topic.title}」吗？删除后该专题将不再对外展示。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            state.submitting = true;
            try {
                await deleteAdminTopicApi(topic.id);
                await loadTopics();
            } catch (error) {
                toast.error(error.message || '专题删除失败');
            } finally {
                state.submitting = false;
            }
        }
    });
};

useAdminRefresh(loadTopics);

watch(
    () => [route.query.page, route.query.keyword],
    () => {
        applyRouteState();
        loadTopics();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel admin-resource-page">
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar admin-create-toolbar" @submit.prevent="submitTopic">
                <label>
                    <span>专题标题</span>
                    <input v-model.trim="form.title" type="text" placeholder="专题标题" required>
                </label>
                <label>
                    <span>简介</span>
                    <input v-model.trim="form.summary" type="text" placeholder="专题简介">
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
                                @change="uploadTopicCover($event, form, 'createCoverUploading')"
                            >
                            {{ state.createCoverUploading ? '上传中' : '本地上传' }}
                        </label>
                    </div>
                    <img v-if="form.coverUrl" class="cover-preview" :src="form.coverUrl" alt="专题封面预览" @click="openPreview(form.coverUrl)">
                </div>
                <label>
                    <span>难度</span>
                    <select v-model="form.difficulty">
                        <option value="BEGINNER">入门</option>
                        <option value="INTERMEDIATE">进阶</option>
                        <option value="ADVANCED">深入</option>
                    </select>
                </label>
                <label>
                    <span>排序值</span>
                    <input v-model.number="form.sortOrder" type="number" placeholder="排序值">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit" :disabled="state.submitting">
                        新增专题
                    </button>
                </div>
            </form>

            <div class="admin-filter-toolbar">
                <label>
                    <span>关键字搜索</span>
                    <input v-model.trim="state.keyword" type="text" placeholder="标题/简介关键字"
                           @keyup.enter="changeFilter">
                </label>
                <div class="admin-filter-actions">
                    <button type="button" @click="changeFilter">搜索</button>
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
                <div class="admin-table-wrap" data-testid="admin-topics-table">
                    <table class="admin-table">
                        <colgroup>
                            <col style="width: 7%">
                            <col style="width: 12%">
                            <col style="width: 14%">
                            <col style="width: 12%">
                            <col style="width: 8%">
                            <col style="width: 6%">
                            <col style="width: 7%">
                            <col style="width: 8%">
                            <col style="width: 26%">
                        </colgroup>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>标题</th>
                                <th>简介</th>
                                <th>封面</th>
                                <th>难度</th>
                                <th>排序</th>
                                <th>文章数</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="topic in state.items" :key="topic.id">
                                <td>#{{ topic.id }}</td>

                                <td v-if="state.editingId === topic.id" class="admin-edit-cell">
                                    <input v-model.trim="state.editForm.title" class="admin-edit-input" type="text">
                                </td>
                                <td v-else><span class="admin-cell-text">{{ topic.title }}</span></td>

                                <td v-if="state.editingId === topic.id" class="admin-edit-cell admin-edit-cell-wide">
                                    <input v-model.trim="state.editForm.summary" class="admin-edit-input" type="text">
                                </td>
                                <td v-else class="summary-cell">{{ topic.summary || '-' }}</td>

                                <td v-if="state.editingId === topic.id" class="admin-edit-cell cover-edit-cell">
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
                                                    uploadTopicCover($event, state.editForm, 'editCoverUploading')
                                                "
                                            >
                                            {{ state.editCoverUploading ? '上传中' : '上传' }}
                                        </label>
                                        <img
                                            v-if="state.editForm.coverUrl"
                                            class="cover-thumb"
                                            :src="state.editForm.coverUrl"
                                            alt="专题封面预览"
                                            @click="openPreview(state.editForm.coverUrl)"
                                        >
                                    </div>
                                </td>
                                <td v-else>
                                    <img v-if="topic.coverUrl" class="cover-thumb" :src="topic.coverUrl" alt="专题封面" @click="openPreview(topic.coverUrl)">
                                    <span v-else class="admin-subtext">-</span>
                                </td>

                                <td v-if="state.editingId === topic.id" class="admin-edit-cell admin-edit-cell-narrow">
                                    <select v-model="state.editForm.difficulty" class="admin-edit-select">
                                        <option value="BEGINNER">入门</option>
                                        <option value="INTERMEDIATE">进阶</option>
                                        <option value="ADVANCED">深入</option>
                                    </select>
                                </td>
                                <td v-else>{{ getDifficultyLabel(topic.difficulty) }}</td>

                                <td v-if="state.editingId === topic.id" class="admin-edit-cell admin-edit-cell-narrow">
                                    <input v-model.number="state.editForm.sortOrder" class="admin-edit-input" type="number">
                                </td>
                                <td v-else>{{ topic.sortOrder ?? 0 }}</td>

                                <td>{{ topic.articleCount }}</td>

                                <td v-if="state.editingId === topic.id">
                                    <select v-model="state.editForm.status" class="admin-edit-select">
                                        <option value="PUBLISHED">已发布</option>
                                        <option value="DRAFT">草稿</option>
                                    </select>
                                </td>
                                <td v-else>
                                    <span class="status-pill" :class="{ danger: topic.status !== 'PUBLISHED' }">
                                        {{ topic.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                                    </span>
                                </td>

                                <td class="table-actions">
                                    <template v-if="state.editingId === topic.id">
                                        <button type="button" class="admin-edit-btn primary" :disabled="state.submitting"
                                                @click="saveEdit(topic.id)">保存</button>
                                        <button type="button" class="admin-edit-btn secondary" :disabled="state.submitting"
                                                @click="cancelEdit">取消</button>
                                    </template>
                                    <template v-else>
                                        <button type="button" :disabled="state.submitting"
                                                @click="startEdit(topic)">编辑</button>
                                        <button type="button" :disabled="state.submitting || state.managingTopic"
                                                @click="startManageArticles(topic)">管理文章</button>
                                        <button type="button" class="danger-link" :disabled="state.submitting"
                                                @click="removeTopic(topic)">删除</button>
                                    </template>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有专题</p>
            </template>
        </div>

        <AdminPagination :state="state" label="专题分页" @page-change="changePage" />
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

        <!-- 专题文章管理弹窗 -->
        <Teleport to="body">
            <div v-if="state.managingTopic" class="modal-overlay" @click.self="stopManageArticles">
                <div class="modal-container">
                    <div class="modal-header">
                        <h2>管理专题文章</h2>
                        <p class="modal-subtitle">专题：{{ state.managingTopic.title }}</p>
                        <button type="button" class="modal-close-btn" @click="stopManageArticles">&times;</button>
                    </div>

                    <div class="modal-search">
                        <input v-model.trim="state.searchKeyword" type="text"
                               placeholder="搜索已发布文章标题，输入后按回车搜索"
                               @keyup.enter="handleSearchArticles">
                        <button type="button" :disabled="state.searching" @click="handleSearchArticles">
                            搜索
                        </button>
                    </div>

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

                    <div class="modal-section" style="flex:1; min-height:0; display:flex; flex-direction:column;">
                        <p class="modal-section-title">已有文章（{{ state.topicArticles.length }} 篇）</p>
                        <div v-if="state.topicArticlesLoading" class="modal-hint">加载中...</div>
                        <div v-else-if="state.topicArticles.length" class="modal-article-list" style="flex:1; overflow-y:auto;">
                            <div v-for="article in state.topicArticles" :key="'c-' + article.id" class="modal-article-row">
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
                        <p v-else class="modal-hint">该专题暂无文章</p>
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

<style scoped>
.admin-table {
    table-layout: fixed;
}

.admin-filter-toolbar select {
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6' fill='none'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%237f8fa4' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 10px center;
    padding-right: 28px;
    cursor: pointer;
}

.cover-field {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 260px;
}

.cover-field > span {
    font-size: 13px;
    font-weight: 600;
    color: var(--muted);
}

.cover-input-row,
.cover-cell-actions {
    display: flex;
    align-items: center;
    gap: 8px;
}

.cover-input-row input {
    flex: 1;
    min-width: 180px;
}

.cover-upload-button {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 38px;
    padding: 0 12px;
    border: 1px solid var(--brand);
    border-radius: var(--radius-md);
    background: var(--surface);
    color: var(--brand);
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
    cursor: pointer;
}

.cover-upload-button:hover:not(.disabled) {
    background: rgba(37, 99, 235, 0.08);
}

.cover-upload-button.disabled {
    cursor: not-allowed;
    opacity: 0.55;
}

.cover-upload-button input {
    position: absolute;
    inset: 0;
    opacity: 0;
    cursor: pointer;
}

.cover-upload-button.disabled input {
    cursor: not-allowed;
}

.cover-upload-button.compact {
    min-height: 30px;
    padding: 0 10px;
    border-radius: var(--radius-sm);
}

.cover-preview {
    width: 96px;
    height: 54px;
    object-fit: cover;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface-soft);
    cursor: pointer;
}

.cover-thumb {
    width: 72px;
    height: 42px;
    object-fit: cover;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface-soft);
    cursor: pointer;
}

.cover-thumb:hover,
.cover-preview:hover {
    border-color: var(--brand);
}

.image-preview-overlay {
    position: fixed;
    inset: 0;
    z-index: 2000;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.65);
    backdrop-filter: blur(4px);
    padding: 24px;
}

.image-preview-close {
    position: fixed;
    top: 16px;
    right: 16px;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: #fff;
    background: rgba(255, 255, 255, 0.15);
    border: 1px solid rgba(255, 255, 255, 0.25);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: background 0.15s;
}

.image-preview-close:hover {
    background: rgba(255, 255, 255, 0.3);
}

.image-preview-full {
    max-width: 90vw;
    max-height: 90vh;
    object-fit: contain;
    border-radius: var(--radius-md);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35);
}

.cover-edit-cell {
    min-width: 180px;
}

.cover-edit-cell .admin-edit-input {
    margin-bottom: 8px;
}

.summary-cell {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.admin-edit-cell {
    min-width: 160px;
}

.admin-edit-cell-wide {
    min-width: 220px;
}

.admin-edit-cell-narrow {
    min-width: 96px;
}

.admin-edit-input,
.admin-edit-select {
    width: 100%;
    min-height: 38px;
    padding: 0 12px;
    color: var(--text);
    font: inherit;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    outline: 0;
    box-sizing: border-box;
}

.admin-edit-input:focus,
.admin-edit-select:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.admin-edit-btn {
    min-width: 72px;
}

.admin-edit-btn.primary {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.admin-edit-btn.primary:hover:not(:disabled) {
    color: #ffffff;
    background: var(--brand-strong);
    border-color: var(--brand-strong);
}

.admin-edit-btn.primary:focus-visible {
    color: #ffffff;
}

.admin-edit-btn.secondary {
    color: var(--text);
    background: var(--surface);
    border-color: var(--line);
}

.admin-edit-btn.secondary:hover:not(:disabled) {
    color: var(--brand);
    border-color: var(--brand);
}

/* 弹窗样式 */
.modal-overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.45);
    backdrop-filter: blur(2px);
    padding: 24px;
}

.modal-container {
    display: flex;
    flex-direction: column;
    width: min(680px, 100%);
    height: min(600px, 85vh);
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
    padding: 24px;
    gap: 16px;
}

.modal-header {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    padding-right: 32px;
    position: relative;
}

.modal-header h2 {
    margin: 0;
    font-size: 18px;
    color: var(--text-strong);
}

.modal-subtitle {
    margin: 0;
    font-size: 13px;
    color: var(--muted);
}

.modal-close-btn {
    position: absolute;
    top: 0;
    right: 0;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    font-size: 22px;
    color: var(--muted);
    cursor: pointer;
    border-radius: 6px;
    line-height: 1;
}

.modal-close-btn:hover {
    background: var(--surface-soft);
    color: var(--text);
}

.modal-search {
    display: flex;
    gap: 10px;
}

.modal-search input {
    flex: 1;
    min-height: 40px;
    padding: 0 14px;
    border: 1px solid var(--line);
    border-radius: 8px;
    font: inherit;
    font-size: 14px;
    outline: 0;
    background: var(--surface-soft);
}

.modal-search input:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.modal-search button {
    display: inline-flex;
    align-items: center;
    min-height: 40px;
    padding: 0 18px;
    border: 0;
    border-radius: 8px;
    background: var(--brand);
    color: #fff;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    white-space: nowrap;
}


.modal-search button:hover:not(:disabled) {
    filter: brightness(1.05);
}

.modal-section-title {
    margin: 0 0 8px;
    font-size: 13px;
    font-weight: 600;
    color: var(--muted);
    text-transform: uppercase;
    letter-spacing: 0.04em;
}

.modal-article-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.modal-article-row {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 8px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
}

.modal-article-info {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
}

.modal-article-id {
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
}

.modal-article-title {
    color: var(--text);
    font-size: 14px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.modal-article-category {
    font-size: 12px;
    color: var(--muted);
    background: var(--surface);
    padding: 2px 8px;
    border-radius: 4px;
    white-space: nowrap;
}

.modal-article-row button {
    min-height: 32px;
    padding: 0 14px;
    border: 1px solid var(--line);
    border-radius: 6px;
    background: #fff;
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    white-space: nowrap;
}

.modal-article-row button:hover:not(:disabled) {
    background: var(--brand);
    color: #fff;
    border-color: var(--brand);
}

.modal-article-row button.danger-link {
    color: var(--danger, #dc2626);
    border-color: var(--danger, #dc2626);
    background: #fff;
}

.modal-article-row button.danger-link:hover:not(:disabled) {
    background: var(--danger, #dc2626);
    color: #fff;
}


.modal-hint {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
    text-align: center;
    padding: 16px 0;
}

.modal-section {
    border-top: 1px solid var(--line);
    padding-top: 14px;
}
</style>
