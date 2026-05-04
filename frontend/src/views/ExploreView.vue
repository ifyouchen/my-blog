<script setup>
import {computed, onMounted, ref} from 'vue';
import {RouterLink, useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import EmptyState from '@/components/EmptyState.vue';
import {getCategoriesApi} from '@/api/categories';
import {getHotTagsApi} from '@/api/tags';
import {getTopicsApi} from '@/api/topic';
import {
    getFeaturedRecommendationArticlesApi,
    getRecommendedAuthorsApi,
    getRecommendedColumnsFromHubApi
} from '@/api/recommendations';

const router = useRouter();
const categories = ref([]);
const hotTags = ref([]);
const topics = ref([]);
const columns = ref([]);
const authors = ref([]);
const featuredArticles = ref([]);
const loading = ref(true);
const errorMessage = ref('');

const quickEntries = [
    { title: '搜索文章', desc: '按关键词、作者、分类继续筛选', to: '/search' },
    { title: '阅读榜单', desc: '先看近期热度最高的内容', to: '/ranking' },
    { title: '专题合集', desc: '围绕一个技术议题系统阅读', to: '/topics' },
    { title: '专栏连载', desc: '订阅作者整理好的长期内容', to: '/columns' }
];

const loadData = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const [cats, tags, topicPage, recColumns, recAuthors, recArticles] = await Promise.all([
            getCategoriesApi(),
            getHotTagsApi(32),
            getTopicsApi({ page: 1, pageSize: 6 }),
            getRecommendedColumnsFromHubApi(6),
            getRecommendedAuthorsApi(6),
            getFeaturedRecommendationArticlesApi({ page: 1, pageSize: 6 })
        ]);
        categories.value = Array.isArray(cats) ? cats : [];
        hotTags.value = Array.isArray(tags) ? tags : [];
        topics.value = topicPage?.items || [];
        columns.value = recColumns || [];
        authors.value = recAuthors || [];
        featuredArticles.value = recArticles || [];
    } catch (err) {
        errorMessage.value = '内容加载失败，请稍后重试';
    } finally {
        loading.value = false;
    }
};

const hotTagGroups = computed(() => hotTags.value.slice(0, 24));

const goToCategory = (category) => {
    router.push({ name: 'categoryDetail', params: { id: category.id } });
};

const goToTag = (tag) => {
    router.push({ name: 'tagDetail', params: { id: tag.id } });
};

onMounted(loadData);
</script>

<template>
    <SiteHeader />
    <main class="page-shell explore-page" data-testid="explore-page">
        <section class="explore-hero">
            <div>
                <p class="eyebrow">发现</p>
                <h1>技术内容导航</h1>
                <p class="explore-hero-desc">从分类、标签、专题、专栏和作者出发，找到下一篇值得读完的文章。</p>
            </div>
            <RouterLink class="explore-search-link" to="/search">搜索内容</RouterLink>
        </section>

        <EmptyState
            v-if="errorMessage"
            eyebrow="发现"
            title="加载失败"
            :description="errorMessage"
            tone="error"
        />

        <template v-else>
            <section class="explore-quick-grid" aria-label="发现入口">
                <RouterLink
                    v-for="entry in quickEntries"
                    :key="entry.to"
                    class="explore-quick-card"
                    :to="entry.to"
                >
                    <strong>{{ entry.title }}</strong>
                    <span>{{ entry.desc }}</span>
                </RouterLink>
            </section>

            <section class="explore-two-column">
                <section class="explore-section">
                    <div class="explore-section-header">
                        <h2>全部分类</h2>
                    </div>
                    <div v-if="loading" class="explore-grid">
                        <div v-for="i in 8" :key="i" class="explore-category-card explore-skeleton"></div>
                    </div>
                    <div v-else-if="categories.length" class="explore-grid">
                        <button
                            v-for="cat in categories"
                            :key="cat.id"
                            class="explore-category-card"
                            type="button"
                            @click="goToCategory(cat)"
                        >
                            <span class="explore-category-name">{{ cat.name }}</span>
                            <span v-if="cat.description" class="explore-category-desc">{{ cat.description }}</span>
                        </button>
                    </div>
                    <p v-else class="explore-empty">暂无分类</p>
                </section>

                <section class="explore-section">
                    <div class="explore-section-header">
                        <h2>热门标签</h2>
                        <p class="explore-section-sub">按使用量排序</p>
                    </div>
                    <div v-if="loading" class="explore-tags">
                        <div v-for="i in 20" :key="i" class="explore-tag explore-tag-skeleton"></div>
                    </div>
                    <div v-else-if="hotTagGroups.length" class="explore-tags">
                        <button
                            v-for="tag in hotTagGroups"
                            :key="tag.id"
                            class="explore-tag"
                            type="button"
                            @click="goToTag(tag)"
                        >
                            <span class="explore-tag-hash">#</span>{{ tag.name }}
                            <span v-if="tag.useCount" class="explore-tag-count">{{ tag.useCount }}</span>
                        </button>
                    </div>
                    <p v-else class="explore-empty">暂无热门标签</p>
                </section>
            </section>

            <section class="explore-section">
                <div class="explore-section-header">
                    <h2>热门专题</h2>
                    <RouterLink to="/topics">全部专题</RouterLink>
                </div>
                <div v-if="loading" class="explore-list">
                    <div v-for="i in 6" :key="i" class="explore-list-item explore-skeleton"></div>
                </div>
                <div v-else-if="topics.length" class="explore-list">
                    <RouterLink
                        v-for="topic in topics"
                        :key="topic.id"
                        class="explore-list-item"
                        :to="`/topics/${topic.id}`"
                    >
                        <img :src="topic.coverUrl" :alt="`${topic.title} 封面`" loading="lazy" decoding="async">
                        <span>
                            <strong>{{ topic.title }}</strong>
                            <em>{{ topic.articleCount }} 篇文章</em>
                        </span>
                    </RouterLink>
                </div>
                <p v-else class="explore-empty">暂无专题</p>
            </section>

            <section class="explore-two-column">
                <section class="explore-section">
                    <div class="explore-section-header">
                        <h2>推荐专栏</h2>
                        <RouterLink to="/columns">全部专栏</RouterLink>
                    </div>
                    <div v-if="loading" class="explore-list">
                        <div v-for="i in 4" :key="i" class="explore-list-item explore-skeleton"></div>
                    </div>
                    <div v-else-if="columns.length" class="explore-list">
                        <RouterLink
                            v-for="column in columns"
                            :key="column.id"
                            class="explore-list-item"
                            :to="`/columns/${column.id}`"
                        >
                            <img :src="column.coverUrl" :alt="`${column.title} 封面`" loading="lazy" decoding="async">
                            <span>
                                <strong>{{ column.title }}</strong>
                                <em>{{ column.articleCount }} 篇 · {{ column.subscriberCount }} 订阅</em>
                            </span>
                        </RouterLink>
                    </div>
                    <p v-else class="explore-empty">暂无推荐专栏</p>
                </section>

                <section class="explore-section">
                    <div class="explore-section-header">
                        <h2>推荐作者</h2>
                        <RouterLink to="/ranking">作者榜</RouterLink>
                    </div>
                    <div v-if="loading" class="explore-author-list">
                        <div v-for="i in 4" :key="i" class="explore-author-card explore-skeleton"></div>
                    </div>
                    <div v-else-if="authors.length" class="explore-author-list">
                        <div v-for="author in authors" :key="author.id" class="explore-author-card">
                            <RouterLink class="explore-author-avatar" :to="`/users/${author.id}`">
                                <img :src="author.avatar" :alt="`${author.name} 的头像`" loading="lazy" decoding="async">
                            </RouterLink>
                            <div class="explore-author-copy">
                                <RouterLink :to="`/users/${author.id}`">{{ author.name }}</RouterLink>
                                <p>{{ author.bio || '热爱技术写作' }}</p>
                            </div>
                            <div class="explore-author-stats">
                                <span>{{ author.articleCount }} 文章</span>
                                <span>{{ author.totalLikeCount }} 赞</span>
                                <span>{{ author.followerCount }} 粉丝</span>
                            </div>
                        </div>
                    </div>
                    <p v-else class="explore-empty">暂无推荐作者</p>
                </section>
            </section>

            <section class="explore-section">
                <div class="explore-section-header">
                    <h2>精选文章</h2>
                    <RouterLink to="/?sort=featured">更多精选</RouterLink>
                </div>
                <div v-if="loading" class="explore-featured-list">
                    <div v-for="i in 4" :key="i" class="post-item explore-skeleton"></div>
                </div>
                <div v-else-if="featuredArticles.length" class="explore-featured-list">
                    <article
                        v-for="article in featuredArticles"
                        :key="article.id"
                        class="post-item interactive-post"
                        role="link"
                        tabindex="0"
                        @click="$router.push(`/articles/${article.id}`)"
                        @keydown.enter="$router.push(`/articles/${article.id}`)"
                        @keydown.space.prevent="$router.push(`/articles/${article.id}`)"
                    >
                        <div class="post-cover">
                            <img :src="article.cover" :alt="article.coverAlt" loading="lazy" decoding="async">
                        </div>
                        <div class="post-content">
                            <div class="post-meta">
                                <span>{{ article.category }}</span>
                                <span>{{ article.readingTime }}</span>
                                <span>{{ article.publishedText }}</span>
                            </div>
                            <h3>{{ article.title }}</h3>
                            <p>{{ article.summary }}</p>
                            <div class="post-footer">
                                <RouterLink
                                    class="author author-hotspot"
                                    :to="`/users/${article.author.id}`"
                                    @click.stop
                                    @keydown.enter.stop
                                    @keydown.space.stop
                                >
                                    <img :src="article.author.avatar" :alt="`${article.author.name} 头像`" loading="lazy" decoding="async">
                                    <span>{{ article.author.name }}</span>
                                </RouterLink>
                                <span>{{ article.stats.views }}</span>
                                <span>{{ article.stats.likes }}</span>
                                <span>{{ article.stats.comments }}</span>
                            </div>
                        </div>
                    </article>
                </div>
                <p v-else class="explore-empty">暂无精选文章</p>
            </section>
        </template>
    </main>
</template>

<style scoped>
.explore-page {
    display: grid;
    gap: 24px;
}

.explore-hero {
    display: flex;
    gap: 20px;
    align-items: flex-end;
    justify-content: space-between;
    padding: 30px 0 8px;
    border-bottom: 1px solid var(--line);
}

.explore-hero h1 {
    margin: 4px 0 0;
    color: var(--text-strong);
    font-size: 34px;
    line-height: 1.18;
}

.explore-hero-desc {
    max-width: 680px;
    margin: 10px 0 0;
    color: var(--muted);
    font-size: 15px;
    line-height: 1.8;
}

.explore-search-link,
.explore-section-header a {
    color: var(--brand);
    font-size: 13px;
    font-weight: 700;
    text-decoration: none;
}

.explore-search-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 36px;
    padding: 0 14px;
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-sm);
}

.explore-quick-grid,
.explore-card-grid,
.explore-article-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
    gap: 12px;
}

.explore-quick-card,
.explore-card,
.explore-article-card,
.explore-list-item,
.explore-category-card {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    text-decoration: none;
    transition: border-color 0.15s, background 0.15s, transform 0.15s;
}

.explore-quick-card:hover,
.explore-card:hover,
.explore-article-card:hover,
.explore-list-item:hover,
.explore-category-card:hover {
    background: var(--surface-soft);
    border-color: var(--brand-hover);
    transform: translateY(-1px);
}

.explore-quick-card {
    display: grid;
    gap: 5px;
    padding: 16px;
}

.explore-quick-card strong,
.explore-card strong,
.explore-article-card strong,
.explore-list-item strong,
.explore-category-name {
    color: var(--text);
    font-size: 15px;
    font-weight: 700;
}

.explore-quick-card span,
.explore-card span,
.explore-list-item em,
.explore-article-card em,
.explore-category-desc {
    color: var(--muted);
    font-size: 12px;
    font-style: normal;
    line-height: 1.55;
}

.explore-two-column {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 24px;
    align-items: start;
}

.explore-section {
    display: grid;
    gap: 14px;
    min-width: 0;
}

.explore-section-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 10px;
}

.explore-section-header h2 {
    margin: 0;
    color: var(--text-strong);
    font-size: 18px;
    font-weight: 700;
}

.explore-section-sub {
    margin: 0 auto 0 0;
    color: var(--muted);
    font-size: 13px;
}

.explore-empty {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
}

.explore-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 10px;
}

.explore-category-card {
    display: grid;
    gap: 6px;
    min-height: 78px;
    padding: 14px;
    cursor: pointer;
    text-align: left;
}

.explore-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.explore-tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    min-height: 32px;
    padding: 0 12px;
    color: var(--text);
    font-size: 13px;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.explore-tag:hover {
    border-color: var(--brand-hover);
    background: var(--surface-soft);
}

.explore-tag-hash {
    color: var(--brand);
    font-weight: 700;
}

.explore-tag-count {
    margin-left: 4px;
    color: var(--muted);
    font-size: 11px;
}

.explore-card {
    display: grid;
    gap: 8px;
    padding: 12px;
}

.explore-card img {
    width: 100%;
    aspect-ratio: 16 / 8;
    object-fit: cover;
    border-radius: var(--radius-sm);
}

.explore-list {
    display: grid;
    gap: 10px;
}

.explore-list-item {
    display: grid;
    grid-template-columns: 48px minmax(0, 1fr);
    gap: 12px;
    align-items: center;
    min-height: 66px;
    padding: 9px 10px;
}

.explore-list-item img {
    width: 48px;
    height: 48px;
    object-fit: cover;
    border-radius: var(--radius-sm);
}

.explore-list-item span {
    display: grid;
    gap: 4px;
    min-width: 0;
}

.explore-list-item strong,
.explore-list-item em {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.explore-article-card {
    display: grid;
    gap: 8px;
    min-height: 130px;
    padding: 16px;
}

.explore-article-card span {
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
}

.explore-article-card strong {
    line-height: 1.55;
}

.explore-skeleton,
.explore-tag-skeleton {
    min-height: 72px;
    background: linear-gradient(90deg, var(--surface-soft) 25%, var(--line) 37%, var(--surface-soft) 63%);
    background-size: 400% 100%;
    border: 0;
    animation: explore-skeleton 1.2s ease-in-out infinite;
}

.explore-tag-skeleton {
    width: 80px;
    min-height: 32px;
}

@keyframes explore-skeleton {
    0% {
        background-position: 100% 0;
    }

    100% {
        background-position: 0 0;
    }
}

.explore-featured-list {
    display: flex;
    flex-direction: column;
}

/* 推荐作者卡片 */
.explore-author-list {
    display: grid;
    gap: 0;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    overflow: hidden;
}

.explore-author-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--line);
    transition: background 0.12s;
}

.explore-author-card:last-child {
    border-bottom: none;
}

.explore-author-card:hover {
    background: var(--surface-soft);
}

.explore-author-avatar {
    flex-shrink: 0;
    display: inline-flex;
}

.explore-author-avatar img {
    width: 38px;
    height: 38px;
    object-fit: cover;
    border-radius: 50%;
    border: 1px solid var(--line);
}

.explore-author-copy {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.explore-author-copy a {
    display: block;
    color: var(--text-strong);
    font-weight: 600;
    font-size: 14px;
    text-decoration: none;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: color 0.15s;
}

.explore-author-copy a:hover {
    color: var(--brand);
}

.explore-author-copy p {
    margin: 0;
    color: var(--muted);
    font-size: 12px;
    line-height: 1.4;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.explore-author-follower {
    flex-shrink: 0;
    color: var(--muted);
    font-size: 12px;
    white-space: nowrap;
}

.explore-author-stats {
    flex-shrink: 0;
    display: flex;
    gap: 12px;
    color: var(--muted);
    font-size: 12px;
    white-space: nowrap;
}

/* interactive-post 交互效果 */
.interactive-post {
    cursor: pointer;
    transition: background 0.12s;
}

.interactive-post:hover,
.interactive-post:focus-visible {
    background: var(--surface-soft);
}

.interactive-post:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: 2px;
}

.interactive-post:hover h3,
.interactive-post:focus-visible h3 {
    color: var(--brand);
}

.interactive-post:hover .post-cover img,
.interactive-post:focus-visible .post-cover img {
    transform: scale(1.02);
}

.post-cover img {
    transition: transform 0.2s ease;
}

.author-hotspot {
    position: relative;
    z-index: 1;
    transition: color 0.12s;
}

.author-hotspot:hover,
.author-hotspot:focus-visible {
    color: var(--brand);
}

@media (max-width: 820px) {
    .explore-hero,
    .explore-two-column {
        grid-template-columns: 1fr;
    }

    .explore-hero {
        display: grid;
        align-items: start;
    }
}

@media (max-width: 640px) {
    .explore-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}
</style>
