<script setup>
import {ref, watch} from 'vue';
import {onBeforeRouteLeave, useRoute} from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getTagApi, getTagArticlesApi} from '@/api/tags';
import {useInfiniteArticleFeed} from '@/composables/useInfiniteArticleFeed';
import {useStableListRequest} from '@/composables/useStableListRequest';
import {ARTICLE_SORT_ITEMS, ARTICLE_SORT_LATEST} from '@/constants/articleSort';

const route = useRoute();
const tag = ref(null);
const activeSort = ref(ARTICLE_SORT_LATEST);
const pageSize = 10;
const {
    articles,
    currentPage,
    total,
    hasMore,
    loadingMore,
    loadMoreError,
    applyPageResult,
    resetFeed,
    saveFeedCache,
    restoreFeedCache,
    loadMore
} = useInfiniteArticleFeed({ pageSize });

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

const buildFeedCacheKey = () => `tag:${String(route.params.id || '')}:${activeSort.value}`;

const fetchData = async ({ reset = false } = {}) => {
    let restored = false;
    if (reset) {
        resetStableRequest();
        resetFeed();
        tag.value = null;
    }
    if (articles.value.length === 0) {
        restored = restoreFeedCache(buildFeedCacheKey());
    }

    const tagId = String(route.params.id || '');
    const { result } = await runStableRequest(
        () => Promise.all([
            getTagApi(tagId),
            restored
                ? Promise.resolve({ items: articles.value, page: currentPage.value, total: total.value })
                : getTagArticlesApi(tagId, {
                    page: 1,
                    pageSize,
                    sort: activeSort.value
                })
        ]),
        {
            silent: restored || hasLoadedOnce.value,
            initialErrorMessage: '标签加载失败',
            refreshErrorMessage: '文章刷新失败，请稍后重试'
        }
    );

    if (!result) return;

    const [tagDetail, articlePage] = result;
    tag.value = tagDetail;
    if (!restored) {
        applyPageResult(articlePage);
        saveFeedCache(buildFeedCacheKey());
    }
};

const loadMoreArticles = async () => {
    const tagId = String(route.params.id || '');
    const response = await loadMore(
        (page) => getTagArticlesApi(tagId, { page, pageSize, sort: activeSort.value }),
        { errorMessage: '标签文章加载失败，请稍后重试' }
    );
    if (response?.result) {
        saveFeedCache(buildFeedCacheKey());
    }
};

const changeSort = async (sort) => {
    saveFeedCache(buildFeedCacheKey());
    activeSort.value = sort;
    resetFeed();
    await fetchData();
};

watch(() => route.params.id, async () => {
    saveFeedCache(buildFeedCacheKey());
    activeSort.value = ARTICLE_SORT_LATEST;
    await fetchData({ reset: true });
}, { immediate: true });

onBeforeRouteLeave(() => {
    saveFeedCache(buildFeedCacheKey());
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell tag-detail-page">
        <section v-if="tag" class="tag-detail-hero">
            <div class="tag-detail-inner">
                <p class="eyebrow">标签</p>
                <h1><span class="tag-hash">#</span>{{ tag.name }}</h1>
                <div class="tag-detail-meta">
                    <span class="tag-use-count">{{ tag.useCount || total }} 篇文章</span>
                </div>
            </div>
        </section>

        <section v-else-if="initialLoading" class="tag-detail-hero tag-detail-loading">
            <div class="tag-detail-inner">
                <p class="eyebrow">标签</p>
                <h1>正在加载标签...</h1>
            </div>
        </section>

        <EmptyState
            v-else-if="errorMessage && !hasLoadedOnce"
            eyebrow="标签"
            title="暂时无法加载这个标签"
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
            :sort="activeSort"
            pagination-mode="infinite"
            :has-more="hasMore"
            :loading-more="loadingMore"
            :load-more-error="loadMoreError"
            eyebrow="标签文章"
            :title="tag ? `#${tag.name}` : ''"
            empty-text="这个标签下暂时还没有公开文章"
            @load-more="loadMoreArticles"
            @sort-change="changeSort"
        />
    </main>
</template>

<style scoped>
.tag-detail-page {
    display: grid;
    gap: 20px;
}

.tag-detail-hero {
    padding: 32px 28px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.tag-detail-loading {
    min-height: 160px;
}

.tag-detail-inner {
    display: grid;
    gap: 10px;
    max-width: 680px;
}

.tag-detail-inner h1 {
    margin: 0;
    font-size: clamp(24px, 3vw, 34px);
    line-height: 1.2;
}

.tag-hash {
    color: var(--accent);
    margin-right: 2px;
}

.tag-detail-meta {
    display: flex;
    align-items: center;
    gap: 10px;
}

.tag-use-count {
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

