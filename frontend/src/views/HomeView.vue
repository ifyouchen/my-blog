<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {onBeforeRouteLeave, useRoute, useRouter} from 'vue-router';
import {listArticlesApi} from '@/api/articles';
import {getFollowingFeedApi} from '@/api/following';
import {getHomeBootstrapApi} from '@/api/home';
import {getActiveAnnouncementsApi} from '@/api/notifications';
import ArticleFeed from '@/components/ArticleFeed.vue';
import {useInfiniteArticleFeed} from '@/composables/useInfiniteArticleFeed';
import {useStableListRequest} from '@/composables/useStableListRequest';
import {useWindowSize} from '@/composables/useWindowSize';
import {
    ARTICLE_SORT_FEATURED,
    ARTICLE_SORT_HOT,
    ARTICLE_SORT_ITEMS,
    ARTICLE_SORT_LATEST,
    ARTICLE_SORT_RECOMMEND,
    isDefaultArticleSort,
    normalizeArticleSort
} from '@/constants/articleSort';
import HomePortalHero from '@/components/HomePortalHero.vue';
import HomeSidebar from '@/components/HomeSidebar.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import TopicStrip from '@/components/TopicStrip.vue';
import {useLoginModal} from '@/composables/useLoginModal';
import {useSession} from '@/stores/session';
import {track} from '@/utils/track';

const homeStats = ref({
    totalArticles: 0,
    totalAuthors: 0,
    totalColumns: 0
});
const pageSize = 10;
const {
    articles,
    currentPage,
    total,
    hasMore,
    loadingMore,
    loadMoreError,
    applyPageResult,
    getFeedState,
    setFeedState,
    resetFeed,
    saveFeedCache,
    restoreFeedCache,
    loadMore
} = useInfiniteArticleFeed({ pageSize });
const featuredArticles = ref([]);
const portalFallbackArticles = ref([]);
const portalArticles = ref([]);
const sidebarTopics = ref([]);
const sidebarColumns = ref([]);
const learningTopics = ref([]);
const knowledgeTags = ref([]);
const knowledgeKeywords = ref([]);
const activeSort = ref(ARTICLE_SORT_RECOMMEND);
const activeCategory = ref('');
const bootstrapLoaded = ref(false);

const HOME_FEED_CHANNELS = [
    { key: 'recommend', label: '推荐', feedTab: 'recommend', sort: ARTICLE_SORT_RECOMMEND },
    { key: 'latest', label: '最新', feedTab: 'recommend', sort: ARTICLE_SORT_LATEST },
    { key: 'hot', label: '热门', feedTab: 'recommend', sort: ARTICLE_SORT_HOT }
];

const HOME_TOPIC_CACHE_KEY = 'home-topic-items-v1';
const RECENT_READING_KEY = 'my-blog:recent-reading';
const DEFAULT_TOPIC_ITEMS = ['全部'];

const buildTopicItems = (categories = []) => {
    const categoryNames = categories.map((item) => item.name).filter(Boolean);
    return [...new Set([...DEFAULT_TOPIC_ITEMS, ...categoryNames])];
};

const readCachedTopicItems = () => {
    try {
        const cached = JSON.parse(localStorage.getItem(HOME_TOPIC_CACHE_KEY) || '[]');
        if (!Array.isArray(cached)) {
            return DEFAULT_TOPIC_ITEMS;
        }
        const items = cached
            .filter((item) => typeof item === 'string' && item.trim())
            .map((item) => item.trim());
        return items.length ? [...new Set([...DEFAULT_TOPIC_ITEMS, ...items])] : DEFAULT_TOPIC_ITEMS;
    } catch {
        return DEFAULT_TOPIC_ITEMS;
    }
};

const writeCachedTopicItems = (items) => {
    if (items.length > 1) {
        localStorage.setItem(HOME_TOPIC_CACHE_KEY, JSON.stringify(items));
    } else {
        localStorage.removeItem(HOME_TOPIC_CACHE_KEY);
    }
};

const topicItems = ref(readCachedTopicItems());
const readRecentArticles = () => {
    try {
        const cached = JSON.parse(localStorage.getItem(RECENT_READING_KEY) || '[]');
        return Array.isArray(cached)
            ? cached.filter((item) => item?.id && item?.title).slice(0, 5)
            : [];
    } catch {
        localStorage.removeItem(RECENT_READING_KEY);
        return [];
    }
};
const recentArticles = ref(readRecentArticles());

const {width: windowWidth} = useWindowSize();
const {isLoggedIn} = useSession();
const {showLoginModal} = useLoginModal();
const SIDEBAR_BREAKPOINT = 980;
const showSidebar = computed(() => windowWidth.value >= SIDEBAR_BREAKPOINT);
const useMobileFeaturedLarge = computed(() =>
    feedTab.value === 'recommend' && activeSort.value === ARTICLE_SORT_FEATURED
);

// 公告横幅
const activeBanners = ref([]);
const dismissedBannerIds = ref(new Set(JSON.parse(localStorage.getItem('dismissed-banners') || '[]')));
const visibleBanners = computed(() =>
    activeBanners.value.filter(b => !dismissedBannerIds.value.has(b.id))
);
const dismissBanner = (id) => {
    dismissedBannerIds.value.add(id);
    const ids = [...dismissedBannerIds.value];
    localStorage.setItem('dismissed-banners', JSON.stringify(ids));
};
const loadBanners = async () => {
    try {
        const list = await getActiveAnnouncementsApi();
        activeBanners.value = Array.isArray(list) ? list : [];
    } catch {}
};

const route = useRoute();
const router = useRouter();

// 双通道 feedTab
const VALID_FEED_TABS = ['recommend', 'following'];
const resolveDefaultFeedTab = () => 'recommend';
const feedTab = ref(VALID_FEED_TABS.includes(route.query.feedTab) ? route.query.feedTab : resolveDefaultFeedTab());
const resolveDefaultSort = (tab = feedTab.value) => (
    tab === 'following' ? ARTICLE_SORT_LATEST : ARTICLE_SORT_RECOMMEND
);
const getSortQueryValue = (tab, sort) => {
    const normalizedSort = normalizeArticleSort(sort || resolveDefaultSort(tab));
    if (normalizedSort === resolveDefaultSort(tab)) {
        return undefined;
    }
    return isDefaultArticleSort(normalizedSort) ? undefined : normalizedSort;
};
const getChannelSortQueryValue = (channel, sort) => {
    if (channel.key === 'recommend') {
        return undefined;
    }
    if (channel.key === 'latest') {
        return ARTICLE_SORT_LATEST;
    }
    return getSortQueryValue(channel.feedTab, sort);
};
const createFeedCacheState = () => ({
    items: [],
    page: 1,
    total: 0,
    category: '',
    sort: resolveDefaultSort(),
    loaded: false
});
const feedCache = ref({
    recommend: createFeedCacheState(),
    following: {
        ...createFeedCacheState(),
        sort: ARTICLE_SORT_LATEST
    }
});

const activeFeedChannelKey = computed(() => {
    if (feedTab.value === 'following') {
        return 'following';
    }
    if (activeSort.value === ARTICLE_SORT_HOT) {
        return 'hot';
    }
    if (route.query.sort === ARTICLE_SORT_LATEST) {
        return 'latest';
    }
    return 'recommend';
});
const activeFeedChannel = computed(() =>
    HOME_FEED_CHANNELS.find((item) => item.key === activeFeedChannelKey.value)
    || { key: 'following', label: '关注', feedTab: 'following', sort: ARTICLE_SORT_LATEST }
);
const feedTitle = computed(() => {
    if (activeFeedChannelKey.value === 'following') return '关注动态';
    if (activeFeedChannelKey.value === 'latest') return '最新文章';
    if (activeFeedChannelKey.value === 'hot') return '热门文章';
    return '推荐阅读';
});
const feedEyebrow = computed(() => activeCategory.value || '全部内容');
const sidebarTopicItems = computed(() => sidebarTopics.value.slice(0, 5));
const sidebarColumnItems = computed(() => sidebarColumns.value.slice(0, 4));
const knowledgeTopicItems = computed(() =>
    (learningTopics.value.length ? learningTopics.value : sidebarTopicItems.value).slice(0, 3)
);

const switchFeedTab = async (tab) => {
    if (tab === feedTab.value) return;
    if (tab === 'following' && !isLoggedIn.value) {
        showLoginModal(() => switchFeedTab('following'), {
            title: '登录后查看关注',
            message: '登录后可以查看你关注的创作者的最新文章。',
            actionText: '登录并查看'
        });
        return;
    }
    track('home_feed_tab_clicked', {from_tab: feedTab.value, to_tab: tab, is_login: isLoggedIn.value});
    feedTab.value = tab;
    const targetState = feedCache.value[tab] || createFeedCacheState();
    await router.replace({
        query: {
            ...route.query,
            category: targetState.category || undefined,
            sort: getSortQueryValue(tab, targetState.sort),
            page: undefined,
            feedTab: tab === resolveDefaultFeedTab() ? undefined : tab
        }
    });
};

const switchFeedChannel = async (channel) => {
    if (channel.feedTab === 'following') {
        await switchFeedTab('following');
        return;
    }
    const targetSort = normalizeArticleSort(channel.sort);
    if (loading.value || loadingMore.value || activeFeedChannelKey.value === channel.key) {
        return;
    }
    track('home_feed_tab_clicked', {
        from_tab: activeFeedChannelKey.value,
        to_tab: channel.key,
        is_login: isLoggedIn.value
    });
    feedTab.value = 'recommend';
    await router.push({
        query: {
            ...route.query,
            category: activeCategory.value || undefined,
            sort: getChannelSortQueryValue(channel, targetSort),
            page: undefined,
            feedTab: undefined
        }
    });
};

const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    loading,
    runStableRequest,
    resetStableRequest
} = useStableListRequest();

const loadHomeBootstrap = async () => {
    try {
        const bootstrap = await getHomeBootstrapApi();
        if (bootstrap?.stats) {
            homeStats.value = {
                totalArticles: bootstrap.stats.totalArticles || 0,
                totalAuthors: bootstrap.stats.totalAuthors || 0,
                totalColumns: bootstrap.stats.totalColumns || 0
            };
        }
        const nextTopicItems = buildTopicItems(bootstrap?.categories || []);
        topicItems.value = nextTopicItems;
        writeCachedTopicItems(nextTopicItems);
        featuredArticles.value = bootstrap?.featuredArticles || [];
        setPortalArticlesOnce(featuredArticles.value);
        sidebarTopics.value = bootstrap?.hotTopics || [];
        sidebarColumns.value = bootstrap?.recommendedColumns || [];
        learningTopics.value = bootstrap?.learningTopics || [];
        knowledgeTags.value = bootstrap?.hotTags || [];
        knowledgeKeywords.value = bootstrap?.hotKeywords || [];
    } catch (error) {
        topicItems.value = topicItems.value.length ? topicItems.value : DEFAULT_TOPIC_ITEMS;
        sidebarColumns.value = [];
        sidebarTopics.value = [];
        learningTopics.value = [];
        knowledgeTags.value = [];
        knowledgeKeywords.value = [];
    } finally {
        bootstrapLoaded.value = true;
    }
};

const normalizeCategory = (category) => (category && category !== '全部' ? category : '');

const setPortalArticlesOnce = (items = []) => {
    const nextItems = (items || []).filter(Boolean).slice(0, 5);
    if (!portalArticles.value.length && nextItems.length) {
        portalArticles.value = nextItems;
    }
};

const setPortalFallbackArticles = (items = [], { tab, sort, category } = {}) => {
    if (
        featuredArticles.value.length
        || portalFallbackArticles.value.length
        || tab !== 'recommend'
        || normalizeArticleSort(sort) !== ARTICLE_SORT_RECOMMEND
        || normalizeCategory(category)
    ) {
        return;
    }
    portalFallbackArticles.value = (items || []).filter(Boolean).slice(0, 5);
    setPortalArticlesOnce(portalFallbackArticles.value);
};

const buildFeedCacheKey = (
    tab = feedTab.value,
    sort = activeSort.value,
    category = activeCategory.value
) => `home:${tab}:${normalizeArticleSort(sort)}:${normalizeCategory(category) || 'all'}`;

const saveActiveFeedCache = () => {
    feedCache.value[feedTab.value] = {
        ...feedCache.value[feedTab.value],
        ...getFeedState(),
        category: activeCategory.value,
        sort: activeSort.value,
        loaded: true
    };
    saveFeedCache(buildFeedCacheKey());
};

const restoreMemoryFeedState = (state) => {
    setFeedState(state);
    activeCategory.value = state.category || '';
    activeSort.value = normalizeArticleSort(state.sort || resolveDefaultSort());
};

const loadArticles = async ({ sort, category } = {}) => {
    const targetSort = normalizeArticleSort(sort || resolveDefaultSort(feedTab.value));
    const normalizedCategory = normalizeCategory(category);
    const cachedState = feedCache.value[feedTab.value] || createFeedCacheState();
    if (
        cachedState.loaded
        && cachedState.category === normalizedCategory
        && normalizeArticleSort(cachedState.sort) === targetSort
    ) {
        resetStableRequest();
        resetFeed();
        restoreMemoryFeedState(cachedState);
        setPortalFallbackArticles(cachedState.items, {
            tab: feedTab.value,
            sort: targetSort,
            category: normalizedCategory
        });
        return;
    }
    if (restoreFeedCache(buildFeedCacheKey(feedTab.value, targetSort, normalizedCategory))) {
        activeSort.value = targetSort;
        activeCategory.value = normalizedCategory;
        feedCache.value[feedTab.value] = {
            ...feedCache.value[feedTab.value],
            ...getFeedState(),
            category: normalizedCategory,
            sort: targetSort,
            loaded: true
        };
        setPortalFallbackArticles(articles.value, {
            tab: feedTab.value,
            sort: targetSort,
            category: normalizedCategory
        });
        return;
    }

    resetStableRequest();
    resetFeed();
    const isFollowing = feedTab.value === 'following' && isLoggedIn.value;
    const apiCall = isFollowing
        ? () => getFollowingFeedApi({page: 1, pageSize, sort: targetSort, category: normalizedCategory})
        : () => listArticlesApi({page: 1, pageSize, sort: targetSort, category: normalizedCategory});
    const response = await runStableRequest(apiCall, {
        silent: hasLoadedOnce.value,
        initialErrorMessage: '文章列表加载失败，请稍后重试',
        refreshErrorMessage: '文章列表刷新失败，请稍后重试'
    });
    if (response?.ignored || response?.error) {
        return;
    }

    const pageResult = response.result || {};
    activeSort.value = targetSort;
    activeCategory.value = normalizedCategory;
    applyPageResult(pageResult);
    setPortalFallbackArticles(pageResult.items || articles.value, {
        tab: feedTab.value,
        sort: targetSort,
        category: normalizedCategory
    });
    saveActiveFeedCache();
    track('home_feed_list_loaded', {tab: feedTab.value, item_count: total.value, is_login: isLoggedIn.value});
};

const loadMoreArticles = async () => {
    const isFollowing = feedTab.value === 'following' && isLoggedIn.value;
    const response = await loadMore(
        (page) => isFollowing
            ? getFollowingFeedApi({page, pageSize, sort: activeSort.value, category: activeCategory.value})
            : listArticlesApi({page, pageSize, sort: activeSort.value, category: activeCategory.value}),
        { errorMessage: '文章列表加载失败，请稍后重试' }
    );
    if (response?.result) {
        saveActiveFeedCache();
    }
};

const changeSort = async (sort) => {
    const targetSort = normalizeArticleSort(sort);
    if (loading.value || loadingMore.value || targetSort === activeSort.value) {
        return;
    }
    await router.push({
        query: {
            ...route.query,
            category: activeCategory.value || undefined,
            sort: getSortQueryValue(feedTab.value, targetSort),
            page: undefined
        }
    });
};

watch(
    () => [route.query.sort, route.query.category, route.query.feedTab],
    ([sort, category, queryFeedTab]) => {
        const nextFeedTab = VALID_FEED_TABS.includes(queryFeedTab) ? queryFeedTab : resolveDefaultFeedTab();

        // sync feedTab from query
        if (nextFeedTab !== feedTab.value) {
            feedTab.value = nextFeedTab;
        }

        loadArticles({
            sort: normalizeArticleSort(sort || resolveDefaultSort(nextFeedTab)),
            category: String(category || '')
        });
    },
    { immediate: true }
);

onMounted(() => {
    recentArticles.value = readRecentArticles();
    loadHomeBootstrap();
    loadBanners();
    track('home_feed_tab_exposed', {tab: feedTab.value, is_login: isLoggedIn.value});
});

onBeforeRouteLeave(() => {
    saveActiveFeedCache();
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell" data-testid="home-page">
        <!-- 公告横幅 -->
        <div v-if="visibleBanners.length" class="announcement-banners">
            <div
                v-for="banner in visibleBanners"
                :key="banner.id"
                class="announcement-banner"
            >
                <span class="announcement-banner-icon">📢</span>
                <strong class="announcement-banner-title">{{ banner.title }}</strong>
                <span class="announcement-banner-content">{{ banner.content }}</span>
                <button
                    class="announcement-banner-close"
                    type="button"
                    aria-label="关闭公告"
                    @click="dismissBanner(banner.id)"
                >×</button>
            </div>
        </div>
        <HomePortalHero
            :articles="portalArticles"
            :topics="sidebarTopicItems"
            :columns="sidebarColumnItems"
            :stats="homeStats"
            :loading="!bootstrapLoaded"
            @article-click="(payload) => track('home_featured_article_clicked', payload)"
        />
        <section
            v-if="knowledgeTopicItems.length || knowledgeTags.length || knowledgeKeywords.length"
            class="knowledge-map-band"
            aria-label="知识地图"
        >
            <div class="knowledge-map-heading">
                <p class="eyebrow">知识地图</p>
                <h2>按专题、标签和搜索热度继续深入</h2>
            </div>
            <div class="knowledge-map-grid">
                <RouterLink
                    v-for="topic in knowledgeTopicItems"
                    :key="`topic-${topic.id}`"
                    class="knowledge-topic-link"
                    :to="`/topics/${topic.id}`"
                >
                    <span>{{ topic.difficulty || 'INTERMEDIATE' }}</span>
                    <strong>{{ topic.title }}</strong>
                    <small>{{ topic.articleCount }} 篇文章</small>
                </RouterLink>
                <div v-if="knowledgeTags.length" class="knowledge-chip-group">
                    <RouterLink
                        v-for="tag in knowledgeTags.slice(0, 8)"
                        :key="`tag-${tag.id || tag.name}`"
                        class="knowledge-chip"
                        :to="{ path: '/search', query: { tag: tag.name } }"
                    >
                        {{ tag.name }}
                    </RouterLink>
                </div>
                <div v-if="knowledgeKeywords.length" class="knowledge-chip-group">
                    <RouterLink
                        v-for="keyword in knowledgeKeywords.slice(0, 8)"
                        :key="`keyword-${keyword}`"
                        class="knowledge-chip hot"
                        :to="{ path: '/search', query: { keyword } }"
                    >
                        {{ keyword }}
                    </RouterLink>
                </div>
            </div>
        </section>
        <section class="home-channel-bar" aria-label="频道导航" data-testid="home-channel-bar">
            <TopicStrip :topics="topicItems" :loading="!bootstrapLoaded" />
        </section>
        <section class="home-feed-toolbar" aria-label="内容频道">
            <div class="feed-tabs" role="tablist" aria-label="文章通道">
                <button
                    v-for="channel in HOME_FEED_CHANNELS"
                    :key="channel.key"
                    type="button"
                    role="tab"
                    :aria-selected="activeFeedChannelKey === channel.key"
                    :class="{ active: activeFeedChannelKey === channel.key }"
                    @click="switchFeedChannel(channel)"
                >{{ channel.label }}</button>
            </div>
            <p class="feed-result-hint">
                {{ activeFeedChannel.label }} · {{ activeCategory || '全部' }}（{{ total }}）
            </p>
        </section>
        <div class="content-grid home-main-grid">
            <ArticleFeed
                :articles="articles"
                :eyebrow="feedEyebrow"
                :title="feedTitle"
                :page="currentPage"
                :page-size="pageSize"
                :total="total"
                :loading="loading"
                :initial-loading="initialLoading"
                :refreshing="refreshing"
                :has-loaded-once="hasLoadedOnce"
                :error-message="errorMessage"
                :inline-error-message="inlineError"
                :sort="activeSort"
                :sort-items="ARTICLE_SORT_ITEMS"
                hide-sort
                pagination-mode="infinite"
                :has-more="hasMore"
                :loading-more="loadingMore"
                :load-more-error="loadMoreError"
                :mobile-featured-large="useMobileFeaturedLarge"
                :empty-text="feedTab === 'following' ? '关注的创作者暂无新文章，去推荐看看吧' : undefined"
                @load-more="loadMoreArticles"
                @sort-change="changeSort"
            />
            <HomeSidebar
                v-if="showSidebar"
                :topics="sidebarTopicItems"
                :columns="sidebarColumnItems"
                :recent-articles="recentArticles"
            />
        </div>
    </main>
</template>

<style scoped>
.feed-tabs {
    display: flex;
    gap: 4px;
    margin-bottom: 4px;
}

.feed-result-hint {
    margin: 0;
    font-size: 12px;
    color: var(--muted);
}

.feed-tabs button {
    position: relative;
    padding: 8px 16px;
    border: none;
    background: transparent;
    cursor: pointer;
    color: var(--muted);
    font-size: 15px;
    font-weight: 600;
    border-radius: var(--radius-sm);
    transition: color 0.12s, background 0.12s;
}

.feed-tabs button.active {
    color: var(--brand-strong);
    background: var(--brand-soft);
}

.feed-tabs button:hover:not(.active) {
    color: var(--text);
    background: var(--surface-soft);
}

.home-channel-bar {
    margin-bottom: 8px;
}

.home-feed-toolbar {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--line);
}

.home-main-grid {
    align-items: start;
}

.knowledge-map-band {
    display: grid;
    gap: 14px;
    margin-bottom: 18px;
    padding: 18px 0;
    border-top: 1px solid var(--line);
    border-bottom: 1px solid var(--line);
}

.knowledge-map-heading {
    display: flex;
    align-items: end;
    justify-content: space-between;
    gap: 16px;
}

.knowledge-map-heading h2,
.knowledge-map-heading p {
    margin: 0;
}

.knowledge-map-heading h2 {
    color: var(--text-strong);
    font-size: 18px;
}

.knowledge-map-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
}

.knowledge-topic-link {
    display: grid;
    gap: 6px;
    min-width: 0;
    padding: 12px 14px;
    color: inherit;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.knowledge-topic-link:hover {
    border-color: var(--brand);
}

.knowledge-topic-link span,
.knowledge-topic-link small {
    color: var(--muted);
    font-size: 12px;
}

.knowledge-topic-link strong {
    overflow: hidden;
    color: var(--text-strong);
    font-size: 15px;
    line-height: 1.45;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.knowledge-chip-group {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-content: start;
}

.knowledge-chip {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0 10px;
    color: var(--text);
    font-size: 13px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.knowledge-chip.hot {
    color: #9a3412;
    background: #fff7ed;
    border-color: #fed7aa;
}

.knowledge-chip:hover {
    color: var(--brand);
    border-color: var(--brand);
    background: var(--brand-soft);
}

.announcement-banners {
    display: grid;
    gap: 8px;
    margin: 0 auto 12px;
}

.announcement-banner {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 12px 16px;
    background: var(--brand-soft);
    border: 1px solid var(--brand);
    border-radius: var(--radius-sm);
    font-size: 14px;
    line-height: 1.55;
}

.announcement-banner-icon {
    flex-shrink: 0;
    font-size: 16px;
}

.announcement-banner-title {
    flex-shrink: 0;
    color: var(--brand-strong);
    font-weight: 700;
}

.announcement-banner-content {
    flex: 1;
    color: var(--text);
    min-width: 0;
    word-break: break-word;
}

.announcement-banner-close {
    flex-shrink: 0;
    width: 22px;
    height: 22px;
    padding: 0;
    color: var(--muted);
    cursor: pointer;
    background: transparent;
    border: 0;
    border-radius: 50%;
    font-size: 18px;
    line-height: 1;
    transition: color 0.12s, background 0.12s;
}

.announcement-banner-close:hover {
    color: var(--text);
    background: var(--surface-muted);
}

@media (max-width: 720px) {
    .home-channel-bar {
        margin-bottom: 6px;
    }

    .home-feed-toolbar {
        align-items: flex-start;
        flex-direction: column;
        gap: 6px;
    }

    .knowledge-map-heading {
        align-items: flex-start;
        flex-direction: column;
        gap: 6px;
    }

    .knowledge-map-grid {
        grid-template-columns: 1fr;
    }

    .feed-tabs {
        width: 100%;
        overflow-x: auto;
        scrollbar-width: none;
    }

    .feed-tabs::-webkit-scrollbar {
        display: none;
    }

    .feed-tabs button {
        flex: 0 0 auto;
    }
}
</style>
