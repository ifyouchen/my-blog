<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import EmptyState from '@/components/EmptyState.vue';
import SidebarBlock from '@/components/SidebarBlock.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { getFollowingFeedApi, getMyFollowingApi } from '@/api/following';
import { getAuthorRankingsApi } from '@/api/rankings';
import { ARTICLE_SORT_ITEMS, ARTICLE_SORT_LATEST, normalizeArticleSort } from '@/constants/articleSort';
import { useSession } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const { isLoggedIn, state } = useSession();

const articles = ref([]);
const followingUsers = ref([]);
const recommendedAuthors = ref([]);
const loading = ref(false);
const sidebarLoading = ref(false);
const errorMessage = ref('');
const currentPage = ref(Number.parseInt(route.query.page || '1', 10) || 1);
const activeSort = ref(normalizeArticleSort(route.query.sort || ARTICLE_SORT_LATEST));
const total = ref(0);
const pageSize = 10;

const hasFollowing = computed(() => followingUsers.value.length > 0);

const syncRoute = () => {
    router.replace({
        path: '/following',
        query: {
            sort: activeSort.value === ARTICLE_SORT_LATEST ? undefined : activeSort.value,
            page: currentPage.value === 1 ? undefined : String(currentPage.value)
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

const fetchFeed = async () => {
    if (!isLoggedIn.value) {
        articles.value = [];
        total.value = 0;
        return;
    }
    loading.value = true;
    errorMessage.value = '';
    try {
        const pageResult = await getFollowingFeedApi({
            page: currentPage.value,
            pageSize,
            sort: activeSort.value
        });
        articles.value = pageResult.items || [];
        total.value = pageResult.total || 0;
    } catch (error) {
        articles.value = [];
        total.value = 0;
        errorMessage.value = error.message || '关注流加载失败';
    } finally {
        loading.value = false;
    }
};

const refreshAll = async () => {
    await Promise.all([fetchMeta(), fetchFeed()]);
};

const changePage = async (page) => {
    currentPage.value = page;
    syncRoute();
};

const changeSort = async (sort) => {
    activeSort.value = normalizeArticleSort(sort);
    currentPage.value = 1;
    syncRoute();
};

const handleFollowChange = async () => {
    currentPage.value = 1;
    await refreshAll();
};

onMounted(fetchMeta);

watch(isLoggedIn, async () => {
    currentPage.value = 1;
    await refreshAll();
});

watch(() => route.query, async (query) => {
    currentPage.value = Number.parseInt(query.page || '1', 10) || 1;
    activeSort.value = normalizeArticleSort(query.sort || ARTICLE_SORT_LATEST);
    await fetchFeed();
}, { immediate: true });
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
        </section>

        <div class="content-grid">
            <section class="following-main">
                <EmptyState
                    v-if="!isLoggedIn"
                    eyebrow="登录后查看"
                    title="先登录，再建立自己的关注流"
                    description="关注作者后，这里会变成你的私人内容更新入口。"
                    data-testid="following-login-empty"
                />
                <template v-else>
                    <ArticleFeed
                        :articles="articles"
                        :page="currentPage"
                        :page-size="pageSize"
                        :total="total"
                        :loading="loading"
                        :error-message="errorMessage"
                        :sort="activeSort"
                        :sort-items="ARTICLE_SORT_ITEMS"
                        eyebrow="关注动态"
                        title="关注作者的新文章"
                        :empty-text="hasFollowing ? '你关注的作者最近还没有新内容' : '先关注几位作者，关注流就会热闹起来'"
                        @page-change="changePage"
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
                            <img :src="author.avatar" alt="作者头像" loading="lazy">
                            <div>
                                <strong>{{ author.name }}</strong>
                                <span>{{ author.bio || '持续分享工程实践与技术经验。' }}</span>
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
                                <img :src="item.user.avatar" alt="作者头像" loading="lazy">
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
    background: linear-gradient(180deg, rgba(248, 251, 255, 0.98), #ffffff);
    border: 1px solid rgba(208, 219, 236, 0.92);
    border-radius: 18px;
    transition: transform 0.16s ease, border-color 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease;
}

.following-author-item {
    grid-template-columns: 48px minmax(0, 1fr);
    text-decoration: none;
}

.following-author-item img,
.rank-author-info img {
    width: 48px;
    height: 48px;
    object-fit: cover;
    border-radius: 14px;
}

.following-author-item strong,
.rank-author-info a {
    color: var(--text);
    text-decoration: none;
}

.following-author-item span,
.rank-author-info span {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
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
    background: rgba(40, 118, 255, 0.05);
    border-color: rgba(40, 118, 255, 0.14);
    box-shadow: 0 16px 28px rgba(31, 78, 168, 0.08);
    transform: translateY(-1px);
}

@media (max-width: 960px) {
    .content-grid {
        grid-template-columns: 1fr;
    }
}
</style>
