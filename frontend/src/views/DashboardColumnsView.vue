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
const formError = ref('');

// 抽屉：管理专栏文章
const drawerColumnId = ref(null);
const drawerColumn = ref(null);
const drawerArticles = ref([]);
const drawerLoading = ref(false);
const drawerError = ref('');
const drawerArticleActionId = ref(null);

// 弹窗：从已有文章里选择加入专栏
const showPickArticle = ref(false);
const myArticles = ref([]);
const myArticlesLoading = ref(false);
const pickArticleFeedback = ref('');

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

const openCreateForm = () => {
    editingColumn.value = null;
    formTitle.value = '';
    formSummary.value = '';
    formCoverUrl.value = '';
    formError.value = '';
    showColumnForm.value = true;
};

const openEditForm = (column) => {
    editingColumn.value = column;
    formTitle.value = column.title;
    formSummary.value = column.summary || '';
    formCoverUrl.value = column.coverUrl || '';
    formError.value = '';
    showColumnForm.value = true;
};

const closeColumnForm = () => {
    showColumnForm.value = false;
    editingColumn.value = null;
};

const submitColumnForm = async () => {
    const title = formTitle.value.trim();
    if (!title) {
        formError.value = '专栏标题不能为空';
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
    showPickArticle.value = false;
    pickArticleFeedback.value = '';
};

const removeArticleFromColumn = async (article) => {
    drawerArticleActionId.value = article.id;
    try {
        await removeMyColumnArticleApi(drawerColumnId.value, article.id);
        drawerArticles.value = drawerArticles.value.filter((a) => a.id !== article.id);
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

// ========== 选择文章加入专栏 ==========

const openPickArticle = async () => {
    showPickArticle.value = true;
    pickArticleFeedback.value = '';
    myArticlesLoading.value = true;
    try {
        const result = await getMyArticlesApi({ page: 1, pageSize: 100, status: 'PUBLISHED' });
        const existIds = new Set(drawerArticles.value.map((a) => a.id));
        myArticles.value = (result.items || []).filter((a) => !existIds.has(a.id));
    } catch (e) {
        pickArticleFeedback.value = e.message || '加载文章失败';
    } finally {
        myArticlesLoading.value = false;
    }
};

const addArticleToColumn = async (article) => {
    try {
        await addMyColumnArticleApi(drawerColumnId.value, article.id);
        // 重新拉取专栏文章
        const result = await getColumnArticlesApi(drawerColumnId.value, { page: 1, pageSize: 100 });
        drawerArticles.value = result.items || [];
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
                        <label for="col-cover">封面图 URL</label>
                        <input
                            id="col-cover"
                            v-model="formCoverUrl"
                            type="url"
                            placeholder="https://example.com/cover.jpg（可选）"
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
                    >
                        <div class="drawer-article-info">
                            <span class="drawer-article-title">{{ article.title }}</span>
                            <span class="drawer-article-meta">{{ article.stats?.views || `${article.viewCount} 阅读` }}</span>
                        </div>
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

<style scoped>
.section-heading {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 24px;
    flex-wrap: wrap;
}

.columns-feedback {
    padding: 10px 14px;
    margin-bottom: 16px;
    border-radius: var(--radius-sm);
    font-size: 14px;
    line-height: 1.5;
}

.columns-feedback.success {
    color: #15803d;
    background: rgba(21, 128, 61, 0.08);
    border: 1px solid rgba(21, 128, 61, 0.16);
}

.columns-feedback.error {
    color: #b42318;
    background: rgba(180, 35, 24, 0.08);
    border: 1px solid rgba(180, 35, 24, 0.16);
}

.loading-text {
    margin: 0 0 12px;
    color: var(--muted);
    font-size: 13px;
}

.columns-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 18px;
}

.column-card {
    display: flex;
    flex-direction: column;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    overflow: hidden;
    transition: box-shadow 0.15s, border-color 0.15s;
}

.column-card:hover {
    border-color: var(--line-strong);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.column-card-cover {
    aspect-ratio: 16 / 7;
    overflow: hidden;
    background: var(--surface-soft);
}

.column-card-cover img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.3s;
}

.column-card:hover .column-card-cover img {
    transform: scale(1.03);
}

.column-card-body {
    flex: 1;
    padding: 14px 16px 10px;
    display: grid;
    gap: 6px;
}

.column-card-title {
    margin: 0;
    color: var(--text);
    font-size: 15px;
    font-weight: 700;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.column-card-summary {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.column-card-meta {
    display: flex;
    gap: 12px;
    color: var(--muted);
    font-size: 12px;
}

.column-card-actions {
    display: flex;
    gap: 8px;
    padding: 10px 16px 14px;
    flex-wrap: wrap;
    border-top: 1px solid var(--line);
}

/* ===== 通用 action-link ===== */
.action-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 30px;
    padding: 0 10px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    font-size: 13px;
    font-weight: 500;
    line-height: 1;
    cursor: pointer;
    text-decoration: none;
    transition: color 0.12s, border-color 0.12s, background 0.12s;
    background: var(--surface);
}

.action-link.sm {
    min-height: 26px;
    padding: 0 8px;
    font-size: 12px;
}

.action-link:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}

.action-link-primary {
    color: var(--brand);
    border-color: var(--brand);
}

.action-link-primary:hover:not(:disabled) {
    color: #fff;
    background: var(--brand);
}

.action-link-secondary {
    color: var(--text);
}

.action-link-secondary:hover:not(:disabled) {
    color: var(--brand);
    border-color: var(--brand);
}

.action-link-danger {
    color: var(--accent);
    border-color: var(--accent);
}

.action-link-danger:hover:not(:disabled) {
    color: #fff;
    background: var(--accent);
}

/* ===== 按钮 ===== */
.btn-primary {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 36px;
    padding: 0 16px;
    color: #fff;
    background: var(--brand);
    border: 1px solid var(--brand);
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.12s, border-color 0.12s;
}

.btn-primary:hover:not(:disabled) {
    background: var(--brand-hover);
    border-color: var(--brand-hover);
}

.btn-secondary {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 36px;
    padding: 0 16px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: color 0.12s, border-color 0.12s;
}

.btn-secondary:hover:not(:disabled) {
    color: var(--brand);
    border-color: var(--brand);
}

.required {
    color: var(--accent);
}

/* ===== 弹窗 ===== */
.modal-overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: rgba(0, 0, 0, 0.38);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
}

.modal-box {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius);
    box-shadow: 0 8px 40px rgba(0, 0, 0, 0.16);
    width: 100%;
    max-width: 480px;
    animation: modal-in 0.18s ease;
}

@keyframes modal-in {
    from { opacity: 0; transform: scale(0.96) translateY(8px); }
    to   { opacity: 1; transform: none; }
}

.modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 18px 20px 14px;
    border-bottom: 1px solid var(--line);
}

.modal-header h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--text);
}

.modal-close {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    color: var(--muted);
    background: transparent;
    border: none;
    border-radius: var(--radius-sm);
    font-size: 13px;
    cursor: pointer;
    transition: color 0.12s, background 0.12s;
}

.modal-close:hover {
    color: var(--text);
    background: var(--surface-soft);
}

.column-form {
    padding: 18px 20px 20px;
    display: grid;
    gap: 14px;
}

.form-field {
    display: grid;
    gap: 6px;
}

.form-field label {
    font-size: 13px;
    font-weight: 600;
    color: var(--text);
}

.form-field input,
.form-field textarea {
    padding: 8px 10px;
    font-size: 14px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    outline: none;
    transition: border-color 0.12s;
    resize: vertical;
    font-family: inherit;
}

.form-field input:focus,
.form-field textarea:focus {
    border-color: var(--brand);
}

.form-message.error {
    margin: 0;
    color: #b42318;
    font-size: 13px;
}

.form-actions {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
    padding-top: 4px;
}

/* ===== 抽屉 ===== */
.drawer-overlay {
    position: fixed;
    inset: 0;
    z-index: 900;
    background: rgba(0, 0, 0, 0.32);
    display: flex;
    justify-content: flex-end;
}

.drawer-panel {
    width: 420px;
    max-width: 100vw;
    height: 100%;
    background: var(--surface);
    border-left: 1px solid var(--line);
    box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
    display: flex;
    flex-direction: column;
    animation: drawer-in 0.2s ease;
    overflow: hidden;
}

@keyframes drawer-in {
    from { transform: translateX(100%); }
    to   { transform: translateX(0); }
}

.drawer-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    padding: 18px 20px 14px;
    border-bottom: 1px solid var(--line);
    flex-shrink: 0;
}

.drawer-header h2 {
    margin: 0;
    font-size: 15px;
    font-weight: 700;
    color: var(--text);
    line-height: 1.4;
}

.drawer-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 20px;
    border-bottom: 1px solid var(--line);
    flex-shrink: 0;
}

.drawer-count {
    color: var(--muted);
    font-size: 13px;
}

.drawer-article-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px 0;
}

.drawer-article-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 10px 20px;
    border-bottom: 1px solid var(--line);
    transition: background 0.12s;
}

.drawer-article-item:last-child {
    border-bottom: 0;
}

.drawer-article-item:hover {
    background: var(--surface-soft);
}

.drawer-article-info {
    flex: 1;
    min-width: 0;
    display: grid;
    gap: 2px;
}

.drawer-article-title {
    color: var(--text);
    font-size: 14px;
    font-weight: 500;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.drawer-article-meta {
    color: var(--muted);
    font-size: 12px;
}

.muted-text {
    padding: 12px 20px;
    color: var(--muted);
    font-size: 13px;
}

/* ===== 选择文章面板 ===== */
.pick-article-panel {
    border-bottom: 1px solid var(--line);
    background: var(--surface-soft);
    flex-shrink: 0;
}

.pick-article-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 20px 8px;
    font-size: 13px;
    font-weight: 600;
    color: var(--text);
}

.pick-article-feedback {
    padding: 0 20px 8px;
    color: var(--brand);
    font-size: 13px;
}

.pick-article-list {
    list-style: none;
    margin: 0;
    padding: 0 0 8px;
    max-height: 220px;
    overflow-y: auto;
}

.pick-article-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 8px 20px;
    transition: background 0.12s;
}

.pick-article-item:hover {
    background: var(--line);
}

.pick-article-title {
    flex: 1;
    min-width: 0;
    font-size: 13px;
    color: var(--text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

@media (max-width: 640px) {
    .drawer-panel {
        width: 100vw;
    }

    .columns-grid {
        grid-template-columns: 1fr;
    }
}
</style>

