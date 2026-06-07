<script setup>
import {computed, onMounted, ref} from 'vue';
import {useHead} from '@unhead/vue';
import SiteHeader from '@/components/SiteHeader.vue';
import CreatorSidebar from '@/components/CreatorSidebar.vue';
import EmptyState from '@/components/EmptyState.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import {useConfirmDialog} from '@/composables/useConfirmDialog';
import {
  addMyColumnArticleApi,
  createMyColumnApi,
  deleteMyColumnApi,
  getColumnArticlesApi,
  getMyColumnsApi,
  removeMyColumnArticleApi,
  updateMyColumnApi
} from '@/api/columns';
import {getMyArticlesApi} from '@/api/articles';
import {uploadImage} from '@/api/uploads';

useHead({ title: '我的专栏 - DevNotes' });

const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const columns = ref([]);
const loading = ref(false);
const errorMsg = ref('');
const feedback = ref('');
const feedbackType = ref('success');

// 弹窗：创建/编辑专栏
const showColumnForm = ref(false);
const editingColumn = ref(null); // null = 新建，否则 = 编辑
const formTitle = ref('');
const formSummary = ref('');
const formCoverUrl = ref('');
const formSubmitting = ref(false);
const formCoverUploading = ref(false);
const formError = ref('');

// 抽屉：管理专栏文章
const drawerColumnId = ref(null);
const drawerColumn = ref(null);
const drawerArticles = ref([]);
const drawerLoading = ref(false);
const drawerError = ref('');
const drawerArticleActionId = ref(null);
const drawerSortDrafts = ref({});
const drawerDraggingArticleId = ref(null);

// 弹窗：从已有文章里选择加入专栏
const showPickArticle = ref(false);
const myArticles = ref([]);
const myArticlesLoading = ref(false);
const pickArticleFeedback = ref('');
const pickSortDrafts = ref({});

const showFeedback = (msg, type = 'success') => {
    feedback.value = msg;
    feedbackType.value = type;
    setTimeout(() => { feedback.value = ''; }, 3000);
};

const fetchColumns = async () => {
    loading.value = true;
    errorMsg.value = '';
    try {
        columns.value = await getMyColumnsApi();
    } catch (e) {
        errorMsg.value = e.message || '加载专栏失败';
    } finally {
        loading.value = false;
    }
};

const getNextSortOrder = () => {
    if (!drawerArticles.value.length) {
        return 10;
    }
    const maxSort = Math.max(...drawerArticles.value.map((article, index) => (
        Number(article.relationSortOrder ?? (index + 1) * 10)
    )));
    return maxSort + 10;
};

const syncDrawerSortDrafts = () => {
    drawerSortDrafts.value = drawerArticles.value.reduce((drafts, article, index) => {
        drafts[article.id] = Number(article.relationSortOrder ?? (index + 1) * 10);
        return drafts;
    }, {});
};

const replaceDrawerSortDraft = (articleId, sortOrder) => {
    drawerSortDrafts.value = {
        ...drawerSortDrafts.value,
        [articleId]: sortOrder
    };
};

const applyDrawerArticleSortLocally = (articleId, sortOrder) => {
    drawerArticles.value = drawerArticles.value.map((item) => (
        item.id === articleId ? { ...item, relationSortOrder: sortOrder } : item
    ));
    replaceDrawerSortDraft(articleId, sortOrder);
};

const reorderDrawerArticles = (fromIndex, toIndex) => {
    if (fromIndex < 0 || toIndex < 0 || fromIndex === toIndex) {
        return false;
    }
    const nextArticles = [...drawerArticles.value];
    const [moved] = nextArticles.splice(fromIndex, 1);
    nextArticles.splice(toIndex, 0, moved);
    drawerArticles.value = nextArticles.map((article, index) => ({
        ...article,
        relationSortOrder: (index + 1) * 10
    }));
    syncDrawerSortDrafts();
    return true;
};

const openCreateForm = () => {
    editingColumn.value = null;
    formTitle.value = '';
    formSummary.value = '';
    formCoverUrl.value = '';
    formCoverUploading.value = false;
    formError.value = '';
    showColumnForm.value = true;
};

const openEditForm = (column) => {
    editingColumn.value = column;
    formTitle.value = column.title;
    formSummary.value = column.summary || '';
    formCoverUrl.value = column.coverUrl || '';
    formCoverUploading.value = false;
    formError.value = '';
    showColumnForm.value = true;
};

const closeColumnForm = () => {
    showColumnForm.value = false;
    editingColumn.value = null;
};

const uploadColumnCover = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
        formError.value = '请选择图片文件';
        event.target.value = '';
        return;
    }
    formCoverUploading.value = true;
    formError.value = '';
    try {
        const result = await uploadImage(file, 'cover');
        formCoverUrl.value = result.mediumUrl || result.url || result.originalUrl || '';
        showFeedback('封面上传成功');
    } catch (e) {
        formError.value = e.message || '封面上传失败';
    } finally {
        formCoverUploading.value = false;
        event.target.value = '';
    }
};

const submitColumnForm = async () => {
    const title = formTitle.value.trim();
    if (!title) {
        formError.value = '专栏标题不能为空';
        return;
    }
    if (formCoverUploading.value) {
        formError.value = '封面图片还在上传中';
        return;
    }
    formSubmitting.value = true;
    formError.value = '';
    try {
        if (editingColumn.value) {
            await updateMyColumnApi(editingColumn.value.id, {
                title,
                summary: formSummary.value.trim(),
                coverUrl: formCoverUrl.value.trim()
            });
            showFeedback('专栏已更新');
        } else {
            await createMyColumnApi({
                title,
                summary: formSummary.value.trim(),
                coverUrl: formCoverUrl.value.trim()
            });
            showFeedback('专栏已创建');
        }
        closeColumnForm();
        await fetchColumns();
    } catch (e) {
        formError.value = e.message || '操作失败，请稍后重试';
    } finally {
        formSubmitting.value = false;
    }
};

const confirmDeleteColumn = (column) => {
    openConfirmDialog({
        eyebrow: '专栏操作',
        title: '删除专栏',
        message: `确定删除专栏《${column.title}》吗？删除后专栏内的文章归属将被解除，文章本身不会被删除。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            try {
                await deleteMyColumnApi(column.id);
                showFeedback('专栏已删除');
                await fetchColumns();
            } catch (e) {
                showFeedback(e.message || '删除失败', 'error');
            }
        }
    });
};

// ========== 专栏文章管理抽屉 ==========

const openDrawer = async (column) => {
    drawerColumnId.value = column.id;
    drawerColumn.value = column;
    drawerArticles.value = [];
    drawerError.value = '';
    drawerLoading.value = true;
    try {
        const result = await getColumnArticlesApi(column.id, { page: 1, pageSize: 100 });
        drawerArticles.value = result.items || [];
        syncDrawerSortDrafts();
    } catch (e) {
        drawerError.value = e.message || '加载文章失败';
    } finally {
        drawerLoading.value = false;
    }
};

const closeDrawer = () => {
    drawerColumnId.value = null;
    drawerColumn.value = null;
    drawerArticles.value = [];
    drawerSortDrafts.value = {};
    showPickArticle.value = false;
    pickArticleFeedback.value = '';
    pickSortDrafts.value = {};
    drawerDraggingArticleId.value = null;
};

const removeArticleFromColumn = async (article) => {
    drawerArticleActionId.value = article.id;
    try {
        await removeMyColumnArticleApi(drawerColumnId.value, article.id);
        drawerArticles.value = drawerArticles.value.filter((a) => a.id !== article.id);
        syncDrawerSortDrafts();
        // 更新本地 articleCount
        if (drawerColumn.value) {
            drawerColumn.value = { ...drawerColumn.value, articleCount: Math.max(0, drawerColumn.value.articleCount - 1) };
            columns.value = columns.value.map((c) =>
                c.id === drawerColumn.value.id
                    ? { ...c, articleCount: drawerColumn.value.articleCount }
                    : c
            );
        }
    } catch (e) {
        drawerError.value = e.message || '移除文章失败';
    } finally {
        drawerArticleActionId.value = null;
    }
};

const saveArticleSort = async (article) => {
    const sortOrder = Number(drawerSortDrafts.value[article.id] || 0);
    drawerArticleActionId.value = article.id;
    drawerError.value = '';
    try {
        await addMyColumnArticleApi(drawerColumnId.value, article.id, sortOrder);
        applyDrawerArticleSortLocally(article.id, sortOrder);
        showFeedback('文章顺序已更新');
    } catch (e) {
        drawerError.value = e.message || '保存排序失败';
    } finally {
        drawerArticleActionId.value = null;
    }
};

const startDrawerArticleDrag = (article, event) => {
    drawerDraggingArticleId.value = article.id;
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', String(article.id));
};

const dropDrawerArticle = async (targetArticle) => {
    const sourceArticleId = drawerDraggingArticleId.value;
    drawerDraggingArticleId.value = null;
    if (!sourceArticleId || sourceArticleId === targetArticle.id || drawerArticleActionId.value) {
        return;
    }
    const fromIndex = drawerArticles.value.findIndex((article) => article.id === sourceArticleId);
    const toIndex = drawerArticles.value.findIndex((article) => article.id === targetArticle.id);
    if (!reorderDrawerArticles(fromIndex, toIndex)) {
        return;
    }
    drawerArticleActionId.value = sourceArticleId;
    drawerError.value = '';
    try {
        await Promise.all(drawerArticles.value.map((article) => (
            addMyColumnArticleApi(drawerColumnId.value, article.id, Number(drawerSortDrafts.value[article.id] || 0))
        )));
        showFeedback('拖拽排序已保存');
    } catch (e) {
        drawerError.value = e.message || '拖拽排序保存失败';
        await openDrawer(drawerColumn.value);
    } finally {
        drawerArticleActionId.value = null;
    }
};

// ========== 选择文章加入专栏 ==========

const openPickArticle = async () => {
    showPickArticle.value = true;
    pickArticleFeedback.value = '';
    myArticlesLoading.value = true;
    try {
        const result = await getMyArticlesApi({ page: 1, pageSize: 100, status: 'PUBLISHED' });
        const existIds = new Set(drawerArticles.value.map((a) => a.id));
        myArticles.value = (result.items || []).filter((a) => !existIds.has(a.id));
        const nextSortOrder = getNextSortOrder();
        pickSortDrafts.value = myArticles.value.reduce((drafts, article, index) => {
            drafts[article.id] = nextSortOrder + index * 10;
            return drafts;
        }, {});
    } catch (e) {
        pickArticleFeedback.value = e.message || '加载文章失败';
    } finally {
        myArticlesLoading.value = false;
    }
};

const addArticleToColumn = async (article) => {
    try {
        await addMyColumnArticleApi(drawerColumnId.value, article.id, Number(pickSortDrafts.value[article.id] || 0));
        // 重新拉取专栏文章
        const result = await getColumnArticlesApi(drawerColumnId.value, { page: 1, pageSize: 100 });
        drawerArticles.value = result.items || [];
        syncDrawerSortDrafts();
        // 更新本地 articleCount
        if (drawerColumn.value) {
            drawerColumn.value = { ...drawerColumn.value, articleCount: drawerArticles.value.length };
            columns.value = columns.value.map((c) =>
                c.id === drawerColumn.value.id
                    ? { ...c, articleCount: drawerColumn.value.articleCount }
                    : c
            );
        }
        // 移出候选列表
        myArticles.value = myArticles.value.filter((a) => a.id !== article.id);
        const nextDrafts = { ...pickSortDrafts.value };
        delete nextDrafts[article.id];
        pickSortDrafts.value = nextDrafts;
        pickArticleFeedback.value = `《${article.title}》已加入专栏`;
    } catch (e) {
        pickArticleFeedback.value = e.message || '添加失败';
    }
};

const formModalTitle = computed(() => (editingColumn.value ? '编辑专栏' : '创建专栏'));

onMounted(fetchColumns);
</script>

<template>
    <SiteHeader />
    <main class="page-shell dashboard-layout" data-testid="dashboard-columns-page">
        <CreatorSidebar />

        <section class="dashboard-main">
            <div class="section-heading">
                <div>
                    <p class="eyebrow">创作者工作台</p>
                    <h1>我的专栏</h1>
                </div>
                <button
                    type="button"
                    class="btn-primary"
                    data-testid="create-column-btn"
                    @click="openCreateForm"
                >
                    + 创建专栏
                </button>
            </div>

            <!-- 反馈提示 -->
            <p v-if="feedback" :class="['columns-feedback', feedbackType]" role="status">{{ feedback }}</p>
            <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>

            <!-- 加载中 -->
            <p v-if="loading" class="loading-text">加载中...</p>

            <!-- 专栏列表 -->
            <div v-else-if="columns.length" class="columns-grid" data-testid="columns-grid">
                <article
                    v-for="col in columns"
                    :key="col.id"
                    class="column-card"
                    data-testid="column-card"
                >
                    <div class="column-card-cover">
                        <img
                            :src="col.coverUrl"
                            :alt="`${col.title} 封面`"
                            loading="lazy"
                            decoding="async"
                        >
                    </div>
                    <div class="column-card-body">
                        <h2 class="column-card-title">{{ col.title }}</h2>
                        <p v-if="col.summary" class="column-card-summary">{{ col.summary }}</p>
                        <div class="column-card-meta">
                            <span>{{ col.articleCount }} 篇文章</span>
                            <span>{{ col.subscriberCount }} 订阅者</span>
                        </div>
                    </div>
                    <div class="column-card-actions">
                        <button
                            type="button"
                            class="action-link action-link-secondary"
                            @click="openDrawer(col)"
                        >
                            管理文章
                        </button>
                        <button
                            type="button"
                            class="action-link action-link-secondary"
                            @click="openEditForm(col)"
                        >
                            编辑
                        </button>
                        <button
                            type="button"
                            class="action-link action-link-danger"
                            @click="confirmDeleteColumn(col)"
                        >
                            删除
                        </button>
                    </div>
                </article>
            </div>

            <!-- 空状态 -->
            <EmptyState
                v-else-if="!loading && !errorMsg"
                eyebrow="我的专栏"
                title="还没有专栏"
                description="创建专栏，把相关文章归档在一起，方便读者系统阅读。每位创作者最多可创建 5 个专栏。"
                compact
            >
                <template #action>
                    <button type="button" class="btn-primary" @click="openCreateForm">立即创建</button>
                </template>
            </EmptyState>
        </section>
    </main>

    <!-- 创建/编辑专栏弹窗 -->
    <Teleport to="body">
        <div v-if="showColumnForm" class="modal-overlay" role="dialog" aria-modal="true" @click.self="closeColumnForm">
            <div class="modal-box column-form-modal">
                <header class="modal-header">
                    <h2>{{ formModalTitle }}</h2>
                    <button type="button" class="modal-close" aria-label="关闭" @click="closeColumnForm">✕</button>
                </header>
                <form class="column-form" @submit.prevent="submitColumnForm">
                    <div class="form-field">
                        <label for="col-title">专栏标题 <span class="required">*</span></label>
                        <input
                            id="col-title"
                            v-model="formTitle"
                            type="text"
                            placeholder="给专栏起个名字"
                            maxlength="100"
                            autofocus
                        >
                    </div>
                    <div class="form-field">
                        <label for="col-summary">专栏简介</label>
                        <textarea
                            id="col-summary"
                            v-model="formSummary"
                            placeholder="简单介绍一下这个专栏的内容方向（可选）"
                            maxlength="300"
                            rows="3"
                        ></textarea>
                    </div>
                    <div class="form-field">
                        <label for="col-cover">封面图</label>
                        <div class="cover-input-row">
                            <input
                                id="col-cover"
                                v-model="formCoverUrl"
                                type="text"
                                placeholder="封面图片 URL 或上传本地图片（可选）"
                            >
                            <label
                                class="cover-upload-button"
                                :class="{disabled: formSubmitting || formCoverUploading}"
                            >
                                <input
                                    type="file"
                                    accept="image/*"
                                    :disabled="formSubmitting || formCoverUploading"
                                    @change="uploadColumnCover"
                                >
                                {{ formCoverUploading ? '上传中' : '本地上传' }}
                            </label>
                        </div>
                        <img
                            v-if="formCoverUrl"
                            class="cover-preview"
                            :src="formCoverUrl"
                            alt="专栏封面预览"
                        >
                    </div>
                    <p v-if="formError" class="form-message error">{{ formError }}</p>
                    <div class="form-actions">
                        <button type="button" class="btn-secondary" :disabled="formSubmitting" @click="closeColumnForm">取消</button>
                        <button type="submit" class="btn-primary" :disabled="formSubmitting">
                            {{ formSubmitting ? '提交中...' : (editingColumn ? '保存修改' : '创建专栏') }}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </Teleport>

    <!-- 专栏文章管理抽屉 -->
    <Teleport to="body">
        <div v-if="drawerColumnId" class="drawer-overlay" @click.self="closeDrawer">
            <aside class="drawer-panel column-articles-drawer">
                <header class="drawer-header">
                    <div>
                        <p class="eyebrow">专栏管理</p>
                        <h2>{{ drawerColumn?.title }}</h2>
                    </div>
                    <button type="button" class="modal-close" aria-label="关闭抽屉" @click="closeDrawer">✕</button>
                </header>

                <div class="drawer-actions">
                    <button type="button" class="btn-primary" @click="openPickArticle">
                        + 添加文章
                    </button>
                    <span class="drawer-count">{{ drawerArticles.length }} 篇文章</span>
                </div>

                <p v-if="drawerError" class="error-text">{{ drawerError }}</p>
                <p v-if="drawerLoading" class="loading-text">加载中...</p>

                <!-- 选择文章面板 -->
                <div v-if="showPickArticle" class="pick-article-panel">
                    <header class="pick-article-header">
                        <span>选择文章加入专栏</span>
                        <button type="button" class="modal-close sm" @click="showPickArticle = false">✕</button>
                    </header>
                    <p v-if="pickArticleFeedback" class="pick-article-feedback">{{ pickArticleFeedback }}</p>
                    <p v-if="myArticlesLoading" class="loading-text">加载文章...</p>
                    <p v-else-if="!myArticles.length" class="muted-text">暂无可添加的已发布文章</p>
                    <ul v-else class="pick-article-list">
                        <li v-for="article in myArticles" :key="article.id" class="pick-article-item">
                            <span class="pick-article-title">{{ article.title }}</span>
                            <label class="article-sort-field sm">
                                <span>排序</span>
                                <input v-model.number="pickSortDrafts[article.id]" type="number">
                            </label>
                            <button
                                type="button"
                                class="action-link action-link-primary sm"
                                @click="addArticleToColumn(article)"
                            >
                                添加
                            </button>
                        </li>
                    </ul>
                </div>

                <!-- 已加入文章列表 -->
                <div v-if="!drawerLoading" class="drawer-article-list">
                    <p v-if="!drawerArticles.length && !showPickArticle" class="muted-text">
                        还没有文章，点击"添加文章"将已发布的文章归入本专栏。
                    </p>
                    <div
                        v-for="article in drawerArticles"
                        :key="article.id"
                        class="drawer-article-item"
                        :class="{ dragging: drawerDraggingArticleId === article.id }"
                        draggable="true"
                        @dragstart="startDrawerArticleDrag(article, $event)"
                        @dragover.prevent
                        @drop.prevent="dropDrawerArticle(article)"
                        @dragend="drawerDraggingArticleId = null"
                    >
                        <button type="button" class="drag-handle" aria-label="拖拽调整文章顺序">⋮⋮</button>
                        <div class="drawer-article-info">
                            <span class="drawer-article-title">{{ article.title }}</span>
                            <span class="drawer-article-meta">{{ article.stats?.views || `${article.viewCount} 阅读` }}</span>
                        </div>
                        <label class="article-sort-field">
                            <span>排序</span>
                            <input v-model.number="drawerSortDrafts[article.id]" type="number">
                        </label>
                        <button
                            type="button"
                            class="action-link action-link-secondary sm"
                            :disabled="drawerArticleActionId === article.id"
                            @click="saveArticleSort(article)"
                        >
                            保存排序
                        </button>
                        <button
                            type="button"
                            class="action-link action-link-danger sm"
                            :disabled="drawerArticleActionId === article.id"
                            @click="removeArticleFromColumn(article)"
                        >
                            {{ drawerArticleActionId === article.id ? '处理中' : '移除' }}
                        </button>
                    </div>
                </div>
            </aside>
        </div>
    </Teleport>

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
</template>

<style scoped src="@/styles/views/DashboardColumnsView.part-1.css"></style>
<style scoped src="@/styles/views/DashboardColumnsView.part-2.css"></style>
