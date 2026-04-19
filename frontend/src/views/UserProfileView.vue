<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { articles } from '@/data/home';

const route = useRoute();
const author = computed(() => {
    const userId = Number(route.params.id);
    return articles.find((item) => item.author.id === userId)?.author || articles[0].author;
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <section class="profile-head">
            <img :src="author.avatar" alt="用户头像">
            <div>
                <p class="eyebrow">技术作者</p>
                <h1>{{ author.name }}</h1>
                <p>专注后端架构、前端工程和项目实战复盘。把能落地的经验写清楚。</p>
                <div class="profile-stats">
                    <span><strong>36</strong> 文章</span>
                    <span><strong>128k</strong> 阅读</span>
                    <span><strong>4,216</strong> 获赞</span>
                </div>
            </div>
        </section>
        <ArticleFeed />
    </main>
</template>
