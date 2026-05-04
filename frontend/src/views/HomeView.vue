<script setup>
import {computed, nextTick, onMounted, ref, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import {listArticlesApi} from '@/api/articles';
import {getFollowingFeedApi} from '@/api/following';
import {getHomeBootstrapApi} from '@/api/home';
import {getActiveAnnouncementsApi} from '@/api/notifications';
import ArticleFeed from '@/components/ArticleFeed.vue';
import {useStableListRequest} from '@/composables/useStableListRequest';
import {useWindowSize} from '@/composables/useWindowSize';
import {
    ARTICLE_SORT_ITEMS,
    ARTICLE_SORT_LATEST,
    isDefaultArticleSort,
    normalizeArticleSort
} from '@/constants/articleSort';
import FeaturedArticlesStrip from '@/components/FeaturedArticlesStrip.vue';
import HomeIntro from '@/components/HomeIntro.vue';
import HomeSidebar from '@/components/HomeSidebar.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import TopicStrip from '@/components/TopicStrip.vue';
import {useSession} from '@/stores/session';
import {track} from '@/utils/track';

const homeStats = ref({
    totalArticles: 0,
    totalAuthors: 0,
    totalColumns: 0
});
const articles = ref([]);
const currentPage = ref(1);
const pageSize = 10;
const total = ref(0);
const featuredArticles = ref([]);
const sidebarColumns = ref([]);
const sidebarAuthors = ref([]);
const sidebarTopics = ref([]);
const activeSort = ref(ARTICLE_SORT_LATEST);
const activeCategory = ref('');
const bootstrapLoaded = ref(false);

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
const SIDEBAR_BREAKPOINT = 980;
const showSidebar = computed(() => windowWidth.value >= SIDEBAR_BREAKPOINT);

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
const tabStates = ref({
    recommend: { category: '', sort: undefined, page: undefined },
    following: { category: '', sort: undefined, page: undefined }
});

const switchFeedTab = async (tab) => {
    if (tab === feedTab.value) return;
    if (tab === 'following' && !isLoggedIn.value) {
        const {showLoginModal} = await import('@/composables/useLoginModal');
        showLoginModal(() => switchFeedTab('following'), {
            title: '登录后查看关注',
            message: '登录后可以查看你关注的创作者的最新文章。',
            actionText: '登录并查看'
        });
        return;
    }
    track('home_feed_tab_clicked', {from_tab: feedTab.value, to_tab: tab, is_login: isLoggedIn.value});
    feedTab.value = tab;
    const targetState = tabStates.value[tab] || {};
    await router.replace({
        query: {
            ...route.query,
            category: targetState.category || undefined,
            sort: targetState.sort,
            page: targetState.page,
            feedTab: tab === resolveDefaultFeedTab() ? undefined : tab,
        }
    });
};

let firstLoad = true;
let previousRouteState = {
    page: undefined,
    sort: undefined,
    category: undefined
};
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    loading,
    runStableRequest
} = useStableListRequest();

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const normalizePage = (value) => {
    const page = Number.parseInt(value, 10);
    return Number.isNaN(page) || page < 1 ? 1 : page;
};

const scrollToFeed = async () => {
    await nextTick();
    document.querySelector('[data-feed-root]')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
    });
};

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
        sidebarColumns.value = bootstrap?.recommendedColumns || [];
        sidebarAuthors.value = bootstrap?.authorRankings || [];
        sidebarTopics.value = bootstrap?.hotTopics || [];
    } catch (error) {
        topicItems.value = topicItems.value.length ? topicItems.value : DEFAULT_TOPIC_ITEMS;
        sidebarColumns.value = [];
        sidebarAuthors.value = [];
        sidebarTopics.value = [];
    } finally {
        bootstrapLoaded.value = true;
    }
};

const normalizeCategory = (category) => (category && category !== '全部' ? category : '');

const loadArticles = async (page, sort, category, shouldScroll = false) => {
    const isFollowing = feedTab.value === 'following' && isLoggedIn.value;
    const normalizedCategory = normalizeCategory(category);
    const apiCall = isFollowing
        ? () => getFollowingFeedApi({page, pageSize, sort, category: normalizedCategory})
        : () => listArticlesApi({page, pageSize, sort, category: normalizedCategory});
    const response = await runStableRequest(apiCall, {
        silent: hasLoadedOnce.value,
        initialErrorMessage: '文章列表加载失败，请稍后重试',
        refreshErrorMessage: '文章列表刷新失败，请稍后重试'
    });
    if (response?.ignored || response?.error) {
        return;
    }

    const pageResult = response.result || {};
    const nextItems = pageResult.items || [];
    const nextTotal = Number(pageResult.total || 0);
    const lastPage = Math.max(1, Math.ceil(nextTotal / pageSize));
    if (nextTotal > 0 && page > lastPage) {
        await router.replace({
            query: {
                ...route.query,
                category: normalizedCategory || undefined,
                sort: isDefaultArticleSort(sort) ? undefined : sort,
                page: lastPage === 1 ? undefined : String(lastPage)
            }
        });
        return;
    }

    currentPage.value = page;
    activeSort.value = sort;
    activeCategory.value = normalizedCategory;
    total.value = nextTotal;
    articles.value = nextItems;
    tabStates.value[feedTab.value] = {
        category: normalizedCategory,
        sort: isDefaultArticleSort(sort) ? undefined : sort,
        page: page === 1 ? undefined : String(page)
    };
    track('home_feed_list_loaded', {tab: feedTab.value, item_count: nextTotal, is_login: isLoggedIn.value});
    if (shouldScroll) {
        await scrollToFeed();
    }
};

const changePage = async (page) => {
    const targetPage = Math.min(Math.max(1, page), totalPages.value);
    if (loading.value || targetPage === currentPage.value) {
        return;
    }
    await router.push({
        query: {
            ...route.query,
            category: activeCategory.value || undefined,
            sort: isDefaultArticleSort(activeSort.value) ? undefined : activeSort.value,
            page: targetPage === 1 ? undefined : String(targetPage)
        }
    });
};

const changeSort = async (sort) => {
    const targetSort = normalizeArticleSort(sort);
    if (loading.value || targetSort === activeSort.value) {
        return;
    }
    await router.push({
        query: {
            ...route.query,
            category: activeCategory.value || undefined,
            sort: isDefaultArticleSort(targetSort) ? undefined : targetSort,
            page: undefined
        }
    });
};

watch(
    () => [route.query.page, route.query.sort, route.query.category, route.query.feedTab],
    ([page, sort, category, queryFeedTab]) => {
        const nextPage = page === undefined ? undefined : String(page);
        const nextSort = sort === undefined ? undefined : String(sort);
        const nextCategory = category === undefined ? undefined : String(category);
        const pageChanged = previousRouteState.page !== nextPage;
        const shouldScroll = !firstLoad && pageChanged;

        // sync feedTab from query
        const nextFeedTab = VALID_FEED_TABS.includes(queryFeedTab) ? queryFeedTab : resolveDefaultFeedTab();
        if (nextFeedTab !== feedTab.value) {
            feedTab.value = nextFeedTab;
        }

        previousRouteState = {
            page: nextPage,
            sort: nextSort,
            category: nextCategory
        };
        firstLoad = false;
        loadArticles(
            normalizePage(page),
            normalizeArticleSort(sort),
            String(category || ''),
            shouldScroll
        );
    },
    { immediate: true }
);

onMounted(() => {
    recentArticles.value = readRecentArticles();
    loadHomeBootstrap();
    loadBanners();
    track('home_feed_tab_exposed', {tab: feedTab.value, is_login: isLoggedIn.value});
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
        <section class="home-top-layout" aria-label="首页推荐内容">
            <HomeIntro
                :total-articles="homeStats.totalArticles"
                :total-authors="homeStats.totalAuthors"
                :total-columns="homeStats.totalColumns"
            />
            <FeaturedArticlesStrip
                class="home-featured-primary"
                :articles="featuredArticles"
                :loading="!bootstrapLoaded"
                @article-click="(payload) => track('home_featured_article_clicked', payload)"
            />
        </section>
        <section class="home-filter-bar" aria-label="内容筛选">
            <div class="home-filter-main">
                <div class="feed-tabs" role="tablist" aria-label="文章通道">
                <button
                    type="button"
                    role="tab"
                    :aria-selected="feedTab === 'recommend'"
                    :class="{ active: feedTab === 'recommend' }"
                    @click="switchFeedTab('recommend')"
                >推荐</button>
                <button
                    type="button"
                    role="tab"
                    :aria-selected="feedTab === 'following'"
                    :class="{ active: feedTab === 'following' }"
                    @click="switchFeedTab('following')"
                >关注</button>
                </div>
                <p class="feed-result-hint">
                    {{ feedTab === 'following' ? '关注' : '推荐' }} · {{ activeCategory || '全部' }}（{{ total }}）
                </p>
                <TopicStrip :topics="topicItems" :loading="!bootstrapLoaded" />
            </div>
        </section>
        <div class="content-grid">
            <ArticleFeed
                :articles="articles"
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
                :empty-text="feedTab === 'following' ? '关注的创作者暂无新文章，去推荐看看吧' : undefined"
                @page-change="changePage"
                @sort-change="changeSort"
            />
            <HomeSidebar
                v-if="showSidebar"
                :specials="sidebarColumns"
                :authors="sidebarAuthors"
                :topics="sidebarTopics"
            />
        </div>
    </main>
</template>

<style scoped>
.home-top-layout {
    display: grid;
    gap: 14px;
}

.feed-tabs {
    display: flex;
    gap: 4px;
    margin-bottom: 4px;
}

.feed-result-hint {
    margin: 0 0 6px;
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

.home-featured-primary {
    margin-bottom: 2px;
}

.recent-reading-strip {
    display: grid;
    gap: 12px;
    padding: 14px 16px;
    margin-bottom: 8px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.recent-reading-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.recent-reading-head .eyebrow {
    margin: 0;
}

.recent-reading-head a {
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
    text-decoration: none;
}

.recent-reading-list {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 10px;
}

.recent-reading-item {
    display: grid;
    grid-template-columns: 48px minmax(0, 1fr);
    gap: 10px;
    align-items: center;
    min-height: 48px;
    padding: 10px 12px;
    color: var(--text);
    text-decoration: none;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: border-color 0.15s, background 0.15s;
}

.recent-reading-item:hover {
    border-color: var(--brand-hover);
    background: var(--surface-soft);
}

.recent-reading-item img,
.recent-reading-fallback {
    width: 48px;
    height: 48px;
    border-radius: var(--radius-sm);
}

.recent-reading-item img {
    object-fit: cover;
}

.recent-reading-fallback {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 6px;
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
    background: var(--brand-soft);
    box-sizing: border-box;
}

.recent-reading-title {
    overflow: hidden;
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.home-filter-bar {
    display: block;
}

.home-filter-main {
    min-width: 0;
}

.announcement-banners {
    display: grid;
    gap: 8px;
    max-width: 1320px;
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

@media (max-width: 640px) {
    .recent-reading-strip {
        padding: 10px 12px;
        gap: 8px;
    }

    .recent-reading-list {
        grid-template-columns: 1fr;
        gap: 4px;
    }

    .recent-reading-item {
        grid-template-columns: 36px minmax(0, 1fr);
        gap: 8px;
        min-height: 40px;
        padding: 4px;
    }

    .recent-reading-item img,
    .recent-reading-fallback {
        width: 36px;
        height: 36px;
    }

    .recent-reading-title {
        font-size: 12px;
        -webkit-line-clamp: 1;
    }
}
</style>
