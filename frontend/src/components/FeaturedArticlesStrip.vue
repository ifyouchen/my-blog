<script setup>
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';

const SKELETON_CARD_COUNT = 5;

const props = defineProps({
    articles: {
        type: Array,
        default: () => []
    },
    loading: {
        type: Boolean,
        default: false
    }
});

const showSkeleton = computed(() => props.loading && props.articles.length === 0);
const shouldRenderStrip = computed(() => showSkeleton.value || props.articles.length > 0);
const FEATURED_PREVIEW_COUNT = 4;
const expanded = ref(false);
const visibleArticles = computed(() => expanded.value ? props.articles : props.articles.slice(0, FEATURED_PREVIEW_COUNT));
</script>

<template>
    <section
        v-if="shouldRenderStrip"
        class="featured-strip"
        :class="{ 'is-loading': showSkeleton }"
        aria-label="精选推荐"
        :aria-busy="showSkeleton"
    >
        <div class="featured-header">
            <p class="eyebrow">编辑精选</p>
            <h2>精选推荐</h2>
        </div>
        <div v-if="showSkeleton" class="featured-scroll" aria-hidden="true">
            <div
                v-for="i in SKELETON_CARD_COUNT"
                :key="i"
                class="featured-card featured-card-skeleton"
            >
                <div class="featured-card-cover">
                    <span class="featured-skeleton-cover"></span>
                </div>
                <div class="featured-card-body">
                    <span class="featured-skeleton-block featured-skeleton-category"></span>
                    <span class="featured-skeleton-block featured-skeleton-title"></span>
                    <span class="featured-skeleton-block featured-skeleton-title short"></span>
                    <div class="featured-skeleton-meta">
                        <span class="featured-skeleton-block"></span>
                        <span class="featured-skeleton-block short"></span>
                    </div>
                </div>
            </div>
        </div>
        <div v-else class="featured-scroll">
            <RouterLink
                v-for="article in visibleArticles"
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
        <button
            v-if="articles.length > FEATURED_PREVIEW_COUNT"
            class="featured-toggle"
            type="button"
            @click="expanded = !expanded"
        >{{ expanded ? '收起精选' : '查看更多精选' }}</button>
    </section>
</template>

<style scoped>
.featured-strip {
    display: grid;
    gap: 10px;
}

.featured-header {
    display: grid;
    gap: 2px;
}

.featured-header h2 {
    margin: 0;
    font-size: 17px;
}

.featured-scroll {
    display: flex;
    overflow-x: auto;
    padding-bottom: 2px;
    scroll-snap-type: x proximity;
    scrollbar-width: thin;
    gap: 10px;
}

.featured-card {
    display: flex;
    flex-direction: column;
    flex: 0 0 240px;
    overflow: hidden;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    text-decoration: none;
    scroll-snap-align: start;
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
    aspect-ratio: 16 / 8;
    overflow: hidden;
    background: var(--surface-muted);
}

.featured-card-cover img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}

.featured-card-body {
    display: grid;
    gap: 4px;
    flex: 1;
    min-height: 98px;
    padding: 10px 12px 12px;
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
    font-size: 13px;
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
    gap: 2px;
    align-items: center;
    min-width: 0;
    color: var(--muted);
    font-size: 12px;
    margin-top: auto;
    overflow: hidden;
    white-space: nowrap;
}

.featured-card-meta span:first-child {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
}

.middot {
    color: var(--line-strong);
}

.featured-card-skeleton {
    cursor: default;
    pointer-events: none;
}

.featured-skeleton-cover,
.featured-skeleton-block {
    display: block;
    background: var(--surface-muted);
    border-radius: var(--radius-sm);
}

.featured-skeleton-cover {
    width: 100%;
    height: 100%;
}

.featured-skeleton-category {
    width: 42%;
    height: 13px;
}

.featured-skeleton-title {
    width: 88%;
    height: 17px;
}

.featured-skeleton-title.short {
    width: 66%;
}

.featured-skeleton-meta {
    display: flex;
    gap: 4px;
    align-items: center;
    margin-top: auto;
}

.featured-skeleton-meta .featured-skeleton-block {
    width: 72px;
    height: 13px;
}

.featured-skeleton-meta .featured-skeleton-block.short {
    width: 42px;
}

.featured-toggle {
    justify-self: start;
    padding: 6px 10px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    color: var(--text);
    cursor: pointer;
}

@media (max-width: 1080px) {
    .featured-card {
        flex-basis: 220px;
    }
}

@media (max-width: 720px) {
    .featured-card {
        flex-basis: 200px;
    }
}

@media (max-width: 480px) {
    .featured-card {
        flex-basis: calc(100vw - 64px);
    }
}
</style>
