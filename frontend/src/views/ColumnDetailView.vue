<script setup>
import { onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import ColumnSubscribeButton from '@/components/ColumnSubscribeButton.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { getColumnArticlesApi, getColumnDetailApi } from '@/api/columns';

const route = useRoute();
const column = ref(null);
const articles = ref([]);
const loading = ref(false);
const errorMessage = ref('');
const currentPage = ref(1);
const total = ref(0);
const pageSize = 10;

const fetchColumn = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const columnId = Number(route.params.id);
        const [columnDetail, articlePage] = await Promise.all([
            getColumnDetailApi(columnId),
            getColumnArticlesApi(columnId, { page: currentPage.value, pageSize })
        ]);
        column.value = columnDetail;
        articles.value = articlePage.items || [];
        total.value = articlePage.total || 0;
    } catch (error) {
        column.value = null;
        articles.value = [];
        total.value = 0;
        errorMessage.value = error.message || '专栏加载失败';
    } finally {
        loading.value = false;
    }
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

onMounted(fetchColumn);

watch(() => route.params.id, async () => {
    currentPage.value = 1;
    await fetchColumn();
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

        <EmptyState
            v-else
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
            :error-message="errorMessage"
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
    background:
        radial-gradient(circle at top right, rgba(40, 118, 255, 0.12), transparent 28%),
        linear-gradient(180deg, rgba(248, 251, 255, 0.98), #ffffff);
    border: 1px solid rgba(208, 219, 236, 0.92);
    border-radius: 24px;
    box-shadow: 0 22px 50px rgba(31, 78, 168, 0.08);
}

.column-detail-hero img {
    width: 100%;
    height: 100%;
    min-height: 280px;
    object-fit: cover;
    border-radius: 20px;
    box-shadow: 0 18px 36px rgba(31, 78, 168, 0.12);
}

.column-detail-content {
    display: grid;
    align-content: start;
    gap: 14px;
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
