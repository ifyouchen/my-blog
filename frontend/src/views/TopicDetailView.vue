<script setup>
import {onMounted, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getTopicArticlesApi, getTopicDetailApi} from '@/api/topic';
import {useStableListRequest} from '@/composables/useStableListRequest';

const route = useRoute();
const topic = ref(null);
const articles = ref([]);
const currentPage = ref(1);
const total = ref(0);
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

const fetchTopic = async ({ reset = false } = {}) => {
    if (reset) {
        resetStableRequest();
        topic.value = null;
        articles.value = [];
        total.value = 0;
    }

    const topicId = String(route.params.id || '');
    const { result } = await runStableRequest(
        () => Promise.all([
            getTopicDetailApi(topicId),
            getTopicArticlesApi(topicId, { page: currentPage.value, pageSize })
        ]),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '专题加载失败',
            refreshErrorMessage: '专题文章刷新失败，请稍后重试'
        }
    );

    if (!result) {
        return;
    }

    const [topicDetail, articlePage] = result;
    topic.value = topicDetail;
    articles.value = articlePage.items || [];
    total.value = articlePage.total || 0;
};

const changePage = async (page) => {
    currentPage.value = page;
    await fetchTopic();
};

onMounted(() => fetchTopic({ reset: true }));

watch(() => route.params.id, async () => {
    currentPage.value = 1;
    await fetchTopic({ reset: true });
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
            eyebrow="专题文章"
            title="继续阅读"
            empty-text="这个专题暂时还没有公开文章"
            @page-change="changePage"
        />
    </main>
</template>

<style scoped>
.topic-detail-hero {
    display: grid;
    gap: 22px;
    padding: 24px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.topic-detail-page {
    display: grid;
    gap: 20px;
}

.topic-detail-copy {
    display: grid;
    gap: 10px;
}

.topic-detail-cover-panel {
    position: relative;
    overflow: hidden;
    aspect-ratio: 21 / 8;
    min-height: 220px;
    max-height: 330px;
    border-radius: var(--radius-md);
    border: 1px solid var(--line);
    background: var(--surface-soft);
}

.topic-detail-cover-panel img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center;
    display: block;
}

.topic-detail-loading {
    min-height: 320px;
}

.topic-detail-loading-media {
    min-height: 220px;
    aspect-ratio: 21 / 8;
    background: linear-gradient(90deg, #f2f5f8 25%, #e8eef4 37%, #f2f5f8 63%);
    background-size: 400% 100%;
    border-radius: var(--radius-md);
    animation: topic-skeleton 1.2s ease-in-out infinite;
}

.topic-detail-content {
    display: grid;
    gap: 14px;
    max-width: 820px;
}

@keyframes topic-skeleton {
    0% { background-position: 100% 0; }
    100% { background-position: 0 0; }
}

.topic-detail-content h1,
.topic-detail-content p {
    margin: 0;
}

.topic-detail-content h1 {
    font-size: clamp(28px, 3vw, 38px);
    line-height: 1.2;
}

.topic-detail-summary {
    color: var(--muted);
    font-size: 15px;
    line-height: 1.8;
}

.topic-detail-stats span {
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

.topic-detail-cover-note {
    position: absolute;
    right: 16px;
    bottom: 16px;
    display: grid;
    gap: 4px;
    max-width: calc(100% - 32px);
    padding: 12px 14px;
    background: rgba(255, 255, 255, 0.92);
    border: 1px solid rgba(255, 255, 255, 0.7);
    border-radius: var(--radius-sm);
    backdrop-filter: blur(10px);
}

.topic-detail-cover-note span {
    color: var(--muted);
    font-size: 12px;
}

.topic-detail-cover-note strong {
    overflow: hidden;
    color: var(--text-strong);
    text-overflow: ellipsis;
    white-space: nowrap;
}

@media (max-width: 960px) {
    .topic-detail-cover-panel {
        aspect-ratio: 16 / 9;
        min-height: 180px;
        max-height: none;
    }

    .topic-detail-loading-media {
        aspect-ratio: 16 / 9;
        min-height: 180px;
    }
}
</style>
