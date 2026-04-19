<script setup>
import { onMounted, ref } from 'vue';
import { listArticlesApi } from '@/api/articles';
import ArticleFeed from '@/components/ArticleFeed.vue';
import HomeIntro from '@/components/HomeIntro.vue';
import HomeSidebar from '@/components/HomeSidebar.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import TopicStrip from '@/components/TopicStrip.vue';
import { articles as fallbackArticles } from '@/data/home';

const articles = ref(fallbackArticles);

onMounted(async () => {
    try {
        const page = await listArticlesApi({ page: 1, pageSize: 10 });
        if (page.items.length) {
            articles.value = page.items;
        }
    } catch (error) {
        articles.value = fallbackArticles;
    }
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <HomeIntro />
        <TopicStrip />
        <div class="content-grid">
            <ArticleFeed :articles="articles" />
            <HomeSidebar />
        </div>
    </main>
</template>
