<script setup>
import {computed} from 'vue';
import {RouterLink} from 'vue-router';
import {DEFAULT_ARTICLE_COVER_URL} from '@/utils/media';

const props = defineProps({
    articles: {
        type: Array,
        default: () => []
    },
    topics: {
        type: Array,
        default: () => []
    },
    columns: {
        type: Array,
        default: () => []
    },
    stats: {
        type: Object,
        default: () => ({})
    },
    loading: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['article-click']);

const PICK_SKELETON_ROWS = [
    { tag: '56px', title: '88%', meta: '48%' },
    { tag: '64px', title: '76%', meta: '42%' },
    { tag: '52px', title: '82%', meta: '46%' },
    { tag: '60px', title: '70%', meta: '38%' }
];
const HOTSPOT_SKELETON_ROWS = ['78%', '64%', '72%'];

const primaryArticle = computed(() => props.articles[0] || null);
const secondaryArticles = computed(() => props.articles.slice(1, 5));
const visibleTopics = computed(() => props.topics.slice(0, 3));
const visibleColumns = computed(() => props.columns.slice(0, 3));
const showPickSkeleton = computed(() => props.loading && !secondaryArticles.value.length);
const showTopicSkeleton = computed(() => props.loading && !visibleTopics.value.length);
const showColumnSkeleton = computed(() => props.loading && !visibleColumns.value.length);

const articlePath = (article) => {
    if (!article) return '/';
    return article.slug ? `/articles/${article.id}-${article.slug}` : `/articles/${article.id}`;
};

const compactNumber = (value) => {
    const number = Number(value || 0);
    if (number >= 10000) {
        return `${(number / 10000).toFixed(number >= 100000 ? 0 : 1)}万`;
    }
    return String(number);
};

const pickMetaText = (article) => {
    const publishedText = article?.publishedText || '刚刚更新';
    const likesText = article?.stats?.likes || `${article?.likeCount || 0} 赞`;
    return `${publishedText} · ${likesText}`;
};

const emitArticleClick = (article, area) => {
    emit('article-click', {
        article_id: article?.id,
        area
    });
};

const setCoverFallback = (event) => {
    if (event.target.src.includes(DEFAULT_ARTICLE_COVER_URL)) {
        return;
    }
    event.target.src = DEFAULT_ARTICLE_COVER_URL;
};
</script>

<template>
    <section class="home-portal-hero" aria-label="首页精选" data-testid="home-portal-hero">
        <RouterLink
            v-if="primaryArticle"
            class="portal-focus-card"
            data-testid="home-focus-article"
            :to="articlePath(primaryArticle)"
            @click="emitArticleClick(primaryArticle, 'focus')"
        >
            <span class="portal-focus-cover">
                <img
                    :src="primaryArticle.cover"
                    :alt="primaryArticle.coverAlt"
                    loading="eager"
                    decoding="async"
                    @error="setCoverFallback"
                >
            </span>
            <span class="portal-focus-body">
                <span class="portal-eyebrow">今日焦点</span>
                <span class="portal-focus-title">{{ primaryArticle.title }}</span>
                <span class="portal-focus-summary">{{ primaryArticle.summary }}</span>
                <span class="portal-meta">
                    <span>{{ primaryArticle.category || '技术' }}</span>
                    <span>{{ primaryArticle.readingTime || '深度阅读' }}</span>
                    <span>{{ primaryArticle.stats?.views || `${primaryArticle.viewCount || 0} 阅读` }}</span>
                </span>
            </span>
        </RouterLink>

        <div v-else class="portal-focus-card portal-empty-card" data-testid="home-focus-article" :aria-busy="loading">
            <span class="portal-empty-cover"></span>
            <span class="portal-focus-body">
                <span class="portal-eyebrow">今日焦点</span>
                <span class="portal-focus-title">正在整理今日值得读的内容</span>
                <span class="portal-focus-summary">稍后会展示精选文章、热门专题和推荐专栏。</span>
            </span>
        </div>

        <section class="portal-picks" aria-labelledby="portal-picks-title" data-testid="home-weekly-picks">
            <header class="portal-panel-head">
                <p class="portal-eyebrow">本周必读</p>
                <h2 id="portal-picks-title">精选文章</h2>
            </header>
            <div class="portal-pick-list">
                <template v-if="showPickSkeleton">
                    <div
                        v-for="(row, index) in PICK_SKELETON_ROWS"
                        :key="`pick-skeleton-${index}`"
                        class="portal-pick-item portal-pick-skeleton"
                        aria-hidden="true"
                    >
                        <span class="portal-skeleton-block portal-pick-skeleton-tag" :style="{ width: row.tag }"></span>
                        <span
                            class="portal-skeleton-block portal-pick-skeleton-title"
                            :style="{ width: row.title }"
                        ></span>
                        <span
                            class="portal-skeleton-block portal-pick-skeleton-meta"
                            :style="{ width: row.meta }"
                        ></span>
                    </div>
                </template>
                <template v-else>
                    <RouterLink
                        v-for="article in secondaryArticles"
                        :key="article.id"
                        class="portal-pick-item"
                        :to="articlePath(article)"
                        @click="emitArticleClick(article, 'weekly_pick')"
                    >
                        <span class="portal-pick-tag">{{ article.category || '技术' }}</span>
                        <span class="portal-pick-title">{{ article.title }}</span>
                        <span class="portal-pick-meta">
                            {{ pickMetaText(article) }}
                        </span>
                    </RouterLink>
                    <div v-if="!secondaryArticles.length" class="portal-muted-state">
                        暂时没有更多精选文章
                    </div>
                </template>
            </div>
        </section>

        <aside class="portal-hotspots" aria-label="首页热点入口" data-testid="home-hotspots">
            <section class="portal-hot-section">
                <header class="portal-panel-head inline">
                    <p class="portal-eyebrow">热门专题</p>
                    <RouterLink to="/topics">更多</RouterLink>
                </header>
                <div class="portal-hot-list">
                    <template v-if="showTopicSkeleton">
                        <div
                            v-for="(width, index) in HOTSPOT_SKELETON_ROWS"
                            :key="`topic-skeleton-${index}`"
                            class="portal-hot-item portal-hot-skeleton"
                            aria-hidden="true"
                        >
                            <span class="portal-skeleton-block portal-hot-skeleton-title" :style="{ width }"></span>
                            <small class="portal-skeleton-block portal-hot-skeleton-count"></small>
                        </div>
                    </template>
                    <template v-else>
                        <RouterLink
                            v-for="topic in visibleTopics"
                            :key="topic.id"
                            class="portal-hot-item"
                            :to="`/topics/${topic.id}`"
                        >
                            <span>{{ topic.title }}</span>
                            <small>{{ compactNumber(topic.articleCount) }} 篇</small>
                        </RouterLink>
                        <div v-if="!visibleTopics.length" class="portal-muted-state compact">
                            暂无专题
                        </div>
                    </template>
                </div>
            </section>

            <section class="portal-hot-section">
                <header class="portal-panel-head inline">
                    <p class="portal-eyebrow">热门专栏</p>
                    <RouterLink to="/columns">更多</RouterLink>
                </header>
                <div class="portal-hot-list">
                    <template v-if="showColumnSkeleton">
                        <div
                            v-for="(width, index) in HOTSPOT_SKELETON_ROWS"
                            :key="`column-skeleton-${index}`"
                            class="portal-hot-item portal-hot-skeleton"
                            aria-hidden="true"
                        >
                            <span class="portal-skeleton-block portal-hot-skeleton-title" :style="{ width }"></span>
                            <small class="portal-skeleton-block portal-hot-skeleton-count"></small>
                        </div>
                    </template>
                    <template v-else>
                        <RouterLink
                            v-for="column in visibleColumns"
                            :key="column.id"
                            class="portal-hot-item"
                            :to="`/columns/${column.id}`"
                        >
                            <span>{{ column.title }}</span>
                            <small>{{ compactNumber(column.articleCount) }} 篇</small>
                        </RouterLink>
                        <div v-if="!visibleColumns.length" class="portal-muted-state compact">
                            暂无专栏
                        </div>
                    </template>
                </div>
            </section>

            <RouterLink class="portal-rank-card" to="/ranking">
                <span>
                    <strong>排行榜</strong>
                    <small>看看最近被反复阅读的内容</small>
                </span>
                <span class="portal-rank-count">{{ compactNumber(stats.totalArticles) }}</span>
            </RouterLink>
        </aside>
    </section>
</template>

<style scoped>
.home-portal-hero {
    display: grid;
    grid-template-columns: minmax(0, 1.14fr) minmax(300px, 0.82fr) minmax(280px, 0.64fr);
    gap: 16px;
    align-items: stretch;
    margin: 12px 0 20px;
}

.portal-focus-card,
.portal-picks,
.portal-hotspots {
    min-width: 0;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: var(--shadow-soft);
}

.portal-focus-card {
    position: relative;
    display: grid;
    min-height: 342px;
    overflow: hidden;
    color: #ffffff;
    text-decoration: none;
}

.portal-focus-cover {
    position: absolute;
    inset: 0;
    background: var(--surface-muted);
}

.portal-focus-cover img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.25s ease;
}

.portal-focus-card:hover .portal-focus-cover img,
.portal-focus-card:focus-visible .portal-focus-cover img {
    transform: scale(1.02);
}

.portal-focus-card::after {
    position: absolute;
    inset: 0;
    content: "";
    background: linear-gradient(180deg, rgba(15, 23, 42, 0.06) 0%, rgba(15, 23, 42, 0.78) 72%);
}

.portal-focus-body {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    gap: 10px;
    min-width: 0;
    padding: 24px;
}

.portal-eyebrow {
    margin: 0;
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0;
}

.portal-focus-card .portal-eyebrow {
    color: rgba(255, 255, 255, 0.82);
}

.portal-focus-title {
    display: -webkit-box;
    overflow: hidden;
    color: inherit;
    font-size: 28px;
    font-weight: 800;
    line-height: 1.25;
    letter-spacing: 0;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.portal-focus-summary {
    display: -webkit-box;
    overflow: hidden;
    max-width: 640px;
    color: rgba(255, 255, 255, 0.86);
    font-size: 14px;
    line-height: 1.65;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.portal-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    color: rgba(255, 255, 255, 0.76);
    font-size: 12px;
}

.portal-meta span:not(:last-child)::after {
    margin-left: 8px;
    content: "/";
    color: rgba(255, 255, 255, 0.42);
}

.portal-empty-card {
    color: var(--text);
    background: var(--surface-soft);
}

.portal-empty-card::after {
    display: none;
}

.portal-empty-cover {
    position: absolute;
    inset: 0;
    background:
        linear-gradient(120deg, rgba(37, 99, 235, 0.08), transparent 48%),
        var(--surface-soft);
}

.portal-empty-card .portal-focus-summary {
    color: var(--muted);
}

.portal-picks,
.portal-hotspots {
    display: grid;
    align-content: start;
    gap: 14px;
    padding: 18px;
}

.portal-panel-head {
    display: grid;
    gap: 3px;
}

.portal-panel-head h2 {
    margin: 0;
    color: var(--text-strong);
    font-size: 20px;
    line-height: 1.3;
}

.portal-panel-head.inline {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.portal-panel-head.inline a {
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
    text-decoration: none;
}

.portal-panel-head.inline a:hover,
.portal-panel-head.inline a:focus-visible {
    color: var(--brand);
}

.portal-pick-list,
.portal-hot-list {
    display: grid;
    gap: 8px;
}

.portal-pick-item {
    display: grid;
    gap: 5px;
    padding: 12px 0;
    color: inherit;
    text-decoration: none;
    border-top: 1px solid var(--line);
}

.portal-pick-item:first-child {
    border-top: 0;
}

.portal-pick-item:hover .portal-pick-title,
.portal-pick-item:focus-visible .portal-pick-title {
    color: var(--brand);
}

.portal-pick-tag {
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
}

.portal-pick-title {
    display: -webkit-box;
    overflow: hidden;
    color: var(--text-strong);
    font-size: 15px;
    font-weight: 700;
    line-height: 1.45;
    transition: color 0.12s;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.portal-pick-meta {
    overflow: hidden;
    color: var(--muted);
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.portal-hotspots {
    box-shadow: none;
}

.portal-hot-section {
    display: grid;
    gap: 9px;
}

.portal-hot-item,
.portal-rank-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    min-width: 0;
    padding: 10px 0;
    color: var(--text);
    text-decoration: none;
    border-top: 1px solid var(--line);
}

.portal-hot-item:first-child {
    border-top: 0;
}

.portal-hot-item span {
    overflow: hidden;
    color: var(--text-strong);
    font-size: 13px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
    transition: color 0.12s;
}

.portal-hot-item small {
    flex: 0 0 auto;
    color: var(--muted);
    font-size: 12px;
}

.portal-hot-item:hover span,
.portal-hot-item:focus-visible span {
    color: var(--brand);
}

.portal-rank-card {
    align-items: flex-start;
    margin-top: 2px;
    padding: 14px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.portal-rank-card span:first-child {
    display: grid;
    gap: 4px;
    min-width: 0;
}

.portal-rank-card strong {
    color: var(--text-strong);
    font-size: 14px;
}

.portal-rank-card small {
    color: var(--muted);
    font-size: 12px;
    line-height: 1.5;
}

.portal-rank-count {
    flex: 0 0 auto;
    color: var(--brand);
    font-size: 16px;
    font-weight: 800;
}

.portal-muted-state {
    padding: 12px 0;
    color: var(--muted);
    font-size: 13px;
}

.portal-muted-state.compact {
    padding: 8px 0;
    font-size: 12px;
}

.portal-pick-skeleton,
.portal-hot-skeleton {
    cursor: default;
    pointer-events: none;
}

.portal-skeleton-block {
    display: block;
    height: 12px;
    background: var(--surface-muted);
    border-radius: var(--radius-sm);
}

.portal-pick-skeleton-tag {
    height: 10px;
}

.portal-pick-skeleton-title {
    height: 15px;
}

.portal-pick-skeleton-meta {
    height: 10px;
}

.portal-hot-skeleton-title {
    height: 13px;
}

.portal-hot-skeleton-count {
    width: 36px;
    height: 11px;
}

@media (max-width: 1180px) {
    .home-portal-hero {
        grid-template-columns: minmax(0, 1fr) minmax(300px, 0.68fr);
    }

    .portal-hotspots {
        grid-column: 1 / -1;
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }
}

@media (max-width: 860px) {
    .home-portal-hero {
        grid-template-columns: minmax(0, 1fr);
        gap: 12px;
        margin-top: 10px;
    }

    .portal-hotspots {
        grid-template-columns: minmax(0, 1fr);
    }

    .portal-focus-card {
        min-height: 300px;
    }
}

@media (max-width: 560px) {
    .home-portal-hero {
        margin-bottom: 16px;
    }

    .portal-focus-card {
        min-height: 260px;
    }

    .portal-focus-body {
        padding: 18px;
    }

    .portal-focus-title {
        font-size: 22px;
    }

    .portal-picks,
    .portal-hotspots {
        padding: 14px;
    }
}
</style>
