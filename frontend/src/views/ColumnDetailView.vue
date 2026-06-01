<script setup>
import {computed, ref, watch} from 'vue';
import {onBeforeRouteLeave, useRoute} from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import ColumnSubscribeButton from '@/components/ColumnSubscribeButton.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getColumnArticlesApi, getColumnDetailApi} from '@/api/columns';
import {updateLearningProgressApi} from '@/api/learning';
import {useInfiniteArticleFeed} from '@/composables/useInfiniteArticleFeed';
import {useStableListRequest} from '@/composables/useStableListRequest';

const route = useRoute();
const column = ref(null);
const progressUpdatingId = ref(null);
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

const COLUMN_ARTICLE_CACHE_VERSION = 'column-articles-v2';
const buildFeedCacheKey = () => `column:${String(route.params.id || '')}`;

const fetchColumn = async ({ reset = false } = {}) => {
    let restored = false;
    const cacheOptions = { cacheVersion: COLUMN_ARTICLE_CACHE_VERSION };
    if (reset) {
        resetStableRequest();
        resetFeed();
        column.value = null;
        restored = restoreFeedCache(buildFeedCacheKey(), cacheOptions);
    }

    const columnId = String(route.params.id || '');
    const { result } = await runStableRequest(
        () => Promise.all([
            getColumnDetailApi(columnId),
            getColumnArticlesApi(columnId, { page: 1, pageSize })
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
    applyPageResult(articlePage);
    saveFeedCache(buildFeedCacheKey(), cacheOptions);
};

const loadMoreArticles = async () => {
    const cacheOptions = { cacheVersion: COLUMN_ARTICLE_CACHE_VERSION };
    const columnId = String(route.params.id || '');
    const response = await loadMore(
        (page) => getColumnArticlesApi(columnId, { page, pageSize }),
        { errorMessage: '专栏文章加载失败，请稍后重试' }
    );
    if (response?.result) {
        saveFeedCache(buildFeedCacheKey(), cacheOptions);
    }
};

const handleSubscribeChange = (subscribed) => {
    if (!column.value) {
        return;
    }
    column.value.subscribed = subscribed;
    column.value.subscriberCount = Math.max(0, (column.value.subscriberCount || 0) + (subscribed ? 1 : -1));
};

const outlineSections = computed(() => {
    const sections = [];
    for (const item of column.value?.outline || []) {
        const title = item.sectionTitle || '推荐阅读';
        let section = sections.find((entry) => entry.title === title);
        if (!section) {
            section = { title, items: [] };
            sections.push(section);
        }
        section.items.push(item);
    }
    return sections;
});

const markArticleProgress = async (item, completed = true) => {
    if (!column.value || !item?.article?.id) {
        return;
    }
    progressUpdatingId.value = item.article.id;
    try {
        await updateLearningProgressApi({
            assetType: 'COLUMN',
            assetId: column.value.id,
            articleId: item.article.id,
            completed
        });
        column.value = await getColumnDetailApi(column.value.id);
    } finally {
        progressUpdatingId.value = null;
    }
};

watch(() => route.params.id, async () => {
    await fetchColumn({ reset: true });
}, { immediate: true });

onBeforeRouteLeave(() => {
    saveFeedCache(buildFeedCacheKey(), { cacheVersion: COLUMN_ARTICLE_CACHE_VERSION });
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

        <section v-if="false && column && column.outline.length" class="learning-path-panel">
            <div class="learning-path-head">
                <div>
                    <p class="eyebrow">专栏路径</p>
                    <h2>{{ column.intro || column.summary }}</h2>
                </div>
                <div class="learning-progress">
                    <strong>{{ column.progress.progressPercent }}%</strong>
                    <span>{{ column.progress.completedCount }} / {{ column.progress.totalCount }} 已读</span>
                </div>
            </div>
            <div class="learning-progress-bar">
                <span :style="{ width: `${column.progress.progressPercent}%` }"></span>
            </div>
            <RouterLink
                v-if="column.nextArticle"
                class="continue-link"
                :to="{ path: `/articles/${column.nextArticle.id}`, query: { from: 'column', columnId: column.id, columnTitle: column.title } }"
            >
                继续阅读：{{ column.nextArticle.title }}
            </RouterLink>
            <div class="learning-section-list">
                <section v-for="section in outlineSections" :key="section.title" class="learning-section">
                    <h3>{{ section.title }}</h3>
                    <article v-for="item in section.items" :key="item.article.id" class="learning-step">
                        <RouterLink
                            class="learning-step-title"
                            :to="{ path: `/articles/${item.article.id}`, query: { from: 'column', columnId: column.id, columnTitle: column.title } }"
                        >
                            <span>{{ item.required ? '必读' : '选读' }}</span>
                            <strong>{{ item.article.title }}</strong>
                        </RouterLink>
                        <p v-if="item.editorNote">{{ item.editorNote }}</p>
                        <button
                            type="button"
                            class="learning-step-action"
                            :disabled="progressUpdatingId === item.article.id"
                            @click="markArticleProgress(item, !item.completed)"
                        >
                            {{ item.completed ? '已读' : '标记已读' }}
                        </button>
                    </article>
                </section>
            </div>
        </section>

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
            :article-link-query="column ? { from: 'column', columnId: column.id, columnTitle: column.title } : {}"
            @load-more="loadMoreArticles"
        />
    </main>
</template>

<style scoped src="@/styles/views/ColumnDetailView.part-1.css"></style>
<style scoped src="@/styles/views/ColumnDetailView.part-2.css"></style>
