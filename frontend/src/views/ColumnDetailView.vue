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
    <main class="page-shell">
        <section v-if="column" class="column-detail-hero">
            <img :src="column.coverUrl" :alt="`${column.title} 封面`">
            <div class="column-detail-content">
                <p class="eyebrow">专栏</p>
                <h1>{{ column.title }}</h1>
                <p>{{ column.summary }}</p>
                <div class="column-detail-meta">
                    <RouterLink :to="`/users/${column.author.id}`">{{ column.author.name }}</RouterLink>
                    <span>{{ column.articleCount }} 篇文章</span>
                    <span>{{ column.subscriberCount }} 人订阅</span>
                </div>
                <ColumnSubscribeButton
                    :column-id="column.id"
                    :subscribed="column.subscribed"
                    @change="handleSubscribeChange"
                />
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
    grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
    gap: 24px;
    padding: 24px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.column-detail-hero img {
    width: 100%;
    height: 100%;
    min-height: 280px;
    object-fit: cover;
    border-radius: var(--radius-sm);
    box-shadow: none;
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
    gap: 14px;
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

.column-detail-meta {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    color: var(--muted);
    font-size: 13px;
}

.column-detail-meta a {
    color: var(--text);
    text-decoration: none;
    font-weight: 700;
}

.column-detail-meta a:hover,
.column-detail-meta a:focus-visible {
    color: var(--brand-strong);
}

@media (max-width: 960px) {
    .column-detail-hero {
        grid-template-columns: 1fr;
    }

    .column-detail-hero img {
        min-height: 220px;
    }
}
</style>
