<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { listArticlesApi } from '@/api/articles';
import { getHomeBootstrapApi } from '@/api/home';
import ArticleFeed from '@/components/ArticleFeed.vue';
import {
    ARTICLE_SORT_ITEMS,
    ARTICLE_SORT_LATEST,
    isDefaultArticleSort,
    normalizeArticleSort
} from '@/constants/articleSort';
import HomeIntro from '@/components/HomeIntro.vue';
import HomeSidebar from '@/components/HomeSidebar.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import TopicStrip from '@/components/TopicStrip.vue';
import { articles as fallbackArticles, topics as fallbackTopics } from '@/data/home';

const homeStats = ref({
    totalArticles: 0,
    totalAuthors: 0,
    totalColumns: 0
});
const articles = ref([]);
const currentPage = ref(1);
const pageSize = 10;
const total = ref(0);
const topicItems = ref([]);
const sidebarColumns = ref([]);
const sidebarAuthors = ref([]);
const loading = ref(false);
const errorMessage = ref('');
const activeSort = ref(ARTICLE_SORT_LATEST);
const activeCategory = ref('');
const bootstrapLoaded = ref(false);
const route = useRoute();
const router = useRouter();
let requestSeq = 0;
let firstLoad = true;
let previousRouteState = {
    page: undefined,
    sort: undefined,
    category: undefined
};

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
        const categoryNames = (bootstrap?.categories || []).map((item) => item.name).filter(Boolean);
        topicItems.value = ['全部', ...categoryNames];
        sidebarColumns.value = bootstrap?.recommendedColumns || [];
        sidebarAuthors.value = bootstrap?.authorRankings || [];
    } catch (error) {
        topicItems.value = ['全部'];
        sidebarColumns.value = [];
        sidebarAuthors.value = [];
    } finally {
        bootstrapLoaded.value = true;
    }
};

const loadArticles = async (page, sort, category, shouldScroll = false) => {
    const seq = requestSeq + 1;
    requestSeq = seq;
    loading.value = true;
    errorMessage.value = '';

    try {
        const pageResult = await listArticlesApi({ page, pageSize, sort, category });
        if (seq !== requestSeq) {
            return;
        }

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
        articles.value = pageResult.items;
    } catch (error) {
        errorMessage.value = '文章列表加载失败，请稍后重试';
        articles.value = [];
        total.value = 0;
        currentPage.value = 1;
        activeSort.value = ARTICLE_SORT_LATEST;
        activeCategory.value = category || '';
    } finally {
        if (seq === requestSeq) {
            loading.value = false;
            if (shouldScroll) {
                await scrollToFeed();
            }
        }
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
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell" data-testid="home-page">
        <HomeIntro
            :total-articles="homeStats.totalArticles"
            :total-authors="homeStats.totalAuthors"
            :total-columns="homeStats.totalColumns"
        />
        <TopicStrip :topics="topicItems" />
        <div class="content-grid">
            <ArticleFeed
                :articles="articles"
                :page="currentPage"
                :page-size="pageSize"
                :total="total"
                :loading="loading"
                :error-message="errorMessage"
                :sort="activeSort"
                :sort-items="ARTICLE_SORT_ITEMS"
                @page-change="changePage"
                @sort-change="changeSort"
            />
            <HomeSidebar :specials="sidebarColumns" :authors="sidebarAuthors" />
        </div>
    </main>
</template>
