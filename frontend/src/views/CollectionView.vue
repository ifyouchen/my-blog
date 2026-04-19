<script setup>
import ArticleFeed from '@/components/ArticleFeed.vue';
import SiteHeader from '@/components/SiteHeader.vue';
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <section class="collection-head">
            <p class="eyebrow">技术专题</p>
            <h1>按主题持续学习</h1>
            <p>这里聚合高质量文章、作者和实践路径。第一版先呈现内容入口，后续接入专栏订阅。</p>
        </section>
        <ArticleFeed />
    </main>
</template>
