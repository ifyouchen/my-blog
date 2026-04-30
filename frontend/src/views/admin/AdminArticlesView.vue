<script setup>
import {onMounted, reactive, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
  deleteAdminArticleApi,
  featureAdminArticleApi,
  getAdminArticlesApi,
  getAdminStatsApi,
  getCategoriesApi,
  unfeatureAdminArticleApi,
  updateAdminArticleStatusApi
} from '@/api/admin';
import {createPagedState, readPositiveInt, readQueryText, resolveAdminOverflowPage, syncAdminQuery, useAdminRefresh} from '@/views/admin/adminShared';
import {useConfirmDialog} from '@/composables/useConfirmDialog';

const route = useRoute();
const router = useRouter();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const state = reactive({
    ...createPagedState(),
    status: '',
    keyword: '',
    category: '',
    categoryOptions: [],
    stats: null,
    feedback: '',
    feedbackType: 'success',
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
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        status: patch.status ?? (state.status || undefined),
        keyword: patch.keyword ?? (state.keyword || undefined),
        category: patch.category ?? (state.category || undefined)
    });
};

const submitFilters = async () => {
    state.page = 1;
    await syncQuery({
        page: undefined,
        status: state.status || undefined,
        keyword: state.keyword || undefined,
        category: state.category || undefined
    });
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

const setFeedback = (message, type = 'success') => {
    state.feedback = message;
    state.feedbackType = type;
};

const quickStatusCards = [
    { label: '全部文章', key: 'ALL', status: '', value: () => state.stats?.totalArticles || 0 },
    { label: '草稿', key: 'DRAFT', status: 'DRAFT', value: () => state.stats?.draftArticles || 0 },
    { label: '已发布', key: 'PUBLISHED', status: 'PUBLISHED', value: () => state.stats?.publishedArticles || 0 },
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
                setFeedback(nextStatus === 'OFFLINE' ? '文章已下架' : '文章已重新发布');
                await Promise.all([loadArticles(), loadStats()]);
            } catch (error) {
                setFeedback(error.message || '文章状态更新失败', 'error');
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
                setFeedback('文章已删除');
                await Promise.all([loadArticles(), loadStats()]);
            } catch (error) {
                setFeedback(error.message || '文章删除失败', 'error');
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

const toggleFeatured = async (article) => {
    const newFeatured = !article.featured;
    state.actionLoadingId = article.id;
    try {
        if (newFeatured) {
            await featureAdminArticleApi(article.id);
        } else {
            await unfeatureAdminArticleApi(article.id);
        }
        article.featured = newFeatured;
        setFeedback(newFeatured ? '文章已设为精选' : '文章已取消精选');
    } catch (error) {
        setFeedback(error.message || '精选操作失败', 'error');
    } finally {
        state.actionLoadingId = null;
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
                </div>
            </form>
            <p v-if="state.feedback" :class="['backend-state-text', state.feedbackType === 'error' ? 'error-text' : 'success-text']">
                {{ state.feedback }}
            </p>
        </div>

        <div class="admin-table-shell">
            <p v-if="state.loading && state.items.length" class="backend-state-text subtle">
                正在更新文章数据...
            </p>
            <p v-if="state.error && state.items.length" class="backend-state-text error-text subtle">
                {{ state.error }}
            </p>
            <p v-if="state.loading && !state.items.length" class="backend-state-text">
                文章数据加载中...
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
                                    <span v-if="article.featured" class="status-pill brand">精选</span>
                                    <span v-else class="admin-subtext">—</span>
                                </td>
                                <td>
                                    {{ article.viewCount }} / {{ article.likeCount }} / {{ article.favoriteCount }} / {{ article.commentCount }}
                                </td>
                                <td>{{ article.publishedAt || '-' }}</td>
                                <td>{{ article.createdAt }}</td>
                                <td class="table-actions">
                                    <button
                                        v-if="article.status !== 'DELETED'"
                                        type="button"
                                        :disabled="state.actionLoadingId === article.id"
                                        @click="toggleArticleStatus(article)"
                                    >
                                        {{ state.actionLoadingId === article.id ? '处理中...' : (article.status === 'PUBLISHED' ? '下架' : '发布') }}
                                    </button>
                                    <span v-else class="admin-subtext">已删除</span>
                                    <button
                                        v-if="article.status === 'PUBLISHED'"
                                        type="button"
                                        :disabled="state.actionLoadingId === article.id"
                                        @click="toggleFeatured(article)"
                                    >
                                        {{ state.actionLoadingId === article.id ? '处理中...' : (article.featured ? '取消精选' : '精选') }}
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
    </section>
</template>

<style scoped>
.admin-status-overview {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 18px;
}

.admin-status-card {
    display: grid;
    gap: 8px;
    padding: 14px 16px;
    color: var(--text);
    text-align: left;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: border-color 0.12s, background 0.12s;
}

.admin-status-card span {
    color: var(--muted);
    font-size: 13px;
}

.admin-status-card strong {
    font-size: 22px;
    line-height: 1.1;
}

.admin-status-card:hover,
.admin-status-card.active {
    border-color: rgba(37, 99, 235, 0.22);
    background: var(--brand-soft);
}

@media (max-width: 980px) {
    .admin-status-overview {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 560px) {
    .admin-status-overview {
        grid-template-columns: 1fr;
    }
}

.status-pill.review-pending {
    background: #fff7ed;
    color: #c2410c;
    border: 1px solid #fed7aa;
    margin-left: 4px;
    font-size: 11px;
}

.admin-status-card[data-key="REVIEW_PENDING"].active,
.admin-status-card[data-key="REVIEW_PENDING"]:hover {
    border-color: #fb923c;
    background: #fff7ed;
}
</style>
