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
    applyHomepageRecommendationApi,
    deleteArticleApi,
    exportMyArticlesApi,
    getMyArticlesApi,
    updateArticleStatusApi,
    updateArticleUnlockRuleApi
} from '@/api/articles';
import {getMyGrowthApi} from '@/api/growth';
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
import {getNotificationDetail, getNotificationTargetUrl, getNotificationText} from '@/utils/notifications';
import {track} from '@/utils/track';

const route = useRoute();
const router = useRouter();
const { state: session, isLoggedIn } = useSession();
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
const recommendationApplyingId = ref(null);
const jumpPage = ref(String(currentPage.value));
const unlockDialogVisible = ref(false);
const unlockDialogArticle = ref(null);
const growthAccess = ref(null);
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
const RECOMMENDATION_STATUS_LABELS = {
    PENDING: '推荐申请审核中',
    APPROVED: '已通过推荐申请',
    REJECTED: '推荐申请已拒绝'
};
const creatorPrivilegeCodes = computed(() => (
    growthAccess.value?.ownedPrivilegeCodes
    || session.user?.privilegeCodes
    || []
));
const hasPaidArticlePrivilege = computed(() => creatorPrivilegeCodes.value.includes('PAID_ARTICLE_PUBLISH'));
const hasHomepageRecommendationPrivilege = computed(() => creatorPrivilegeCodes.value.includes('HOMEPAGE_RECOMMEND_ELIGIBLE'));

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
        growthAccess.value = null;
        return;
    }
    await loadCreatorPrivileges();
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

const loadCreatorPrivileges = async () => {
    try {
        growthAccess.value = await getMyGrowthApi();
    } catch {
        growthAccess.value = growthAccess.value || null;
    }
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
    if (unlockForm.value.needUnlock && !hasPaidArticlePrivilege.value) {
        unlockForm.value.needUnlock = false;
        unlockError.value = '当前等级未解锁付费文章发布权限，达到 Lv.4 后可开启积分解锁。';
        return;
    }
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
    if (unlockForm.value.needUnlock && !hasPaidArticlePrivilege.value) {
        unlockError.value = '当前等级未解锁付费文章发布权限，达到 Lv.4 后可开启积分解锁。';
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

const getRecommendationStatusLabel = (status) => RECOMMENDATION_STATUS_LABELS[status] || '';

const canApplyRecommendation = (article) => (
    article?.status === 'PUBLISHED'
    && hasHomepageRecommendationPrivilege.value
    && article?.recommendationApplicationStatus !== 'PENDING'
    && article?.recommendationApplicationStatus !== 'APPROVED'
);

const submitRecommendationApplication = async (article) => {
    if (!article?.id || recommendationApplyingId.value === article.id || !canApplyRecommendation(article)) {
        return;
    }
    recommendationApplyingId.value = article.id;
    feedback.value = '';
    try {
        const result = await applyHomepageRecommendationApi(article.id);
        articles.value = articles.value.map((item) => (
            String(item.id) === String(article.id)
                ? {
                    ...item,
                    recommendationApplicationId: result.id,
                    recommendationApplicationStatus: result.status
                }
                : item
        ));
        feedback.value = `《${article.title}》已提交首页推荐申请`;
        feedbackType.value = 'success';
    } catch (error) {
        feedback.value = error.message || '提交首页推荐申请失败';
        feedbackType.value = 'error';
    } finally {
        recommendationApplyingId.value = null;
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
                                    :to="getNotificationTargetUrl(notification) || '/notifications'"
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
                                        v-if="canApplyRecommendation(article)"
                                        type="button"
                                        class="action-link action-link-secondary"
                                        :disabled="recommendationApplyingId === article.id"
                                        @click="submitRecommendationApplication(article)"
                                    >
                                        {{
                                            recommendationApplyingId === article.id
                                                ? '提交中...'
                                                : (article.recommendationApplicationStatus === 'REJECTED'
                                                    ? '重新申请推荐'
                                                    : '申请首页推荐')
                                        }}
                                    </button>
                                    <span
                                        v-else-if="article.status === 'PUBLISHED' && article.recommendationApplicationStatus"
                                        class="article-action-muted"
                                    >
                                        {{ getRecommendationStatusLabel(article.recommendationApplicationStatus) }}
                                    </span>
                                    <span
                                        v-else-if="article.status === 'PUBLISHED' && !hasHomepageRecommendationPrivilege"
                                        class="article-action-muted"
                                    >
                                        Lv.7 解锁首页推荐申请资格
                                    </span>
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
                                v-if="canApplyRecommendation(article)"
                                type="button"
                                class="action-link action-link-secondary"
                                :disabled="recommendationApplyingId === article.id"
                                @click="submitRecommendationApplication(article)"
                            >
                                {{
                                    recommendationApplyingId === article.id
                                        ? '提交中...'
                                        : (article.recommendationApplicationStatus === 'REJECTED'
                                            ? '重新申请推荐'
                                            : '申请推荐')
                                }}
                            </button>
                            <span
                                v-else-if="article.status === 'PUBLISHED' && article.recommendationApplicationStatus"
                                class="article-action-muted"
                            >
                                {{ getRecommendationStatusLabel(article.recommendationApplicationStatus) }}
                            </span>
                            <span
                                v-else-if="article.status === 'PUBLISHED' && !hasHomepageRecommendationPrivilege"
                                class="article-action-muted"
                            >
                                Lv.7 解锁推荐资格
                            </span>
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
                    <p
                        v-if="!hasPaidArticlePrivilege"
                        class="unlock-rule-tip"
                    >
                        当前等级未解锁付费文章发布权限，达到 Lv.4 后可开启积分解锁。
                    </p>
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
                                :disabled="unlockSaving || !hasPaidArticlePrivilege"
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
                            :disabled="!unlockForm.needUnlock || unlockSaving || !hasPaidArticlePrivilege"
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

<style scoped src="@/styles/views/DashboardView.part-1.css"></style>
<style scoped src="@/styles/views/DashboardView.part-2.css"></style>
<style scoped src="@/styles/views/DashboardView.part-3.css"></style>
<style scoped src="@/styles/views/DashboardView.part-4.css"></style>
<style scoped src="@/styles/views/DashboardView.part-5.css"></style>
<style scoped src="@/styles/views/DashboardView.part-6.css"></style>
