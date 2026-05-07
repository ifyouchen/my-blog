<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {onBeforeRouteLeave, useRoute, useRouter} from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import EmptyState from '@/components/EmptyState.vue';
import SidebarBlock from '@/components/SidebarBlock.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getFollowingFeedApi, getMyFollowingApi} from '@/api/following';
import {getAuthorRankingsApi} from '@/api/rankings';
import {ARTICLE_SORT_ITEMS, ARTICLE_SORT_LATEST, normalizeArticleSort} from '@/constants/articleSort';
import {useLoginModal} from '@/composables/useLoginModal';
import {useSession} from '@/stores/session';
import {useInfiniteArticleFeed} from '@/composables/useInfiniteArticleFeed';
import {useStableListRequest} from '@/composables/useStableListRequest';

const route = useRoute();
const router = useRouter();
const { isLoggedIn, state } = useSession();
const { showLoginModal } = useLoginModal();

const followingUsers = ref([]);
const recommendedAuthors = ref([]);
const sidebarLoading = ref(false);
const pageSize = 10;
const {
    articles,
    currentPage,
    total,
    hasMore,
    loadingMore,
    loadMoreError,
    applyPageResult,
    resetFeed,
    saveFeedCache,
    restoreFeedCache,
    loadMore
} = useInfiniteArticleFeed({ pageSize });
const activeSort = ref(normalizeArticleSort(route.query.sort || ARTICLE_SORT_LATEST));
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    loading,
    runStableRequest,
    resetStableRequest
} = useStableListRequest();

const hasFollowing = computed(() => followingUsers.value.length > 0);
const latestArticleText = computed(() => {
    const latest = articles.value[0];
    if (!latest) {
        return '暂无更新';
    }
    return latest.publishedText || latest.updatedText || '刚刚更新';
});
const authorLatestMap = computed(() => {
    const result = {};
    for (const article of articles.value) {
        const authorId = article.author?.id;
        if (authorId && !result[authorId]) {
            result[authorId] = article.publishedText || article.updatedText || '刚刚更新';
        }
    }
    return result;
});
const buildFeedCacheKey = () => `following:${activeSort.value}`;

const getAuthorLatestText = (authorId) => authorLatestMap.value[authorId] || '最近更新待同步';

const syncRoute = () => {
    router.replace({
        path: '/following',
        query: {
            sort: activeSort.value === ARTICLE_SORT_LATEST ? undefined : activeSort.value
        }
    });
};

const fetchMeta = async () => {
    if (!isLoggedIn.value) {
        followingUsers.value = [];
        recommendedAuthors.value = [];
        return;
    }
    sidebarLoading.value = true;
    try {
        const [followingList, authorRanks] = await Promise.all([
            getMyFollowingApi(),
            getAuthorRankingsApi(5)
        ]);
        followingUsers.value = followingList || [];
        recommendedAuthors.value = (authorRanks || [])
            .filter((item) => item.user.id !== state.user?.id)
            .filter((item) => !followingUsers.value.some((author) => author.id === item.user.id));
    } finally {
        sidebarLoading.value = false;
    }
};

const fetchFeed = async ({ reset = false } = {}) => {
    if (!isLoggedIn.value) {
        resetStableRequest();
        resetFeed();
        return;
    }
    let restored = false;
    if (reset) {
        resetStableRequest();
        resetFeed();
        restored = restoreFeedCache(buildFeedCacheKey());
    }
    const response = await runStableRequest(
        () => restored
            ? Promise.resolve({ items: articles.value, page: currentPage.value, total: total.value })
            : getFollowingFeedApi({
                page: 1,
                pageSize,
                sort: activeSort.value
            }),
        {
            silent: restored || hasLoadedOnce.value,
            initialErrorMessage: '关注流加载失败',
            refreshErrorMessage: '关注流刷新失败，请稍后重试'
        }
    );
    if (response?.ignored || response?.error) {
        return;
    }
    const pageResult = response.result || {};
    if (!restored) {
        applyPageResult(pageResult);
        saveFeedCache(buildFeedCacheKey());
    }
};

const refreshAll = async () => {
    await Promise.all([fetchMeta(), fetchFeed({ reset: true })]);
};

const loadMoreArticles = async () => {
    const response = await loadMore(
        (page) => getFollowingFeedApi({ page, pageSize, sort: activeSort.value }),
        { errorMessage: '关注流加载失败，请稍后重试' }
    );
    if (response?.result) {
        saveFeedCache(buildFeedCacheKey());
    }
};

const changeSort = async (sort) => {
    saveFeedCache(buildFeedCacheKey());
    activeSort.value = normalizeArticleSort(sort);
    syncRoute();
};

const handleFollowChange = async () => {
    await refreshAll();
};

const openLoginGuide = () => {
    showLoginModal(() => refreshAll(), {
        title: '登录后查看关注流',
        message: '登录后可以持续追踪你关注作者的新文章。',
        actionText: '登录查看关注流'
    });
};

onMounted(fetchMeta);

watch(isLoggedIn, async () => {
    await refreshAll();
});

watch(() => route.query, async (query) => {
    activeSort.value = normalizeArticleSort(query.sort || ARTICLE_SORT_LATEST);
    await fetchFeed({ reset: true });
}, { immediate: true });

onBeforeRouteLeave(() => {
    saveFeedCache(buildFeedCacheKey());
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell" data-testid="following-page">
        <section class="following-hero">
            <p class="eyebrow">关注</p>
            <h1>追踪你在意的作者更新</h1>
            <p v-if="isLoggedIn">
                关注作者后，他们新发布的文章会在这里持续出现。
            </p>
            <p v-else>
                登录后可以关注喜欢的作者，把他们的最新内容聚到一个地方。
            </p>
            <div v-if="isLoggedIn" class="following-stats-bar" data-testid="following-stats-bar">
                <span><strong>{{ followingUsers.length }}</strong> 关注作者</span>
                <span><strong>{{ total }}</strong> 关注流文章</span>
                <span><strong>{{ latestArticleText }}</strong> 最近更新</span>
            </div>
        </section>

        <div class="content-grid">
            <section class="following-main">
                <EmptyState
                    v-if="!isLoggedIn"
                    eyebrow="登录后查看"
                    title="先登录，再建立自己的关注流"
                    description="关注作者后，这里会变成你的私人内容更新入口。"
                    data-testid="following-login-empty"
                >
                    <div class="following-empty-actions">
                        <button
                            type="button"
                            class="following-primary-action"
                            data-testid="following-login-cta"
                            @click="openLoginGuide"
                        >
                            登录查看关注流
                        </button>
                        <RouterLink
                            class="following-secondary-action"
                            to="/ranking"
                            data-testid="following-ranking-cta"
                        >
                            去排行榜发现作者
                        </RouterLink>
                    </div>
                </EmptyState>
                <template v-else>
                    <section
                        v-if="!hasFollowing && recommendedAuthors.length"
                        class="following-onboarding"
                        data-testid="following-main-recommendations"
                    >
                        <div class="section-heading compact">
                            <div>
                                <p class="eyebrow">推荐关注</p>
                                <h2>先关注几位活跃作者</h2>
                            </div>
                        </div>
                        <div class="following-recommend-grid">
                            <article
                                v-for="item in recommendedAuthors"
                                :key="item.user.id"
                                class="following-recommend-card"
                            >
                                <RouterLink class="following-recommend-author" :to="`/users/${item.user.id}`">
                                    <img :src="item.user.avatar" alt="作者头像" loading="lazy" decoding="async">
                                    <div>
                                        <strong>{{ item.user.name }}</strong>
                                        <span>{{ item.user.bio || '持续分享工程实践与技术经验。' }}</span>
                                    </div>
                                </RouterLink>
                                <div class="following-recommend-meta">
                                    <span>{{ item.totalViewCount }} 阅读</span>
                                    <span>{{ item.articleCount }} 文章</span>
                                </div>
                                <AuthorFollowButton
                                    :user-id="item.user.id"
                                    :followed="item.followed"
                                    compact
                                    @change="handleFollowChange"
                                />
                            </article>
                        </div>
                    </section>
                    <ArticleFeed
                        :articles="articles"
                        :page="currentPage"
                        :page-size="pageSize"
                        :total="total"
                        :loading="loading"
                        :initial-loading="initialLoading"
                        :refreshing="refreshing"
                        :has-loaded-once="hasLoadedOnce"
                        :error-message="errorMessage"
                        :inline-error-message="inlineError"
                        :sort="activeSort"
                        :sort-items="ARTICLE_SORT_ITEMS"
                        pagination-mode="infinite"
                        :has-more="hasMore"
                        :loading-more="loadingMore"
                        :load-more-error="loadMoreError"
                        eyebrow="关注动态"
                        title="关注作者的新文章"
                        :empty-text="hasFollowing ? '你关注的作者最近还没有新内容' : '先关注几位作者，关注流就会热闹起来'"
                        @load-more="loadMoreArticles"
                        @sort-change="changeSort"
                    />
                </template>
            </section>

            <aside class="sidebar" aria-label="关注侧栏">
                <SidebarBlock eyebrow="我的关注" title="作者列表" compact>
                    <div v-if="sidebarLoading" class="sidebar-state">加载中...</div>
                    <EmptyState
                        v-else-if="!isLoggedIn"
                        eyebrow="我的关注"
                        title="登录后查看关注作者"
                        description="登录后可以把你在意的作者集中到这里持续追踪。"
                        compact
                    />
                    <EmptyState
                        v-else-if="!followingUsers.length"
                        eyebrow="我的关注"
                        title="你还没有关注任何作者"
                        description="先去首页或排行榜关注几位作者，关注流就会热闹起来。"
                        compact
                    />
                    <div v-else class="following-author-list" data-testid="following-author-list">
                        <RouterLink
                            v-for="author in followingUsers"
                            :key="author.id"
                            class="following-author-item"
                            :to="`/users/${author.id}`"
                        >
                            <img :src="author.avatar" alt="作者头像" loading="lazy" decoding="async">
                            <div>
                                <strong>{{ author.name }}</strong>
                                <span>{{ author.bio || '持续分享工程实践与技术经验。' }}</span>
                                <small>最近更新：{{ getAuthorLatestText(author.id) }}</small>
                            </div>
                        </RouterLink>
                    </div>
                </SidebarBlock>

                <SidebarBlock eyebrow="推荐关注" title="值得追踪的作者" compact>
                    <div v-if="sidebarLoading" class="sidebar-state">加载中...</div>
                    <EmptyState
                        v-else-if="!recommendedAuthors.length"
                        eyebrow="推荐关注"
                        title="推荐作者正在整理"
                        description="稍后再来看看，我们会把活跃创作者放到这里。"
                        compact
                    />
                    <div v-else class="rank-author-list" data-testid="following-recommended-authors">
                        <div
                            v-for="item in recommendedAuthors"
                            :key="item.user.id"
                            class="rank-author-item"
                        >
                            <div class="rank-author-info">
                                <img :src="item.user.avatar" alt="作者头像" loading="lazy" decoding="async">
                                <div>
                                    <RouterLink :to="`/users/${item.user.id}`">
                                        {{ item.user.name }}
                                    </RouterLink>
                                    <span>{{ item.totalViewCount }} 阅读 · {{ item.articleCount }} 文章</span>
                                </div>
                            </div>
                            <AuthorFollowButton
                                :user-id="item.user.id"
                                :followed="item.followed"
                                compact
                                @change="handleFollowChange"
                            />
                        </div>
                    </div>
                </SidebarBlock>
            </aside>
        </div>
    </main>
</template>

<style scoped>
.following-hero {
    display: grid;
    gap: 8px;
}

.following-hero h1,
.following-hero p {
    margin: 0;
}

.following-main {
    min-width: 0;
}

.following-stats-bar {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 6px;
}

.following-stats-bar span {
    display: inline-flex;
    min-height: 32px;
    align-items: center;
    gap: 5px;
    padding: 0 10px;
    color: var(--muted);
    font-size: 13px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.following-stats-bar strong {
    color: var(--text-strong);
    font-weight: 700;
}

.following-empty-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px;
    margin-top: 6px;
}

.following-primary-action,
.following-secondary-action {
    display: inline-flex;
    min-height: 36px;
    align-items: center;
    justify-content: center;
    padding: 0 14px;
    font-size: 14px;
    font-weight: 700;
    text-decoration: none;
    border-radius: var(--radius-sm);
}

.following-primary-action {
    color: #fff;
    cursor: pointer;
    background: var(--brand);
    border: 1px solid var(--brand);
}

.following-primary-action:hover,
.following-primary-action:focus-visible {
    background: var(--brand-strong);
    border-color: var(--brand-strong);
}

.following-secondary-action {
    color: var(--brand);
    background: var(--surface);
    border: 1px solid var(--line);
}

.following-secondary-action:hover,
.following-secondary-action:focus-visible {
    color: var(--brand-strong);
    border-color: var(--brand);
}

.following-onboarding {
    display: grid;
    gap: 12px;
    padding: 16px;
    margin-bottom: 16px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.following-recommend-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.following-recommend-card {
    display: grid;
    gap: 10px;
    padding: 12px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface-soft);
}

.following-recommend-author {
    display: grid;
    grid-template-columns: 44px minmax(0, 1fr);
    gap: 10px;
    align-items: center;
    color: var(--text);
    text-decoration: none;
}

.following-recommend-author img {
    width: 44px;
    height: 44px;
    object-fit: cover;
    border-radius: var(--radius-md);
}

.following-recommend-author div {
    display: grid;
    gap: 3px;
    min-width: 0;
}

.following-recommend-author strong {
    color: var(--text-strong);
    line-height: 1.35;
}

.following-recommend-author span {
    overflow: hidden;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.5;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.following-recommend-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    color: var(--muted);
    font-size: 12px;
}

.sidebar-state {
    color: var(--muted);
    font-size: 14px;
    line-height: 1.7;
}

.following-author-list,
.rank-author-list {
    display: grid;
    gap: 12px;
}

.following-author-item,
.rank-author-item {
    display: grid;
    gap: 12px;
    padding: 12px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.following-author-item {
    grid-template-columns: 48px minmax(0, 1fr);
    text-decoration: none;
}

.following-author-item > div,
.rank-author-info > div {
    display: grid;
    min-width: 0;
    gap: 4px;
}

.following-author-item img,
.rank-author-info img {
    width: 48px;
    height: 48px;
    object-fit: cover;
    border-radius: var(--radius-md);
}

.following-author-item strong,
.rank-author-info a {
    color: var(--text);
    line-height: 1.35;
    text-decoration: none;
}

.following-author-item span,
.rank-author-info span {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
}

.following-author-item small {
    color: var(--muted);
    font-size: 12px;
    line-height: 1.4;
}

.rank-author-item {
    align-items: center;
}

.rank-author-info {
    display: grid;
    grid-template-columns: 48px minmax(0, 1fr);
    gap: 12px;
    align-items: center;
}

.following-author-item:hover,
.following-author-item:focus-visible,
.rank-author-item:hover {
    background: rgba(37, 99, 235, 0.05);
    border-color: rgba(37, 99, 235, 0.14);
    box-shadow: 0 16px 28px rgba(31, 78, 168, 0.08);
    transform: translateY(-1px);
}

@media (max-width: 960px) {
    .content-grid {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 760px) {
    .following-stats-bar span,
    .following-primary-action,
    .following-secondary-action {
        width: 100%;
    }

    .following-recommend-grid {
        grid-template-columns: 1fr;
    }
}
</style>
