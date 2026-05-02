<script setup>
import {computed, nextTick, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {listArticlesApi} from '@/api/articles';
import {getHomeBootstrapApi} from '@/api/home';
import {getActiveAnnouncementsApi} from '@/api/notifications';
import ArticleFeed from '@/components/ArticleFeed.vue';
import {useStableListRequest} from '@/composables/useStableListRequest';
import {useWindowSize} from '@/composables/useWindowSize';
import {ARTICLE_SORT_ITEMS, ARTICLE_SORT_LATEST, isDefaultArticleSort, normalizeArticleSort} from '@/constants/articleSort';
import FeaturedArticlesStrip from '@/components/FeaturedArticlesStrip.vue';
import HomeIntro from '@/components/HomeIntro.vue';
import HomeSidebar from '@/components/HomeSidebar.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import TopicStrip from '@/components/TopicStrip.vue';

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

const {width: windowWidth} = useWindowSize();
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

const loadArticles = async (page, sort, category, shouldScroll = false) => {
    const response = await runStableRequest(
        () => listArticlesApi({ page, pageSize, sort, category }),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '文章列表加载失败，请稍后重试',
            refreshErrorMessage: '文章列表刷新失败，请稍后重试'
        }
    );
    if (response?.ignored || response?.error) {
        return;
    }

    const pageResult = response.result || {};
    const nextTotal = Number(pageResult.total || 0);
    const lastPage = Math.max(1, Math.ceil(nextTotal / pageSize));
    if (nextTotal > 0 && page > lastPage) {
        await router.replace({
            query: {
                ...route.query,
                category: category || undefined,
                sort: isDefaultArticleSort(sort) ? undefined : sort,
                page: lastPage === 1 ? undefined : String(lastPage)
            }
        });
        return;
    }

    currentPage.value = page;
    activeSort.value = sort;
    activeCategory.value = category || '';
    total.value = nextTotal;
    articles.value = pageResult.items || [];
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
    () => [route.query.page, route.query.sort, route.query.category],
    ([page, sort, category]) => {
        const nextPage = page === undefined ? undefined : String(page);
        const nextSort = sort === undefined ? undefined : String(sort);
        const nextCategory = category === undefined ? undefined : String(category);
        const pageChanged = previousRouteState.page !== nextPage;
        const shouldScroll = !firstLoad && pageChanged;

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
    loadHomeBootstrap();
    loadBanners();
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
        <HomeIntro
            :total-articles="homeStats.totalArticles"
            :total-authors="homeStats.totalAuthors"
            :total-columns="homeStats.totalColumns"
        />
        <TopicStrip :topics="topicItems" :loading="!bootstrapLoaded" />
        <FeaturedArticlesStrip
            :articles="featuredArticles"
            :loading="!bootstrapLoaded"
        />
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
                @page-change="changePage"
                @sort-change="changeSort"
            />
            <HomeSidebar v-if="showSidebar" :specials="sidebarColumns" :authors="sidebarAuthors" :topics="sidebarTopics" />
        </div>
    </main>
</template>

<style scoped>
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
</style>
