<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { getFollowingFeedApi, getMyFollowingApi } from '@/api/following';
import { getAuthorRankingsApi } from '@/api/rankings';
import { ARTICLE_SORT_ITEMS, ARTICLE_SORT_LATEST, normalizeArticleSort } from '@/constants/articleSort';
import { useSession } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const { isLoggedIn } = useSession();

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
    await fetchMeta();
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
    await Promise.all([fetchMeta(), fetchFeed()]);
};

onMounted(refreshAll);

watch(isLoggedIn, async () => {
    currentPage.value = 1;
    await Promise.all([fetchMeta(), fetchFeed()]);
});

watch(() => route.query, async (query) => {
    currentPage.value = Number.parseInt(query.page || '1', 10) || 1;
    activeSort.value = normalizeArticleSort(query.sort || ARTICLE_SORT_LATEST);
    await fetchFeed();
}, { immediate: true });
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
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
                <div v-if="!isLoggedIn" class="placeholder-panel">
                    <p class="eyebrow">登录后查看</p>
                    <h1>先登录，再建立自己的关注流</h1>
                    <p>关注作者后，这里会变成你的私人内容更新入口。</p>
                </div>
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
                <section class="side-section">
                    <div class="section-heading compact">
                        <div>
                            <p class="eyebrow">我的关注</p>
                            <h2>作者列表</h2>
                        </div>
                    </div>
                    <div v-if="sidebarLoading" class="sidebar-state">加载中...</div>
                    <div v-else-if="!isLoggedIn" class="sidebar-state">登录后查看关注作者</div>
                    <div v-else-if="!followingUsers.length" class="sidebar-state">你还没有关注任何作者</div>
                    <div v-else class="following-author-list">
                        <RouterLink
                            v-for="author in followingUsers"
                            :key="author.id"
                            class="following-author-item"
                            :to="`/users/${author.id}`"
                        >
                            <img :src="author.avatar" alt="作者头像">
                            <div>
                                <strong>{{ author.name }}</strong>
                                <span>{{ author.bio || '持续分享工程实践与技术经验。' }}</span>
                            </div>
                        </RouterLink>
                    </div>
                </section>

                <section class="side-section">
                    <div class="section-heading compact">
                        <div>
                            <p class="eyebrow">推荐关注</p>
                            <h2>值得追踪的作者</h2>
                        </div>
                    </div>
                    <div v-if="sidebarLoading" class="sidebar-state">加载中...</div>
                    <div v-else class="rank-author-list">
                        <div
                            v-for="item in recommendedAuthors"
                            :key="item.user.id"
                            class="rank-author-item"
                        >
                            <div class="rank-author-info">
                                <img :src="item.user.avatar" alt="作者头像">
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
                </section>
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
    background: #f8fbfa;
    border: 1px solid rgba(219, 227, 223, 0.92);
    border-radius: 8px;
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
    border-radius: 8px;
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

@media (max-width: 960px) {
    .content-grid {
        grid-template-columns: 1fr;
    }
}
</style>
