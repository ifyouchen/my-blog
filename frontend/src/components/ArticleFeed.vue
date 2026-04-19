<script setup>
import { computed, ref } from 'vue';
import { articles as defaultArticles } from '@/data/home';

const props = defineProps({
    articles: {
        type: Array,
        default: () => defaultArticles
    },
    eyebrow: {
        type: String,
        default: '推荐阅读'
    },
    title: {
        type: String,
        default: '最新技术文章'
    },
    emptyText: {
        type: String,
        default: '暂时没有找到文章'
    }
});

const activeSort = ref('最新');
const sortItems = ['最新', '最热', '精选'];

const parseCount = (text) => Number(String(text).replace(/[^\d]/g, '')) || 0;

const visibleArticles = computed(() => {
    const articleList = [...props.articles];

    if (activeSort.value === '最热') {
        return articleList.sort((a, b) => parseCount(b.stats.views) - parseCount(a.stats.views));
    }

    if (activeSort.value === '精选') {
        return articleList.sort((a, b) => parseCount(b.stats.likes) - parseCount(a.stats.likes));
    }

    return articleList;
});
</script>

<template>
    <section class="feed" aria-labelledby="feed-title">
        <div class="section-heading">
            <div>
                <p class="eyebrow">{{ eyebrow }}</p>
                <h2 id="feed-title">{{ title }}</h2>
            </div>
            <div class="sort-tabs" aria-label="排序">
                <button
                    v-for="item in sortItems"
                    :key="item"
                    type="button"
                    :class="{ active: activeSort === item }"
                    @click="activeSort = item"
                >
                    {{ item }}
                </button>
            </div>
        </div>

        <article
            v-for="article in visibleArticles"
            :key="article.id"
            class="post-item"
            :class="{ 'featured-post': article.featured }"
        >
            <RouterLink class="post-cover" :to="`/articles/${article.id}`" :aria-label="`查看文章：${article.title}`">
                <img :src="article.cover" :alt="article.coverAlt">
            </RouterLink>
            <div class="post-content">
                <div class="post-meta">
                    <span>{{ article.category }}</span>
                    <span>{{ article.readingTime }}</span>
                    <span>{{ article.publishedText }}</span>
                </div>
                <h3>
                    <RouterLink :to="`/articles/${article.id}`">
                        {{ article.title }}
                    </RouterLink>
                </h3>
                <p>{{ article.summary }}</p>
                <div class="post-footer">
                    <RouterLink class="author" :to="`/users/${article.author.id}`">
                        <img :src="article.author.avatar" alt="作者头像">
                        <span>{{ article.author.name }}</span>
                    </RouterLink>
                    <span>{{ article.stats.views }}</span>
                    <span>{{ article.stats.likes }}</span>
                    <span>{{ article.stats.comments }}</span>
                </div>
            </div>
        </article>

        <div v-if="visibleArticles.length === 0" class="empty-state">
            {{ emptyText }}
        </div>
    </section>
</template>
