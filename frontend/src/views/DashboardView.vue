<script setup>
import {computed, onUnmounted, ref, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import CreatorSidebar from '@/components/CreatorSidebar.vue';
import {getMyFavoritesApi, unfavoriteArticleApi} from '@/api/favorites';
import {getMyColumnsApi} from '@/api/columns';
import {
    deleteArticleApi,
    exportMyArticlesApi,
    getMyArticlesApi,
    updateArticleStatusApi,
    updateArticleUnlockRuleApi
} from '@/api/articles';
import {
  getArticleStatsApi,
  getDashboardArticlePerformanceApi,
  getDashboardContentOpportunitiesApi,
  getDashboardInteractionsApi,
  getDashboardOverviewApi,
  getDashboardTrendsApi
} from '@/api/dashboard';
import {useStableListRequest} from '@/composables/useStableListRequest';
import {useSession} from '@/stores/session';
import {useConfirmDialog} from '@/composables/useConfirmDialog';
import {getNotificationDetail, getNotificationText} from '@/utils/notifications';
import {track} from '@/utils/track';

const route = useRoute();
const router = useRouter();
const { isLoggedIn } = useSession();
const SCHEDULED_ARTICLE_REFRESH_MS = 30000;
const MIN_UNLOCK_POINT_PRICE = 10;
const MAX_UNLOCK_POINT_PRICE = 1000000;
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const isFavorites = computed(() => route.name === 'favorites');
const isOverview = computed(() => route.name === 'dashboardOverview');
const isArticles = computed(() => route.name === 'dashboardArticles');
const articleStatus = ref(String(route.query.status || ''));
const articleKeyword = ref(String(route.query.keyword || ''));
const favoriteKeyword = ref('');
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
    followerCount: 0,
    latestArticleId: null,
    latestArticleTitle: '',
    latestArticleStatus: '',
    recommendedActionType: '',
    recommendedActionText: '',
    recommendedActionHint: '',
    recommendedActionRoute: '',
    latestUpdatedAt: ''
});
const trends = ref([]);
const trendRange = ref(String(route.query.range || '7d') === '30d' ? '30d' : '7d');
const performanceSort = ref(String(route.query.sort || 'view'));
const performanceArticles = ref([]);
const interactions = ref([]);
const contentOpportunities = ref([]);
const columnCount = ref(0);
const dashboardLoading = ref(false);
const dashboardError = ref('');
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    loading: isLoading,
    runStableRequest,
    resetStableRequest
} = useStableListRequest();
const feedback = ref('');
const feedbackType = ref('success');
const actionLoadingId = ref(null);
const jumpPage = ref(String(currentPage.value));
const unlockDialogVisible = ref(false);
const unlockDialogArticle = ref(null);
const unlockForm = ref({
    needUnlock: false,
    unlockPointPrice: 0
});
const unlockSaving = ref(false);
const unlockError = ref('');
let scheduledArticleRefreshTimer = null;
let unlockOverlayPointerDownOnSelf = false;

// 文章统计抽屉
const statsDrawerVisible = ref(false);
const statsArticle = ref(null);
const statsData = ref(null);
const statsRange = ref('7d');
const statsLoading = ref(false);
const statsError = ref('');

async function openStatsDrawer(article) {
    statsArticle.value = article;
    statsRange.value = '7d';
    statsDrawerVisible.value = true;
    await loadArticleStats(article.id, '7d');
}

function closeStatsDrawer() {
    statsDrawerVisible.value = false;
    statsArticle.value = null;
    statsData.value = null;
    statsError.value = '';
}

async function loadArticleStats(articleId, range) {
    statsLoading.value = true;
    statsError.value = '';
    try {
        statsData.value = await getArticleStatsApi(articleId, range);
    } catch (e) {
        statsError.value = e?.message || '加载失败，请重试';
    } finally {
        statsLoading.value = false;
    }
}

async function changeStatsRange(range) {
    if (statsRange.value === range) return;
    statsRange.value = range;
    await loadArticleStats(statsArticle.value.id, range);
}

const statsTrendMax = computed(() => {
    if (!statsData.value?.trends?.length) return 1;
    return Math.max(1, ...statsData.value.trends.map((p) => Math.max(p.viewCount || 0, p.likeCount || 0, p.favoriteCount || 0, p.commentCount || 0)));
});

const statusOptions = [
    { label: '全部', value: '' },
    { label: '草稿', value: 'DRAFT' },
    { label: '定时发布', value: 'SCHEDULED' },
    { label: '已发布', value: 'PUBLISHED' },
    { label: '已下架', value: 'OFFLINE' },
    { label: '已删除', value: 'DELETED' }
];

const articleStatusLabels = {
    DRAFT: '草稿',
    SCHEDULED: '定时发布',
    PUBLISHED: '已发布',
    OFFLINE: '已下架',
    DELETED: '已删除'
};

const emptyOverview = () => ({
    totalCount: 0,
    draftCount: 0,
    publishedCount: 0,
    offlineCount: 0,
    deletedCount: 0,
    totalViewCount: 0,
    totalLikeCount: 0,
    totalFavoriteCount: 0,
    totalCommentCount: 0,
    followerCount: 0,
    latestArticleId: null,
    latestArticleTitle: '',
    latestArticleStatus: '',
    recommendedActionType: '',
    recommendedActionText: '',
    recommendedActionHint: '',
    recommendedActionRoute: '',
    latestUpdatedAt: ''
});

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
    const nextKeyword = overrides.keyword ?? articleKeyword.value;
    const path = isFavorites.value
        ? '/dashboard/favorites'
        : (isOverview.value ? '/dashboard/overview' : '/dashboard/articles');
    const query = {
        page: isOverview.value || nextPage === '1' ? undefined : nextPage,
        status: isArticles.value ? (nextStatus || undefined) : undefined,
        keyword: isArticles.value ? (nextKeyword || undefined) : undefined,
        range: isOverview.value && trendRange.value !== '7d' ? trendRange.value : undefined,
        sort: isOverview.value && performanceSort.value !== 'view' ? performanceSort.value : undefined
    };
    // Use history.replaceState to avoid Vue Router scrollBehavior triggering on query changes
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(query)) {
        if (value !== undefined && value !== null && value !== '') {
            params.set(key, String(value));
        }
    }
    const qs = params.toString();
    window.history.replaceState(null, '', qs ? `${path}?${qs}` : path);
};

const fetchArticles = async (options = {}) => {
    const { result } = await runStableRequest(
        () => getMyArticlesApi({
            page: currentPage.value,
            pageSize,
            status: articleStatus.value,
            keyword: articleKeyword.value
        }),
        {
            silent: options.silent ?? hasLoadedOnce.value,
            initialErrorMessage: '加载文章失败',
            refreshErrorMessage: '文章列表刷新失败，请稍后重试'
        }
    );

    if (!result) {
        return;
    }

    articles.value = result.items || [];
    total.value = result.total || 0;
};

const fetchOverview = async () => {
    if (!isLoggedIn.value || isFavorites.value) {
        overview.value = emptyOverview();
        return;
    }
    try {
        overview.value = await getDashboardOverviewApi();
    } catch (error) {
        overview.value = emptyOverview();
    }
};

const fetchFavorites = async (options = {}) => {
    const { result } = await runStableRequest(
        () => getMyFavoritesApi(currentPage.value, pageSize, favoriteKeyword.value),
        {
            silent: options.silent ?? hasLoadedOnce.value,
            initialErrorMessage: '加载收藏失败',
            refreshErrorMessage: '收藏列表刷新失败，请稍后重试'
        }
    );

    if (!result) {
        return;
    }

    favorites.value = result.items || [];
    total.value = result.total || 0;
};

const fetchDashboardAnalytics = async () => {
    if (!isLoggedIn.value) {
        trends.value = [];
        performanceArticles.value = [];
        interactions.value = [];
        contentOpportunities.value = [];
        columnCount.value = 0;
        return;
    }
    dashboardLoading.value = true;
    dashboardError.value = '';
    try {
        const [
            overviewResult,
            trendResult,
            performanceResult,
            interactionResult,
            opportunityResult,
            columnsResult
        ] = await Promise.all([
            getDashboardOverviewApi(),
            getDashboardTrendsApi(trendRange.value),
            getDashboardArticlePerformanceApi(performanceSort.value),
            getDashboardInteractionsApi(),
            getDashboardContentOpportunitiesApi().catch(() => []),
            getMyColumnsApi().catch(() => [])
        ]);
        overview.value = overviewResult;
        trends.value = trendResult || [];
        performanceArticles.value = performanceResult || [];
        interactions.value = interactionResult || [];
        contentOpportunities.value = opportunityResult || [];
        columnCount.value = (columnsResult || []).length;
    } catch (error) {
        dashboardError.value = error.message || '创作台数据加载失败';
    } finally {
        dashboardLoading.value = false;
    }
};

const fetchDashboardTrends = async () => {
    try {
        trends.value = await getDashboardTrendsApi(trendRange.value);
    } catch (error) {
        dashboardError.value = error.message || '趋势数据加载失败';
    }
};

const fetchDashboardPerformance = async () => {
    try {
        performanceArticles.value = await getDashboardArticlePerformanceApi(performanceSort.value);
    } catch (error) {
        dashboardError.value = error.message || '文章表现加载失败';
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
        resetStableRequest();
        errorMessage.value = '请先登录后查看个人中心内容';
        articles.value = [];
        favorites.value = [];
        total.value = 0;
        return;
    }
    if (isOverview.value) {
        await fetchDashboardAnalytics();
        return;
    }
    if (isFavorites.value) {
        await fetchFavorites({ silent: hasLoadedOnce.value && favorites.value.length > 0 });
        return;
    }
    await Promise.all([
        fetchArticles({ silent: hasLoadedOnce.value && articles.value.length > 0 }),
        fetchOverview()
    ]);
};

const changePage = async (page) => {
    const totalPages = Math.max(1, Math.ceil(total.value / pageSize));
    if (page < 1 || page > totalPages || page === currentPage.value || isLoading.value) {
        return;
    }
    currentPage.value = page;
    syncRoute({ page });
    await fetchCurrentTab();
};

const changeStatus = (status) => {
    articleStatus.value = status;
    currentPage.value = 1;
    syncRoute({ page: 1, status });
    fetchArticles();
};

let keywordDebounceTimer = null;
const onKeywordInput = (e) => {
    articleKeyword.value = e.target.value;
};

const doArticleSearch = () => {
    if (keywordDebounceTimer) clearTimeout(keywordDebounceTimer);
    currentPage.value = 1;
    syncRoute({ page: 1, keyword: articleKeyword.value });
    fetchArticles();
};

const resetArticleSearch = () => {
    if (keywordDebounceTimer) clearTimeout(keywordDebounceTimer);
    articleKeyword.value = '';
    currentPage.value = 1;
    syncRoute({ page: 1, keyword: '' });
    fetchArticles();
};

const clearKeyword = () => {
    if (!articleKeyword.value) return;
    resetArticleSearch();
};

let favKeywordDebounceTimer = null;
const onFavoriteKeywordInput = (e) => {
    favoriteKeyword.value = e.target.value;
};

const doFavoriteSearch = () => {
    if (favKeywordDebounceTimer) clearTimeout(favKeywordDebounceTimer);
    currentPage.value = 1;
    fetchFavorites();
};

const resetFavoriteSearch = () => {
    if (favKeywordDebounceTimer) clearTimeout(favKeywordDebounceTimer);
    favoriteKeyword.value = '';
    currentPage.value = 1;
    fetchFavorites();
};

const clearFavoriteKeyword = () => {
    if (!favoriteKeyword.value) return;
    resetFavoriteSearch();
};

const editArticle = (articleId, hash = '') => {
    router.push({ path: `/editor/${articleId}`, hash });
};

const openUnlockDialog = (article) => {
    if (!article || article.status === 'DELETED') {
        return;
    }
    unlockDialogArticle.value = article;
    unlockForm.value = {
        needUnlock: Boolean(article.needUnlock),
        unlockPointPrice: Number(article.unlockPointPrice || 0)
    };
    clampUnlockFormPointPrice();
    unlockError.value = '';
    unlockDialogVisible.value = true;
};

const closeUnlockDialog = () => {
    if (unlockSaving.value) {
        return;
    }
    unlockDialogVisible.value = false;
    unlockDialogArticle.value = null;
    unlockError.value = '';
};

const onUnlockOverlayPointerDown = (event) => {
    unlockOverlayPointerDownOnSelf = event.target === event.currentTarget;
};

const resetUnlockOverlayPointer = () => {
    unlockOverlayPointerDownOnSelf = false;
};

const onUnlockOverlayPointerUp = (event) => {
    const shouldClose = unlockOverlayPointerDownOnSelf && event.target === event.currentTarget;
    resetUnlockOverlayPointer();
    if (shouldClose) {
        closeUnlockDialog();
    }
};

const normalizeUnlockPointPrice = () => {
    const price = Number.parseInt(unlockForm.value.unlockPointPrice, 10);
    return Number.isNaN(price) ? 0 : price;
};

const clampUnlockFormPointPrice = () => {
    if (!unlockForm.value.needUnlock) {
        return;
    }
    const price = Number.parseInt(unlockForm.value.unlockPointPrice, 10);
    unlockForm.value.unlockPointPrice = Number.isNaN(price)
        ? MIN_UNLOCK_POINT_PRICE
        : Math.min(MAX_UNLOCK_POINT_PRICE, Math.max(MIN_UNLOCK_POINT_PRICE, price));
};

const onUnlockNeedUnlockChange = () => {
    clampUnlockFormPointPrice();
};

const onUnlockPointPriceCommit = () => {
    clampUnlockFormPointPrice();
};

const validateUnlockForm = () => {
    clampUnlockFormPointPrice();
    if (!unlockForm.value.needUnlock) {
        return '';
    }
    const price = normalizeUnlockPointPrice();
    if (price < MIN_UNLOCK_POINT_PRICE) {
        return `开启积分解锁后，请设置至少 ${MIN_UNLOCK_POINT_PRICE} 的积分。`;
    }
    if (price > MAX_UNLOCK_POINT_PRICE) {
        return `解锁积分不能超过 ${MAX_UNLOCK_POINT_PRICE}。`;
    }
    return '';
};

const saveUnlockRule = async () => {
    if (!unlockDialogArticle.value || unlockSaving.value) {
        return;
    }
    const validationMessage = validateUnlockForm();
    if (validationMessage) {
        unlockError.value = validationMessage;
        return;
    }
    const price = unlockForm.value.needUnlock ? normalizeUnlockPointPrice() : 0;
    unlockSaving.value = true;
    unlockError.value = '';
    try {
        const updated = await updateArticleUnlockRuleApi(unlockDialogArticle.value.id, {
            needUnlock: unlockForm.value.needUnlock,
            unlockPointPrice: price
        });
        articles.value = articles.value.map((article) => (
            String(article.id) === String(updated.id) ? { ...article, ...updated } : article
        ));
        feedback.value = `已更新《${updated.title || unlockDialogArticle.value.title}》阅读权限`;
        feedbackType.value = 'success';
        unlockDialogVisible.value = false;
        unlockDialogArticle.value = null;
    } catch (error) {
        unlockError.value = error.message || '阅读权限保存失败，请稍后重试。';
    } finally {
        unlockSaving.value = false;
    }
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

const removeArticle = async (article) => {
    openConfirmDialog({
        eyebrow: '文章操作确认',
        title: '删除文章',
        message: `确定删除《${article.title}》吗？删除后它会从前台移除，并从你的文章列表中隐藏。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            try {
                await deleteArticleApi(article.id);
                feedback.value = '文章已删除';
                feedbackType.value = 'success';
                await Promise.all([fetchCurrentTab(), fetchOverview()]);
            } catch (error) {
                feedback.value = error.message || '删除失败';
                feedbackType.value = 'error';
            }
        }
    });
};

const exportMyArticles = async () => {
    try {
        await exportMyArticlesApi({ status: articleStatus.value });
        feedback.value = '导出已开始';
        feedbackType.value = 'success';
    } catch (error) {
        feedback.value = error.message || '导出失败';
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
    { label: '总评论', value: overview.value.totalCommentCount, hint: '累计评论互动' },
    { label: '粉丝数', value: overview.value.followerCount, hint: '关注你的读者' }
]));

// 任务中心
const taskCards = computed(() => {
    const tasks = [];
    const o = overview.value;
    // 无文章 → 去写第一篇
    if (o.totalCount === 0) {
        tasks.push({
            taskId: 'create_first',
            type: 'create_first',
            title: '写下你的第一篇文章',
            desc: '分享你的技术经验或项目笔记，让读者认识你。',
            ctaText: '去写第一篇',
            ctaRoute: '/editor/new',
            priority: 1
        });
    }
    // 草稿超3天未更新 → 继续写作
    if (o.draftCount > 0 && o.latestUpdatedAt) {
        const lastUpdate = new Date(o.latestUpdatedAt);
        const daysSince = Math.floor((Date.now() - lastUpdate.getTime()) / 86400000);
        if (daysSince >= 3 && o.latestArticleStatus === 'DRAFT') {
            tasks.push({
                taskId: 'resume_draft',
                type: 'resume_draft',
                title: '草稿待续',
                desc: `《${o.latestArticleTitle || '草稿'}》已 ${daysSince} 天未更新，继续完成它吧。`,
                ctaText: '继续写作',
                ctaRoute: o.latestArticleId ? `/editor/${o.latestArticleId}` : '/editor/new',
                priority: 2
            });
        }
    }
    // 7天未发布 → 建议发布
    if (o.publishedCount === 0 && o.draftCount > 0) {
        tasks.push({
            taskId: 'publish_streak',
            type: 'publish_streak',
            title: '是时候发布了',
            desc: '你有草稿但还没有发布过文章，发布后读者才能看到你的内容。',
            ctaText: '去发布',
            ctaRoute: '/dashboard/articles?status=DRAFT',
            priority: 3
        });
    }
    // 最多显示2条
    return tasks.sort((a, b) => a.priority - b.priority).slice(0, 2);
});

const handleTaskCtaClick = (task) => {
    track('dashboard_task_clicked', {task_id: task.taskId, type: task.type});
    router.push(task.ctaRoute);
};

const performanceSortOptions = [
    { label: '阅读', value: 'view' },
    { label: '点赞', value: 'like' },
    { label: '收藏', value: 'favorite' },
    { label: '评论', value: 'comment' },
    { label: '更新', value: 'updated' }
];

const trendMax = computed(() => Math.max(
    1,
    ...trends.value.map((point) => Math.max(point.viewCount || 0, point.interactionCount || 0))
));

const changeTrendRange = async (range) => {
    if (trendRange.value === range) {
        return;
    }
    trendRange.value = range;
    syncRoute();
    await fetchDashboardTrends();
};

const changePerformanceSort = async (sort) => {
    if (performanceSort.value === sort) {
        return;
    }
    performanceSort.value = sort;
    syncRoute();
    await fetchDashboardPerformance();
};

const formatTrendDate = (date) => String(date || '').slice(5) || '--';
const formatDate = (date) => date ? String(date).replace('T', ' ').slice(0, 19) : '-';
const formatScheduledPublishAt = (date) => date ? formatDate(date) : '-';
const getInteractionText = (notification) => getNotificationText(notification.type);
const getInteractionDetail = (notification) => getNotificationDetail(notification);

const getArticleStatusLabel = (status) => articleStatusLabels[status] || status || '未知状态';
const latestArticleStatusLabel = computed(() => getArticleStatusLabel(overview.value.latestArticleStatus));
const latestArticleEditorRoute = computed(() => (
    overview.value.latestArticleId ? `/editor/${overview.value.latestArticleId}` : ''
));

const getArticleStatusClass = (status) => ({
    published: status === 'PUBLISHED',
    draft: status === 'DRAFT',
    scheduled: status === 'SCHEDULED',
    offline: status === 'OFFLINE',
    deleted: status === 'DELETED'
});

const getArticleUnlockLabel = (article) => (
    article?.needUnlock ? `${Number(article.unlockPointPrice || 0)} 积分解锁` : '免费阅读'
);

const getArticleUnlockClass = (article) => ({
    paid: Boolean(article?.needUnlock),
    free: !article?.needUnlock
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
const getScheduledPublishTimeParts = (date) => getUpdatedTimeParts(formatScheduledPublishAt(date));

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));
const hasCurrentItems = computed(() => (isFavorites.value ? favorites.value.length > 0 : articles.value.length > 0));
const hasScheduledArticles = computed(() => (
    isArticles.value && articles.value.some((article) => article.status === 'SCHEDULED')
));
const showListLoading = computed(() => initialLoading.value || (refreshing.value && !hasCurrentItems.value));
const showEmptyState = computed(() => (
    hasLoadedOnce.value
    && !refreshing.value
    && !errorMessage.value
    && !hasCurrentItems.value
));
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

const clearScheduledArticleRefreshTimer = () => {
    if (scheduledArticleRefreshTimer) {
        window.clearInterval(scheduledArticleRefreshTimer);
        scheduledArticleRefreshTimer = null;
    }
};

const refreshScheduledArticles = () => {
    if (!hasScheduledArticles.value || isLoading.value || refreshing.value) {
        return;
    }
    fetchArticles({ silent: true });
};

watch(
    () => [route.name, route.query.page, route.query.status, route.query.keyword, route.query.range, route.query.sort],
    (next, prev) => {
        if (prev && next[0] !== prev[0]) {
            resetStableRequest();
            articles.value = [];
            favorites.value = [];
            total.value = 0;
        }
        currentPage.value = Number.parseInt(route.query.page || '1', 10) || 1;
        articleStatus.value = String(route.query.status || '');
        articleKeyword.value = String(route.query.keyword || '');
        trendRange.value = String(route.query.range || '7d') === '30d' ? '30d' : '7d';
        performanceSort.value = String(route.query.sort || 'view');
        jumpPage.value = String(currentPage.value);
        fetchCurrentTab();
    },
    { immediate: true }
);

watch(isLoggedIn, () => {
    fetchCurrentTab();
});

watch(taskCards, (tasks) => {
    if (isOverview.value && tasks.length) {
        track('dashboard_task_exposed', {task_ids: tasks.map(t => t.taskId), count: tasks.length});
    }
}, {immediate: true});

watch(hasScheduledArticles, (enabled) => {
    clearScheduledArticleRefreshTimer();
    if (enabled) {
        scheduledArticleRefreshTimer = window.setInterval(
            refreshScheduledArticles,
            SCHEDULED_ARTICLE_REFRESH_MS
        );
    }
}, { immediate: true });

onUnmounted(() => {
    clearScheduledArticleRefreshTimer();
});
</script>

<template>
    <SiteHeader />
    <main
        class="page-shell dashboard-layout"
        :data-testid="isFavorites ? 'dashboard-favorites-page' : (isOverview ? 'dashboard-overview-page' : 'dashboard-articles-page')"
    >
        <CreatorSidebar />

        <section class="dashboard-main">
            <div class="section-heading">
                <div>
                    <p class="eyebrow">{{ isFavorites ? '收藏夹' : (isOverview ? '创作者工作台' : '内容管理') }}</p>
                    <h1>{{ isFavorites ? '我的收藏' : (isOverview ? '创作概览' : '我的文章') }}</h1>
                </div>
                <button v-if="!isFavorites" type="button" class="btn-export" title="导出所有文章为 CSV" @click="exportMyArticles">导出 CSV</button>
            </div>

            <section v-if="isOverview" class="creator-overview">
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
                    <div class="creator-overview-latest-copy">
                        <p class="eyebrow">最近修改</p>
                        <div class="creator-overview-latest-main">
                            <strong>{{ overview.latestArticleTitle || '还没有内容更新' }}</strong>
                            <span
                                v-if="overview.latestArticleStatus"
                                class="creator-overview-status"
                                :class="overview.latestArticleStatus.toLowerCase()"
                            >
                                {{ latestArticleStatusLabel }}
                            </span>
                        </div>
                        <span>{{ overview.latestUpdatedAt || '创建或编辑一篇文章后，这里会显示最近的更新时间。' }}</span>
                        <RouterLink
                            v-if="latestArticleEditorRoute"
                            class="creator-overview-inline-link"
                            :to="latestArticleEditorRoute"
                        >
                            继续编辑最近一篇
                        </RouterLink>
                    </div>
                    <div class="creator-overview-next-step">
                        <p class="eyebrow">建议下一步</p>
                        <strong>{{ overview.recommendedActionText || '继续创作' }}</strong>
                        <span>{{ overview.recommendedActionHint || '创作台会根据你的内容状态持续给出下一步建议。' }}</span>
                        <RouterLink class="creator-overview-link" :to="overview.recommendedActionRoute || '/editor/new'">
                            {{ overview.recommendedActionText || '马上前往' }}
                        </RouterLink>
                    </div>
                    <div class="creator-overview-next-step columns-entry">
                        <p class="eyebrow">专栏</p>
                        <strong>{{ columnCount }} 个专栏</strong>
                        <span>将相关文章归类成专栏，方便读者系统阅读。</span>
                        <RouterLink class="creator-overview-link" to="/dashboard/columns">
                            {{ columnCount ? '管理专栏' : '创建专栏' }}
                        </RouterLink>
                    </div>
                </div>
            </section>

            <!-- 任务中心 -->
            <section v-if="isOverview && taskCards.length" class="task-center">
                <p class="eyebrow">今日建议</p>
                <div class="task-cards">
                    <article v-for="task in taskCards" :key="task.taskId" class="task-card">
                        <div class="task-card-body">
                            <strong class="task-card-title">{{ task.title }}</strong>
                            <p class="task-card-desc">{{ task.desc }}</p>
                        </div>
                        <button
                            type="button"
                            class="task-card-cta"
                            @click="handleTaskCtaClick(task)"
                        >{{ task.ctaText }}</button>
                    </article>
                </div>
            </section>

            <section v-if="isOverview" class="dashboard-analytics">
                <p v-if="dashboardLoading" class="loading-text">创作台数据加载中...</p>
                <p v-else-if="dashboardError" class="error-text">{{ dashboardError }}</p>
                <template v-else>
                    <div>
                        <header class="dashboard-panel-header">
                            <div>
                                <p class="eyebrow">趋势</p>
                                <h2>阅读与互动</h2>
                            </div>
                            <div class="status-tabs compact">
                                <button
                                    type="button"
                                    :class="{ active: trendRange === '7d' }"
                                    @click="changeTrendRange('7d')"
                                >
                                    7 天
                                </button>
                                <button
                                    type="button"
                                    :class="{ active: trendRange === '30d' }"
                                    @click="changeTrendRange('30d')"
                                >
                                    30 天
                                </button>
                            </div>
                        </header>
                        <div v-if="trends.length" class="trend-chart" aria-label="阅读和互动趋势">
                            <div v-for="point in trends" :key="point.date" class="trend-column">
                                <div class="trend-bars">
                                    <span
                                        class="trend-bar views"
                                        :style="{ height: `${Math.max(4, ((point.viewCount || 0) / trendMax) * 100)}%` }"
                                        :title="`阅读 ${point.viewCount || 0}`"
                                    ></span>
                                    <span
                                        class="trend-bar interactions"
                                        :style="{ height: `${Math.max(4, ((point.interactionCount || 0) / trendMax) * 100)}%` }"
                                        :title="`互动 ${point.interactionCount || 0}`"
                                    ></span>
                                </div>
                                <span>{{ formatTrendDate(point.date) }}</span>
                            </div>
                        </div>
                        <div class="trend-legend">
                            <span><i class="views"></i>阅读</span>
                            <span><i class="interactions"></i>互动</span>
                        </div>
                    </div>

                    <div class="dashboard-analytics-grid">
                        <section>
                            <header class="dashboard-panel-header">
                                <div>
                                    <p class="eyebrow">文章表现</p>
                                    <h2>表现最佳文章</h2>
                                </div>
                                <div class="status-tabs compact">
                                    <button
                                        v-for="option in performanceSortOptions"
                                        :key="option.value"
                                        type="button"
                                        :class="{ active: performanceSort === option.value }"
                                        @click="changePerformanceSort(option.value)"
                                    >
                                        {{ option.label }}
                                    </button>
                                </div>
                            </header>
                            <div v-if="performanceArticles.length" class="performance-list">
                                <RouterLink
                                    v-for="article in performanceArticles"
                                    :key="article.id"
                                    class="performance-item"
                                    :to="`/articles/${article.id}`"
                                >
                                    <div class="performance-head">
                                        <strong>{{ article.title }}</strong>
                                        <span class="performance-meta">{{ getArticleStatusLabel(article.status) }} · {{ formatDate(article.updatedAt) }}</span>
                                    </div>
                                    <div class="performance-metrics">
                                        <i>阅读 {{ article.viewCount }}</i>
                                        <i>赞 {{ article.likeCount }}</i>
                                        <i>收藏 {{ article.favoriteCount }}</i>
                                        <i>评论 {{ article.commentCount }}</i>
                                    </div>
                                </RouterLink>
                            </div>
                            <EmptyState
                                v-else
                                eyebrow="文章表现"
                                title="暂无文章数据"
                                description="发布文章后，这里会展示阅读、点赞、收藏和评论表现。"
                                compact
                            />
                        </section>

                        <section>
                            <header class="dashboard-panel-header">
                                <div>
                                    <p class="eyebrow">互动反馈</p>
                                    <h2>最近互动</h2>
                                </div>
                                <RouterLink class="creator-overview-inline-link" to="/notifications">通知中心</RouterLink>
                            </header>
                            <div v-if="interactions.length" class="interaction-list">
                                <RouterLink
                                    v-for="notification in interactions"
                                    :key="notification.id"
                                    class="interaction-item"
                                    :to="notification.targetUrl || '/notifications'"
                                >
                                    <strong>{{ notification.actor?.nickname || notification.actor?.username || '读者' }}</strong>
                                    <span>{{ getInteractionText(notification) }}</span>
                                    <small>{{ getInteractionDetail(notification) || notification.createdAt }}</small>
                                </RouterLink>
                            </div>
                            <EmptyState
                                v-else
                                eyebrow="互动反馈"
                                title="暂时还没有互动"
                                description="新的点赞、收藏、评论和关注会汇总在这里。"
                                compact
                            />
                        </section>
                    </div>
                    <section v-if="contentOpportunities.length" class="content-opportunity-panel">
                        <header class="dashboard-panel-header">
                            <div>
                                <p class="eyebrow">选题机会</p>
                                <h2>搜索驱动的内容建议</h2>
                            </div>
                            <RouterLink class="creator-overview-inline-link" to="/editor/new">开始写作</RouterLink>
                        </header>
                        <div class="content-opportunity-list">
                            <article
                                v-for="item in contentOpportunities"
                                :key="item.keyword || item.title"
                                class="content-opportunity-item"
                            >
                                <strong>{{ item.title }}</strong>
                                <span>{{ item.reason }}</span>
                            </article>
                        </div>
                    </section>
                </template>
            </section>

            <div v-if="isArticles" class="dashboard-toolbar">
                <div class="article-search-bar">
                    <div class="search-input-wrap">
                        <input
                            type="text"
                            :value="articleKeyword"
                            placeholder="搜索文章标题..."
                            @input="onKeywordInput"
                            @keydown.enter.prevent="doArticleSearch"
                        >
                        <button
                            v-if="articleKeyword"
                            type="button"
                            class="search-clear-btn"
                            @click="clearKeyword"
                        >✕</button>
                    </div>
                    <div class="admin-filter-actions">
                        <button type="button" @click="doArticleSearch">查询</button>
                        <button type="button" @click="resetArticleSearch">重置</button>
                    </div>
                </div>
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
                <div class="favorite-search-bar">
                    <div class="search-input-wrap">
                        <input
                            type="text"
                            :value="favoriteKeyword"
                            placeholder="搜索收藏文章标题..."
                            @input="onFavoriteKeywordInput"
                            @keydown.enter.prevent="doFavoriteSearch"
                        >
                        <button
                            v-if="favoriteKeyword"
                            type="button"
                            class="search-clear-btn"
                            @click="clearFavoriteKeyword"
                        >✕</button>
                    </div>
                    <div class="admin-filter-actions">
                        <button type="button" @click="doFavoriteSearch">查询</button>
                        <button type="button" @click="resetFavoriteSearch">重置</button>
                    </div>
                </div>
                <p v-if="feedback" :class="['form-message', feedbackType]">{{ feedback }}</p>
                <p v-if="refreshing && favorites.length" class="loading-text subtle">正在更新收藏...</p>
                <p v-if="inlineError" class="error-text">{{ inlineError }}</p>
                <p v-if="showListLoading" class="loading-text">加载中...</p>
                <EmptyState
                    v-else-if="errorMessage && !favorites.length"
                    tone="error"
                    compact
                    eyebrow="收藏夹"
                    title="收藏加载失败"
                    :description="errorMessage"
                >
                    <button type="button" class="empty-inline-action" :disabled="isLoading" @click="fetchFavorites()">
                        {{ isLoading ? '重试中...' : '重试加载' }}
                    </button>
                </EmptyState>
                <div v-else-if="favorites.length" class="favorite-list">
<article v-for="article in favorites" :key="article.id" class="favorite-list-item">
<RouterLink :to="`/articles/${article.id}`" class="favorite-list-cover">
    <img :src="article.cover" :alt="article.coverAlt" loading="lazy" decoding="async">
</RouterLink>
<div class="favorite-list-body">
<div class="favorite-list-info">
<h3>
    <RouterLink :to="`/articles/${article.id}`" class="favorite-list-title-link">{{ article.title }}</RouterLink>
</h3>
                                <span class="favorite-list-meta">
                                    <span v-if="article.category">{{ article.category }}</span>
                                    <span>作者：{{ article.author.name }}</span>
                                    <span>{{ article.favoritedAt || '刚刚收藏' }}</span>
                                </span>
                            </div>
                            <div class="favorite-list-actions">
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
                    v-else-if="showEmptyState"
                    eyebrow="收藏夹"
                    title="暂无收藏"
                    description="去首页、搜索或排行榜逛逛，把感兴趣的文章先收藏起来。"
                    compact
                >
                    <RouterLink class="empty-inline-link" to="/">去首页看看</RouterLink>
                    <RouterLink class="empty-inline-link" to="/search">去搜索发现</RouterLink>
                </EmptyState>
            </section>

            <section v-else-if="isArticles" class="dashboard-content-panel table-panel" data-testid="dashboard-articles-panel">
                <p v-if="refreshing && articles.length" class="loading-text subtle">正在更新文章...</p>
                <p v-if="inlineError" class="error-text">{{ inlineError }}</p>
                <p v-if="showListLoading" class="loading-text">正在加载文章...</p>
                <EmptyState
                    v-else-if="errorMessage && !articles.length"
                    tone="error"
                    compact
                    eyebrow="我的文章"
                    title="文章列表加载失败"
                    :description="errorMessage"
                >
                    <button type="button" class="empty-inline-action" :disabled="isLoading" @click="fetchArticles()">
                        {{ isLoading ? '重试中...' : '重试加载' }}
                    </button>
                </EmptyState>
                <table v-else-if="articles.length">
                    <thead>
                        <tr>
                            <th>标题</th>
                            <th>状态</th>
                            <th>阅读权限</th>
                            <th>阅读</th>
                            <th>点赞</th>
                            <th>收藏</th>
                            <th>评论</th>
                            <th>更新时间</th>
                            <th>定时发布时间</th>
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
                            <td class="article-unlock-cell">
                                <div class="article-unlock-rule">
                                    <span :class="['unlock-rule-pill', getArticleUnlockClass(article)]">
                                        {{ getArticleUnlockLabel(article) }}
                                    </span>
                                    <button
                                        v-if="article.status !== 'DELETED'"
                                        type="button"
                                        class="article-unlock-link"
                                        @click="openUnlockDialog(article)"
                                    >
                                        设置
                                    </button>
                                </div>
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
                            <td class="article-time-cell">
                                <span>{{ getScheduledPublishTimeParts(article.scheduledPublishAt).date }}</span>
                                <small v-if="getScheduledPublishTimeParts(article.scheduledPublishAt).time">
                                    {{ getScheduledPublishTimeParts(article.scheduledPublishAt).time }}
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
                                        v-else-if="article.status === 'DRAFT' || article.status === 'OFFLINE' || article.status === 'SCHEDULED'"
                                        type="button"
                                        class="action-link action-link-primary"
                                        :disabled="actionLoadingId === article.id"
                                        @click="publishOrOfflineArticle(article, 'PUBLISHED')"
                                    >
                                        {{ actionLoadingId === article.id ? '处理中...' : (article.status === 'SCHEDULED' ? '立即发布' : '发布') }}
                                    </button>
                                    <span v-else class="article-action-muted">不可操作</span>
                                </div>
                            </td>
                            <td class="article-action-cell">
                                <div class="article-inline-actions">
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
                                    <button
                                        type="button"
                                        class="action-link action-link-secondary"
                                        title="查看统计趋势"
                                        @click="openStatsDrawer(article)"
                                    >
                                        统计
                                    </button>
                                    <button
                                        v-if="article.status !== 'DELETED'"
                                        type="button"
                                        class="action-link action-link-danger"
                                        @click="removeArticle(article)"
                                    >
                                        删除
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <!-- Mobile card layout -->
                <div v-if="articles.length" class="article-cards">
                    <article v-for="article in articles" :key="article.id" class="article-card">
                        <div class="article-card-head">
                            <span class="status-pill" :class="getArticleStatusClass(article.status)">
                                {{ getArticleStatusLabel(article.status) }}
                            </span>
                            <span :class="['unlock-rule-pill', getArticleUnlockClass(article)]">
                                {{ getArticleUnlockLabel(article) }}
                            </span>
                            <span class="article-card-time">{{ getUpdatedTimeParts(article.updatedText).date }}</span>
                        </div>
                        <div class="article-card-title">{{ article.title }}</div>
                        <div class="article-card-times">
                            <span>更新时间 {{ formatDate(article.updatedAt) }}</span>
                            <span>定时发布时间 {{ formatScheduledPublishAt(article.scheduledPublishAt) }}</span>
                        </div>
                        <div class="article-card-metrics">
                            <span>阅读 {{ article.viewCount }}</span>
                            <span>赞 {{ article.likeCount }}</span>
                            <span>收藏 {{ article.favoriteCount }}</span>
                            <span>评论 {{ article.commentCount }}</span>
                        </div>
                        <div class="article-card-actions">
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
                                v-else-if="article.status === 'DRAFT' || article.status === 'OFFLINE' || article.status === 'SCHEDULED'"
                                type="button"
                                class="action-link action-link-primary"
                                :disabled="actionLoadingId === article.id"
                                @click="publishOrOfflineArticle(article, 'PUBLISHED')"
                            >
                                {{ actionLoadingId === article.id ? '处理中...' : (article.status === 'SCHEDULED' ? '立即发布' : '发布') }}
                            </button>
                            <RouterLink class="action-link action-link-secondary" :to="'/articles/' + article.id">
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
                            <button
                                v-if="article.status !== 'DELETED'"
                                type="button"
                                class="action-link action-link-secondary"
                                @click="openUnlockDialog(article)"
                            >
                                权限
                            </button>
                            <button
                                v-if="article.status !== 'DELETED'"
                                type="button"
                                class="action-link action-link-danger"
                                @click="removeArticle(article)"
                            >
                                删除
                            </button>
                        </div>
                    </article>
                </div>
                <EmptyState
                    v-if="showEmptyState"
                    eyebrow="内容管理"
                    title="还没有文章"
                    description="先去写下第一篇内容，把你的项目经验或技术笔记发布出来。"
                    compact
                >
                    <RouterLink class="empty-inline-link" to="/editor/new">去写第一篇</RouterLink>
                </EmptyState>
            </section>

            <nav v-if="!isOverview && totalPages > 1" class="dashboard-pagination" aria-label="后台分页">
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
        </section>
    </main>

    <Teleport to="body">
        <div
            v-if="unlockDialogVisible"
            class="unlock-rule-modal-overlay"
            @pointerdown="onUnlockOverlayPointerDown"
            @pointerup="onUnlockOverlayPointerUp"
            @pointercancel="resetUnlockOverlayPointer"
        >
            <section
                class="unlock-rule-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="unlock-rule-title"
            >
                <header class="unlock-rule-modal-header">
                    <div>
                        <p class="eyebrow">阅读权限</p>
                        <h2 id="unlock-rule-title" class="unlock-rule-modal-title">
                            {{ unlockDialogArticle?.title || '文章权限设置' }}
                        </h2>
                    </div>
                    <button
                        type="button"
                        class="unlock-rule-modal-close"
                        aria-label="关闭"
                        :disabled="unlockSaving"
                        @click="closeUnlockDialog"
                    >
                        ×
                    </button>
                </header>
                <div class="unlock-rule-modal-body">
                    <div class="unlock-option-grid" role="radiogroup" aria-label="阅读权限">
                        <label :class="['unlock-option-card', { active: !unlockForm.needUnlock }]">
                            <input
                                v-model="unlockForm.needUnlock"
                                type="radio"
                                :value="false"
                                :disabled="unlockSaving"
                                @change="onUnlockNeedUnlockChange"
                            >
                            <span>
                                <strong>免费阅读</strong>
                                <small>公开阅读全文</small>
                            </span>
                        </label>
                        <label :class="['unlock-option-card', { active: unlockForm.needUnlock }]">
                            <input
                                v-model="unlockForm.needUnlock"
                                type="radio"
                                :value="true"
                                :disabled="unlockSaving"
                                @change="onUnlockNeedUnlockChange"
                            >
                            <span>
                                <strong>积分解锁</strong>
                                <small>读者消耗积分后阅读全文</small>
                            </span>
                        </label>
                    </div>
                    <label class="unlock-point-field" for="unlock-point-price">
                        <span>解锁积分</span>
                        <input
                            id="unlock-point-price"
                            v-model.number="unlockForm.unlockPointPrice"
                            type="number"
                            :min="MIN_UNLOCK_POINT_PRICE"
                            :max="MAX_UNLOCK_POINT_PRICE"
                            step="1"
                            inputmode="numeric"
                            :disabled="!unlockForm.needUnlock || unlockSaving"
                            @change="onUnlockPointPriceCommit"
                            @blur="onUnlockPointPriceCommit"
                        >
                    </label>
                    <p v-if="unlockError" class="unlock-rule-error">{{ unlockError }}</p>
                </div>
                <footer class="unlock-rule-modal-actions">
                    <button type="button" class="action-link action-link-secondary" @click="closeUnlockDialog">
                        取消
                    </button>
                    <button
                        type="button"
                        class="action-link action-link-primary"
                        :disabled="unlockSaving"
                        @click="saveUnlockRule"
                    >
                        {{ unlockSaving ? '保存中...' : '保存设置' }}
                    </button>
                </footer>
            </section>
        </div>
    </Teleport>

    <!-- 文章统计趋势抽屉 -->
    <Teleport to="body">
        <div v-if="statsDrawerVisible" class="stats-drawer-overlay" @click.self="closeStatsDrawer">
            <aside class="stats-drawer" role="dialog" aria-modal="true" aria-label="文章数据统计">
                <header class="stats-drawer-header">
                    <div>
                        <p class="eyebrow">数据统计</p>
                        <h2 class="stats-drawer-title">{{ statsArticle?.title || '文章统计' }}</h2>
                    </div>
                    <button type="button" class="stats-drawer-close" aria-label="关闭" @click="closeStatsDrawer">✕</button>
                </header>
                <div class="stats-drawer-body">
                    <div v-if="statsLoading" class="stats-loading">加载中...</div>
                    <div v-else-if="statsError" class="stats-error">{{ statsError }}</div>
                    <template v-else-if="statsData">
                        <!-- 汇总指标 -->
                        <div class="stats-metrics-grid">
                            <div class="stats-metric-card">
                                <span class="stats-metric-value">{{ statsData.viewCount }}</span>
                                <span class="stats-metric-label">累计阅读</span>
                            </div>
                            <div class="stats-metric-card">
                                <span class="stats-metric-value">{{ statsData.likeCount }}</span>
                                <span class="stats-metric-label">累计点赞</span>
                            </div>
                            <div class="stats-metric-card">
                                <span class="stats-metric-value">{{ statsData.favoriteCount }}</span>
                                <span class="stats-metric-label">累计收藏</span>
                            </div>
                            <div class="stats-metric-card">
                                <span class="stats-metric-value">{{ statsData.commentCount }}</span>
                                <span class="stats-metric-label">累计评论</span>
                            </div>
                        </div>
                        <!-- 趋势图 -->
                        <div class="stats-trend-section">
                            <div class="stats-trend-header">
                                <span class="stats-trend-title">互动趋势</span>
                                <div class="stats-trend-range">
                                    <button
                                        type="button"
                                        :class="{ active: statsRange === '7d' }"
                                        @click="changeStatsRange('7d')"
                                    >7日</button>
                                    <button
                                        type="button"
                                        :class="{ active: statsRange === '30d' }"
                                        @click="changeStatsRange('30d')"
                                    >30日</button>
                                </div>
                            </div>
                            <div v-if="statsData.trends?.length" class="stats-trend-chart">
                                <div
                                    v-for="point in statsData.trends"
                                    :key="point.date"
                                    class="stats-trend-col"
                                    :title="`${point.date}\n阅读:${point.viewCount} 赞:${point.likeCount} 收:${point.favoriteCount} 评:${point.commentCount}`"
                                >
                                    <div class="stats-trend-bars">
                                        <div
                                            class="stats-trend-bar views"
                                            :style="{ height: `${Math.max(4, ((point.viewCount || 0) / statsTrendMax) * 100)}%` }"
                                        ></div>
                                        <div
                                            class="stats-trend-bar interactions"
                                            :style="{ height: `${Math.max(4, (((point.likeCount || 0) + (point.favoriteCount || 0) + (point.commentCount || 0)) / statsTrendMax) * 100)}%` }"
                                        ></div>
                                    </div>
                                    <span class="stats-trend-label">{{ point.date.slice(5) }}</span>
                                </div>
                            </div>
                            <p v-else class="stats-no-data">暂无趋势数据</p>
                            <div class="stats-legend">
                                <span class="stats-legend-item views">阅读</span>
                                <span class="stats-legend-item interactions">点赞+收藏+评论</span>
                            </div>
                        </div>
                    </template>
                </div>
            </aside>
        </div>
    </Teleport>
</template>

<style scoped>
.creator-overview {
    display: grid;
    gap: 16px;
    margin-bottom: 26px;
}

.task-center {
    display: grid;
    gap: 12px;
    margin-bottom: 20px;
}

.task-center .eyebrow {
    margin: 0;
}

.task-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 12px;
}

.task-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    border-left: 3px solid var(--brand);
}

.task-card-body {
    flex: 1;
    min-width: 0;
    display: grid;
    gap: 4px;
}

.task-card-title {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-strong);
}

.task-card-desc {
    margin: 0;
    font-size: 13px;
    color: var(--muted);
    line-height: 1.5;
}

.task-card-cta {
    flex-shrink: 0;
    min-height: 34px;
    padding: 0 14px;
    border: 1px solid var(--brand);
    border-radius: var(--radius-sm);
    background: var(--brand);
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: opacity 0.12s;
}

.task-card-cta:hover {
    opacity: 0.9;
}

.creator-overview-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

.creator-overview-grid.secondary {
    grid-template-columns: repeat(5, minmax(0, 1fr));
}

.creator-overview-card,
.creator-overview-latest {
    display: grid;
    gap: 8px;
    padding: 16px 18px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
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
    align-items: stretch;
    justify-content: space-between;
}

.creator-overview-latest-copy,
.creator-overview-next-step {
    display: grid;
    gap: 8px;
}

.creator-overview-latest-copy {
    min-width: 0;
}

.creator-overview-latest-main {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
}

.creator-overview-status {
    display: inline-flex;
    align-items: center;
    min-height: 28px;
    padding: 0 10px;
    color: var(--muted);
    font-size: 12px;
    font-weight: 700;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 999px;
}

.creator-overview-status.published {
    color: var(--brand-strong);
    background: var(--brand-soft);
    border-color: rgba(37, 99, 235, 0.16);
}

.creator-overview-status.draft {
    color: #9a6700;
    background: rgba(240, 201, 73, 0.12);
    border-color: rgba(212, 160, 23, 0.24);
}

.creator-overview-status.scheduled {
    color: #0f766e;
    background: rgba(20, 184, 166, 0.1);
    border-color: rgba(13, 148, 136, 0.18);
}

.creator-overview-status.offline {
    color: #8b5e00;
    background: rgba(245, 158, 11, 0.1);
    border-color: rgba(217, 119, 6, 0.18);
}

.creator-overview-inline-link {
    width: fit-content;
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
}

.creator-overview-next-step {
    min-width: 280px;
    padding: 16px 18px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.creator-overview-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 34px;
    padding: 0 12px;
    color: var(--brand);
    font-size: 13px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: color 0.12s, border-color 0.12s;
}

.dashboard-toolbar {
    display: grid;
    gap: 14px;
    margin-bottom: 20px;
}

.article-search-bar,
.favorite-search-bar {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
}

.search-input-wrap {
    position: relative;
    flex: 1;
    min-width: 180px;
    max-width: 320px;
}

.search-input-wrap input {
    width: 100%;
    min-height: 32px;
    padding: 0 28px 0 12px;
    color: var(--text);
    font-size: 13px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    outline: 0;
    transition: border-color 0.15s;
}

.search-input-wrap input:focus {
    border-color: var(--brand);
}

.search-input-wrap input::placeholder {
    color: var(--muted);
}

.search-clear-btn {
    position: absolute;
    right: 6px;
    top: 50%;
    transform: translateY(-50%);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    padding: 0;
    color: var(--muted);
    font-size: 11px;
    background: none;
    border: none;
    border-radius: 50%;
    cursor: pointer;
    transition: color 0.12s, background 0.12s;
}

.search-clear-btn:hover {
    color: var(--text);
    background: var(--surface-soft);
}

.status-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.status-tabs button {
    min-height: 32px;
    padding: 0 12px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    color: var(--muted);
    font-size: 13px;
    cursor: pointer;
    transition: color 0.12s, border-color 0.12s, background 0.12s;
}

.status-tabs button:hover {
    color: var(--text);
    border-color: var(--line-strong);
}

.status-tabs button.active {
    color: #ffffff;
    border-color: var(--brand);
    background: var(--brand);
}

.status-tabs.compact {
    justify-content: flex-end;
}

.status-tabs.compact button {
    min-height: 30px;
    padding: 0 10px;
}

.dashboard-analytics {
    display: grid;
    gap: 20px;
    margin-bottom: 26px;
}

.dashboard-analytics-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
}

.content-opportunity-panel {
    display: grid;
    gap: 10px;
}

.content-opportunity-list {
    display: grid;
    gap: 8px;
}

.content-opportunity-item {
    display: grid;
    gap: 4px;
    padding: 12px 0;
    border-bottom: 1px solid var(--line);
}

.content-opportunity-item:last-child {
    border-bottom: 0;
}

.content-opportunity-item strong {
    color: var(--text);
    font-size: 14px;
}

.content-opportunity-item span {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
}

.dashboard-panel-header {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
}

.dashboard-panel-header h2 {
    margin: 0;
    color: var(--text);
    font-size: 15px;
    font-weight: 700;
}

.trend-chart {
    display: grid;
    grid-auto-flow: column;
    grid-auto-columns: minmax(24px, 1fr);
    gap: 6px;
    min-height: 172px;
    align-items: end;
    overflow-x: auto;
    padding: 6px 0 2px;
}

.trend-column {
    display: grid;
    gap: 6px;
    min-width: 24px;
    color: var(--muted);
    font-size: 11px;
    text-align: center;
}

.trend-bars {
    display: flex;
    gap: 5px;
    align-items: end;
    justify-content: center;
    height: 132px;
    padding-top: 8px;
    border-bottom: 1px solid var(--line);
}

.trend-bar {
    display: block;
    width: 9px;
    min-height: 4px;
    border-radius: 3px 3px 0 0;
    transition: opacity 0.15s, transform 0.15s;
    cursor: default;
}

.trend-column:hover .trend-bar {
    opacity: 0.7;
}

.trend-column:hover .trend-bar:first-child {
    opacity: 1;
    transform: scaleX(1.15);
}

.trend-bar.views,
.trend-legend i.views {
    background: linear-gradient(180deg, var(--brand), color-mix(in srgb, var(--brand) 80%, transparent));
}

.trend-bar.interactions,
.trend-legend i.interactions {
    background: linear-gradient(180deg, var(--success), color-mix(in srgb, var(--success) 80%, transparent));
}

.trend-legend {
    display: flex;
    gap: 14px;
    color: var(--muted);
    font-size: 12px;
}

.trend-legend span {
    display: inline-flex;
    gap: 6px;
    align-items: center;
}

.trend-legend i {
    width: 10px;
    height: 10px;
    border-radius: 2px;
}

.performance-list,
.interaction-list {
    display: grid;
    gap: 6px;
}

.performance-item,
.interaction-item {
    display: grid;
    gap: 2px;
    padding: 8px 10px;
    margin: 0 -10px;
    color: inherit;
    text-decoration: none;
    border-radius: var(--radius-sm);
    border-bottom: 1px solid var(--line);
    transition: background 0.15s;
}

.performance-item {
    padding-left: 12px;
    border-left: 3px solid transparent;
}

.performance-item:hover {
    border-left-color: var(--brand);
}

.performance-item:hover,
.interaction-item:hover {
    background: var(--surface-soft);
}

.performance-item:last-child,
.interaction-item:last-child {
    border-bottom: 0;
}

.performance-item strong,
.interaction-item strong {
    color: var(--text);
    font-size: 14px;
    line-height: 1.45;
}

.performance-meta,
.interaction-item span,
.interaction-item small {
    color: var(--muted);
    line-height: 1.4;
}

.performance-head {
    display: flex;
    gap: 8px;
    align-items: baseline;
    justify-content: space-between;
}

.performance-head strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
}

.performance-meta {
    flex: none;
    font-size: 12px;
}

.performance-metrics {
    display: flex;
    flex-wrap: wrap;
    gap: 0;
    font-size: 12px;
    color: var(--muted);
}

.performance-metrics i {
    font-style: normal;
    line-height: 1.5;
}

.performance-metrics i + i::before {
    content: "·";
    margin: 0 6px;
    color: var(--line-strong);
}

.dashboard-feedback {
    display: flex;
    gap: 10px;
    align-items: center;
    padding: 10px 14px;
    margin: 0;
    border: 1px solid transparent;
    border-radius: var(--radius-sm);
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

.loading-text {
    margin: 0 0 12px;
    color: var(--muted);
    font-size: 13px;
}

.loading-text.subtle {
    display: inline-flex;
    width: fit-content;
    padding: 6px 10px;
    color: var(--brand-strong);
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-sm);
}

.table-panel :deep(tbody tr:hover td) {
    background: var(--surface-soft);
}

.table-panel table {
    min-width: 1060px;
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

.article-unlock-cell {
    min-width: 132px;
}

.article-unlock-rule {
    display: inline-flex;
    gap: 8px;
    align-items: center;
    flex-wrap: wrap;
}

.unlock-rule-pill {
    display: inline-flex;
    align-items: center;
    min-height: 22px;
    padding: 0 8px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    color: var(--muted);
    background: var(--surface-soft);
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
}

.unlock-rule-pill.paid {
    color: var(--brand-strong);
    background: var(--brand-soft);
    border-color: rgba(37, 99, 235, 0.22);
}

.article-unlock-link {
    min-height: 24px;
    padding: 0;
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
    background: none;
    border: 0;
    cursor: pointer;
}

.article-unlock-link:hover {
    color: var(--brand-strong);
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
}

.action-link:disabled {
    opacity: 0.62;
    cursor: not-allowed;
}

.action-link-primary {
    color: var(--brand);
    background: var(--surface);
    border-color: var(--brand);
}

.action-link-primary:hover:not(:disabled) {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.action-link-secondary {
    color: var(--text);
    background: var(--surface);
    border-color: var(--line);
}

.action-link-secondary:hover:not(:disabled) {
    color: var(--brand);
    border-color: var(--brand);
}

.action-link-danger {
    color: var(--accent);
    background: var(--surface);
    border-color: var(--accent);
}

.action-link-danger:hover:not(:disabled) {
    color: #ffffff;
    background: var(--accent);
    border-color: var(--accent);
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
    min-height: 32px;
    padding: 0 12px;
    color: var(--accent);
    background: var(--surface);
    border: 1px solid var(--accent);
    border-radius: var(--radius-sm);
    font-size: 13px;
    cursor: pointer;
    transition: color 0.12s, background 0.12s;
}

.btn-danger-secondary:hover:not(:disabled) {
    color: #ffffff;
    background: var(--accent);
}

.btn-danger-secondary:focus {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
}

.btn-danger-secondary:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.favorite-search-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    max-width: 420px;
    margin-bottom: 14px;
}

.favorite-search-bar input {
    flex: 1;
    min-width: 0;
    min-height: 36px;
    padding: 0 32px 0 12px;
    color: var(--text);
    font-size: 14px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    outline: 0;
    transition: border-color 0.15s;
}

.favorite-search-bar input:focus {
    border-color: var(--brand);
}

.favorite-search-bar input::placeholder {
    color: var(--muted);
}

.favorite-list {
    display: flex;
    flex-direction: column;
    gap: 0;
}

.favorite-list-item {
    display: grid;
    grid-template-columns: 80px minmax(0, 1fr);
    gap: 14px;
    align-items: center;
    padding: 12px 14px;
    margin: 0 -14px;
    border-bottom: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: background 0.12s, box-shadow 0.12s;
}

.favorite-list-item:hover {
    background: var(--surface-soft);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.favorite-list-item:last-child {
    border-bottom: none;
}

.favorite-list-cover {
    display: block;
    flex-shrink: 0;
    width: 80px;
    height: 56px;
    border-radius: var(--radius-sm);
    overflow: hidden;
}

.favorite-list-cover img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.2s ease;
}

.favorite-list-cover:hover img {
    transform: scale(1.05);
}

.favorite-list-body {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    min-width: 0;
}

.favorite-list-info {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.favorite-list-info h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    line-height: 1.45;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.favorite-list-title-link {
    color: var(--text);
    text-decoration: none;
    transition: color 0.12s;
}

.favorite-list-title-link:hover {
    color: var(--brand);
}

.favorite-list-meta {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    color: var(--muted);
    font-size: 12px;
}

.favorite-list-actions {
    display: flex;
    gap: 8px;
    align-items: center;
    flex-shrink: 0;
}

.favorite-list-actions a {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 30px;
    padding: 0 10px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    color: var(--text);
    font-size: 13px;
    text-decoration: none;
    transition: color 0.12s, border-color 0.12s;
}

.favorite-list-actions a:hover {
    color: var(--brand);
    border-color: var(--brand);
}

.dashboard-pagination {
    display: grid;
    gap: 10px;
    padding: 14px 0;
    background: transparent;
    border: 0;
    border-top: 1px solid var(--line);
    border-radius: 0;
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
    min-width: 32px;
    min-height: 30px;
    padding: 0 8px;
    color: var(--text);
    font-size: 13px;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: color 0.12s, border-color 0.12s;
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
    width: 60px;
    min-height: 30px;
    padding: 0 8px;
    color: var(--text);
    font-size: 13px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    outline: 0;
}

.dashboard-pagination-jump input:focus {
    border-color: var(--brand);
}

.status-pill.published {
    color: var(--brand-strong);
    background: var(--brand-soft);
    border-left-color: var(--brand);
}

.status-pill.draft {
    color: #9a6700;
    background: rgba(240, 201, 73, 0.12);
    border-left-color: #d4a017;
}

.status-pill.scheduled {
    color: #0f766e;
    background: rgba(20, 184, 166, 0.1);
    border-left-color: #0d9488;
}

.status-pill.offline {
    color: #8b5e00;
    background: rgba(245, 158, 11, 0.1);
    border-left-color: #d97706;
}

.status-pill.deleted {
    color: #b42318;
    background: rgba(180, 35, 24, 0.08);
    border-left-color: var(--accent);
}

@media (max-width: 760px) {
    .creator-overview-grid,
    .creator-overview-grid.secondary {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .dashboard-analytics-grid {
        grid-template-columns: 1fr;
    }

    .dashboard-panel-header {
        align-items: stretch;
        flex-direction: column;
        margin-bottom: 4px;
    }

    .status-tabs.compact {
        justify-content: flex-start;
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

/* ===== Mobile card layout (hidden on desktop) ===== */
.article-cards {
    display: none;
    grid-template-columns: 1fr;
    gap: 12px;
}

.article-card {
    display: grid;
    gap: 8px;
    padding: 14px 16px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.article-card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    flex-wrap: wrap;
}

.article-card-time {
    color: var(--muted);
    font-size: 12px;
}

.article-card-title {
    color: var(--text);
    font-size: 15px;
    font-weight: 700;
    line-height: 1.45;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.article-card-times {
    display: grid;
    gap: 4px;
    color: var(--muted);
    font-size: 12px;
}

.article-card-metrics {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    color: var(--muted);
    font-size: 12px;
}

.article-card-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding-top: 4px;
    border-top: 1px solid var(--line);
}

@media (max-width: 768px) {
    .table-panel table {
        display: none;
    }

    .article-cards {
        display: grid;
    }
}

@media (max-width: 560px) {
    .creator-overview-grid,
    .creator-overview-grid.secondary {
        grid-template-columns: 1fr;
    }
}

.unlock-rule-modal-overlay {
    position: fixed;
    inset: 0;
    z-index: 1200;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 18px;
    background: rgba(15, 23, 42, 0.46);
}

.unlock-rule-modal {
    width: min(460px, 100%);
    max-height: calc(100vh - 36px);
    overflow: auto;
    background: var(--surface, #fff);
    border: 1px solid var(--line, #e5e7eb);
    border-radius: var(--radius-sm);
    box-shadow: 0 22px 60px rgba(15, 23, 42, 0.18);
}

.unlock-rule-modal-header,
.unlock-rule-modal-actions {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 14px;
    padding: 18px 20px;
}

.unlock-rule-modal-header {
    border-bottom: 1px solid var(--line, #e5e7eb);
}

.unlock-rule-modal-title {
    margin: 0;
    color: var(--text);
    font-size: 16px;
    font-weight: 800;
    line-height: 1.45;
    overflow-wrap: anywhere;
}

.unlock-rule-modal-close {
    flex: 0 0 auto;
    width: 30px;
    height: 30px;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    cursor: pointer;
}

.unlock-rule-modal-close:disabled {
    cursor: not-allowed;
    opacity: 0.65;
}

.unlock-rule-modal-body {
    display: grid;
    gap: 16px;
    padding: 18px 20px 6px;
}

.unlock-option-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
}

.unlock-option-card {
    display: flex;
    gap: 10px;
    align-items: flex-start;
    min-height: 78px;
    padding: 13px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    cursor: pointer;
}

.unlock-option-card.active {
    border-color: rgba(37, 99, 235, 0.45);
    background: var(--brand-soft);
}

.unlock-option-card input {
    width: 16px;
    height: 16px;
    margin-top: 2px;
    accent-color: var(--brand);
}

.unlock-option-card span {
    display: grid;
    gap: 4px;
    min-width: 0;
}

.unlock-option-card strong {
    font-size: 14px;
    line-height: 1.3;
}

.unlock-option-card small {
    color: var(--muted);
    font-size: 12px;
    line-height: 1.45;
}

.unlock-point-field {
    display: grid;
    gap: 8px;
    color: var(--text);
    font-size: 13px;
    font-weight: 700;
}

.unlock-point-field input {
    width: 100%;
    min-height: 40px;
    padding: 0 12px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    outline: none;
}

.unlock-point-field input:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.unlock-point-field input:disabled {
    color: var(--muted);
    background: var(--surface-soft);
}

.unlock-rule-error {
    margin: 0;
    padding: 10px 12px;
    color: var(--danger, #dc2626);
    background: rgba(220, 38, 38, 0.08);
    border: 1px solid rgba(220, 38, 38, 0.18);
    border-radius: var(--radius-sm);
    font-size: 13px;
    line-height: 1.5;
}

.unlock-rule-modal-actions {
    justify-content: flex-end;
    border-top: 1px solid var(--line, #e5e7eb);
}

@media (max-width: 520px) {
    .unlock-rule-modal-overlay {
        align-items: flex-end;
        padding: 12px;
    }

    .unlock-option-grid {
        grid-template-columns: 1fr;
    }

    .unlock-rule-modal-actions {
        flex-direction: column-reverse;
    }

    .unlock-rule-modal-actions .action-link {
        width: 100%;
    }
}

/* ── 文章统计抽屉 ── */

.empty-inline-link,
.empty-inline-action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 32px;
    padding: 0 12px;
    margin: 2px 4px 0;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    color: var(--brand);
    font-size: 13px;
}

.empty-inline-action {
    cursor: pointer;
}

.empty-inline-action:disabled {
    cursor: not-allowed;
    color: var(--muted);
    border-color: var(--line);
    background: var(--surface-soft);
}

.empty-inline-link:hover,
.empty-inline-action:hover {
    border-color: var(--brand);
    background: var(--brand-soft);
}

.stats-drawer-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.45);
    z-index: 1100;
    display: flex;
    align-items: stretch;
    justify-content: flex-end;
}

.stats-drawer {
    width: min(480px, 100vw);
    height: 100%;
    background: var(--surface, #fff);
    display: flex;
    flex-direction: column;
    box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
    animation: slideInRight 0.22s ease;
}

@keyframes slideInRight {
    from { transform: translateX(100%); }
    to { transform: translateX(0); }
}

.stats-drawer-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 20px 24px 16px;
    border-bottom: 1px solid var(--line, #eee);
    gap: 12px;
}

.stats-drawer-title {
    font-size: 15px;
    font-weight: 700;
    color: var(--text);
    margin: 0;
    line-height: 1.4;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.stats-drawer-close {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 16px;
    color: var(--muted);
    padding: 4px;
    line-height: 1;
    flex-shrink: 0;
}

.stats-drawer-body {
    flex: 1;
    overflow-y: auto;
    padding: 20px 24px;
}

.stats-loading, .stats-error, .stats-no-data {
    text-align: center;
    color: var(--muted);
    padding: 32px 0;
    font-size: 14px;
}

.stats-error {
    color: var(--danger, #e53e3e);
}

.stats-metrics-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin-bottom: 24px;
}

.stats-metric-card {
    background: var(--bg, #f8f9fa);
    border-radius: 10px;
    padding: 14px 16px;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.stats-metric-value {
    font-size: 24px;
    font-weight: 800;
    color: var(--text);
    line-height: 1;
}

.stats-metric-label {
    font-size: 12px;
    color: var(--muted);
}

.stats-trend-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.stats-trend-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.stats-trend-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--text);
}

.stats-trend-range {
    display: flex;
    gap: 4px;
}

.stats-trend-range button {
    background: none;
    border: 1px solid var(--line, #ddd);
    border-radius: 6px;
    padding: 3px 10px;
    font-size: 12px;
    cursor: pointer;
    color: var(--muted);
    transition: all 0.15s;
}

.stats-trend-range button.active {
    background: var(--primary, #2563eb);
    border-color: var(--primary, #2563eb);
    color: #fff;
}

.stats-trend-chart {
    display: flex;
    align-items: flex-end;
    gap: 4px;
    height: 120px;
    padding-bottom: 20px;
    position: relative;
}

.stats-trend-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    height: 100%;
    cursor: default;
}

.stats-trend-bars {
    flex: 1;
    width: 100%;
    display: flex;
    align-items: flex-end;
    gap: 2px;
}

.stats-trend-bar {
    flex: 1;
    border-radius: 3px 3px 0 0;
    min-height: 4px;
    transition: height 0.3s ease;
}

.stats-trend-bar.views {
    background: var(--primary, #2563eb);
    opacity: 0.7;
}

.stats-trend-bar.interactions {
    background: var(--accent, #f59e0b);
    opacity: 0.85;
}

.stats-trend-label {
    font-size: 10px;
    color: var(--muted);
    margin-top: 4px;
    white-space: nowrap;
}

.stats-legend {
    display: flex;
    gap: 16px;
    font-size: 12px;
}

.stats-legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    color: var(--muted);
}

.stats-legend-item::before {
    content: '';
    display: inline-block;
    width: 12px;
    height: 8px;
    border-radius: 2px;
}

.stats-legend-item.views::before {
    background: var(--primary, #2563eb);
    opacity: 0.7;
}

.stats-legend-item.interactions::before {
    background: var(--accent, #f59e0b);
    opacity: 0.85;
}
</style>
