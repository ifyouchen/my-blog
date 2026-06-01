<script setup>
import {computed, ref} from 'vue';
import {RouterLink} from 'vue-router';

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
                <p class="eyebrow">编辑精选 · 本周</p>
                <h2>本周必读</h2>
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

<style scoped src="@/styles/components/FeaturedArticlesStrip.part-1.css"></style>
<style scoped src="@/styles/components/FeaturedArticlesStrip.part-2.css"></style>
