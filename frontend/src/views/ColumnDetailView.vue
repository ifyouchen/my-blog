<script setup>
import {onMounted, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import ColumnSubscribeButton from '@/components/ColumnSubscribeButton.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getColumnArticlesApi, getColumnDetailApi} from '@/api/columns';
import {useStableListRequest} from '@/composables/useStableListRequest';

const route = useRoute();
const column = ref(null);
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

const fetchColumn = async ({ reset = false } = {}) => {
    if (reset) {
        resetStableRequest();
        column.value = null;
        articles.value = [];
        total.value = 0;
    }

    const columnId = String(route.params.id || '');
    const { result } = await runStableRequest(
        () => Promise.all([
            getColumnDetailApi(columnId),
            getColumnArticlesApi(columnId, { page: currentPage.value, pageSize })
        ]),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '专栏加载失败',
            refreshErrorMessage: '专栏文章刷新失败，请稍后重试'
        }
    );

    if (!result) {
        return;
    }

    const [columnDetail, articlePage] = result;
    column.value = columnDetail;
    articles.value = articlePage.items || [];
    total.value = articlePage.total || 0;
};

const changePage = async (page) => {
    currentPage.value = page;
    await fetchColumn();
};

const handleSubscribeChange = (subscribed) => {
    if (!column.value) {
        return;
    }
    column.value.subscribed = subscribed;
    column.value.subscriberCount = Math.max(0, (column.value.subscriberCount || 0) + (subscribed ? 1 : -1));
};

onMounted(() => fetchColumn({ reset: true }));

watch(() => route.params.id, async () => {
    currentPage.value = 1;
    await fetchColumn({ reset: true });
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
            eyebrow="专栏文章"
            title="继续阅读"
            empty-text="这个专栏暂时还没有公开文章"
            @page-change="changePage"
        />
    </main>
</template>

<style scoped>
.column-detail-hero {
    display: grid;
    grid-template-columns: minmax(0, 1.08fr) minmax(300px, 0.92fr);
    gap: 26px;
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
    min-height: 280px;
    max-height: 360px;
    border-radius: var(--radius-md);
    border: 1px solid var(--line);
    background: var(--surface-soft);
}

.column-detail-cover-panel img {
    width: 100%;
    height: 100%;
    object-fit: cover;
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
    right: 16px;
    bottom: 16px;
    display: grid;
    gap: 4px;
    padding: 12px 14px;
    background: rgba(255, 255, 255, 0.92);
    border: 1px solid rgba(255, 255, 255, 0.7);
    border-radius: var(--radius-sm);
    backdrop-filter: blur(10px);
}

.column-detail-cover-note span {
    color: var(--muted);
    font-size: 12px;
}

.column-detail-author:hover,
.column-detail-author:focus-visible {
    color: var(--brand-strong);
}

@media (max-width: 960px) {
    .column-detail-hero {
        grid-template-columns: 1fr;
    }

    .column-detail-cover-panel {
        min-height: 220px;
        max-height: none;
    }

    .column-detail-actions {
        align-items: stretch;
    }
}
</style>
