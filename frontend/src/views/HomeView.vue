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
import {useLoginModal} from '@/composables/useLoginModal';
import {useSession} from '@/stores/session';
import {track} from '@/utils/track';

const homeStats = ref({
    totalArticles: 0,
    totalAuthors: 0,
    totalColumns: 0
});
const pageSize = 10;
const HOME_RECOMMEND_FEED_CACHE_VERSION = 'recommend-algorithm-v2-20260511';
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
const activeSort = ref(ARTICLE_SORT_RECOMMEND);
const activeCategory = ref('');
const activeGroup = ref('');
const bootstrapLoaded = ref(false);
const taxonomyDrawerOpen = ref(false);
const selectedDrawerGroup = ref('');

const HOME_FEED_CHANNELS = [
    { key: 'recommend', label: '推荐', feedTab: 'recommend', sort: ARTICLE_SORT_RECOMMEND },
    { key: 'latest', label: '最新', feedTab: 'recommend', sort: ARTICLE_SORT_LATEST },
    { key: 'hot', label: '热门', feedTab: 'recommend', sort: ARTICLE_SORT_HOT }
];

const RECENT_READING_KEY = 'my-blog:recent-reading';

const categoryGroups = ref({});
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
const getFeedCacheVersion = (
    tab = feedTab.value,
    sort = activeSort.value
) => (
    tab === 'recommend' && normalizeArticleSort(sort) === ARTICLE_SORT_RECOMMEND
        ? HOME_RECOMMEND_FEED_CACHE_VERSION
        : ''
);
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
    group: '',
    sort: resolveDefaultSort(),
    cacheVersion: '',
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
const feedEyebrow = computed(() => activeGroup.value || activeCategory.value || '全部内容');
const sidebarTopicItems = computed(() => sidebarTopics.value.slice(0, 5));
const sidebarColumnItems = computed(() => sidebarColumns.value.slice(0, 4));
const categoryGroupList = computed(() =>
    Object.entries(categoryGroups.value || {}).map(([name, items]) => ({
        name,
        items: Array.isArray(items) ? items : []
    }))
);
const drawerCategories = computed(() => {
    const group = categoryGroupList.value.find((item) => item.name === selectedDrawerGroup.value);
    return group ? group.items : [];
});
const activeTaxonomyLabel = computed(() => {
    if (activeGroup.value && activeCategory.value) {
        return `${activeGroup.value} / ${activeCategory.value}`;
    }
    if (activeGroup.value) {
        return activeGroup.value;
    }
    if (activeCategory.value) {
        return activeCategory.value;
    }
    return '全部分类';
});
const activeTaxonomyHint = computed(() => {
    if (activeGroup.value && activeCategory.value) {
        return '当前小分类';
    }
    if (activeGroup.value) {
        return '当前分类组';
    }
    return '当前范围';
});

const buildPortalArticles = (focus = null, weekly = [], featured = []) => {
    const seenIds = new Set();
    const items = [];
    const append = (article) => {
        if (!article || seenIds.has(article.id)) {
            return;
        }
        seenIds.add(article.id);
        items.push(article);
    };
    append(focus);
    (weekly || []).forEach(append);
    (featured || []).forEach(append);
    return items.slice(0, 5);
};

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
            group: activeGroup.value || undefined,
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
        categoryGroups.value = bootstrap?.categoryGroups || {};
        featuredArticles.value = buildPortalArticles(
            bootstrap?.todayFocus,
            bootstrap?.weeklyArticles || [],
            bootstrap?.featuredArticles || []
        );
        if (featuredArticles.value.length) {
            portalFallbackArticles.value = [];
            portalArticles.value = featuredArticles.value;
        } else if (!portalArticles.value.length && portalFallbackArticles.value.length) {
            portalArticles.value = portalFallbackArticles.value;
        }
        sidebarTopics.value = bootstrap?.hotTopics || [];
        sidebarColumns.value = bootstrap?.recommendedColumns || [];
    } catch (error) {
        sidebarColumns.value = [];
        sidebarTopics.value = [];
    } finally {
        bootstrapLoaded.value = true;
    }
};

const normalizeCategory = (category) => (category && category !== '全部' ? category : '');
const normalizeGroup = (group) => (group && group !== '全部' ? group : '');

const syncSelectedDrawerGroup = () => {
    if (activeGroup.value && categoryGroupList.value.some((item) => item.name === activeGroup.value)) {
        selectedDrawerGroup.value = activeGroup.value;
        return;
    }
    if (!selectedDrawerGroup.value && categoryGroupList.value.length) {
        selectedDrawerGroup.value = categoryGroupList.value[0].name;
    }
};

const openTaxonomyDrawer = () => {
    syncSelectedDrawerGroup();
    taxonomyDrawerOpen.value = true;
};

const closeTaxonomyDrawer = () => {
    taxonomyDrawerOpen.value = false;
};

const selectAllTaxonomy = async () => {
    taxonomyDrawerOpen.value = false;
    await router.push({
        query: {
            ...route.query,
            group: undefined,
            category: undefined,
            page: undefined
        }
    });
};

const selectGroupTaxonomy = async (groupName) => {
    selectedDrawerGroup.value = groupName;
    taxonomyDrawerOpen.value = false;
    await router.push({
        query: {
            ...route.query,
            group: groupName || undefined,
            category: undefined,
            page: undefined
        }
    });
};

const selectCategoryTaxonomy = async (groupName, categoryName) => {
    taxonomyDrawerOpen.value = false;
    await router.push({
        query: {
            ...route.query,
            group: groupName || undefined,
            category: categoryName || undefined,
            page: undefined
        }
    });
};

const setPortalArticlesOnce = (items = []) => {
    const nextItems = (items || []).filter(Boolean).slice(0, 5);
    if (!portalArticles.value.length && nextItems.length) {
        portalArticles.value = nextItems;
    }
};

const setPortalFallbackArticles = (items = [], { tab, sort, category } = {}) => {
    const nextItems = (items || []).filter(Boolean).slice(0, 5);
    if (
        featuredArticles.value.length
        || portalFallbackArticles.value.length
        || tab !== 'recommend'
        || normalizeArticleSort(sort) !== ARTICLE_SORT_RECOMMEND
        || normalizeCategory(category)
    ) {
        return;
    }
    portalFallbackArticles.value = nextItems;
    if (!bootstrapLoaded.value) {
        return;
    }
    setPortalArticlesOnce(portalFallbackArticles.value);
};

const buildFeedCacheKey = (
    tab = feedTab.value,
    sort = activeSort.value,
    category = activeCategory.value,
    group = activeGroup.value
) => `home:${tab}:${normalizeArticleSort(sort)}:${normalizeCategory(category) || 'all'}:${normalizeGroup(group) || 'all'}`;

const getFeedCacheOptions = (
    tab = feedTab.value,
    sort = activeSort.value
) => {
    const cacheVersion = getFeedCacheVersion(tab, sort);
    return cacheVersion ? { cacheVersion } : {};
};

const saveActiveFeedCache = () => {
    feedCache.value[feedTab.value] = {
        ...feedCache.value[feedTab.value],
        ...getFeedState(),
        category: activeCategory.value,
        group: activeGroup.value,
        sort: activeSort.value,
        cacheVersion: getFeedCacheVersion(),
        loaded: true
    };
    saveFeedCache(buildFeedCacheKey(), getFeedCacheOptions());
};

const restoreMemoryFeedState = (state) => {
    setFeedState(state);
    activeCategory.value = state.category || '';
    activeSort.value = normalizeArticleSort(state.sort || resolveDefaultSort());
};

const loadArticles = async ({ sort, category } = {}) => {
    const targetSort = normalizeArticleSort(sort || resolveDefaultSort(feedTab.value));
    const normalizedCategory = normalizeCategory(category);
    const targetCacheVersion = getFeedCacheVersion(feedTab.value, targetSort);
    const normalizedGroup = normalizeGroup(activeGroup.value);
    const cachedState = feedCache.value[feedTab.value] || createFeedCacheState();
    if (
        cachedState.loaded
        && cachedState.category === normalizedCategory
        && (cachedState.group || '') === normalizedGroup
        && normalizeArticleSort(cachedState.sort) === targetSort
        && (cachedState.cacheVersion || '') === targetCacheVersion
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
    if (
        restoreFeedCache(
            buildFeedCacheKey(feedTab.value, targetSort, normalizedCategory, normalizedGroup),
            getFeedCacheOptions(feedTab.value, targetSort)
        )
    ) {
        activeSort.value = targetSort;
        activeCategory.value = normalizedCategory;
        feedCache.value[feedTab.value] = {
            ...feedCache.value[feedTab.value],
            ...getFeedState(),
            category: normalizedCategory,
            group: normalizedGroup,
            sort: targetSort,
            cacheVersion: targetCacheVersion,
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
        : () => listArticlesApi({page: 1, pageSize, sort: targetSort, category: normalizedCategory, group: normalizedGroup || undefined});
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
    const groupToSave = normalizeGroup(activeGroup.value);
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
    const normalizedGroup = normalizeGroup(activeGroup.value);
    const response = await loadMore(
        (page) => isFollowing
            ? getFollowingFeedApi({page, pageSize, sort: activeSort.value, category: activeCategory.value})
            : listArticlesApi({page, pageSize, sort: activeSort.value, category: activeCategory.value, group: normalizedGroup || undefined}),
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
            group: activeGroup.value || undefined,
            sort: getSortQueryValue(feedTab.value, targetSort),
            page: undefined
        }
    });
};

watch(
    () => [route.query.sort, route.query.category, route.query.group, route.query.feedTab],
    ([sort, category, group, queryFeedTab]) => {
        const nextFeedTab = VALID_FEED_TABS.includes(queryFeedTab) ? queryFeedTab : resolveDefaultFeedTab();

        // sync feedTab from query
        if (nextFeedTab !== feedTab.value) {
            feedTab.value = nextFeedTab;
        }

        activeGroup.value = String(group || '');
        loadArticles({
            sort: normalizeArticleSort(sort || resolveDefaultSort(nextFeedTab)),
            category: String(category || '')
        });
    },
    { immediate: true }
);

watch([activeGroup, categoryGroupList], () => {
    syncSelectedDrawerGroup();
});

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
        <section class="home-feed-toolbar" aria-label="内容频道">
            <div class="feed-taxonomy-control">
                <button
                    class="taxonomy-trigger"
                    type="button"
                    aria-haspopup="dialog"
                    :aria-expanded="taxonomyDrawerOpen"
                    @click="openTaxonomyDrawer"
                >
                    分类
                </button>
                <p class="feed-result-hint">
                    <span>{{ activeTaxonomyHint }}</span>
                    <strong>{{ activeTaxonomyLabel }}</strong>
                    <em>{{ total }}</em>
                </p>
            </div>
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
        </section>
        <div
            v-if="taxonomyDrawerOpen"
            class="taxonomy-drawer-backdrop"
            role="presentation"
            @click="closeTaxonomyDrawer"
        ></div>
        <aside
            v-if="taxonomyDrawerOpen"
            class="taxonomy-drawer"
            role="dialog"
            aria-modal="true"
            aria-label="选择文章分类"
        >
            <div class="taxonomy-drawer-head">
                <div>
                    <p class="taxonomy-drawer-eyebrow">文章分类</p>
                    <h3>{{ activeTaxonomyLabel }}</h3>
                </div>
                <button type="button" class="taxonomy-drawer-close" aria-label="关闭分类选择" @click="closeTaxonomyDrawer">×</button>
            </div>
            <button
                type="button"
                class="taxonomy-all-option"
                :class="{ active: !activeGroup && !activeCategory }"
                @click="selectAllTaxonomy"
            >
                <strong>全部分类</strong>
                <span>查看所有文章</span>
            </button>
            <div v-if="categoryGroupList.length" class="taxonomy-drawer-body">
                <nav class="taxonomy-group-list" aria-label="分类组">
                    <button
                        v-for="group in categoryGroupList"
                        :key="group.name"
                        type="button"
                        :class="{ active: selectedDrawerGroup === group.name }"
                        @click="selectedDrawerGroup = group.name"
                    >
                        <span>{{ group.name }}</span>
                        <em>{{ group.items.length }}</em>
                    </button>
                </nav>
                <section class="taxonomy-category-panel" aria-label="小分类">
                    <div class="taxonomy-category-panel-head">
                        <div>
                            <p>分类组</p>
                            <h4>{{ selectedDrawerGroup }}</h4>
                        </div>
                        <button type="button" @click="selectGroupTaxonomy(selectedDrawerGroup)">
                            查看本组全部
                        </button>
                    </div>
                    <div v-if="drawerCategories.length" class="taxonomy-category-grid">
                        <button
                            v-for="cat in drawerCategories"
                            :key="cat.id || cat.name"
                            type="button"
                            :class="{ active: activeCategory === cat.name }"
                            @click="selectCategoryTaxonomy(selectedDrawerGroup, cat.name)"
                        >
                            {{ cat.name }}
                        </button>
                    </div>
                    <p v-else class="taxonomy-empty">该分类组暂无启用小分类</p>
                </section>
            </div>
            <p v-else class="taxonomy-empty taxonomy-empty-standalone">
                暂无分类组，请稍后刷新或联系管理员维护分类。
            </p>
        </aside>
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

.feed-taxonomy-control {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
}

.taxonomy-trigger {
    min-height: 38px;
    padding: 0 16px;
    color: var(--brand-strong);
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    background: var(--brand-soft);
    border: 1px solid var(--brand);
    border-radius: var(--radius-sm);
    transition: background 0.12s, border-color 0.12s, transform 0.12s;
}

.taxonomy-trigger:hover {
    background: var(--surface);
    transform: translateY(-1px);
}

.feed-result-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0;
    color: var(--muted);
    font-size: 13px;
    white-space: nowrap;
}

.feed-result-hint strong {
    max-width: 260px;
    overflow: hidden;
    color: var(--text);
    font-size: 14px;
    text-overflow: ellipsis;
}

.feed-result-hint em {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 28px;
    height: 24px;
    padding: 0 8px;
    color: var(--brand-strong);
    font-style: normal;
    font-weight: 700;
    background: var(--brand-soft);
    border-radius: var(--radius-sm);
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

.home-feed-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--line);
}

.home-main-grid {
    align-items: start;
}

.taxonomy-drawer-backdrop {
    position: fixed;
    top: 61px;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 80;
    background: rgba(15, 23, 42, 0.36);
}

.taxonomy-drawer {
    position: fixed;
    top: 61px;
    bottom: 0;
    left: 0;
    z-index: 81;
    display: flex;
    flex-direction: column;
    width: min(760px, calc(100vw - 32px));
    padding: 24px;
    overflow-y: auto;
    background: var(--surface);
    border-right: 1px solid var(--line);
    box-shadow: 24px 0 48px rgba(15, 23, 42, 0.18);
}

.taxonomy-drawer-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;
}

.taxonomy-drawer-eyebrow,
.taxonomy-category-panel-head p {
    margin: 0 0 4px;
    color: var(--muted);
    font-size: 12px;
    font-weight: 700;
}

.taxonomy-drawer-head h3,
.taxonomy-category-panel-head h4 {
    margin: 0;
    color: var(--text);
    font-size: 22px;
    line-height: 1.25;
}

.taxonomy-drawer-close {
    width: 36px;
    height: 36px;
    padding: 0;
    color: var(--muted);
    cursor: pointer;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    font-size: 24px;
    line-height: 1;
}

.taxonomy-all-option {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    width: 100%;
    margin-bottom: 16px;
    padding: 14px 16px;
    text-align: left;
    cursor: pointer;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.taxonomy-all-option strong {
    color: var(--text);
    font-size: 16px;
}

.taxonomy-all-option span {
    color: var(--muted);
    font-size: 13px;
}

.taxonomy-all-option.active,
.taxonomy-group-list button.active,
.taxonomy-category-grid button.active {
    color: var(--brand-strong);
    background: var(--brand-soft);
    border-color: var(--brand);
}

.taxonomy-drawer-body {
    display: grid;
    grid-template-columns: 240px minmax(0, 1fr);
    gap: 16px;
    min-height: 0;
}

.taxonomy-group-list {
    display: grid;
    align-content: start;
    gap: 8px;
}

.taxonomy-group-list button {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    min-height: 42px;
    padding: 0 12px;
    color: var(--text);
    text-align: left;
    cursor: pointer;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.taxonomy-group-list span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.taxonomy-group-list em {
    color: var(--muted);
    font-size: 12px;
    font-style: normal;
}

.taxonomy-category-panel {
    min-width: 0;
    padding: 16px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.taxonomy-category-panel-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
}

.taxonomy-category-panel-head button {
    flex: 0 0 auto;
    min-height: 34px;
    padding: 0 12px;
    color: var(--brand-strong);
    font-weight: 700;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--brand);
    border-radius: var(--radius-sm);
}

.taxonomy-category-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
}

.taxonomy-category-grid button {
    min-height: 40px;
    padding: 0 12px;
    overflow: hidden;
    color: var(--text);
    text-overflow: ellipsis;
    white-space: nowrap;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.taxonomy-empty {
    margin: 0;
    padding: 18px;
    color: var(--muted);
    text-align: center;
    background: var(--surface-soft);
    border: 1px dashed var(--line);
    border-radius: var(--radius-sm);
}

.taxonomy-empty-standalone {
    margin-top: 12px;
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
    .home-feed-toolbar {
        align-items: flex-start;
        flex-direction: column;
        gap: 6px;
    }

    .feed-taxonomy-control {
        justify-content: space-between;
        width: 100%;
    }

    .feed-result-hint {
        flex: 1;
        justify-content: flex-end;
    }

    .feed-result-hint span {
        display: none;
    }

    .feed-result-hint strong {
        max-width: min(52vw, 220px);
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

    .taxonomy-drawer {
        top: auto;
        right: 0;
        left: 0;
        width: 100%;
        max-height: 88vh;
        padding: 18px;
        border-top: 1px solid var(--line);
        border-right: 0;
        border-radius: 16px 16px 0 0;
        box-shadow: 0 -18px 36px rgba(15, 23, 42, 0.18);
    }

    .taxonomy-drawer-backdrop {
        top: 0;
    }

    .taxonomy-drawer-head h3 {
        font-size: 18px;
    }

    .taxonomy-drawer-body {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .taxonomy-group-list {
        display: flex;
        gap: 8px;
        margin: 0 -18px;
        padding: 0 18px 4px;
        overflow-x: auto;
        scrollbar-width: none;
    }

    .taxonomy-group-list::-webkit-scrollbar {
        display: none;
    }

    .taxonomy-group-list button {
        flex: 0 0 auto;
        min-width: 112px;
    }

    .taxonomy-category-panel {
        padding: 14px;
    }

    .taxonomy-category-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}
</style>
