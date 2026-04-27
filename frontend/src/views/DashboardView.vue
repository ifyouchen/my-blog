<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import CreatorSidebar from '@/components/CreatorSidebar.vue';
import { getMyFavoritesApi, unfavoriteArticleApi } from '@/api/favorites';
import { deleteArticleApi, getMyArticleOverviewApi, getMyArticlesApi, updateArticleStatusApi } from '@/api/articles';
import { useSession } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const { isLoggedIn } = useSession();

const isFavorites = computed(() => route.name === 'favorites');
const articleStatus = ref(String(route.query.status || ''));
const currentPage = ref(Number.parseInt(route.query.page || '1', 10) || 1);
const pageSize = 10;
const articles = ref([]);
const favorites = ref([]);
const total = ref(0);
const overview = ref({
    totalCount: 0,
    draftCount: 0,
    publishedCount: 0,
    offlineCount: 0,
    deletedCount: 0,
    totalViewCount: 0,
    totalLikeCount: 0,
    totalFavoriteCount: 0,
    totalCommentCount: 0,
    latestArticleTitle: '',
    latestUpdatedAt: ''
});
const isLoading = ref(false);
const loadError = ref('');
const feedback = ref('');
const feedbackType = ref('success');
const actionLoadingId = ref(null);
const jumpPage = ref(String(currentPage.value));

const statusOptions = [
    { label: '全部', value: '' },
    { label: '草稿', value: 'DRAFT' },
    { label: '已发布', value: 'PUBLISHED' },
    { label: '已下架', value: 'OFFLINE' },
    { label: '已删除', value: 'DELETED' }
];

const articleStatusLabels = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    OFFLINE: '已下架',
    DELETED: '已删除'
};

const resolveArticleActionError = (nextStatus, message) => {
    const source = String(message || '').trim();
    if (nextStatus === 'PUBLISHED') {
        if (source.includes('文章正文不能为空')) {
            return '发布前请先补全文章正文。';
        }
        if (source.includes('文章分类不能为空')) {
            return '发布前请先选择文章分类。';
        }
        if (source.includes('文章标题不能为空')) {
            return '发布前请先填写文章标题。';
        }
        return source || '发布失败，请稍后再试。';
    }
    return source || '下架失败，请稍后再试。';
};

const syncRoute = (overrides = {}) => {
    const nextPage = String(overrides.page ?? currentPage.value);
    const nextStatus = overrides.status ?? articleStatus.value;
    router.replace({
        path: isFavorites.value ? '/dashboard/favorites' : '/dashboard/articles',
        query: {
            page: nextPage === '1' ? undefined : nextPage,
            status: isFavorites.value ? undefined : (nextStatus || undefined)
        }
    });
};

const fetchArticles = async (options = {}) => {
    const silent = Boolean(options.silent);
    if (!silent) {
        isLoading.value = true;
        loadError.value = '';
    }
    try {
        const pageResult = await getMyArticlesApi({
            page: currentPage.value,
            pageSize,
            status: articleStatus.value
        });
        articles.value = pageResult.items || [];
        total.value = pageResult.total || 0;
    } catch (error) {
        articles.value = [];
        total.value = 0;
        loadError.value = error.message || '加载文章失败';
    } finally {
        if (!silent) {
            isLoading.value = false;
        }
    }
};

const fetchOverview = async () => {
    if (!isLoggedIn.value || isFavorites.value) {
        overview.value = {
            totalCount: 0,
            draftCount: 0,
            publishedCount: 0,
            offlineCount: 0,
            deletedCount: 0,
            totalViewCount: 0,
            totalLikeCount: 0,
            totalFavoriteCount: 0,
            totalCommentCount: 0,
            latestArticleTitle: '',
            latestUpdatedAt: ''
        };
        return;
    }
    try {
        overview.value = await getMyArticleOverviewApi();
    } catch (error) {
        overview.value = {
            totalCount: 0,
            draftCount: 0,
            publishedCount: 0,
            offlineCount: 0,
            deletedCount: 0,
            totalViewCount: 0,
            totalLikeCount: 0,
            totalFavoriteCount: 0,
            totalCommentCount: 0,
            latestArticleTitle: '',
            latestUpdatedAt: ''
        };
    }
};

const fetchFavorites = async () => {
    isLoading.value = true;
    loadError.value = '';
    try {
        const pageResult = await getMyFavoritesApi(currentPage.value, pageSize);
        favorites.value = pageResult.items || [];
        total.value = pageResult.total || 0;
    } catch (error) {
        favorites.value = [];
        total.value = 0;
        loadError.value = error.message || '加载收藏失败';
    } finally {
        isLoading.value = false;
    }
};

const removeFavorite = async (article) => {
    actionLoadingId.value = article.id;
    feedback.value = '';
    try {
        await unfavoriteArticleApi(article.id);
        favorites.value = favorites.value.filter((item) => item.id !== article.id);
        total.value = Math.max(0, total.value - 1);
        feedback.value = `已取消收藏《${article.title}》`;
        feedbackType.value = 'success';
        const totalPagesAfterRemove = Math.max(1, Math.ceil(total.value / pageSize));
        if (!favorites.value.length && currentPage.value > totalPagesAfterRemove) {
            currentPage.value = totalPagesAfterRemove;
            syncRoute({ page: totalPagesAfterRemove });
            return;
        }
    } catch (error) {
        feedback.value = error.message || '取消收藏失败';
        feedbackType.value = 'error';
    } finally {
        actionLoadingId.value = null;
    }
};

const fetchCurrentTab = async () => {
    if (!isLoggedIn.value) {
        loadError.value = '请先登录后查看个人中心内容';
        articles.value = [];
        favorites.value = [];
        total.value = 0;
        return;
    }
    if (isFavorites.value) {
        await fetchFavorites();
        return;
    }
    // Use silent mode when data already exists to prevent page flickering on tab/page switch
    const hasExistingData = articles.value.length > 0;
    await Promise.all([fetchArticles({ silent: hasExistingData }), fetchOverview()]);
};

const changePage = (page) => {
    const totalPages = Math.max(1, Math.ceil(total.value / pageSize));
    if (page < 1 || page > totalPages || page === currentPage.value || isLoading.value) {
        return;
    }
    currentPage.value = page;
    syncRoute({ page });
};

const changeStatus = (status) => {
    articleStatus.value = status;
    currentPage.value = 1;
    syncRoute({ page: 1, status });
};

const editArticle = (articleId) => {
    router.push(`/editor/${articleId}`);
};

const publishOrOfflineArticle = async (article, nextStatus) => {
    actionLoadingId.value = article.id;
    if (feedbackType.value !== 'error') {
        feedback.value = '';
    }
    try {
        await updateArticleStatusApi(article.id, nextStatus);
        await Promise.all([fetchArticles({ silent: true }), fetchOverview()]);
    } catch (error) {
        feedback.value = resolveArticleActionError(nextStatus, error.message);
        feedbackType.value = 'error';
    } finally {
        actionLoadingId.value = null;
    }
};

const removeArticle = async (articleId) => {
    if (!confirm('确定删除这篇文章吗？')) {
        return;
    }
    try {
        await deleteArticleApi(articleId);
        feedback.value = '文章已删除';
        feedbackType.value = 'success';
        await Promise.all([fetchCurrentTab(), fetchOverview()]);
    } catch (error) {
        feedback.value = error.message || '删除失败';
        feedbackType.value = 'error';
    }
};

const overviewCards = computed(() => ([
    { label: '全部文章', value: overview.value.totalCount, hint: '当前账号下的全部内容' },
    { label: '草稿', value: overview.value.draftCount, hint: '还没发布的创作内容' },
    { label: '已发布', value: overview.value.publishedCount, hint: '正在对外展示的文章' },
    { label: '已下架', value: overview.value.offlineCount, hint: '可随时重新发布的文章' }
]));

const metricCards = computed(() => ([
    { label: '总阅读', value: overview.value.totalViewCount, hint: '累计阅读量' },
    { label: '总获赞', value: overview.value.totalLikeCount, hint: '累计点赞数' },
    { label: '总收藏', value: overview.value.totalFavoriteCount, hint: '累计收藏数' },
    { label: '总评论', value: overview.value.totalCommentCount, hint: '累计评论互动' }
]));

const getArticleStatusLabel = (status) => articleStatusLabels[status] || status || '未知状态';

const getArticleStatusClass = (status) => ({
    published: status === 'PUBLISHED',
    draft: status === 'DRAFT',
    offline: status === 'OFFLINE',
    deleted: status === 'DELETED'
});

const getUpdatedTimeParts = (text) => {
    if (!text) {
        return { date: '--', time: '' };
    }
    const [date, time] = String(text).split(' ');
    return {
        date: date || text,
        time: time || ''
    };
};

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));
const pageStart = computed(() => {
    if (!total.value) {
        return 0;
    }
    return (currentPage.value - 1) * pageSize + 1;
});
const pageEnd = computed(() => Math.min(currentPage.value * pageSize, total.value));

const paginationItems = computed(() => {
    const pages = [];
    const appendPage = (page) => {
        if (!pages.some((item) => item.type === 'page' && item.value === page)) {
            pages.push({ type: 'page', value: page });
        }
    };
    const appendEllipsis = (key) => {
        pages.push({ type: 'ellipsis', value: key });
    };

    if (totalPages.value <= 7) {
        for (let page = 1; page <= totalPages.value; page++) {
            appendPage(page);
        }
        return pages;
    }

    appendPage(1);
    if (currentPage.value > 4) {
        appendEllipsis('left');
    }

    const start = Math.max(2, currentPage.value - 1);
    const end = Math.min(totalPages.value - 1, currentPage.value + 1);
    for (let page = start; page <= end; page++) {
        appendPage(page);
    }

    if (currentPage.value < totalPages.value - 3) {
        appendEllipsis('right');
    }
    appendPage(totalPages.value);
    return pages;
});

const submitJump = () => {
    const page = Number.parseInt(jumpPage.value, 10);
    if (Number.isNaN(page)) {
        jumpPage.value = String(currentPage.value);
        return;
    }
    const targetPage = Math.min(Math.max(1, page), totalPages.value);
    jumpPage.value = String(targetPage);
    changePage(targetPage);
};

watch(
    () => [route.name, route.query.page, route.query.status],
    () => {
        currentPage.value = Number.parseInt(route.query.page || '1', 10) || 1;
        articleStatus.value = String(route.query.status || '');
        jumpPage.value = String(currentPage.value);
        fetchCurrentTab();
    },
    { immediate: true }
);

watch(isLoggedIn, () => {
    fetchCurrentTab();
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell dashboard-layout" :data-testid="isFavorites ? 'dashboard-favorites-page' : 'dashboard-articles-page'">
        <CreatorSidebar />

        <section class="dashboard-main">
            <div class="section-heading">
                <div>
                    <p class="eyebrow">{{ isFavorites ? '收藏夹' : '内容管理' }}</p>
                    <h1>{{ isFavorites ? '我的收藏' : '我的文章' }}</h1>
                </div>
                <RouterLink v-if="!isFavorites" class="primary-action" to="/editor/new">新建文章</RouterLink>
            </div>

            <section v-if="!isFavorites" class="creator-overview">
                <div class="creator-overview-grid">
                    <article v-for="card in overviewCards" :key="card.label" class="creator-overview-card">
                        <span>{{ card.label }}</span>
                        <strong>{{ card.value }}</strong>
                        <p>{{ card.hint }}</p>
                    </article>
                </div>
                <div class="creator-overview-grid secondary">
                    <article v-for="card in metricCards" :key="card.label" class="creator-overview-card secondary">
                        <span>{{ card.label }}</span>
                        <strong>{{ card.value }}</strong>
                        <p>{{ card.hint }}</p>
                    </article>
                </div>
                <div class="creator-overview-latest">
                    <div>
                        <p class="eyebrow">最近修改</p>
                        <strong>{{ overview.latestArticleTitle || '还没有内容更新' }}</strong>
                        <span>{{ overview.latestUpdatedAt || '创建或编辑一篇文章后，这里会显示最近的更新时间。' }}</span>
                    </div>
                    <RouterLink v-if="overview.draftCount > 0" class="creator-overview-link" to="/dashboard/articles?status=DRAFT">
                        继续处理草稿
                    </RouterLink>
                </div>
            </section>

            <div v-if="!isFavorites" class="dashboard-toolbar">
                <div class="status-tabs">
                    <button
                        v-for="option in statusOptions"
                        :key="option.value || 'ALL'"
                        type="button"
                        :class="{ active: articleStatus === option.value }"
                        @click="changeStatus(option.value)"
                    >
                        {{ option.label }}
                    </button>
                </div>
                <div v-if="feedback && feedbackType === 'error'" :class="['dashboard-feedback', feedbackType]" role="alert">
                    <strong>操作提醒</strong>
                    <span>{{ feedback }}</span>
                </div>
            </div>

            <section v-if="isFavorites" class="dashboard-content-panel" data-testid="dashboard-favorites-panel">
                <p v-if="feedback" :class="['form-message', feedbackType]">{{ feedback }}</p>
                <p v-if="isLoading" class="loading-text">加载中...</p>
                <p v-else-if="loadError" class="error-text">{{ loadError }}</p>
                <div v-else-if="favorites.length" class="favorite-grid">
                    <article v-for="article in favorites" :key="article.id" class="favorite-card">
                        <img :src="article.cover" :alt="article.coverAlt">
                        <div>
                            <span>{{ article.category }}</span>
                            <h2>{{ article.title }}</h2>
                            <p>{{ article.summary }}</p>
                            <p class="favorite-meta">
                                <span>作者：{{ article.author.name }}</span>
                                <span>{{ article.favoritedAt || '刚刚收藏' }}</span>
                            </p>
                            <div class="favorite-actions">
                                <RouterLink :to="`/articles/${article.id}`">继续阅读</RouterLink>
                                <button
                                    type="button"
                                    class="btn-danger-secondary"
                                    :disabled="actionLoadingId === article.id"
                                    @click="removeFavorite(article)"
                                >
                                    {{ actionLoadingId === article.id ? '处理中...' : '取消收藏' }}
                                </button>
                            </div>
                        </div>
                    </article>
                </div>
                <EmptyState
                    v-else
                    eyebrow="收藏夹"
                    title="暂无收藏"
                    description="去首页、搜索或排行榜逛逛，把感兴趣的文章先收藏起来。"
                    compact
                />
            </section>

            <section v-else class="dashboard-content-panel table-panel" data-testid="dashboard-articles-panel">
                <p v-if="isLoading" class="loading-text">正在加载文章...</p>
                <p v-else-if="loadError" class="error-text">{{ loadError }}</p>
                <table v-else>
                    <thead>
                        <tr>
                            <th>标题</th>
                            <th>状态</th>
                            <th>阅读</th>
                            <th>点赞</th>
                            <th>收藏</th>
                            <th>评论</th>
                            <th>更新时间</th>
                            <th>状态操作</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="article in articles" :key="article.id">
                            <td class="article-title-cell">
                                <div class="article-title-text">{{ article.title }}</div>
                            </td>
                            <td class="article-status-cell">
                                <span class="status-pill" :class="getArticleStatusClass(article.status)">
                                    {{ getArticleStatusLabel(article.status) }}
                                </span>
                            </td>
                            <td class="article-metric-cell">{{ article.viewCount }}</td>
                            <td class="article-metric-cell">{{ article.likeCount }}</td>
                            <td class="article-metric-cell">{{ article.favoriteCount }}</td>
                            <td class="article-metric-cell">{{ article.commentCount }}</td>
                            <td class="article-time-cell">
                                <span>{{ getUpdatedTimeParts(article.updatedText).date }}</span>
                                <small v-if="getUpdatedTimeParts(article.updatedText).time">
                                    {{ getUpdatedTimeParts(article.updatedText).time }}
                                </small>
                            </td>
                            <td class="article-action-cell">
                                <div class="article-inline-action">
                                    <button
                                        v-if="article.status === 'PUBLISHED'"
                                        type="button"
                                        class="action-link action-link-primary"
                                        :disabled="actionLoadingId === article.id"
                                        @click="publishOrOfflineArticle(article, 'OFFLINE')"
                                    >
                                        {{ actionLoadingId === article.id ? '处理中...' : '下架' }}
                                    </button>
                                    <button
                                        v-else-if="article.status === 'DRAFT' || article.status === 'OFFLINE'"
                                        type="button"
                                        class="action-link action-link-primary"
                                        :disabled="actionLoadingId === article.id"
                                        @click="publishOrOfflineArticle(article, 'PUBLISHED')"
                                    >
                                        {{ actionLoadingId === article.id ? '处理中...' : '发布' }}
                                    </button>
                                    <span v-else class="article-action-muted">不可操作</span>
                                </div>
                            </td>
                            <td class="article-action-cell">
                                <div class="article-inline-actions">
                                    <div class="article-inline-main">
                                        <RouterLink class="action-link action-link-secondary" :to="`/articles/${article.id}`">
                                            查看
                                        </RouterLink>
                                        <button
                                            v-if="article.status !== 'DELETED'"
                                            type="button"
                                            class="action-link action-link-secondary"
                                            @click="editArticle(article.id)"
                                        >
                                            {{ article.status === 'DRAFT' ? '继续写作' : '编辑' }}
                                        </button>
                                    </div>
                                    <button type="button" class="action-link action-link-danger" @click="removeArticle(article.id)">
                                        删除
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <EmptyState
                    v-if="!isLoading && !loadError && !articles.length"
                    eyebrow="内容管理"
                    title="还没有文章"
                    description="先去写下第一篇内容，把你的项目经验或技术笔记发布出来。"
                    compact
                />
            </section>

            <nav v-if="totalPages > 1" class="dashboard-pagination" aria-label="后台分页">
                <p>
                    第 {{ currentPage }} / {{ totalPages }} 页，
                    共 {{ total }} 条，当前 {{ pageStart }}-{{ pageEnd }} 条
                </p>
                <div class="dashboard-pagination-actions">
                    <button type="button" :disabled="isLoading || currentPage <= 1" @click="changePage(1)">首页</button>
                    <button type="button" :disabled="isLoading || currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
                    <template v-for="item in paginationItems" :key="`${item.type}-${item.value}`">
                        <span v-if="item.type === 'ellipsis'" class="pagination-ellipsis">...</span>
                        <button
                            v-else
                            type="button"
                            :class="{ active: item.value === currentPage }"
                            :disabled="isLoading || item.value === currentPage"
                            @click="changePage(item.value)"
                        >
                            {{ item.value }}
                        </button>
                    </template>
                    <button type="button" :disabled="isLoading || currentPage >= totalPages" @click="changePage(currentPage + 1)">下一页</button>
                    <button type="button" :disabled="isLoading || currentPage >= totalPages" @click="changePage(totalPages)">末页</button>
                </div>
                <form class="dashboard-pagination-jump" @submit.prevent="submitJump">
                    <label for="dashboard-page-jump">跳至</label>
                    <input
                        id="dashboard-page-jump"
                        v-model="jumpPage"
                        type="number"
                        min="1"
                        :max="totalPages"
                        :disabled="isLoading"
                        inputmode="numeric"
                    >
                    <span>页</span>
                    <button type="submit" :disabled="isLoading">跳转</button>
                </form>
            </nav>
        </section>
    </main>
</template>

<style scoped>
.creator-overview {
    display: grid;
    gap: 16px;
    margin-bottom: 26px;
}

.creator-overview-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

.creator-overview-card,
.creator-overview-latest {
    display: grid;
    gap: 8px;
    padding: 18px 20px;
    background: linear-gradient(180deg, rgba(31, 122, 224, 0.03), rgba(31, 122, 224, 0));
    border: 1px solid rgba(31, 122, 224, 0.08);
    border-radius: 18px;
    box-shadow: var(--shadow);
}

.creator-overview-card span,
.creator-overview-latest span {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
}

.creator-overview-card strong,
.creator-overview-latest strong {
    color: var(--text);
    font-size: 24px;
    line-height: 1.2;
}

.creator-overview-card p {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
}

.creator-overview-grid.secondary .creator-overview-card strong {
    font-size: 20px;
}

.creator-overview-latest {
    display: flex;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
}

.creator-overview-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 36px;
    padding: 0 14px;
    color: var(--brand-strong);
    background: rgba(31, 122, 224, 0.08);
    border: 1px solid rgba(31, 122, 224, 0.14);
    border-radius: 999px;
}

.dashboard-toolbar {
    display: grid;
    gap: 14px;
    margin-bottom: 20px;
}

.status-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.status-tabs button {
    min-height: 34px;
    padding: 0 14px;
    border: 1px solid rgba(107, 114, 128, 0.16);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.92);
    color: var(--muted);
    transition: border-color 0.16s ease, background-color 0.16s ease, color 0.16s ease;
}

.status-tabs button:hover {
    color: var(--text);
    border-color: rgba(31, 122, 224, 0.18);
    background: rgba(31, 122, 224, 0.05);
}

.status-tabs button.active {
    color: var(--brand-strong);
    border-color: rgba(31, 122, 224, 0.2);
    background: rgba(31, 122, 224, 0.08);
}

.dashboard-feedback {
    display: flex;
    gap: 10px;
    align-items: center;
    padding: 12px 14px;
    margin: 0;
    border: 1px solid transparent;
    border-radius: 10px;
    line-height: 1.6;
}

.dashboard-feedback strong {
    flex: none;
    color: inherit;
    font-size: 13px;
    font-weight: 700;
}

.dashboard-feedback span {
    min-width: 0;
}

.dashboard-feedback.error {
    color: #b83b3b;
    background: rgba(209, 67, 67, 0.06);
    border-color: rgba(209, 67, 67, 0.12);
}

.table-panel :deep(tbody tr:hover td) {
    background: rgba(31, 122, 224, 0.035);
}

.article-action-cell {
    width: 1%;
    min-width: 156px;
}

.article-title-cell {
    min-width: 140px;
}

.article-title-text {
    display: -webkit-box;
    max-width: 220px;
    overflow: hidden;
    color: var(--text);
    font-weight: 700;
    line-height: 1.55;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.article-status-cell {
    width: 1%;
    white-space: nowrap;
}

.article-metric-cell {
    width: 1%;
    min-width: 54px;
    color: var(--text);
    text-align: center;
    white-space: nowrap;
}

.article-time-cell {
    min-width: 118px;
    color: var(--text);
    white-space: nowrap;
}

.article-time-cell span {
    display: block;
}

.article-time-cell small {
    display: block;
    color: var(--muted);
    font-size: 12px;
}

.article-inline-action,
.article-inline-actions,
.article-inline-main {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
}

.action-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 34px;
    padding: 0 14px;
    border: 1px solid transparent;
    border-radius: 999px;
    font-size: 13px;
    font-weight: 700;
    line-height: 1;
    cursor: pointer;
    text-decoration: none;
    transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

.action-link:disabled {
    opacity: 0.62;
    cursor: not-allowed;
}

.action-link-primary {
    color: var(--brand);
    background: rgba(31, 122, 224, 0.08);
    border-color: rgba(31, 122, 224, 0.14);
}

.action-link-primary:hover:not(:disabled) {
    color: var(--brand-strong);
    background: rgba(31, 122, 224, 0.12);
    border-color: rgba(31, 122, 224, 0.18);
}

.action-link-secondary {
    color: var(--text);
    background: rgba(255, 255, 255, 0.94);
    border-color: rgba(101, 115, 111, 0.14);
}

.action-link-secondary:hover:not(:disabled) {
    color: var(--brand-strong);
    background: rgba(31, 122, 224, 0.04);
    border-color: rgba(31, 122, 224, 0.14);
}

.action-link-danger {
    color: #c64141;
    background: rgba(209, 67, 67, 0.06);
    border-color: rgba(209, 67, 67, 0.12);
}

.action-link-danger:hover:not(:disabled) {
    color: #b33434;
    background: rgba(209, 67, 67, 0.12);
    border-color: rgba(209, 67, 67, 0.18);
}

.article-action-muted {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.5;
    white-space: nowrap;
}

.btn-danger-secondary {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 36px;
    padding: 0 14px;
    color: #d14343;
    background: rgba(209, 67, 67, 0.06);
    border: 1px solid rgba(209, 67, 67, 0.24);
    border-radius: 999px;
    font-size: 14px;
    cursor: pointer;
    transition: background-color 0.15s, border-color 0.15s, color 0.15s;
}

.btn-danger-secondary:hover:not(:disabled) {
    background: rgba(209, 67, 67, 0.12);
    border-color: rgba(209, 67, 67, 0.4);
}

.btn-danger-secondary:focus {
    outline: none;
    box-shadow: 0 0 0 3px rgba(209, 67, 67, 0.16);
}

.btn-danger-secondary:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.favorite-meta {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    margin: 0 0 8px;
    color: var(--muted);
    font-size: 13px;
}

.favorite-actions {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
}

.dashboard-pagination {
    display: grid;
    gap: 12px;
    padding: 16px 18px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 18px;
}

.dashboard-pagination p {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
}

.dashboard-pagination-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
}

.dashboard-pagination-actions button,
.dashboard-pagination-jump button {
    min-width: 40px;
    min-height: 36px;
    padding: 0 12px;
    color: var(--text);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 999px;
}

.dashboard-pagination-actions button:hover:not(:disabled),
.dashboard-pagination-jump button:hover:not(:disabled),
.dashboard-pagination-actions button.active {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.dashboard-pagination-jump {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.dashboard-pagination-jump label,
.dashboard-pagination-jump span {
    color: var(--muted);
    font-size: 14px;
}

.dashboard-pagination-jump input {
    width: 76px;
    min-height: 36px;
    padding: 0 10px;
    color: var(--text);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 999px;
    outline: 0;
}

.dashboard-pagination-jump input:focus {
    border-color: var(--brand);
}

.status-pill.published {
    color: var(--brand-strong);
    background: rgba(15, 143, 117, 0.08);
}

.status-pill.draft {
    color: #9a6700;
    background: rgba(240, 201, 73, 0.1);
}

.status-pill.offline {
    color: #8b5e00;
    background: rgba(245, 158, 11, 0.1);
}

.status-pill.deleted {
    color: #b42318;
    background: rgba(180, 35, 24, 0.08);
}

.status-pill {
    min-height: 26px;
    padding: 0 9px;
    border-radius: 7px;
    font-size: 12px;
    font-weight: 700;
}

@media (max-width: 760px) {
    .creator-overview-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .creator-overview-latest {
        align-items: stretch;
        flex-direction: column;
    }

    .dashboard-pagination-jump {
        width: 100%;
    }

    .dashboard-pagination-jump input,
    .dashboard-pagination-jump button {
        width: 100%;
    }

    .article-action-cell {
        min-width: 148px;
    }

    .article-inline-action,
    .article-inline-actions,
    .article-inline-main {
        width: 100%;
        justify-content: flex-start;
    }

    .action-link {
        min-width: 0;
    }
}

@media (max-width: 560px) {
    .creator-overview-grid {
        grid-template-columns: 1fr;
    }
}
</style>
