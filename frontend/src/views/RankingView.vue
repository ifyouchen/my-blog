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

<style scoped>
/* ============================================================
   排行榜页 Hero 区
   ============================================================ */
.ranking-hero {
    display: grid;
    gap: 10px;
    max-width: 680px;
    padding-bottom: 8px;
}

.ranking-hero h1,
.ranking-hero p {
    margin: 0;
}

.ranking-hero h1 {
    font-size: clamp(26px, 3.5vw, 38px);
    font-weight: 700;
    line-height: 1.2;
    color: var(--text-strong);
}

.ranking-hero p:last-child {
    color: var(--muted);
    font-size: 15px;
    line-height: 1.8;
}

.ranking-toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 14px 20px;
    align-items: center;
    margin-top: 20px;
    padding: 12px 0;
    border-top: 1px solid var(--line);
    border-bottom: 1px solid var(--line);
}

.ranking-filter-group {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    min-width: 0;
}

.ranking-filter-group > span {
    color: var(--muted);
    font-size: 13px;
    font-weight: 700;
}

.ranking-filter-options {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    min-width: 0;
}

.ranking-filter-options.category {
    min-height: 30px;
}

.ranking-filter-placeholder {
    display: block;
    width: 92px;
    height: 30px;
    background: var(--surface-muted);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.ranking-filter-options button,
.ranking-mobile-tabs button {
    min-height: 30px;
    padding: 0 11px;
    color: var(--muted);
    font-size: 13px;
    font-weight: 700;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.ranking-filter-options button.active,
.ranking-mobile-tabs button.active {
    color: var(--brand);
    background: var(--brand-soft);
    border-color: var(--brand);
}

.ranking-filter-options button:hover:not(.active),
.ranking-mobile-tabs button:hover:not(.active) {
    color: var(--text);
    border-color: var(--brand-hover);
}

.ranking-category-toggle {
    display: none;
}

.ranking-rule-note {
    margin: 10px 0 0;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
}

.ranking-mobile-tabs {
    display: none;
    gap: 8px;
    margin-top: 16px;
}

/* ============================================================
   页面整体布局
   ============================================================ */
.ranking-layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 320px;
    gap: 28px;
    align-items: start;
    margin-top: 24px;
}

.ranking-feed,
.ranking-sidebar {
    min-width: 0;
}

.ranking-feed {
    display: grid;
    gap: 0;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    overflow: hidden;
}

.ranking-sidebar {
    display: grid;
    gap: 0;
}

/* ============================================================
   分区标题
   ============================================================ */
.ranking-section-head {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 16px 20px 14px;
    border-bottom: 1px solid var(--line);
}

.ranking-section-icon {
    font-size: 18px;
    line-height: 1;
}

.ranking-section-head h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--text-strong);
}

.ranking-section-head .eyebrow {
    margin: 0 0 0 auto;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--muted-light);
}

/* ============================================================
   文章榜列表
   ============================================================ */
.ranking-post-list {
    display: grid;
    gap: 0;
    padding: 0;
    margin: 0;
    list-style: none;
}

.ranking-post-shell {
    list-style: none;
    border-bottom: 1px solid var(--line);
}

.ranking-post-shell:last-child {
    border-bottom: none;
}

.ranking-post-item {
    display: grid;
    grid-template-columns: 52px 130px minmax(0, 1fr);
    gap: 0;
    align-items: stretch;
    cursor: pointer;
    transition: background 0.12s;
}

.ranking-post-item:hover,
.ranking-post-item:focus-visible {
    background: var(--surface-soft);
}

.ranking-post-item:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: -2px;
}

/* 排名数字列 */
.ranking-post-rank {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0;
    padding: 0 6px;
    border-right: 1px solid var(--line);
    background: var(--surface-soft);
}

.ranking-post-rank span {
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.12em;
    color: var(--muted-light);
    text-transform: uppercase;
}

.ranking-post-rank strong {
    font-size: 20px;
    font-weight: 800;
    line-height: 1;
    color: var(--muted);
}

/* 前三名高亮排名 */
.ranking-post-rank.top strong {
    color: var(--brand);
}

.ranking-post-shell:nth-child(1) .ranking-post-rank {
    background: #fffbeb;
}

.ranking-post-shell:nth-child(1) .ranking-post-rank strong {
    color: #b45309;
}

.ranking-post-shell:nth-child(2) .ranking-post-rank strong {
    color: #6b7280;
}

.ranking-post-shell:nth-child(3) .ranking-post-rank strong {
    color: #b45309;
    opacity: 0.7;
}

/* 封面图列 */
.ranking-post-cover {
    width: 130px;
    aspect-ratio: 4 / 3;
    overflow: hidden;
    background: var(--surface-muted);
    flex-shrink: 0;
    border-right: 1px solid var(--line);
}

.ranking-post-cover img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.22s ease;
}

.ranking-post-item:hover .ranking-post-cover img {
    transform: scale(1.05);
}

/* 内容列 */
.ranking-post-content {
    display: flex;
    flex-direction: column;
    padding: 14px 16px;
    min-width: 0;
    gap: 6px;
}

.ranking-post-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin: 0;
}

.ranking-post-meta span {
    font-size: 12px;
    color: var(--muted);
}

.ranking-post-meta span + span::before {
    content: '·';
    margin-right: 6px;
}

.ranking-post-title {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    line-height: 1.4;
    color: var(--text-strong);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.ranking-post-item:hover .ranking-post-title {
    color: var(--brand);
}

.ranking-post-summary {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.65;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    flex: 1;
}

.ranking-post-footer {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: auto;
    padding-top: 4px;
    flex-wrap: wrap;
}

.ranking-post-author {
    display: inline-flex;
    gap: 6px;
    align-items: center;
    min-width: 0;
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
    text-decoration: none;
    transition: color 0.15s;
    flex-shrink: 0;
}

.ranking-post-author img {
    width: 22px;
    height: 22px;
    object-fit: cover;
    border-radius: 50%;
    flex-shrink: 0;
}

.ranking-post-author:hover {
    color: var(--brand);
}

.ranking-post-stats {
    display: flex;
    gap: 10px;
    margin-left: auto;
    flex-shrink: 0;
}

.ranking-post-stats span {
    font-size: 12px;
    color: var(--muted);
}

/* ============================================================
   作者榜侧边卡片
   ============================================================ */
.ranking-sidebar-card {
    position: sticky;
    top: 72px;
    width: 100%;
    max-width: 100%;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    overflow: hidden;
}

.ranking-author-list {
    display: grid;
    gap: 0;
    padding: 0;
    margin: 0;
    list-style: none;
}

.ranking-author-card {
    display: grid;
    grid-template-columns: 22px 38px minmax(0, 1fr) auto;
    align-items: center;
    gap: 10px;
    width: 100%;
    min-width: 0;
    padding: 12px 16px;
    border-bottom: 1px solid var(--line);
    transition: background 0.12s;
}

.ranking-author-card:last-child {
    border-bottom: none;
}

.ranking-author-card:hover {
    background: var(--surface-soft);
}

/* 排名数字 */
.ranking-side-rank {
    flex-shrink: 0;
    width: 22px;
    text-align: center;
    font-size: 13px;
    font-weight: 800;
    color: var(--muted);
    line-height: 1;
}

.ranking-side-rank.rank-1 {
    color: #b45309;
}

.ranking-side-rank.rank-2 {
    color: #6b7280;
}

.ranking-side-rank.rank-3 {
    color: #c57c3a;
}

/* 头像 */
.ranking-author-avatar {
    flex-shrink: 0;
    display: inline-flex;
}

.ranking-author-avatar img {
    width: 38px;
    height: 38px;
    object-fit: cover;
    border-radius: 50%;
    border: 1px solid var(--line);
}

/* 名字+简介 */
.ranking-author-copy {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.ranking-author-name-row {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
}

.ranking-author-name-row :deep(.user-level-badge) {
    flex: 0 0 auto;
}

.ranking-author-copy a {
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

.ranking-author-copy a:hover {
    color: var(--brand);
}

.ranking-author-copy p {
    margin: 0;
    color: var(--muted);
    font-size: 12px;
    line-height: 1.4;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.ranking-author-top-article {
    display: block;
    color: var(--brand);
    font-size: 12px;
    line-height: 1.35;
    overflow: hidden;
    text-decoration: none;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.ranking-author-top-article:hover {
    color: var(--brand-strong);
}

/* 关注按钮区 */
.ranking-author-action {
    display: flex;
    align-items: center;
    justify-self: end;
    min-width: 0;
}

.ranking-author-self-tag {
    font-size: 11px;
    font-weight: 600;
    color: var(--brand);
    white-space: nowrap;
    padding: 3px 8px;
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-sm);
}

.ranking-author-card :deep(.author-follow-button) {
    min-height: 28px;
    padding-inline: 12px;
    border-radius: var(--radius-sm);
    font-size: 12px;
}

/* ============================================================
   状态 & 加载
   ============================================================ */
.ranking-state {
    padding: 24px 20px;
    color: var(--muted);
    font-size: 14px;
}

.ranking-refresh-note {
    margin: 12px 0 0;
    padding: 8px 12px;
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
    background: var(--brand-soft);
    border: 1px solid rgba(37, 99, 235, 0.14);
    border-radius: var(--radius-sm);
}

/* ============================================================
   响应式
   ============================================================ */
@media (max-width: 960px) {
    .ranking-layout {
        grid-template-columns: 1fr;
        gap: 20px;
    }

    .ranking-sidebar-card {
        position: static;
    }
}

@media (max-width: 760px) {
    .ranking-toolbar {
        display: grid;
        gap: 10px;
        margin-top: 14px;
        padding: 10px 0;
    }

    .ranking-filter-group {
        align-items: start;
        flex-direction: column;
    }

    .ranking-filter-options {
        width: 100%;
        flex-wrap: wrap;
    }

    .ranking-filter-options button {
        flex: 0 0 auto;
    }

    .ranking-category-toggle {
        display: inline-flex;
        align-items: center;
        color: var(--brand) !important;
        background: transparent !important;
        border-color: transparent !important;
    }

    .ranking-filter-options.category:not(.expanded) .ranking-category-button:nth-of-type(n + 5):not(.active) {
        display: none;
    }

    .ranking-mobile-tabs {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .ranking-mobile-tabs button {
        width: 100%;
    }

    .ranking-layout {
        margin-top: 16px;
    }

    .ranking-layout.mobile-articles .ranking-sidebar,
    .ranking-layout.mobile-authors .ranking-feed {
        display: none;
    }

    .ranking-post-item {
        grid-template-columns: 44px minmax(0, 1fr);
    }

    .ranking-post-cover {
        display: none;
    }

    .ranking-post-content {
        padding: 12px 14px;
        overflow: hidden;
    }

    .ranking-post-title {
        font-size: 15px;
    }

    .ranking-post-summary {
        -webkit-line-clamp: 2;
    }

    .ranking-post-footer {
        align-items: start;
        flex-direction: column;
        gap: 7px;
    }

    .ranking-post-stats {
        width: 100%;
        margin-left: 0;
        gap: 8px;
        overflow: hidden;
    }

    .ranking-post-stats span {
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .ranking-post-stats .comments {
        display: none;
    }

    .ranking-author-card {
        grid-template-columns: 22px 38px minmax(0, 1fr);
        align-items: start;
        gap: 8px;
        padding: 12px;
    }

    .ranking-author-action {
        grid-column: 3;
        justify-self: start;
        align-self: start;
        max-width: 100%;
    }

    .ranking-section-head {
        padding: 12px 16px 10px;
    }
}
</style>
