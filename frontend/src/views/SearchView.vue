<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { listArticlesApi } from '@/api/articles';
import {
    getSearchBootstrapApi,
    searchUsersApi,
    searchColumnsApi,
    saveRecentKeywordApi,
    clearRecentKeywordsApi
} from '@/api/search';
import { followUserApi } from '@/api/following';
import { subscribeColumnApi } from '@/api/columns';
import { useSession } from '@/stores/session';
import ArticleFeed from '@/components/ArticleFeed.vue';
import {
    ARTICLE_SORT_ITEMS,
    ARTICLE_SORT_LATEST,
    isDefaultArticleSort,
    normalizeArticleSort
} from '@/constants/articleSort';
import SiteHeader from '@/components/SiteHeader.vue';

const GUEST_RECENT_SEARCHES_KEY = 'my-blog:recent-searches';
const MAX_GUEST_RECENT_SEARCHES = 10;

const route = useRoute();
const router = useRouter();
const { isLoggedIn, state } = useSession();

const activeTab = ref(String(route.query.tab || 'articles'));
const keyword = ref(String(route.query.keyword || ''));
const currentPage = ref(Number.parseInt(route.query.page || '1', 10) || 1);
const pageSize = 10;

// Article filters
const activeCategory = ref(String(route.query.category || ''));
const activeTag = ref(String(route.query.tag || ''));
const activeSort = ref(String(route.query.sort || ARTICLE_SORT_LATEST));
const authorKeyword = ref(String(route.query.authorKeyword || ''));
const dateFrom = ref(String(route.query.dateFrom || ''));
const dateTo = ref(String(route.query.dateTo || ''));
const followingOnly = ref(route.query.followingOnly === 'true');

// Data
const bootstrap = ref(null);
const articles = ref([]);
const articleTotal = ref(0);
const users = ref([]);
const userTotal = ref(0);
const columns = ref([]);
const columnTotal = ref(0);
const loading = ref(false);
const errorMessage = ref('');

// Guest recent searches
const guestRecentSearches = ref([]);

const categoryOptions = computed(() => ['全部', ...(bootstrap.value?.categories || []).map((item) => item.name)]);
const tagOptions = computed(() => ['全部', ...(bootstrap.value?.tags || []).map((item) => item.name)]);
const hotKeywords = computed(() => bootstrap.value?.hotKeywords || []);
const recentKeywords = computed(() => bootstrap.value?.recentKeywords || guestRecentSearches.value);

const syncRoute = (overrides = {}) => {
    const query = {
        keyword: overrides.keyword ?? (keyword.value.trim() || undefined),
        tab: overrides.tab ?? activeTab.value,
        page: String(overrides.page ?? currentPage.value) === '1' ? undefined : String(overrides.page ?? currentPage.value)
    };

    if (activeTab.value === 'articles') {
        const nextSort = overrides.sort ?? activeSort.value;
        query.category = overrides.category ?? (activeCategory.value || undefined);
        query.tag = overrides.tag ?? (activeTag.value || undefined);
        query.sort = isDefaultArticleSort(nextSort) ? undefined : nextSort;
        query.authorKeyword = overrides.authorKeyword ?? (authorKeyword.value || undefined);
        query.dateFrom = overrides.dateFrom ?? (dateFrom.value || undefined);
        query.dateTo = overrides.dateTo ?? (dateTo.value || undefined);
        query.followingOnly = overrides.followingOnly ?? (followingOnly.value ? 'true' : undefined);
    }

    router.replace({ path: '/search', query });
};

const fetchBootstrap = async () => {
    try {
        bootstrap.value = await getSearchBootstrapApi();
        if (!isLoggedIn.value) {
            loadGuestRecentSearches();
        }
    } catch (e) {
        console.error('Failed to fetch search bootstrap:', e);
    }
};

const fetchArticles = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const pageResult = await listArticlesApi({
            keyword: keyword.value.trim(),
            category: activeCategory.value,
            tag: activeTag.value,
            sort: activeSort.value,
            authorKeyword: authorKeyword.value,
            dateFrom: dateFrom.value,
            dateTo: dateTo.value,
            followingOnly: followingOnly.value,
            page: currentPage.value,
            pageSize
        });
        articles.value = pageResult.items || [];
        articleTotal.value = pageResult.total || 0;
    } catch (e) {
        articles.value = [];
        articleTotal.value = 0;
        errorMessage.value = e.message || '搜索失败，请稍后重试';
    } finally {
        loading.value = false;
    }
};

const fetchUsers = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const pageResult = await searchUsersApi({
            keyword: keyword.value.trim(),
            page: currentPage.value,
            pageSize
        });
        users.value = pageResult.items || [];
        userTotal.value = pageResult.total || 0;
    } catch (e) {
        users.value = [];
        userTotal.value = 0;
        errorMessage.value = e.message || '搜索失败，请稍后重试';
    } finally {
        loading.value = false;
    }
};

const fetchColumns = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const pageResult = await searchColumnsApi({
            keyword: keyword.value.trim(),
            page: currentPage.value,
            pageSize
        });
        columns.value = pageResult.items || [];
        columnTotal.value = pageResult.total || 0;
    } catch (e) {
        columns.value = [];
        columnTotal.value = 0;
        errorMessage.value = e.message || '搜索失败，请稍后重试';
    } finally {
        loading.value = false;
    }
};

const runSearch = async () => {
    currentPage.value = 1;
    if (keyword.value.trim()) {
        if (isLoggedIn.value) {
            await saveRecentKeywordApi(keyword.value.trim());
        } else {
            addGuestRecentSearch(keyword.value.trim());
        }
    }
    syncRoute({ page: 1, keyword: keyword.value.trim() || undefined });
    await fetchCurrentTab();
};

const changeTab = (tab) => {
    activeTab.value = tab;
    currentPage.value = 1;
    // Reset article-only filters when switching tabs
    activeCategory.value = '';
    activeTag.value = '';
    activeSort.value = ARTICLE_SORT_LATEST;
    authorKeyword.value = '';
    dateFrom.value = '';
    dateTo.value = '';
    followingOnly.value = false;
    syncRoute({ tab, page: 1 });
};

const changeCategory = (value) => {
    activeCategory.value = value === '全部' ? '' : value;
    currentPage.value = 1;
    syncRoute({ page: 1, category: activeCategory.value || undefined });
};

const changeTag = (value) => {
    activeTag.value = value === '全部' ? '' : value;
    currentPage.value = 1;
    syncRoute({ page: 1, tag: activeTag.value || undefined });
};

const changeSort = (sort) => {
    activeSort.value = normalizeArticleSort(sort);
    currentPage.value = 1;
    syncRoute({ page: 1, sort: activeSort.value });
};

const changeAuthorKeyword = () => {
    currentPage.value = 1;
    syncRoute({ page: 1, authorKeyword: authorKeyword.value || undefined });
};

const changeDateFrom = () => {
    currentPage.value = 1;
    syncRoute({ page: 1, dateFrom: dateFrom.value || undefined });
};

const changeDateTo = () => {
    currentPage.value = 1;
    syncRoute({ page: 1, dateTo: dateTo.value || undefined });
};

const changeFollowingOnly = () => {
    if (!isLoggedIn.value) {
        return;
    }
    currentPage.value = 1;
    syncRoute({ page: 1, followingOnly: followingOnly.value ? 'true' : undefined });
};

const changePage = (page) => {
    currentPage.value = page;
    syncRoute({ page });
};

const fetchCurrentTab = async () => {
    switch (activeTab.value) {
        case 'articles':
            await fetchArticles();
            break;
        case 'users':
            await fetchUsers();
            break;
        case 'columns':
            await fetchColumns();
            break;
    }
};

const clickKeyword = (kw) => {
    keyword.value = kw;
    runSearch();
};

const clearRecentSearches = async () => {
    if (isLoggedIn.value) {
        await clearRecentKeywordsApi();
        if (bootstrap.value) {
            bootstrap.value.recentKeywords = [];
        }
    } else {
        guestRecentSearches.value = [];
        localStorage.removeItem(GUEST_RECENT_SEARCHES_KEY);
    }
};

const loadGuestRecentSearches = () => {
    try {
        const stored = localStorage.getItem(GUEST_RECENT_SEARCHES_KEY);
        if (stored) {
            guestRecentSearches.value = JSON.parse(stored);
        }
    } catch (e) {
        guestRecentSearches.value = [];
    }
};

const addGuestRecentSearch = (kw) => {
    const trimmed = kw.trim();
    if (!trimmed) return;
    guestRecentSearches.value = guestRecentSearches.value.filter((k) => k !== trimmed);
    guestRecentSearches.value.unshift(trimmed);
    if (guestRecentSearches.value.length > MAX_GUEST_RECENT_SEARCHES) {
        guestRecentSearches.value = guestRecentSearches.value.slice(0, MAX_GUEST_RECENT_SEARCHES);
    }
    localStorage.setItem(GUEST_RECENT_SEARCHES_KEY, JSON.stringify(guestRecentSearches.value));
};

const goToUser = (userId) => {
    router.push(`/users/${userId}`);
};

const goToColumn = (columnId) => {
    router.push(`/columns/${columnId}`);
};

const handleFollow = async (user) => {
    if (!isLoggedIn.value) {
        return;
    }
    try {
        await followUserApi(user.id);
        user.followed = true;
        user.followerCount = (user.followerCount || 0) + 1;
    } catch (e) {
        console.error('Failed to follow user:', e);
    }
};

const handleSubscribe = async (column) => {
    if (!isLoggedIn.value) {
        return;
    }
    try {
        await subscribeColumnApi(column.id);
        column.subscribed = true;
        column.subscriberCount = (column.subscriberCount || 0) + 1;
    } catch (e) {
        console.error('Failed to subscribe column:', e);
    }
};

watch(
    () => route.query,
    (query) => {
        const newTab = String(query.tab || 'articles');
        if (newTab !== activeTab.value) {
            activeTab.value = newTab;
        }
        keyword.value = String(query.keyword || '');
        currentPage.value = Number.parseInt(query.page || '1', 10) || 1;
        activeCategory.value = String(query.category || '');
        activeTag.value = String(query.tag || '');
        activeSort.value = normalizeArticleSort(String(query.sort || ARTICLE_SORT_LATEST));
        authorKeyword.value = String(query.authorKeyword || '');
        dateFrom.value = String(query.dateFrom || '');
        dateTo.value = String(query.dateTo || '');
        followingOnly.value = query.followingOnly === 'true';
        fetchCurrentTab();
    },
    { immediate: true }
);

onMounted(fetchBootstrap);
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <section class="search-panel">
            <p class="eyebrow">搜索</p>
            <h1>发现内容</h1>
            <form class="search-large" @submit.prevent="runSearch">
                <input v-model="keyword" type="search" aria-label="搜索关键字">
                <button type="submit">搜索</button>
            </form>

            <!-- Recent + Hot Keywords -->
            <div v-if="recentKeywords.length || hotKeywords.length" class="keyword-section">
                <div v-if="recentKeywords.length" class="keyword-group">
                    <span class="keyword-label">最近搜索</span>
                    <div class="keyword-row">
                        <button
                            v-for="kw in recentKeywords"
                            :key="kw"
                            type="button"
                            class="keyword-chip"
                            @click="clickKeyword(kw)"
                        >
                            {{ kw }}
                        </button>
                        <button type="button" class="keyword-clear" @click="clearRecentSearches">清空</button>
                    </div>
                </div>
                <div v-if="hotKeywords.length" class="keyword-group">
                    <span class="keyword-label">热门</span>
                    <div class="keyword-row">
                        <button
                            v-for="kw in hotKeywords"
                            :key="kw"
                            type="button"
                            class="keyword-chip hot"
                            @click="clickKeyword(kw)"
                        >
                            {{ kw }}
                        </button>
                    </div>
                </div>
            </div>

            <!-- Tabs -->
            <div class="search-tabs">
                <button
                    type="button"
                    :class="{ active: activeTab === 'articles' }"
                    @click="changeTab('articles')"
                >
                    文章
                </button>
                <button
                    type="button"
                    :class="{ active: activeTab === 'users' }"
                    @click="changeTab('users')"
                >
                    作者
                </button>
                <button
                    type="button"
                    :class="{ active: activeTab === 'columns' }"
                    @click="changeTab('columns')"
                >
                    专栏
                </button>
            </div>

            <!-- Article Filters -->
            <div v-if="activeTab === 'articles'" class="search-filters">
                <div class="filter-group">
                    <span>分类</span>
                    <div class="tag-row">
                        <button
                            v-for="category in categoryOptions"
                            :key="category"
                            type="button"
                            :class="{ active: (activeCategory || '全部') === category }"
                            @click="changeCategory(category)"
                        >
                            {{ category }}
                        </button>
                    </div>
                </div>
                <div class="filter-group">
                    <span>标签</span>
                    <div class="tag-row">
                        <button
                            v-for="tag in tagOptions"
                            :key="tag"
                            type="button"
                            :class="{ active: (activeTag || '全部') === tag }"
                            @click="changeTag(tag)"
                        >
                            {{ tag }}
                        </button>
                    </div>
                </div>
                <div class="filter-group enhanced-filters">
                    <span>作者关键字</span>
                    <input
                        v-model="authorKeyword"
                        type="text"
                        placeholder="搜索作者..."
                        class="filter-input"
                        @keydown.enter="changeAuthorKeyword"
                    >
                </div>
                <div class="filter-group date-filters">
                    <span>日期范围</span>
                    <div class="date-inputs">
                        <input
                            v-model="dateFrom"
                            type="date"
                            class="filter-input date-input"
                            @change="changeDateFrom"
                        >
                        <span class="date-separator">至</span>
                        <input
                            v-model="dateTo"
                            type="date"
                            class="filter-input date-input"
                            @change="changeDateTo"
                        >
                    </div>
                </div>
                <div v-if="isLoggedIn" class="filter-group following-filter">
                    <label class="checkbox-label">
                        <input
                            v-model="followingOnly"
                            type="checkbox"
                            @change="changeFollowingOnly"
                        >
                        <span>仅看已关注作者</span>
                    </label>
                </div>
                <div v-else class="filter-group following-filter">
                    <label class="checkbox-label disabled">
                        <input
                            type="checkbox"
                            disabled
                        >
                        <span>仅看已关注作者（需登录）</span>
                    </label>
                </div>
            </div>

            <p class="result-note">
                <template v-if="activeTab === 'articles'">共找到 {{ articleTotal }} 篇文章</template>
                <template v-else-if="activeTab === 'users'">共找到 {{ userTotal }} 位作者</template>
                <template v-else>共找到 {{ columnTotal }} 个专栏</template>
            </p>
        </section>

        <!-- Article Results -->
        <ArticleFeed
            v-if="activeTab === 'articles'"
            :articles="articles"
            :page="currentPage"
            :page-size="pageSize"
            :total="articleTotal"
            :loading="loading"
            :error-message="errorMessage"
            :sort="activeSort"
            :sort-items="ARTICLE_SORT_ITEMS"
            eyebrow="搜索结果"
            title="匹配文章"
            empty-text="换个关键词、分类或标签试试"
            @page-change="changePage"
            @sort-change="changeSort"
        />

        <!-- User Results -->
        <div v-else-if="activeTab === 'users'" class="user-results">
            <div v-if="loading" class="loading-state">加载中...</div>
            <div v-else-if="errorMessage" class="error-state">{{ errorMessage }}</div>
            <div v-else-if="users.length === 0" class="empty-state">暂无匹配作者</div>
            <div v-else class="user-list">
                <div
                    v-for="user in users"
                    :key="user.id"
                    class="user-card"
                    @click="goToUser(user.id)"
                >
                    <img :src="user.avatarUrl || 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80'" :alt="user.nickname" class="user-avatar">
                    <div class="user-info">
                        <div class="user-name">{{ user.nickname || user.username }}</div>
                        <div class="user-bio">{{ user.bio || '' }}</div>
                        <div class="user-stats">
                            <span>粉丝 {{ user.followerCount || 0 }}</span>
                            <span>文章 {{ user.articleCount || 0 }}</span>
                        </div>
                    </div>
                    <button
                        type="button"
                        class="follow-btn"
                        @click.stop="handleFollow(user)"
                    >
                        关注
                    </button>
                </div>
            </div>
            <div v-if="userTotal > pageSize" class="pagination">
                <button
                    v-if="currentPage > 1"
                    type="button"
                    @click="changePage(currentPage - 1)"
                >
                    上一页
                </button>
                <span class="page-info">{{ currentPage }} / {{ Math.ceil(userTotal / pageSize) }}</span>
                <button
                    v-if="currentPage * pageSize < userTotal"
                    type="button"
                    @click="changePage(currentPage + 1)"
                >
                    下一页
                </button>
            </div>
        </div>

        <!-- Column Results -->
        <div v-else-if="activeTab === 'columns'" class="column-results">
            <div v-if="loading" class="loading-state">加载中...</div>
            <div v-else-if="errorMessage" class="error-state">{{ errorMessage }}</div>
            <div v-else-if="columns.length === 0" class="empty-state">暂无匹配专栏</div>
            <div v-else class="column-list">
                <div
                    v-for="column in columns"
                    :key="column.id"
                    class="column-card"
                    @click="goToColumn(column.id)"
                >
                    <img :src="column.coverUrl || 'https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=300&q=80'" :alt="column.name" class="column-cover">
                    <div class="column-info">
                        <div class="column-name">{{ column.name }}</div>
                        <div class="column-description">{{ column.description || '' }}</div>
                        <div class="column-stats">
                            <span>订阅 {{ column.subscriberCount || 0 }}</span>
                            <span>文章 {{ column.articleCount || 0 }}</span>
                        </div>
                    </div>
                    <button
                        type="button"
                        class="subscribe-btn"
                        @click.stop="handleSubscribe(column)"
                    >
                        订阅
                    </button>
                </div>
            </div>
            <div v-if="columnTotal > pageSize" class="pagination">
                <button
                    v-if="currentPage > 1"
                    type="button"
                    @click="changePage(currentPage - 1)"
                >
                    上一页
                </button>
                <span class="page-info">{{ currentPage }} / {{ Math.ceil(columnTotal / pageSize) }}</span>
                <button
                    v-if="currentPage * pageSize < columnTotal"
                    type="button"
                    @click="changePage(currentPage + 1)"
                >
                    下一页
                </button>
            </div>
        </div>
    </main>
</template>

<style scoped>
.search-panel {
    display: grid;
    gap: 14px;
}

.search-panel .eyebrow {
    margin: 0;
}

.search-panel h1 {
    margin: 0;
}

.keyword-section {
    display: grid;
    gap: 10px;
}

.keyword-group {
    display: grid;
    gap: 6px;
}

.keyword-label {
    font-size: 13px;
    color: var(--muted);
}

.keyword-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
}

.keyword-chip {
    padding: 4px 10px;
    border-radius: 14px;
    background: var(--bg-secondary);
    border: 1px solid var(--border);
    font-size: 13px;
    cursor: pointer;
}

.keyword-chip:hover {
    background: var(--bg-tertiary);
}

.keyword-chip.hot {
    background: #fff3e0;
    border-color: #ffb74d;
    color: #e65100;
}

.keyword-clear {
    padding: 4px 10px;
    border-radius: 14px;
    background: transparent;
    border: 1px solid var(--border);
    font-size: 13px;
    color: var(--muted);
    cursor: pointer;
}

.keyword-clear:hover {
    color: var(--error);
    border-color: var(--error);
}

.search-tabs {
    display: flex;
    gap: 4px;
    border-bottom: 1px solid var(--border);
    margin-top: 8px;
}

.search-tabs button {
    padding: 8px 16px;
    border: none;
    background: transparent;
    cursor: pointer;
    border-bottom: 2px solid transparent;
    margin-bottom: -1px;
}

.search-tabs button.active {
    border-bottom-color: var(--primary);
    color: var(--primary);
}

.search-filters {
    display: grid;
    gap: 14px;
    margin-top: 6px;
}

.filter-group {
    display: grid;
    gap: 6px;
}

.filter-group > span {
    font-size: 13px;
    color: var(--muted);
}

.filter-group :deep(.tag-row) {
    margin: 4px 0 8px;
}

.filter-input {
    padding: 6px 10px;
    border: 1px solid var(--border);
    border-radius: 6px;
    font-size: 13px;
    width: 100%;
    max-width: 200px;
}

.filter-input:focus {
    outline: none;
    border-color: var(--primary);
}

.date-inputs {
    display: flex;
    align-items: center;
    gap: 8px;
}

.date-input {
    width: 140px;
}

.date-separator {
    color: var(--muted);
}

.checkbox-label {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    font-size: 13px;
}

.checkbox-label.disabled {
    color: var(--muted);
    cursor: not-allowed;
}

.checkbox-label input[type="checkbox"] {
    width: 16px;
    height: 16px;
}

.search-panel :deep(.search-large) {
    margin-top: 4px;
}

.search-panel :deep(.result-note) {
    margin-top: 0;
}

.user-results, .column-results {
    margin-top: 20px;
}

.loading-state, .error-state, .empty-state {
    text-align: center;
    padding: 40px;
    color: var(--muted);
}

.user-list, .column-list {
    display: grid;
    gap: 12px;
}

.user-card, .column-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    border: 1px solid var(--border);
    border-radius: 8px;
    cursor: pointer;
}

.user-card:hover, .column-card:hover {
    border-color: var(--primary);
}

.user-avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    object-fit: cover;
}

.column-cover {
    width: 80px;
    height: 60px;
    border-radius: 6px;
    object-fit: cover;
}

.user-info, .column-info {
    flex: 1;
    min-width: 0;
}

.user-name, .column-name {
    font-weight: 600;
    font-size: 15px;
}

.user-bio, .column-description {
    font-size: 13px;
    color: var(--muted);
    margin-top: 2px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.user-stats, .column-stats {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: var(--muted);
    margin-top: 4px;
}

.follow-btn, .subscribe-btn {
    padding: 6px 16px;
    border-radius: 18px;
    border: 1px solid var(--primary);
    background: transparent;
    color: var(--primary);
    cursor: pointer;
    font-size: 13px;
}

.follow-btn:hover, .subscribe-btn:hover {
    background: var(--primary);
    color: white;
}

.pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
    margin-top: 20px;
}

.pagination button {
    padding: 6px 14px;
    border: 1px solid var(--border);
    border-radius: 6px;
    background: var(--bg-secondary);
    cursor: pointer;
}

.pagination button:hover {
    border-color: var(--primary);
}

.page-info {
    font-size: 13px;
    color: var(--muted);
}
</style>
