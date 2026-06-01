<script setup>
import {onMounted, reactive, ref, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
  deleteAdminArticleApi,
  exportAdminArticlesApi,
  featureAdminArticleApi,
  getAdminArticlesApi,
  getAdminStatsApi,
  getCategoriesApi,
  unfeatureAdminArticleApi,
  updateAdminArticleStatusApi
} from '@/api/admin';
import {getArticleApi} from '@/api/articles';
import {
  createPagedState,
  formatAdminDateTime,
  readPositiveInt,
  readQueryText,
  resolveAdminOverflowPage,
  syncAdminQuery,
  useAdminRefresh
} from '@/views/admin/adminShared';
import {useConfirmDialog} from '@/composables/useConfirmDialog';
import {useToast} from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const previewId = ref(null);
const previewArticle = ref(null);
const previewLoading = ref(false);

const openPreview = async (article) => {
    previewId.value = article.id;
    previewArticle.value = null;
    previewLoading.value = true;
    try {
        previewArticle.value = await getArticleApi(article.id);
    } catch {
        previewArticle.value = null;
    } finally {
        previewLoading.value = false;
    }
};

const closePreview = () => {
    previewId.value = null;
    previewArticle.value = null;
};

const state = reactive({
    ...createPagedState(),
    status: '',
    keyword: '',
    category: '',
    categoryOptions: [],
    stats: null,
    actionLoadingId: null
});

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.status = readQueryText(route, 'status');
    state.keyword = readQueryText(route, 'keyword');
    state.category = readQueryText(route, 'category');
    state.jumpPage = String(state.page);
};

const loadCategories = async () => {
    try {
        const categories = await getCategoriesApi(true);
        state.categoryOptions = (categories || []).map((item) => item.name).filter(Boolean);
    } catch (error) {
        state.categoryOptions = [];
    }
};

const loadStats = async () => {
    try {
        state.stats = await getAdminStatsApi();
    } catch (error) {
        state.stats = null;
    }
};

const loadArticles = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminArticlesApi(
            state.page,
            state.pageSize,
            state.status || null,
            state.keyword || null,
            state.category || null
        );
        const overflowPage = resolveAdminOverflowPage(state, result);
        if (overflowPage) {
            state.page = overflowPage;
            state.jumpPage = String(overflowPage);
            await syncQuery({
                page: overflowPage > 1 ? String(overflowPage) : undefined
            });
            return;
        }
        state.items = result.items || [];
        state.total = result.total || 0;
        state.page = result.page || state.page;
        state.jumpPage = String(state.page);
    } catch (error) {
        state.error = error.message || '文章列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    return await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        status: patch.status ?? (state.status || undefined),
        keyword: patch.keyword ?? (state.keyword || undefined),
        category: patch.category ?? (state.category || undefined)
    });
};

const submitFilters = async () => {
    state.page = 1;
    const routeChanged = await syncQuery({
        page: undefined,
        status: state.status || undefined,
        keyword: state.keyword || undefined,
        category: state.category || undefined
    });
    if (!routeChanged) {
        await loadArticles();
    }
};

const resetFilters = async () => {
    state.status = '';
    state.keyword = '';
    state.category = '';
    state.page = 1;
    await syncQuery({ page: undefined, status: undefined, keyword: undefined, category: undefined });
};

const applyQuickStatus = async (status) => {
    state.status = status;
    state.page = 1;
    await syncQuery({
        page: undefined,
        status: status || undefined
    });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({
        page: targetPage > 1 ? String(targetPage) : undefined
    });
};

const quickStatusCards = [
    { label: '全部文章', key: 'ALL', status: '', value: () => state.stats?.totalArticles || 0 },
    { label: '草稿', key: 'DRAFT', status: 'DRAFT', value: () => state.stats?.draftArticles || 0 },
    { label: '定时发布', key: 'SCHEDULED', status: 'SCHEDULED', value: () => state.stats?.scheduledArticles || 0 },
    { label: '已发布', key: 'PUBLISHED', status: 'PUBLISHED', value: () => state.stats?.publishedArticles || 0 },
    { label: '精选', key: 'FEATURED', status: 'FEATURED', value: () => state.stats?.featuredArticles || 0 },
    { label: '待审核', key: 'REVIEW_PENDING', status: 'REVIEW_PENDING', value: () => state.stats?.reviewPendingArticles || 0 },
    { label: '已下架', key: 'OFFLINE', status: 'OFFLINE', value: () => state.stats?.offlineArticles || 0 },
    { label: '已删除', key: 'DELETED', status: 'DELETED', value: () => state.stats?.deletedArticles || 0 }
];

const toggleArticleStatus = async (article) => {
    const nextStatus = article.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED';
    openConfirmDialog({
        title: nextStatus === 'OFFLINE' ? '下架文章' : '重新发布',
        message: nextStatus === 'OFFLINE'
            ? `确定下架文章《${article.title}》吗？下架后前台将暂时不可见。`
            : `确定重新发布文章《${article.title}》吗？发布后会重新出现在前台内容流中。`,
        confirmText: nextStatus === 'OFFLINE' ? '确认下架' : '确认发布',
        tone: nextStatus === 'OFFLINE' ? 'warning' : 'primary',
        onConfirm: async () => {
            state.actionLoadingId = article.id;
            try {
                await updateAdminArticleStatusApi(article.id, nextStatus);
                article.status = nextStatus;
                await loadStats();
            } catch (error) {
                toast.error(error.message || '文章状态更新失败');
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

const deleteArticle = async (article) => {
    openConfirmDialog({
        title: '删除文章',
        message: `确定删除文章《${article.title}》吗？删除后会从前台移除，但仍保留后台治理记录。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            state.actionLoadingId = article.id;
            try {
                await deleteAdminArticleApi(article.id);
                const idx = state.items.findIndex(item => item.id === article.id);
                if (idx !== -1) state.items.splice(idx, 1);
                state.total = Math.max(0, state.total - 1);
                if (state.items.length === 0 && state.page > 1) {
                    state.page--;
                    state.jumpPage = String(state.page);
                }
                await loadStats();
            } catch (error) {
                toast.error(error.message || '文章删除失败');
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

// 精选权重弹窗
const featureDialog = reactive({
    visible: false,
    article: null,
    weight: 500
});
const featureDialogMaskPointerStarted = ref(false);

const openFeatureDialog = (article) => {
    featureDialog.article = article;
    featureDialog.weight = article.featureWeight ?? 500;
    featureDialog.visible = true;
};

const closeFeatureDialog = () => {
    featureDialog.visible = false;
    featureDialog.article = null;
    featureDialogMaskPointerStarted.value = false;
};

const handleFeatureDialogMaskPointerDown = (event) => {
    featureDialogMaskPointerStarted.value = event.target === event.currentTarget;
};

const handleFeatureDialogMaskClick = (event) => {
    if (featureDialogMaskPointerStarted.value && event.target === event.currentTarget) {
        closeFeatureDialog();
        return;
    }
    featureDialogMaskPointerStarted.value = false;
};

const confirmFeature = async () => {
    const article = featureDialog.article;
    if (!article) return;
    const weight = Math.max(0, Math.min(1000000, Number(featureDialog.weight) || 500));
    state.actionLoadingId = article.id;
    featureDialog.visible = false;
    try {
        const res = await featureAdminArticleApi(article.id, weight);
        article.featured = true;
        article.featureWeight = res?.featureWeight ?? weight;
        toast.success(`已精选，权重 ${article.featureWeight}`);
    } catch (error) {
        toast.error(error.message || '精选操作失败');
    } finally {
        state.actionLoadingId = null;
        featureDialog.article = null;
    }
};

const toggleFeatured = async (article) => {
    if (article.featured) {
        // 取消精选无需权重，直接执行
        state.actionLoadingId = article.id;
        try {
            await unfeatureAdminArticleApi(article.id);
            article.featured = false;
            article.featureWeight = 0;
            toast.success('已取消精选');
        } catch (error) {
            toast.error(error.message || '取消精选失败');
        } finally {
            state.actionLoadingId = null;
        }
    } else {
        // 设为精选：弹出权重输入框
        openFeatureDialog(article);
    }
};

const exportArticles = async () => {
    try {
        await exportAdminArticlesApi({
            status: state.status || undefined,
            keyword: state.keyword || undefined,
            category: state.category || undefined
        });
    } catch (error) {
        toast.error(error.message || '文章导出失败');
    }
};

const loadPageResources = async () => {
    await Promise.all([loadArticles(), loadStats()]);
};

useAdminRefresh(loadPageResources);

onMounted(async () => {
    await Promise.all([loadCategories(), loadStats()]);
});

watch(
    () => [route.query.page, route.query.status, route.query.keyword, route.query.category],
    () => {
        applyRouteState();
        loadArticles();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel">
        <div class="admin-toolbar">
            <div class="admin-status-overview">
                <button
                    v-for="card in quickStatusCards"
                    :key="card.key"
                    type="button"
                    class="admin-status-card"
                    :class="{ active: state.status === card.status }"
                    @click="applyQuickStatus(card.status)"
                >
                    <span>{{ card.label }}</span>
                    <strong>{{ card.value() }}</strong>
                </button>
            </div>
            <form class="admin-filter-toolbar" @submit.prevent="submitFilters">
                <label>
                    <span>文章状态</span>
                    <select v-model="state.status">
                        <option value="">全部</option>
                        <option value="PUBLISHED">已发布</option>
                        <option value="FEATURED">精选</option>
                        <option value="SCHEDULED">定时发布</option>
                        <option value="REVIEW_PENDING">待审核</option>
                        <option value="DRAFT">草稿</option>
                        <option value="OFFLINE">已下架</option>
                        <option value="DELETED">已删除</option>
                    </select>
                </label>
                <label>
                    <span>分类</span>
                    <select v-model="state.category">
                        <option value="">全部分类</option>
                        <option v-for="category in state.categoryOptions" :key="category" :value="category">
                            {{ category }}
                        </option>
                    </select>
                </label>
                <label class="admin-filter-grow">
                    <span>关键词</span>
                    <input v-model.trim="state.keyword" type="text" placeholder="搜索标题">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit">查询</button>
                    <button type="button" @click="resetFilters">重置</button>
                    <button type="button" class="btn-export" @click="exportArticles" title="导出所有文章为 CSV">导出 CSV</button>
                </div>
            </form>
        </div>

        <div class="admin-table-shell">
            <p v-if="state.error && state.items.length" class="backend-state-text error-text subtle">
                {{ state.error }}
            </p>
            <p v-else-if="state.error && !state.items.length" class="backend-state-text error-text">
                {{ state.error }}
            </p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-articles-table">
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>标题</th>
                                <th>作者</th>
                                <th>分类</th>
                                <th>状态</th>
                                <th>精选</th>
                                <th>阅读 / 点赞 / 收藏 / 评论</th>
                                <th>发布时间</th>
                                <th>创建时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="article in state.items" :key="article.id">
                                <td>
                                    <RouterLink :to="`/articles/${article.id}`">{{ article.title }}</RouterLink>
                                </td>
                                <td>
                                    <strong>{{ article.authorNickname || article.authorUsername || '-' }}</strong>
                                    <p class="admin-subtext">@{{ article.authorUsername || '-' }}</p>
                                </td>
                                <td>{{ article.category || '-' }}</td>
                                <td>
                                    <span class="status-pill" :class="{ warning: article.status === 'OFFLINE' }">
                                        {{ article.status }}
                                    </span>
                                    <span v-if="article.warnFlag" class="status-pill review-pending" title="含敏感词，待审核">待审核</span>
                                </td>
                                <td>
                                    <span v-if="article.featured" class="status-pill" :title="`精选权重 ${article.featureWeight ?? 0}`">
                                        精选 · {{ article.featureWeight ?? 0 }}
                                    </span>
                                    <span v-else class="admin-subtext">—</span>
                                </td>
                                <td>
                                    {{ article.viewCount }} / {{ article.likeCount }} / {{ article.favoriteCount }} / {{ article.commentCount }}
                                </td>
                                <td>
                                    <span v-if="article.status === 'SCHEDULED'">计划 {{ formatAdminDateTime(article.scheduledPublishAt) }}</span>
                                    <span v-else>{{ formatAdminDateTime(article.publishedAt) }}</span>
                                </td>
                                <td>{{ formatAdminDateTime(article.createdAt) }}</td>
                                <td class="table-actions">
                                    <button
                                        v-if="article.status !== 'DELETED'"
                                        type="button"
                                        :disabled="state.actionLoadingId === article.id"
                                        @click="toggleArticleStatus(article)"
                                    >
                                        {{ article.status === 'PUBLISHED' ? '下架' : '发布' }}
                                    </button>
                                    <span v-else class="admin-subtext">已删除</span>
                                    <button
                                        v-if="article.status === 'PUBLISHED'"
                                        class="featured-action-button"
                                        type="button"
                                        :disabled="state.actionLoadingId === article.id"
                                        @click="toggleFeatured(article)"
                                    >
                                        {{ article.featured ? '取消精选' : '精选' }}
                                    </button>
                                    <button
                                        type="button"
                                        @click="openPreview(article)"
                                    >
                                        预览
                                    </button>
                                    <button
                                        type="button"
                                        class="danger-link"
                                        :disabled="state.actionLoadingId === article.id"
                                        @click="deleteArticle(article)"
                                    >
                                        删除
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有文章</p>
            </template>
        </div>

        <AdminPagination :state="state" label="文章分页" @page-change="changePage" />

        <ConfirmDialog
            :visible="confirmDialog.visible"
            eyebrow="文章操作确认"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :confirm-text="confirmDialog.confirmText"
            :tone="confirmDialog.tone"
            :loading="confirmDialog.loading"
            @close="closeConfirmDialog"
            @confirm="executeConfirmDialog"
        />

        <!-- 精选权重设置弹窗 -->
        <Teleport to="body">
            <div
                v-if="featureDialog.visible"
                class="feature-dialog-mask"
                @pointerdown="handleFeatureDialogMaskPointerDown"
                @pointercancel="featureDialogMaskPointerStarted = false"
                @click="handleFeatureDialogMaskClick"
            >
                <div class="feature-dialog">
                    <p class="feature-dialog-title">设置精选权重</p>
                    <p class="feature-dialog-desc">权重越高，文章在精选列表和推荐流中排越靠前（0–1000000）</p>
                    <div class="feature-dialog-field">
                        <label for="feature-weight-input">权重</label>
                        <input
                            id="feature-weight-input"
                            v-model.number="featureDialog.weight"
                            type="number"
                            min="0"
                            max="1000000"
                            step="1000"
                            autofocus
                            @input="featureDialog.weight = Math.max(0, Math.min(1000000, Number($event.target.value) || 0))"
                            @keydown.enter="confirmFeature"
                            @keydown.esc="closeFeatureDialog"
                        >
                        <span class="feature-dialog-hint">0 最低 · 500 默认 · 1000000 最高</span>
                    </div>
                    <div class="feature-dialog-actions">
                        <button type="button" class="feature-dialog-cancel" @click="closeFeatureDialog">取消</button>
                        <button type="button" class="feature-dialog-confirm" @click="confirmFeature">确认精选</button>
                    </div>
                </div>
            </div>
        </Teleport>
        <!-- 快速预览抽屉 -->
        <Teleport to="body">
            <div v-if="previewId" class="preview-overlay" @click.self="closePreview">
                <aside class="preview-drawer">
                    <div class="preview-drawer-header">
                        <span class="preview-drawer-title">文章预览</span>
                        <button class="preview-close-btn" type="button" @click="closePreview">✕</button>
                    </div>
                    <div v-if="previewLoading" class="preview-drawer-body preview-loading">加载中...</div>
                    <div v-else-if="previewArticle" class="preview-drawer-body">
                        <h2 class="preview-article-title">{{ previewArticle.title }}</h2>
                        <div class="preview-article-meta">
                            <span>{{ previewArticle.author?.name }}</span>
                            <span>{{ previewArticle.category }}</span>
                            <span>{{ previewArticle.publishedText }}</span>
                            <span class="status-pill" :class="{ warning: previewArticle.status === 'OFFLINE' }">{{ previewArticle.status }}</span>
                        </div>
                        <p v-if="previewArticle.summary" class="preview-article-summary">{{ previewArticle.summary }}</p>
                        <div class="preview-article-tags">
                            <span v-for="tag in (previewArticle.tags || [])" :key="tag" class="preview-tag">{{ tag }}</span>
                        </div>
                        <div class="preview-article-stats">
                            <span>👁 {{ previewArticle.stats?.views }}</span>
                            <span>❤ {{ previewArticle.stats?.likes }}</span>
                            <span>💬 {{ previewArticle.stats?.comments }}</span>
                        </div>
                        <RouterLink :to="`/articles/${previewArticle.id}`" target="_blank" class="preview-open-link">
                            在新标签页中查看全文 →
                        </RouterLink>
                    </div>
                    <div v-else class="preview-drawer-body preview-loading">加载失败</div>
                </aside>
            </div>
        </Teleport>
    </section>
</template>

<style scoped src="@/styles/views/admin/AdminArticlesView.part-1.css"></style>
<style scoped src="@/styles/views/admin/AdminArticlesView.part-2.css"></style>
