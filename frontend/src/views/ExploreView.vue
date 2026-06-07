<script setup>
import {computed, onMounted, ref} from 'vue';
import {RouterLink, useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import EmptyState from '@/components/EmptyState.vue';
import {getCategoriesApi} from '@/api/categories';
import {getHotTagsApi} from '@/api/tags';
import {getTopicsApi} from '@/api/topic';
import {getRecommendedAuthorsApi, getRecommendedColumnsFromHubApi} from '@/api/recommendations';
import {listArticlesApi} from '@/api/articles';

const router = useRouter();
const categories = ref([]);
const hotTags = ref([]);
const topics = ref([]);
const columns = ref([]);
const authors = ref([]);
const trendingArticles = ref([]);
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
        const [cats, tags, topicPage, recColumns, recAuthors, hotResult] = await Promise.all([
            getCategoriesApi(),
            getHotTagsApi(32),
            getTopicsApi({page: 1, pageSize: 6}),
            getRecommendedColumnsFromHubApi(6),
            getRecommendedAuthorsApi(6),
            listArticlesApi({sort: 'hot', pageSize: 5})
        ]);
        categories.value = Array.isArray(cats) ? cats : [];
        hotTags.value = Array.isArray(tags) ? tags : [];
        topics.value = topicPage?.items || [];
        columns.value = recColumns || [];
        authors.value = recAuthors || [];
        trendingArticles.value = hotResult?.items || [];
    } catch (err) {
        errorMessage.value = '内容加载失败，请稍后重试';
    } finally {
        loading.value = false;
    }
};

const primaryTrending = computed(() => trendingArticles.value[0] || null);
const restTrending = computed(() => trendingArticles.value.slice(1));
const categoryGroups = computed(() => {
    const groups = new Map();
    categories.value.forEach((category) => {
        const groupName = category.groupName || '其他';
        if (!groups.has(groupName)) {
            groups.set(groupName, []);
        }
        groups.get(groupName).push(category);
    });
    return Array.from(groups.entries()).map(([name, items]) => ({ name, items }));
});
const visibleCategoryGroups = computed(() => (
    showAllCategories.value ? categoryGroups.value : categoryGroups.value.slice(0, 4)
));
const visibleTags = computed(() => (
    showAllTags.value ? hotTags.value : hotTags.value.slice(0, 18)
));
const hasMoreCategories = computed(() => categoryGroups.value.length > visibleCategoryGroups.value.length);
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

            <section class="explore-section explore-trending" data-testid="explore-trending">
                <div class="explore-section-header">
                    <div>
                        <h2>今日热读</h2>
                        <p>按热度排序，持续更新</p>
                    </div>
                    <RouterLink to="/ranking">查看热榜</RouterLink>
                </div>

                <div v-if="loading" class="explore-trending-list">
                    <div v-for="i in 5" :key="i" class="explore-trending-item explore-skeleton"></div>
                </div>

                <div v-else-if="trendingArticles.length" class="explore-trending-list">
                    <article
                        v-for="(article, index) in trendingArticles"
                        :key="article.id"
                        class="explore-trending-item"
                        role="link"
                        tabindex="0"
                        :aria-label="`阅读文章：${article.title}`"
                        @click="openArticleCard(article, $event)"
                        @keydown.enter="openArticle(article)"
                        @keydown.space.prevent="openArticle(article)"
                    >
                        <span class="explore-trending-rank" :class="{ 'is-top3': index < 3 }">
                            {{ index + 1 }}
                        </span>
                        <div class="explore-trending-body">
                            <div class="explore-meta">
                                <span v-if="article.category">{{ article.category }}</span>
                                <span>{{ article.readingTime }}</span>
                            </div>
                            <h3>
                                <RouterLink
                                    class="explore-trending-title"
                                    :to="articlePath(article)"
                                    @click.stop
                                    @keydown.enter.stop
                                    @keydown.space.stop
                                >
                                    {{ article.title }}
                                </RouterLink>
                            </h3>
                            <div class="explore-trending-meta">
                                <span>{{ article.author?.name || article.author?.nickname }}</span>
                                <span class="explore-trending-dot"></span>
                                <span class="explore-trending-heat">{{ article.stats.views }}</span>
                            </div>
                        </div>
                        <RouterLink
                            v-if="article.cover"
                            class="explore-trending-cover"
                            :to="articlePath(article)"
                            :aria-label="`打开文章：${article.title}`"
                            tabindex="-1"
                            @click.stop
                        >
                            <img
                                :src="article.cover"
                                :alt="`${article.title} 封面`"
                                loading="lazy"
                                decoding="async"
                            >
                        </RouterLink>
                    </article>
                </div>

                <p v-else class="explore-empty">暂无热读文章</p>
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
                    <div v-else-if="categories.length" class="explore-category-map">
                        <section
                            v-for="group in visibleCategoryGroups"
                            :key="group.name"
                            class="explore-category-group"
                        >
                            <div class="explore-category-group-head">
                                <strong>{{ group.name }}</strong>
                                <span>{{ group.items.length }} 个分类</span>
                            </div>
                            <div class="explore-category-grid">
                                <button
                                    v-for="cat in group.items"
                                    :key="cat.id"
                                    class="explore-category-card"
                                    type="button"
                                    @click="goToCategory(cat)"
                                >
                                    <span class="explore-category-name">{{ cat.name }}</span>
                                    <span v-if="cat.description" class="explore-category-desc">{{ cat.description }}</span>
                                </button>
                            </div>
                        </section>
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
                            <p>{{ author.bio || '持续输出内容' }}</p>
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

<style scoped src="@/styles/views/ExploreView.part-1.css"></style>
<style scoped src="@/styles/views/ExploreView.part-2.css"></style>
