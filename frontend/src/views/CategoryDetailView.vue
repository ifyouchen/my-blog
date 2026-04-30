<script setup>
import {onMounted, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getCategoryApi, getCategoryArticlesApi} from '@/api/categories';
import {useStableListRequest} from '@/composables/useStableListRequest';
import {ARTICLE_SORT_ITEMS, ARTICLE_SORT_LATEST} from '@/constants/articleSort';

const route = useRoute();
const category = ref(null);
const articles = ref([]);
const currentPage = ref(1);
const total = ref(0);
const activeSort = ref(ARTICLE_SORT_LATEST);
const pageSize = 10;

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

const fetchData = async ({ reset = false } = {}) => {
    if (reset) {
        resetStableRequest();
        category.value = null;
        articles.value = [];
        total.value = 0;
    }

    const categoryId = String(route.params.id || '');
    const { result } = await runStableRequest(
        () => Promise.all([
            getCategoryApi(categoryId),
            getCategoryArticlesApi(categoryId, {
                page: currentPage.value,
                pageSize,
                sort: activeSort.value
            })
        ]),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '分类加载失败',
            refreshErrorMessage: '文章刷新失败，请稍后重试'
        }
    );

    if (!result) return;

    const [categoryDetail, articlePage] = result;
    category.value = categoryDetail;
    articles.value = articlePage.items || [];
    total.value = articlePage.total || 0;
};

const changePage = async (page) => {
    currentPage.value = page;
    await fetchData();
};

const changeSort = async (sort) => {
    activeSort.value = sort;
    currentPage.value = 1;
    await fetchData();
};

onMounted(() => fetchData({ reset: true }));

watch(() => route.params.id, async () => {
    currentPage.value = 1;
    activeSort.value = ARTICLE_SORT_LATEST;
    await fetchData({ reset: true });
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell category-detail-page">
        <section v-if="category" class="category-detail-hero">
            <div class="category-detail-inner">
                <p class="eyebrow">分类</p>
                <h1>{{ category.name }}</h1>
                <p v-if="category.description" class="category-detail-desc">{{ category.description }}</p>
                <div class="category-detail-stats">
                    <span>{{ total }} 篇文章</span>
                </div>
            </div>
        </section>

        <section v-else-if="initialLoading" class="category-detail-hero category-detail-loading">
            <div class="category-detail-inner">
                <p class="eyebrow">分类</p>
                <h1>正在加载分类...</h1>
            </div>
        </section>

        <EmptyState
            v-else-if="errorMessage && !hasLoadedOnce"
            eyebrow="分类"
            title="暂时无法加载这个分类"
            :description="errorMessage || '请稍后重试。'"
            tone="error"
        />

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
            :sort-items="ARTICLE_SORT_ITEMS"
            :active-sort="activeSort"
            eyebrow="分类文章"
            :title="category ? category.name : ''"
            empty-text="这个分类暂时还没有公开文章"
            @page-change="changePage"
            @sort-change="changeSort"
        />
    </main>
</template>

<style scoped>
.category-detail-page {
    display: grid;
    gap: 20px;
}

.category-detail-hero {
    padding: 32px 28px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.category-detail-loading {
    min-height: 160px;
}

.category-detail-inner {
    display: grid;
    gap: 10px;
    max-width: 680px;
}

.category-detail-inner h1 {
    margin: 0;
    font-size: clamp(24px, 3vw, 34px);
    line-height: 1.2;
}

.category-detail-desc {
    margin: 0;
    color: var(--muted);
    font-size: 15px;
    line-height: 1.8;
}

.category-detail-stats span {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0 12px;
    color: var(--muted);
    font-size: 13px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}
</style>

