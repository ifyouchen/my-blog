<script>
let cachedRankingCategories = [];
let cachedRankingCategoriesReady = false;
let rankingCategoriesRequest = null;
</script>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import UserEquippedBadge from '@/components/UserEquippedBadge.vue';
import UserLevelBadge from '@/components/UserLevelBadge.vue';

import {getCategoriesApi} from '@/api/categories';
import {getArticleRankingsApi, getAuthorRankingsApi} from '@/api/rankings';
import {useSession} from '@/stores/session';
import {useStableListRequest} from '@/composables/useStableListRequest';
import {DEFAULT_ARTICLE_COVER_URL} from '@/utils/media';

const route = useRoute();
const router = useRouter();
const articleRankings = ref([]);
const authorRankings = ref([]);
const activePeriod = ref(route.query.period === '30d' || route.query.period === 'all' ? route.query.period : '7d');
const activeCategory = ref(typeof route.query.category === 'string' ? route.query.category : '');
const activeMobileBoard = ref('articles');
const categoryExpanded = ref(false);
const categoriesReady = ref(cachedRankingCategoriesReady);
const categoryFallbackEnabled = ref(false);
const availableCategories = ref([...cachedRankingCategories]);
const { state } = useSession();
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    runStableRequest
} = useStableListRequest();

const periodOptions = [
    { label: '7天', value: '7d' },
    { label: '30天', value: '30d' },
    { label: '总榜', value: 'all' }
];
const categoryOptions = computed(() => [
    { label: '全部', value: '' },
    ...availableCategories.value.map((category) => ({ label: category, value: category }))
]);
const hasHiddenCategoryOptions = computed(() => categoryOptions.value.length > 4);

const normalizePeriod = (period) => (
    period === '30d' || period === 'all' ? period : '7d'
);

const sortCategoryNames = (categories) => (
    Array.from(new Set((categories || []).filter(Boolean)))
        .sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
);

const mergeCategories = (categoryNames, markReady = false) => {
    const nextCategories = sortCategoryNames([
        ...availableCategories.value,
        ...(categoryNames || [])
    ]);
    availableCategories.value = nextCategories;
    cachedRankingCategories = nextCategories;
    if (markReady) {
        cachedRankingCategoriesReady = true;
        categoriesReady.value = true;
    }
};

const syncRoute = () => {
    router.replace({
        path: '/ranking',
        query: {
            period: activePeriod.value === '7d' ? undefined : activePeriod.value,
            category: activeCategory.value || undefined
        }
    });
};

const updateCategories = (articles) => {
    if (!categoriesReady.value && !categoryFallbackEnabled.value) {
        return;
    }
    mergeCategories((articles || []).map((article) => article.category));
};

const fetchCategories = async () => {
    if (cachedRankingCategoriesReady) {
        availableCategories.value = [...cachedRankingCategories];
        categoriesReady.value = true;
        return;
    }
    try {
        if (!rankingCategoriesRequest) {
            rankingCategoriesRequest = getCategoriesApi(true)
                .then((categories) => (categories || [])
                    .map((category) => category.name)
                    .filter(Boolean)
                )
                .catch((error) => {
                    rankingCategoriesRequest = null;
                    throw error;
                });
        }
        const categoryNames = await rankingCategoriesRequest;
        mergeCategories(categoryNames, true);
        updateCategories(articleRankings.value);
    } catch (error) {
        categoryFallbackEnabled.value = true;
        updateCategories(articleRankings.value);
    }
};

const fetchRankings = async () => {
    const response = await runStableRequest(
        () => Promise.all([
            getArticleRankingsApi(10, {
                period: activePeriod.value,
                category: activeCategory.value
            }),
            getAuthorRankingsApi(10, {
                period: activePeriod.value,
                category: activeCategory.value
            })
        ]),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '排行榜加载失败',
            refreshErrorMessage: '排行榜刷新失败，请稍后重试'
        }
    );
    if (response?.ignored || response?.error) {
        return;
    }
    const [articleList, authorList] = response.result || [];
    articleRankings.value = articleList || [];
    authorRankings.value = authorList || [];
    updateCategories(articleRankings.value);
};

const handleFollowChange = (target, followed) => {
    target.followed = followed;
    target.followerCount = Math.max(0, (target.followerCount || 0) + (followed ? 1 : -1));
};

const openArticle = (articleId) => {
    router.push(`/articles/${articleId}`);
};

const articlePath = (article) => {
    if (!article) {
        return '/';
    }
    return article.slug ? `/articles/${article.id}-${article.slug}` : `/articles/${article.id}`;
};

const changePeriod = (period) => {
    activePeriod.value = normalizePeriod(period);
    syncRoute();
};

const changeCategory = (category) => {
    activeCategory.value = category || '';
    syncRoute();
};

const toggleCategoryExpanded = () => {
    categoryExpanded.value = !categoryExpanded.value;
};

const setMobileBoard = (board) => {
    activeMobileBoard.value = board === 'authors' ? 'authors' : 'articles';
};

const setCoverFallback = (event) => {
    if (event.target.src.includes(DEFAULT_ARTICLE_COVER_URL)) {
        return;
    }
    event.target.src = DEFAULT_ARTICLE_COVER_URL;
};

watch(() => route.query, async (query) => {
    activePeriod.value = normalizePeriod(query.period);
    activeCategory.value = typeof query.category === 'string' ? query.category : '';
    await fetchRankings();
}, { immediate: true });

onMounted(fetchCategories);
</script>

<template>
    <SiteHeader />
    <main class="page-shell" data-testid="ranking-page">
        <section class="ranking-hero">
            <p class="eyebrow">排行榜</p>
            <h1>看清此刻最有热度的文章和作者</h1>
            <p>先从最值得点开的内容入手，再顺着作者继续深挖，读得更快也更准。</p>
        </section>

        <section class="ranking-toolbar" data-testid="ranking-toolbar" aria-label="排行榜筛选">
            <div class="ranking-filter-group">
                <span>时间范围</span>
                <div class="ranking-filter-options">
                    <button
                        v-for="item in periodOptions"
                        :key="item.value"
                        type="button"
                        :class="{ active: activePeriod === item.value }"
                        @click="changePeriod(item.value)"
                    >
                        {{ item.label }}
                    </button>
                </div>
            </div>
            <div class="ranking-filter-group">
                <span>分类</span>
                <div
                    class="ranking-filter-options category"
                    :class="{
                        expanded: categoryExpanded,
                        loading: !categoriesReady && !availableCategories.length
                    }"
                    :aria-busy="!categoriesReady && !availableCategories.length"
                >
                    <template v-if="categoriesReady || availableCategories.length">
                        <button
                            v-for="item in categoryOptions"
                            :key="item.value || 'all'"
                            type="button"
                            class="ranking-category-button"
                            :class="{ active: activeCategory === item.value }"
                            @click="changeCategory(item.value)"
                        >
                            {{ item.label }}
                        </button>
                        <button
                            v-if="hasHiddenCategoryOptions"
                            type="button"
                            class="ranking-category-toggle"
                            @click="toggleCategoryExpanded"
                        >
                            {{ categoryExpanded ? '收起' : '展开更多' }}
                        </button>
                    </template>
                    <span v-else class="ranking-filter-placeholder" aria-hidden="true"></span>
                </div>
            </div>
        </section>
        <p class="ranking-rule-note">按阅读、点赞、评论综合热度排序，结果会短暂缓存以保证响应速度。</p>

        <p v-if="inlineError" class="feed-message">{{ inlineError }}</p>
        <p
            v-if="errorMessage && !articleRankings.length && !authorRankings.length"
            class="feed-message"
        >
            {{ errorMessage }}
        </p>
        <p v-if="refreshing" class="ranking-refresh-note">正在更新榜单...</p>

        <div class="ranking-mobile-tabs" data-testid="ranking-mobile-tabs" aria-label="榜单类型">
            <button
                type="button"
                :class="{ active: activeMobileBoard === 'articles' }"
                @click="setMobileBoard('articles')"
            >
                文章榜
            </button>
            <button
                type="button"
                :class="{ active: activeMobileBoard === 'authors' }"
                @click="setMobileBoard('authors')"
            >
                作者榜
            </button>
        </div>

        <div class="ranking-layout" :class="`mobile-${activeMobileBoard}`">
            <section class="ranking-feed" data-testid="ranking-articles">
                <div class="ranking-section-head">
                    <span class="ranking-section-icon">🔥</span>
                    <h2>高热文章</h2>
                    <span class="eyebrow">文章榜</span>
                </div>
                <div v-if="initialLoading && !articleRankings.length" class="ranking-state">加载中...</div>
                <ol v-else-if="articleRankings.length" class="ranking-post-list">
                    <li
                        v-for="(article, index) in articleRankings"
                        :key="article.id"
                        class="ranking-post-shell"
                    >
                        <article
                            class="ranking-post-item"
                            role="link"
                            tabindex="0"
                            @click="openArticle(article.id)"
                            @keydown.enter="openArticle(article.id)"
                            @keydown.space.prevent="openArticle(article.id)"
                        >
                            <div class="ranking-post-rank" :class="{ top: index < 3 }">
                                <span>TOP</span>
                                <strong>{{ index + 1 }}</strong>
                            </div>
                            <div class="ranking-post-cover" :aria-label="`查看文章：${article.title}`">
                                <img
                                    :src="article.cover"
                                    :alt="article.coverAlt"
                                    loading="lazy"
                                    decoding="async"
                                    @error="setCoverFallback"
                                >
                            </div>
                            <div class="ranking-post-content">
                                <div class="ranking-post-meta">
                                    <span>{{ article.category || '未分类' }}</span>
                                    <span>{{ article.readingTime }}</span>
                                    <span>{{ article.updatedText }}</span>
                                </div>
                                <h3 class="ranking-post-title">{{ article.title }}</h3>
                                <p class="ranking-post-summary">
                                    {{ article.summary || '这篇文章正在被很多人阅读，适合顺着这条内容线索继续深挖。' }}
                                </p>
                                <div class="ranking-post-footer">
                                    <RouterLink
                                        class="ranking-post-author"
                                        :to="`/users/${article.author.id}`"
                                        @click.stop
                                        @keydown.enter.stop
                                        @keydown.space.stop
                                    >
                                        <img :src="article.author.avatar" alt="作者头像" loading="lazy" decoding="async">
                                        <span>{{ article.author.name }}</span>
                                        <UserLevelBadge :level="article.author.currentLevel" compact />
                                        <UserEquippedBadge :badge="article.author.equippedBadge" compact />
                                    </RouterLink>
                                    <div class="ranking-post-stats">
                                        <span class="views">{{ article.stats.views }}</span>
                                        <span class="likes">{{ article.stats.likes }}</span>
                                        <span class="comments">{{ article.stats.comments }}</span>
                                    </div>
                                </div>
                            </div>
                        </article>
                    </li>
                </ol>
                <div v-else class="ranking-state">当前筛选下暂无文章上榜</div>
            </section>

            <aside class="ranking-sidebar" data-testid="ranking-authors">
                <section class="ranking-sidebar-card">
                    <div class="ranking-section-head">
                        <span class="ranking-section-icon">✨</span>
                        <h2>影响力作者</h2>
                        <span class="eyebrow">作者榜</span>
                    </div>
                    <div v-if="initialLoading && !authorRankings.length" class="ranking-state">加载中...</div>
                    <div v-else-if="authorRankings.length" class="ranking-author-list">
                        <div v-for="author in authorRankings" :key="author.user.id" class="ranking-author-card">
                            <span class="ranking-side-rank" :class="`rank-${author.rank}`">{{ author.rank }}</span>
                            <RouterLink class="ranking-author-avatar" :to="`/users/${author.user.id}`">
                                <img :src="author.user.avatar" alt="作者头像" loading="lazy" decoding="async">
                            </RouterLink>
                            <div class="ranking-author-copy">
                                <div class="ranking-author-name-row">
                                    <RouterLink :to="`/users/${author.user.id}`">{{ author.user.name }}</RouterLink>
                                    <UserLevelBadge :level="author.user.currentLevel" compact />
                                    <UserEquippedBadge :badge="author.user.equippedBadge" compact />
                                </div>
                                <p>
                                    {{ author.articleCount }} 篇 · {{ author.totalLikeCount }} 赞 ·
                                    {{ author.followerCount }} 粉丝
                                </p>
                                <RouterLink
                                    v-if="author.topArticle"
                                    class="ranking-author-top-article"
                                    :to="articlePath(author.topArticle)"
                                    :title="`代表作：${author.topArticle.title}`"
                                    :aria-label="`查看代表作：${author.topArticle.title}`"
                                >
                                    代表作：{{ author.topArticle.title }}
                                </RouterLink>
                            </div>
                            <div class="ranking-author-action">
                                <span v-if="author.user.id === state.user?.id" class="ranking-author-self-tag">我</span>
                                <AuthorFollowButton
                                    v-else
                                    :user-id="author.user.id"
                                    :followed="author.followed"
                                    compact
                                    @change="(followed) => handleFollowChange(author, followed)"
                                />
                            </div>
                        </div>
                    </div>
                    <div v-else class="ranking-state">当前筛选下暂无作者上榜</div>
                </section>
            </aside>
        </div>
    </main>
</template>

<style scoped src="@/styles/views/RankingView.part-1.css"></style>
<style scoped src="@/styles/views/RankingView.part-2.css"></style>
<style scoped src="@/styles/views/RankingView.part-3.css"></style>
