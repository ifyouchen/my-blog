<script setup>
import { RouterLink } from 'vue-router';

defineProps({
    articles: {
        type: Array,
        default: () => []
    }
});
</script>

<template>
    <section v-if="articles.length" class="featured-strip" aria-label="精选推荐">
        <div class="featured-header">
            <p class="eyebrow">编辑精选</p>
            <h2>精选推荐</h2>
        </div>
        <div class="featured-scroll">
            <RouterLink
                v-for="article in articles"
                :key="article.id"
                :to="article.slug ? `/articles/${article.id}-${article.slug}` : `/articles/${article.id}`"
                class="featured-card"
            >
                <div class="featured-card-cover">
                    <img :src="article.cover || article.coverUrl" :alt="`${article.title} 封面`" loading="lazy" decoding="async">
                </div>
                <div class="featured-card-body">
                    <span class="featured-card-category">{{ article.category }}</span>
                    <h3>{{ article.title }}</h3>
                    <div class="featured-card-meta">
                        <span>{{ article.author?.name || article.author?.nickname }}</span>
                        <span class="middot">&middot;</span>
                        <span>{{ article.likeCount }} 赞</span>
                    </div>
                </div>
            </RouterLink>
        </div>
    </section>
</template>

<style scoped>
.featured-strip {
    display: grid;
    gap: 14px;
}

.featured-header {
    display: grid;
    gap: 4px;
}

.featured-header h2 {
    margin: 0;
    font-size: 18px;
}

.featured-scroll {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 14px;
}

.featured-card {
    display: flex;
    flex-direction: column;
    overflow: hidden;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    text-decoration: none;
    transition: background 0.12s, border-color 0.12s;
}

.featured-card:hover,
.featured-card:focus-visible {
    background: var(--surface-soft);
    border-color: var(--line-strong);
}

.featured-card:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: 2px;
}

.featured-card-cover {
    display: block;
    width: 100%;
}

.featured-card-cover img {
    width: 100%;
    aspect-ratio: 16 / 9;
    object-fit: cover;
    display: block;
}

.featured-card-body {
    display: grid;
    gap: 6px;
    flex: 1;
    padding: 12px 14px 14px;
}

.featured-card-category {
    color: var(--brand);
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.featured-card h3 {
    margin: 0;
    color: var(--text);
    font-size: 14px;
    line-height: 1.45;
    display: -webkit-box;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    transition: color 0.18s ease;
}

.featured-card:hover h3,
.featured-card:focus-visible h3 {
    color: var(--brand-strong);
}

.featured-card-meta {
    display: flex;
    gap: 4px;
    align-items: center;
    color: var(--muted);
    font-size: 12px;
    margin-top: auto;
}

.middot {
    color: var(--line-strong);
}

@media (max-width: 1080px) {
    .featured-scroll {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }
}

@media (max-width: 720px) {
    .featured-scroll {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 480px) {
    .featured-scroll {
        grid-template-columns: 1fr;
    }
}
</style>
