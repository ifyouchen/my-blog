<script setup>
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';

const SKELETON_CARD_COUNT = 5;
const FEATURED_PREVIEW_COUNT = 5;

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

const emit = defineEmits(['article-click']);

const showSkeleton = computed(() => props.loading && props.articles.length === 0);
const shouldRenderStrip = computed(() => showSkeleton.value || props.articles.length > 0);
const expanded = ref(false);
const visibleArticles = computed(() => (
    expanded.value ? props.articles : props.articles.slice(0, FEATURED_PREVIEW_COUNT)
));
const primaryArticle = computed(() => visibleArticles.value[0]);
const secondaryArticles = computed(() => visibleArticles.value.slice(1));

const articlePath = (article) => {
    if (!article) {
        return '/';
    }
    return article.slug ? `/articles/${article.id}-${article.slug}` : `/articles/${article.id}`;
};

const formatCount = (value, unit) => {
    const count = Number(value || 0);
    if (count >= 10000) {
        return `${(count / 10000).toFixed(count >= 100000 ? 0 : 1)}万${unit}`;
    }
    return `${count}${unit}`;
};

const getFeaturedReason = (article) => {
    if (!article) {
        return '编辑精选';
    }
    if (article.featured) {
        return '编辑精选';
    }
    if ((article.commentCount || 0) >= 10) {
        return '讨论活跃';
    }
    if ((article.likeCount || 0) >= 20) {
        return '高赞内容';
    }
    if ((article.viewCount || 0) >= 500) {
        return '本周热读';
    }
    if (article.category) {
        return `${article.category} 优选`;
    }
    return '值得一读';
};

const getFeaturedMetric = (article) => {
    if (!article) {
        return '';
    }
    if ((article.commentCount || 0) >= 10) {
        return formatCount(article.commentCount, '评论');
    }
    if ((article.likeCount || 0) >= 1) {
        return formatCount(article.likeCount, '赞');
    }
    return formatCount(article.viewCount, '阅读');
};

const handleArticleClick = (article, slot) => {
    emit('article-click', {
        articleId: article.id,
        slot,
        reason: getFeaturedReason(article)
    });
};
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
            <div>
                <p class="eyebrow">编辑精选</p>
                <h2>先读这几篇</h2>
            </div>
            <RouterLink class="featured-more-link" to="/?sort=featured">全部精选</RouterLink>
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
        <div v-else class="featured-compact-layout">
            <RouterLink
                v-if="primaryArticle"
                :to="articlePath(primaryArticle)"
                class="featured-primary-card"
                @click="handleArticleClick(primaryArticle, 'hero')"
            >
                <span class="featured-primary-cover">
                    <img
                        v-if="primaryArticle.cover || primaryArticle.coverUrl"
                        :src="primaryArticle.cover || primaryArticle.coverUrl"
                        :alt="`${primaryArticle.title} 封面`"
                        loading="lazy"
                        decoding="async"
                    >
                    <span v-else class="featured-primary-fallback" aria-hidden="true"></span>
                </span>
                <span class="featured-primary-body">
                    <div class="featured-reason-row">
                        <span class="featured-reason">{{ getFeaturedReason(primaryArticle) }}</span>
                        <span
                            v-if="primaryArticle.category"
                            class="featured-card-category"
                        >{{ primaryArticle.category }}</span>
                    </div>
                    <h3>{{ primaryArticle.title }}</h3>
                    <p v-if="primaryArticle.summary">{{ primaryArticle.summary }}</p>
                    <span class="featured-primary-meta">
                        <span>{{ primaryArticle.author?.name || primaryArticle.author?.nickname }}</span>
                        <span>{{ primaryArticle.readingTime }}</span>
                        <span>{{ getFeaturedMetric(primaryArticle) }}</span>
                    </span>
                </span>
            </RouterLink>
            <div class="featured-list">
                <RouterLink
                    v-for="article in secondaryArticles"
                    :key="article.id"
                    :to="articlePath(article)"
                    class="featured-list-item"
                    @click="handleArticleClick(article, 'list')"
                >
                    <div class="featured-list-topline">
                        <span class="featured-reason">{{ getFeaturedReason(article) }}</span>
                        <span v-if="article.category" class="featured-card-category">{{ article.category }}</span>
                    </div>
                    <h4>{{ article.title }}</h4>
                    <div class="featured-card-meta">
                        <span>{{ article.author?.name || article.author?.nickname }}</span>
                        <span class="middot">&middot;</span>
                        <span>{{ getFeaturedMetric(article) }}</span>
                    </div>
                </RouterLink>
            </div>
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
    gap: 8px;
}

.featured-header {
    display: flex;
    align-items: end;
    justify-content: space-between;
    gap: 12px;
}

.featured-header h2 {
    margin: 0;
    font-size: 17px;
}

.featured-more-link {
    flex-shrink: 0;
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
    text-decoration: none;
}

.featured-more-link:hover,
.featured-more-link:focus-visible {
    color: var(--brand);
}

.featured-scroll,
.featured-compact-layout {
    display: grid;
    gap: 10px;
}

.featured-compact-layout {
    grid-template-columns: minmax(0, 1.05fr) minmax(0, 1.2fr);
    align-items: stretch;
}

.featured-primary-card {
    display: grid;
    grid-template-columns: 144px minmax(0, 1fr);
    gap: 12px;
    min-height: 122px;
    padding: 10px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    text-decoration: none;
}

.featured-primary-cover {
    display: block;
    min-width: 0;
    overflow: hidden;
    border-radius: var(--radius-sm);
    background: var(--surface-muted);
}

.featured-primary-cover img,
.featured-primary-fallback {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    min-height: 102px;
}

.featured-primary-fallback {
    background: linear-gradient(135deg, var(--surface-muted), var(--brand-soft));
}

.featured-primary-body {
    display: grid;
    gap: 6px;
    min-width: 0;
    align-content: start;
}

.featured-primary-body h3 {
    display: -webkit-box;
    margin: 0;
    color: var(--text);
    overflow: hidden;
    font-size: 15px;
    line-height: 1.4;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    transition: color 0.18s ease;
}

.featured-primary-body p {
    margin: 0;
    display: -webkit-box;
    overflow: hidden;
    color: var(--muted);
    font-size: 12px;
    line-height: 1.45;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
}

.featured-reason-row,
.featured-list-topline,
.featured-primary-meta {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
    flex-wrap: wrap;
}

.featured-primary-meta {
    color: var(--muted);
    font-size: 12px;
}

.featured-primary-meta span:not(:last-child)::after {
    content: "";
    display: inline-block;
    width: 3px;
    height: 3px;
    margin-left: 6px;
    vertical-align: middle;
    border-radius: 999px;
    background: var(--line-strong);
}

.featured-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 6px;
    align-content: stretch;
}

.featured-list-item {
    display: grid;
    gap: 5px;
    min-height: 48px;
    padding: 8px 10px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    text-decoration: none;
    background: var(--surface);
}

.featured-card {
    display: flex;
    flex-direction: column;
    flex: 0 0 240px;
    overflow: hidden;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
}

.featured-card-cover {
    width: 100%;
    aspect-ratio: 16 / 8;
    background: var(--surface-muted);
}

.featured-card-body {
    display: grid;
    gap: 4px;
    min-height: 98px;
    padding: 10px 12px 12px;
}

.featured-list-item:hover,
.featured-list-item:focus-visible,
.featured-primary-card:hover,
.featured-primary-card:focus-visible {
    background: var(--surface-soft);
    border-color: var(--line-strong);
}

.featured-list-item:focus-visible,
.featured-primary-card:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: 2px;
}

.featured-card-category {
    color: var(--brand);
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0;
}

.featured-reason {
    display: inline-flex;
    align-items: center;
    height: 20px;
    padding: 0 7px;
    border-radius: 999px;
    color: var(--brand-strong);
    background: var(--brand-soft);
    font-size: 11px;
    font-weight: 700;
    line-height: 1;
}

.featured-list-item h4 {
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

.featured-list-item:hover h4,
.featured-list-item:focus-visible h4,
.featured-primary-card:hover h3,
.featured-primary-card:focus-visible h3 {
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
    .featured-compact-layout {
        grid-template-columns: 1fr;
    }

    .featured-list {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 720px) {
    .featured-primary-card {
        grid-template-columns: 96px minmax(0, 1fr);
        min-height: 104px;
        gap: 10px;
    }

    .featured-list {
        display: grid;
        grid-template-columns: 1fr;
        overflow-x: visible;
        padding-bottom: 0;
    }

    .featured-list-item {
        min-height: auto;
    }
}

@media (max-width: 480px) {
    .featured-primary-card {
        grid-template-columns: 1fr;
    }

    .featured-primary-cover {
        display: none;
    }
}
</style>
