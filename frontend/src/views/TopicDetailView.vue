<script setup>
import {computed, ref, watch} from 'vue';
import {onBeforeRouteLeave, useRoute} from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getTopicArticlesApi, getTopicDetailApi} from '@/api/topic';
import {updateLearningProgressApi} from '@/api/learning';
import {useInfiniteArticleFeed} from '@/composables/useInfiniteArticleFeed';
import {useStableListRequest} from '@/composables/useStableListRequest';

const route = useRoute();
const topic = ref(null);
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

const buildFeedCacheKey = () => `topic:${String(route.params.id || '')}`;

const fetchTopic = async ({ reset = false } = {}) => {
    let restored = false;
    if (reset) {
        resetStableRequest();
        resetFeed();
        topic.value = null;
        restored = restoreFeedCache(buildFeedCacheKey());
    }

    const topicId = String(route.params.id || '');
    const { result } = await runStableRequest(
        () => Promise.all([
            getTopicDetailApi(topicId),
            restored
                ? Promise.resolve({ items: articles.value, page: currentPage.value, total: total.value })
                : getTopicArticlesApi(topicId, { page: 1, pageSize })
        ]),
        {
            silent: restored || hasLoadedOnce.value,
            initialErrorMessage: '专题加载失败',
            refreshErrorMessage: '专题文章刷新失败，请稍后重试'
        }
    );

    if (!result) {
        return;
    }

    const [topicDetail, articlePage] = result;
    topic.value = topicDetail;
    if (!restored) {
        applyPageResult(articlePage);
        saveFeedCache(buildFeedCacheKey());
    }
};

const loadMoreArticles = async () => {
    const topicId = String(route.params.id || '');
    const response = await loadMore(
        (page) => getTopicArticlesApi(topicId, { page, pageSize }),
        { errorMessage: '专题文章加载失败，请稍后重试' }
    );
    if (response?.result) {
        saveFeedCache(buildFeedCacheKey());
    }
};

const outlineSections = computed(() => {
    const sections = [];
    for (const item of topic.value?.outline || []) {
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
    if (!topic.value || !item?.article?.id) {
        return;
    }
    progressUpdatingId.value = item.article.id;
    try {
        await updateLearningProgressApi({
            assetType: 'TOPIC',
            assetId: topic.value.id,
            articleId: item.article.id,
            completed
        });
        topic.value = await getTopicDetailApi(topic.value.id);
    } finally {
        progressUpdatingId.value = null;
    }
};

watch(() => route.params.id, async () => {
    await fetchTopic({ reset: true });
}, { immediate: true });

onBeforeRouteLeave(() => {
    saveFeedCache(buildFeedCacheKey());
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell topic-detail-page">
        <section v-if="topic" class="topic-detail-hero">
            <div class="topic-detail-cover-panel">
                <img :src="topic.coverUrl" :alt="`${topic.title} 封面`" loading="lazy" decoding="async">
                <div class="topic-detail-cover-note">
                    <span>精选专题</span>
                    <strong>{{ topic.title }}</strong>
                </div>
            </div>
            <div class="topic-detail-content">
                <div class="topic-detail-copy">
                    <p class="eyebrow">专题</p>
                    <h1>{{ topic.title }}</h1>
                    <p class="topic-detail-summary">{{ topic.summary }}</p>
                </div>
                <div class="topic-detail-stats">
                    <span>{{ topic.articleCount }} 篇文章</span>
                </div>
            </div>
        </section>

        <section v-else-if="initialLoading" class="topic-detail-hero topic-detail-loading">
            <div class="topic-detail-loading-media"></div>
            <div class="topic-detail-content">
                <p class="eyebrow">专题</p>
                <h1>正在加载专题...</h1>
                <p>我们正在准备这个专题的内容。</p>
            </div>
        </section>

        <EmptyState
            v-else-if="errorMessage"
            eyebrow="专题"
            title="暂时无法加载这个专题"
            :description="errorMessage || '请稍后重试。'"
            tone="error"
        />

        <section v-if="false && topic && topic.outline.length" class="learning-path-panel">
            <div class="learning-path-head">
                <div>
                    <p class="eyebrow">学习路径</p>
                    <h2>{{ topic.intro || topic.summary }}</h2>
                </div>
                <div class="learning-progress">
                    <strong>{{ topic.progress.progressPercent }}%</strong>
                    <span>{{ topic.progress.completedCount }} / {{ topic.progress.totalCount }} 已读</span>
                </div>
            </div>
            <div class="learning-progress-bar">
                <span :style="{ width: `${topic.progress.progressPercent}%` }"></span>
            </div>
            <RouterLink
                v-if="topic.nextArticle"
                class="continue-link"
                :to="{ path: `/articles/${topic.nextArticle.id}`, query: { from: 'topic', topicId: topic.id, topicTitle: topic.title } }"
            >
                继续阅读：{{ topic.nextArticle.title }}
            </RouterLink>
            <div class="learning-section-list">
                <section v-for="section in outlineSections" :key="section.title" class="learning-section">
                    <h3>{{ section.title }}</h3>
                    <article v-for="item in section.items" :key="item.article.id" class="learning-step">
                        <RouterLink
                            class="learning-step-title"
                            :to="{ path: `/articles/${item.article.id}`, query: { from: 'topic', topicId: topic.id, topicTitle: topic.title } }"
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
            eyebrow="专题文章"
            title="继续阅读"
            empty-text="这个专题暂时还没有公开文章"
            :article-link-query="topic ? { from: 'topic', topicId: topic.id, topicTitle: topic.title } : {}"
            @load-more="loadMoreArticles"
        />
    </main>
</template>

<style scoped src="@/styles/views/TopicDetailView.css"></style>
