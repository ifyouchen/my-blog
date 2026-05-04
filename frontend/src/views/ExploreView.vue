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
const showAllCategories = ref(false);
const showAllTags = ref(false);

const quickEntries = [
    {title: '找文章', desc: '用关键词直达具体内容', to: '/search'},
    {title: '按专题看', desc: '围绕议题系统阅读', to: '/topics'},
    {title: '追专栏', desc: '跟进连续更新的内容', to: '/columns'},
    {title: '看作者', desc: '从活跃作者开始探索', to: '/ranking'}
];

const loadData = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const [cats, tags, topicPage, recColumns, recAuthors, recArticles] = await Promise.all([
            getCategoriesApi(),
            getHotTagsApi(32),
            getTopicsApi({page: 1, pageSize: 6}),
            getRecommendedColumnsFromHubApi(6),
            getRecommendedAuthorsApi(6),
            getFeaturedRecommendationArticlesApi({page: 1, pageSize: 6})
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

const primaryArticle = computed(() => featuredArticles.value[0] || null);
const secondaryArticles = computed(() => featuredArticles.value.slice(1, 5));
const visibleCategories = computed(() => (
    showAllCategories.value ? categories.value : categories.value.slice(0, 8)
));
const visibleTags = computed(() => (
    showAllTags.value ? hotTags.value : hotTags.value.slice(0, 18)
));
const hasMoreCategories = computed(() => categories.value.length > visibleCategories.value.length);
const hasMoreTags = computed(() => hotTags.value.length > visibleTags.value.length);

const goToCategory = (category) => {
    router.push({name: 'categoryDetail', params: {id: category.id}});
};

const goToTag = (tag) => {
    router.push({name: 'tagDetail', params: {id: tag.id}});
};

const articlePath = (article) => `/articles/${article.id}`;

const hasTextSelectionInside = (event) => {
    const selection = window.getSelection?.();
    if (!selection || selection.isCollapsed || !selection.toString().trim()) {
        return false;
    }

    const target = event?.currentTarget;
    if (!target) {
        return true;
    }

    const anchorInside = selection.anchorNode && target.contains(selection.anchorNode);
    const focusInside = selection.focusNode && target.contains(selection.focusNode);
    return anchorInside || focusInside;
};

const openArticle = (article) => {
    router.push(articlePath(article));
};

const openArticleCard = (article, event) => {
    if (hasTextSelectionInside(event)) {
        return;
    }
    openArticle(article);
};

onMounted(loadData);
</script>

<template>
    <SiteHeader />
    <main class="page-shell explore-page" data-testid="explore-page">
        <section class="explore-head">
            <div class="explore-title">
                <p class="eyebrow">发现</p>
                <h1>发现值得读的内容</h1>
            </div>
            <div class="explore-head-actions">
                <RouterLink class="explore-search-link" to="/search">搜索内容</RouterLink>
                <RouterLink class="explore-rank-link" to="/ranking">查看榜单</RouterLink>
            </div>
        </section>

        <EmptyState
            v-if="errorMessage"
            eyebrow="发现"
            title="加载失败"
            :description="errorMessage"
            tone="error"
        />

        <template v-else>
            <section class="explore-paths" aria-label="发现入口">
                <RouterLink
                    v-for="entry in quickEntries"
                    :key="entry.to"
                    class="explore-path"
                    :to="entry.to"
                >
                    <strong>{{ entry.title }}</strong>
                    <span>{{ entry.desc }}</span>
                </RouterLink>
            </section>

            <section class="explore-section explore-featured">
                <div class="explore-section-header">
                    <div>
                        <h2>精选文章</h2>
                        <p>先从这几篇开始读</p>
                    </div>
                    <RouterLink to="/?sort=featured">更多精选</RouterLink>
                </div>

                <div v-if="loading" class="explore-featured-board">
                    <div class="explore-primary-card explore-skeleton"></div>
                    <div class="explore-side-stack">
                        <div v-for="i in 4" :key="i" class="explore-compact-article explore-skeleton"></div>
                    </div>
                </div>

                <div v-else-if="primaryArticle" class="explore-featured-board">
                    <article class="explore-primary-card">
                        <RouterLink class="explore-primary-cover" :to="`/articles/${primaryArticle.id}`">
                            <img
                                v-if="primaryArticle.cover"
                                :src="primaryArticle.cover"
                                :alt="primaryArticle.coverAlt"
                                loading="lazy"
                                decoding="async"
                            >
                            <span v-else>{{ primaryArticle.category || '精选' }}</span>
                        </RouterLink>
                        <div class="explore-primary-copy">
                            <div class="explore-meta">
                                <span>{{ primaryArticle.category }}</span>
                                <span>{{ primaryArticle.readingTime }}</span>
                                <span>{{ primaryArticle.publishedText }}</span>
                            </div>
                            <h3>{{ primaryArticle.title }}</h3>
                            <p>{{ primaryArticle.summary }}</p>
                            <div class="explore-article-footer">
                                <RouterLink
                                    class="explore-author"
                                    :to="`/users/${primaryArticle.author.id}`"
                                >
                                    <img
                                        :src="primaryArticle.author.avatar"
                                        :alt="`${primaryArticle.author.name} 头像`"
                                        loading="lazy"
                                        decoding="async"
                                    >
                                    <span>{{ primaryArticle.author.name }}</span>
                                </RouterLink>
                                <RouterLink class="explore-open-link" :to="`/articles/${primaryArticle.id}`">
                                    打开文章
                                </RouterLink>
                            </div>
                        </div>
                    </article>

                    <div class="explore-side-stack">
                        <article
                            v-for="article in secondaryArticles"
                            :key="article.id"
                            class="explore-compact-article"
                            role="link"
                            tabindex="0"
                            :aria-label="`阅读文章：${article.title}`"
                            @click="openArticleCard(article, $event)"
                            @keydown.enter="openArticle(article)"
                            @keydown.space.prevent="openArticle(article)"
                        >
                            <div class="explore-meta">
                                <span>{{ article.category }}</span>
                                <span>{{ article.readingTime }}</span>
                            </div>
                            <h3>
                                <RouterLink
                                    class="explore-compact-title"
                                    :to="articlePath(article)"
                                    @click.stop
                                    @keydown.enter.stop
                                    @keydown.space.stop
                                >
                                    {{ article.title }}
                                </RouterLink>
                            </h3>
                            <p>{{ article.summary }}</p>
                            <div class="explore-compact-actions">
                                <span>{{ article.stats.views }}</span>
                                <RouterLink
                                    :to="articlePath(article)"
                                    @click.stop
                                    @keydown.enter.stop
                                    @keydown.space.stop
                                >
                                    打开
                                </RouterLink>
                            </div>
                        </article>
                    </div>
                </div>

                <p v-else class="explore-empty">暂无精选文章</p>
            </section>

            <section class="explore-two-column">
                <section class="explore-section">
                    <div class="explore-section-header">
                        <div>
                            <h2>分类</h2>
                            <p>按方向筛选内容</p>
                        </div>
                    </div>
                    <div v-if="loading" class="explore-category-grid">
                        <div v-for="i in 8" :key="i" class="explore-category-card explore-skeleton"></div>
                    </div>
                    <div v-else-if="categories.length" class="explore-category-grid">
                        <button
                            v-for="cat in visibleCategories"
                            :key="cat.id"
                            class="explore-category-card"
                            type="button"
                            @click="goToCategory(cat)"
                        >
                            <span class="explore-category-name">{{ cat.name }}</span>
                            <span v-if="cat.description" class="explore-category-desc">{{ cat.description }}</span>
                        </button>
                        <button
                            v-if="hasMoreCategories || showAllCategories"
                            type="button"
                            class="explore-more-card"
                            @click="showAllCategories = !showAllCategories"
                        >
                            {{ showAllCategories ? '收起分类' : '展开更多' }}
                        </button>
                    </div>
                    <p v-else class="explore-empty">暂无分类</p>
                </section>

                <section class="explore-section">
                    <div class="explore-section-header">
                        <div>
                            <h2>热门标签</h2>
                            <p>按使用量排序</p>
                        </div>
                    </div>
                    <div v-if="loading" class="explore-tags">
                        <div v-for="i in 16" :key="i" class="explore-tag explore-tag-skeleton"></div>
                    </div>
                    <div v-else-if="hotTags.length" class="explore-tags">
                        <button
                            v-for="tag in visibleTags"
                            :key="tag.id"
                            class="explore-tag"
                            type="button"
                            @click="goToTag(tag)"
                        >
                            <span>#{{ tag.name }}</span>
                            <em v-if="tag.useCount">{{ tag.useCount }}</em>
                        </button>
                        <button
                            v-if="hasMoreTags || showAllTags"
                            type="button"
                            class="explore-tag explore-tag-more"
                            @click="showAllTags = !showAllTags"
                        >
                            {{ showAllTags ? '收起' : '更多标签' }}
                        </button>
                    </div>
                    <p v-else class="explore-empty">暂无热门标签</p>
                </section>
            </section>

            <section class="explore-two-column">
                <section class="explore-section">
                    <div class="explore-section-header">
                        <div>
                            <h2>热门专题</h2>
                            <p>围绕一个议题持续阅读</p>
                        </div>
                        <RouterLink to="/topics">全部专题</RouterLink>
                    </div>
                    <div v-if="loading" class="explore-list">
                        <div v-for="i in 4" :key="i" class="explore-list-item explore-skeleton"></div>
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

                <section class="explore-section">
                    <div class="explore-section-header">
                        <div>
                            <h2>推荐专栏</h2>
                            <p>适合长期跟读</p>
                        </div>
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
                            <img
                                :src="column.coverUrl"
                                :alt="`${column.title} 封面`"
                                loading="lazy"
                                decoding="async"
                            >
                            <span>
                                <strong>{{ column.title }}</strong>
                                <em>{{ column.articleCount }} 篇 · {{ column.subscriberCount }} 订阅</em>
                            </span>
                        </RouterLink>
                    </div>
                    <p v-else class="explore-empty">暂无推荐专栏</p>
                </section>
            </section>

            <section class="explore-section">
                <div class="explore-section-header">
                    <div>
                        <h2>推荐作者</h2>
                        <p>从稳定输出的人开始关注</p>
                    </div>
                    <RouterLink to="/ranking">作者榜</RouterLink>
                </div>
                <div v-if="loading" class="explore-author-grid">
                    <div v-for="i in 6" :key="i" class="explore-author-card explore-skeleton"></div>
                </div>
                <div v-else-if="authors.length" class="explore-author-grid">
                    <article v-for="author in authors" :key="author.id" class="explore-author-card">
                        <RouterLink class="explore-author-avatar" :to="`/users/${author.id}`">
                            <img :src="author.avatar" :alt="`${author.name} 的头像`" loading="lazy" decoding="async">
                        </RouterLink>
                        <div class="explore-author-copy">
                            <RouterLink :to="`/users/${author.id}`">{{ author.name }}</RouterLink>
                            <p>{{ author.bio || '持续输出技术内容' }}</p>
                        </div>
                        <div class="explore-author-stats">
                            <span>{{ author.articleCount }} 文章</span>
                            <span>{{ author.totalLikeCount }} 赞</span>
                            <span>{{ author.followerCount }} 粉丝</span>
                        </div>
                    </article>
                </div>
                <p v-else class="explore-empty">暂无推荐作者</p>
            </section>
        </template>
    </main>
</template>

<style scoped>
.explore-page {
    display: grid;
    gap: 18px;
}

.explore-head {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 16px;
    align-items: end;
    padding: 20px 0 10px;
    border-bottom: 1px solid var(--line);
}

.explore-title h1 {
    margin: 4px 0 0;
    color: var(--text-strong);
    font-size: 28px;
    line-height: 1.25;
}

.explore-head-actions {
    display: flex;
    gap: 8px;
    align-items: center;
}

.explore-search-link,
.explore-rank-link,
.explore-open-link,
.explore-compact-actions a,
.explore-section-header a {
    color: var(--brand);
    font-size: 13px;
    font-weight: 700;
    text-decoration: none;
}

.explore-search-link,
.explore-rank-link,
.explore-open-link {
    display: inline-flex;
    min-height: 34px;
    align-items: center;
    justify-content: center;
    padding: 0 14px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.explore-search-link {
    color: #fff;
    background: var(--brand);
    border-color: var(--brand);
}

.explore-rank-link,
.explore-open-link {
    background: var(--surface);
}

.explore-paths {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
}

.explore-path,
.explore-primary-card,
.explore-compact-article,
.explore-category-card,
.explore-more-card,
.explore-list-item,
.explore-author-card {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: border-color 0.15s, background 0.15s;
}

.explore-path:hover,
.explore-compact-article:hover,
.explore-category-card:hover,
.explore-more-card:hover,
.explore-list-item:hover,
.explore-author-card:hover {
    background: var(--surface-soft);
    border-color: var(--brand-hover);
}

.explore-path {
    display: grid;
    gap: 4px;
    padding: 12px;
    text-decoration: none;
}

.explore-path strong,
.explore-list-item strong,
.explore-category-name {
    color: var(--text);
    font-size: 15px;
    font-weight: 700;
}

.explore-path span,
.explore-list-item em,
.explore-category-desc {
    color: var(--muted);
    font-size: 12px;
    font-style: normal;
    line-height: 1.55;
}

.explore-section {
    display: grid;
    gap: 12px;
    min-width: 0;
}

.explore-section-header {
    display: flex;
    align-items: end;
    justify-content: space-between;
    gap: 10px;
}

.explore-section-header h2 {
    margin: 0;
    color: var(--text-strong);
    font-size: 18px;
    font-weight: 700;
}

.explore-section-header p {
    margin: 3px 0 0;
    color: var(--muted);
    font-size: 13px;
}

.explore-featured-board {
    display: grid;
    grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
    gap: 12px;
    align-items: stretch;
}

.explore-primary-card {
    display: grid;
    grid-template-columns: 1fr;
    overflow: hidden;
}

.explore-primary-cover {
    display: block;
    min-height: 240px;
    aspect-ratio: 16 / 9;
    overflow: hidden;
    color: var(--brand);
    font-weight: 700;
    text-decoration: none;
    background: var(--brand-soft);
}

.explore-primary-cover img,
.explore-primary-cover span {
    width: 100%;
    height: 100%;
}

.explore-primary-cover img {
    display: block;
    object-fit: cover;
}

.explore-primary-cover span {
    display: inline-flex;
    align-items: center;
    justify-content: center;
}

.explore-primary-copy,
.explore-compact-article {
    display: grid;
    min-width: 0;
}

.explore-primary-copy {
    gap: 12px;
    padding: 16px;
}

.explore-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    color: var(--muted);
    font-size: 12px;
}

.explore-primary-copy h3,
.explore-compact-article h3 {
    margin: 0;
    color: var(--text-strong);
    line-height: 1.45;
}

.explore-primary-copy h3 {
    font-size: 22px;
}

.explore-primary-copy p,
.explore-compact-article p {
    display: -webkit-box;
    margin: 0;
    overflow: hidden;
    color: var(--muted);
    font-size: 14px;
    line-height: 1.7;
    -webkit-box-orient: vertical;
}

.explore-primary-copy p {
    -webkit-line-clamp: 3;
}

.explore-compact-article p {
    -webkit-line-clamp: 2;
}

.explore-article-footer,
.explore-compact-actions {
    display: flex;
    gap: 10px;
    align-items: center;
    justify-content: space-between;
}

.explore-author {
    display: inline-flex;
    min-width: 0;
    align-items: center;
    gap: 8px;
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
    text-decoration: none;
}

.explore-author img {
    width: 28px;
    height: 28px;
    object-fit: cover;
    border-radius: 50%;
}

.explore-side-stack {
    display: grid;
    gap: 10px;
}

.explore-compact-article {
    gap: 8px;
    padding: 12px;
}

.explore-compact-article h3 {
    display: -webkit-box;
    overflow: hidden;
    font-size: 15px;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.explore-compact-actions {
    color: var(--muted);
    font-size: 12px;
}

.explore-two-column {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 18px;
    align-items: start;
}

.explore-category-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 10px;
}

.explore-category-card,
.explore-more-card {
    display: grid;
    gap: 6px;
    min-height: 72px;
    padding: 12px;
    cursor: pointer;
    text-align: left;
}

.explore-more-card {
    place-items: center;
    color: var(--brand);
    font-size: 13px;
    font-weight: 700;
}

.explore-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.explore-tag {
    display: inline-flex;
    min-height: 32px;
    align-items: center;
    gap: 6px;
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

.explore-tag span {
    color: var(--brand-strong);
    font-weight: 700;
}

.explore-tag em {
    color: var(--muted);
    font-size: 11px;
    font-style: normal;
}

.explore-tag-more {
    color: var(--brand);
    font-weight: 700;
}

.explore-list {
    display: grid;
    gap: 8px;
}

.explore-list-item {
    display: grid;
    grid-template-columns: 56px minmax(0, 1fr);
    gap: 12px;
    align-items: center;
    min-height: 72px;
    padding: 10px;
    text-decoration: none;
}

.explore-list-item img {
    width: 56px;
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

.explore-author-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
}

.explore-author-card {
    display: grid;
    grid-template-columns: 42px minmax(0, 1fr);
    gap: 10px;
    align-items: center;
    padding: 12px;
}

.explore-author-avatar {
    display: inline-flex;
}

.explore-author-avatar img {
    width: 42px;
    height: 42px;
    object-fit: cover;
    border: 1px solid var(--line);
    border-radius: 50%;
}

.explore-author-copy {
    display: grid;
    gap: 3px;
    min-width: 0;
}

.explore-author-copy a {
    overflow: hidden;
    color: var(--text-strong);
    font-size: 14px;
    font-weight: 700;
    text-decoration: none;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.explore-author-copy a:hover {
    color: var(--brand);
}

.explore-author-copy p {
    margin: 0;
    overflow: hidden;
    color: var(--muted);
    font-size: 12px;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.explore-author-stats {
    grid-column: 1 / -1;
    display: flex;
    gap: 12px;
    color: var(--muted);
    font-size: 12px;
    white-space: nowrap;
}

.explore-empty {
    margin: 0;
    padding: 16px 0;
    color: var(--muted);
    font-size: 14px;
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

.explore-primary-card.explore-skeleton {
    min-height: 380px;
}

@keyframes explore-skeleton {
    0% {
        background-position: 100% 0;
    }

    100% {
        background-position: 0 0;
    }
}

@media (max-width: 1080px) {
    .explore-featured-board,
    .explore-primary-card,
    .explore-two-column {
        grid-template-columns: 1fr;
    }

    .explore-author-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 760px) {
    .explore-head {
        grid-template-columns: 1fr;
        align-items: start;
    }

    .explore-head-actions {
        flex-wrap: wrap;
    }

    .explore-search-link,
    .explore-rank-link {
        flex: 1;
    }

    .explore-paths {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .explore-primary-cover {
        min-height: 180px;
    }

    .explore-author-grid {
        grid-template-columns: 1fr;
    }
}
</style>
