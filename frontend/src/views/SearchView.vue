<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { listArticlesApi } from '@/api/articles';
import ArticleFeed from '@/components/ArticleFeed.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { articles, topics } from '@/data/home';

const route = useRoute();
const router = useRouter();
const keyword = ref(route.query.keyword || 'Spring Boot');
const activeTopic = ref('全部');
const remoteArticles = ref(articles);

const filteredArticles = computed(() => {
    const searchText = keyword.value.trim().toLowerCase();

    return remoteArticles.value.filter((article) => {
        const topicMatched = activeTopic.value === '全部' || article.category === activeTopic.value
            || article.tags?.includes(activeTopic.value);
        const keywordMatched = !searchText || [
            article.title,
            article.summary,
            article.category,
            ...(article.tags || []),
            ...(article.content || [])
        ].some((text) => String(text).toLowerCase().includes(searchText));

        return topicMatched && keywordMatched;
    });
});

const runSearch = () => {
    router.replace({
        path: '/search',
        query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {}
    });
};

const fetchRemoteArticles = async () => {
    try {
        const page = await listArticlesApi({
            keyword: keyword.value,
            page: 1,
            pageSize: 20
        });
        remoteArticles.value = page.items.length ? page.items : articles;
    } catch (error) {
        remoteArticles.value = articles;
    }
};

watch(
    () => route.query.keyword,
    (nextKeyword) => {
        keyword.value = nextKeyword || '';
        fetchRemoteArticles();
    }
);

watch(activeTopic, fetchRemoteArticles);

onMounted(fetchRemoteArticles);
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <section class="search-panel">
            <p class="eyebrow">搜索文章</p>
            <h1>找到能解决问题的文章</h1>
            <form class="search-large" @submit.prevent="runSearch">
                <input v-model="keyword" type="search" aria-label="搜索关键字">
                <button type="submit">搜索</button>
            </form>
            <div class="tag-row">
                <button
                    v-for="topic in topics"
                    :key="topic"
                    type="button"
                    :class="{ active: activeTopic === topic }"
                    @click="activeTopic = topic"
                >
                    {{ topic }}
                </button>
            </div>
            <p class="result-note">共找到 {{ filteredArticles.length }} 篇文章</p>
        </section>
        <ArticleFeed
            :articles="filteredArticles"
            eyebrow="搜索结果"
            title="匹配文章"
            empty-text="换个关键词或分类试试"
        />
    </main>
</template>
