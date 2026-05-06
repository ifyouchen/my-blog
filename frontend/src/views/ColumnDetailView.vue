<script setup>
import {ref, watch} from 'vue';
import {onBeforeRouteLeave, useRoute} from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import ColumnSubscribeButton from '@/components/ColumnSubscribeButton.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getColumnArticlesApi, getColumnDetailApi} from '@/api/columns';
import {useInfiniteArticleFeed} from '@/composables/useInfiniteArticleFeed';
import {useStableListRequest} from '@/composables/useStableListRequest';

const route = useRoute();
const column = ref(null);
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

const buildFeedCacheKey = () => `column:${String(route.params.id || '')}`;

const fetchColumn = async ({ reset = false } = {}) => {
    let restored = false;
    if (reset) {
        resetStableRequest();
        resetFeed();
        column.value = null;
        restored = restoreFeedCache(buildFeedCacheKey());
    }

    const columnId = String(route.params.id || '');
    const { result } = await runStableRequest(
        () => Promise.all([
            getColumnDetailApi(columnId),
            restored
                ? Promise.resolve({ items: articles.value, page: currentPage.value, total: total.value })
                : getColumnArticlesApi(columnId, { page: 1, pageSize })
        ]),
        {
            silent: restored || hasLoadedOnce.value,
            initialErrorMessage: '专栏加载失败',
            refreshErrorMessage: '专栏文章刷新失败，请稍后重试'
        }
    );

    if (!result) {
        return;
    }

    const [columnDetail, articlePage] = result;
    column.value = columnDetail;
    if (!restored) {
        applyPageResult(articlePage);
        saveFeedCache(buildFeedCacheKey());
    }
};

const loadMoreArticles = async () => {
    const columnId = String(route.params.id || '');
    const response = await loadMore(
        (page) => getColumnArticlesApi(columnId, { page, pageSize }),
        { errorMessage: '专栏文章加载失败，请稍后重试' }
    );
    if (response?.result) {
        saveFeedCache(buildFeedCacheKey());
    }
};

const handleSubscribeChange = (subscribed) => {
    if (!column.value) {
        return;
    }
    column.value.subscribed = subscribed;
    column.value.subscriberCount = Math.max(0, (column.value.subscriberCount || 0) + (subscribed ? 1 : -1));
};

watch(() => route.params.id, async () => {
    await fetchColumn({ reset: true });
}, { immediate: true });

onBeforeRouteLeave(() => {
    saveFeedCache(buildFeedCacheKey());
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell column-detail-page">
        <section v-if="column" class="column-detail-hero">
            <div class="column-detail-content">
                <div class="column-detail-copy">
                    <p class="eyebrow">专栏</p>
                    <h1>{{ column.title }}</h1>
                    <p class="column-detail-summary">{{ column.summary }}</p>
                </div>
                <div class="column-detail-meta">
                    <RouterLink class="column-detail-author" :to="`/users/${column.author.id}`">
                        <img :src="column.author.avatar" alt="作者头像" loading="lazy" decoding="async">
                        <div>
                            <strong>{{ column.author.name }}</strong>
                            <span>专栏作者</span>
                        </div>
                    </RouterLink>
                    <div class="column-detail-stats">
                        <span>{{ column.articleCount }} 篇文章</span>
                        <span>{{ column.subscriberCount }} 人订阅</span>
                    </div>
                </div>
                <div class="column-detail-actions">
                    <ColumnSubscribeButton
                        :column-id="column.id"
                        :subscribed="column.subscribed"
                        @change="handleSubscribeChange"
                    />
                    <span class="column-detail-helper">订阅后会持续追踪这个主题下的新文章更新。</span>
                </div>
            </div>
            <div class="column-detail-cover-panel">
                <img :src="column.coverUrl" :alt="`${column.title} 封面`" loading="lazy" decoding="async">
                <div class="column-detail-cover-note">
                    <span>内容主题</span>
                    <strong>{{ column.title }}</strong>
                </div>
            </div>
        </section>

        <section v-else-if="initialLoading" class="column-detail-hero column-detail-loading">
            <div class="column-detail-loading-media"></div>
            <div class="column-detail-content">
                <p class="eyebrow">专栏</p>
                <h1>正在加载专栏...</h1>
                <p>我们正在准备这个专栏的内容。</p>
            </div>
        </section>

        <EmptyState
            v-else-if="errorMessage"
            eyebrow="专栏"
            title="暂时无法加载这个专栏"
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
            :sort-items="[]"
            pagination-mode="infinite"
            :has-more="hasMore"
            :loading-more="loadingMore"
            :load-more-error="loadMoreError"
            eyebrow="专栏文章"
            title="继续阅读"
            empty-text="这个专栏暂时还没有公开文章"
            @load-more="loadMoreArticles"
        />
    </main>
</template>

<style scoped>
.column-detail-hero {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(220px, 0.42fr);
    gap: 22px;
    align-items: start;
    padding: 28px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.column-detail-page {
    display: grid;
    gap: 20px;
}

.column-detail-copy {
    display: grid;
    gap: 10px;
}

.column-detail-cover-panel {
    position: relative;
    overflow: hidden;
    align-self: start;
    aspect-ratio: 4 / 3;
    min-height: 210px;
    max-height: 280px;
    border-radius: var(--radius-md);
    border: 1px solid var(--line);
    background: var(--surface-soft);
}

.column-detail-cover-panel img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center;
    display: block;
}

.column-detail-loading {
    min-height: 320px;
}

.column-detail-loading-media {
    min-height: 280px;
    background: linear-gradient(90deg, #f2f5f8 25%, #e8eef4 37%, #f2f5f8 63%);
    background-size: 400% 100%;
    border-radius: var(--radius-sm);
    animation: column-skeleton 1.2s ease-in-out infinite;
}

.column-detail-content {
    display: grid;
    align-content: start;
    gap: 18px;
    max-width: 760px;
}

@keyframes column-skeleton {
    0% {
        background-position: 100% 0;
    }

    100% {
        background-position: 0 0;
    }
}

.column-detail-content h1,
.column-detail-content p {
    margin: 0;
}

.column-detail-content h1 {
    font-size: clamp(28px, 3vw, 38px);
    line-height: 1.2;
}

.column-detail-summary {
    color: var(--muted);
    font-size: 15px;
    line-height: 1.8;
}

.column-detail-meta {
    display: grid;
    gap: 16px;
}

.column-detail-author {
    display: inline-flex;
    gap: 14px;
    align-items: center;
    width: fit-content;
    color: inherit;
}

.column-detail-author img {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    object-fit: cover;
}

.column-detail-author strong,
.column-detail-cover-note strong {
    color: var(--text-strong);
}

.column-detail-author div {
    display: grid;
    gap: 4px;
}

.column-detail-author span,
.column-detail-helper {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.7;
}

.column-detail-stats {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.column-detail-stats span {
    display: inline-flex;
    align-items: center;
    min-height: 32px;
    padding: 0 12px;
    color: var(--muted);
    font-size: 13px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.column-detail-actions {
    display: flex;
    gap: 14px;
    align-items: center;
    flex-wrap: wrap;
}

.column-detail-cover-note {
    position: absolute;
    right: 12px;
    bottom: 12px;
    display: grid;
    gap: 4px;
    max-width: calc(100% - 24px);
    padding: 10px 12px;
    background: rgba(255, 255, 255, 0.92);
    border: 1px solid rgba(255, 255, 255, 0.7);
    border-radius: var(--radius-sm);
    backdrop-filter: blur(10px);
}

.column-detail-cover-note span {
    color: var(--muted);
    font-size: 12px;
}

.column-detail-cover-note strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.column-detail-author:hover,
.column-detail-author:focus-visible {
    color: var(--brand-strong);
}

@media (max-width: 960px) {
    .column-detail-hero {
        grid-template-columns: minmax(0, 1fr);
        min-width: 0;
    }

    .column-detail-cover-panel {
        aspect-ratio: 16 / 9;
        min-height: 220px;
        max-height: none;
        max-width: 100%;
    }

    .column-detail-actions {
        align-items: stretch;
    }
}
</style>
