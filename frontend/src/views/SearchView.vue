<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useHead} from '@unhead/vue';
import {listArticlesApi} from '@/api/articles';
import {clearRecentKeywordsApi, getSearchBootstrapApi, saveRecentKeywordApi, searchColumnsApi, searchUsersApi} from '@/api/search';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import ColumnSubscribeButton from '@/components/ColumnSubscribeButton.vue';
import {useSession} from '@/stores/session';
import ArticleFeed from '@/components/ArticleFeed.vue';
import {ARTICLE_SORT_ITEMS, ARTICLE_SORT_LATEST, isDefaultArticleSort, normalizeArticleSort} from '@/constants/articleSort';
import SiteHeader from '@/components/SiteHeader.vue';
import AdBanner from '@/components/AdBanner.vue';
import {useStableListRequest} from '@/composables/useStableListRequest';

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

// Guest recent searches
const guestRecentSearches = ref([]);

useHead({
    title: computed(() => keyword.value ? `搜索: ${keyword.value} - my-blog` : '搜索 - my-blog')
});

// Expand/collapse state for filters
const categoriesExpanded = ref(false);
const tagsExpanded = ref(false);
const filtersExpanded = ref(false);

const formatDate = (date) => {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
};

const datePresets = [
    { label: '今天', getRange: () => {
        const today = new Date();
        return { from: formatDate(today), to: formatDate(today) };
    }},
    { label: '本周', getRange: () => {
        const now = new Date();
        const monday = new Date(now);
        monday.setDate(now.getDate() - (now.getDay() || 7) + 1);
        return { from: formatDate(monday), to: formatDate(now) };
    }},
    { label: '本月', getRange: () => {
        const now = new Date();
        const first = new Date(now.getFullYear(), now.getMonth(), 1);
        return { from: formatDate(first), to: formatDate(now) };
    }},
    { label: '今年', getRange: () => {
        const now = new Date();
        const first = new Date(now.getFullYear(), 0, 1);
        return { from: formatDate(first), to: formatDate(now) };
    }},
];

const isPresetActive = (preset) => {
    const { from, to } = preset.getRange();
    return dateFrom.value === from && dateTo.value === to;
};

const applyDatePreset = (preset) => {
    const { from, to } = preset.getRange();
    dateFrom.value = from;
    dateTo.value = to;
    currentPage.value = 1;
    syncRoute({ page: 1, dateFrom: from, dateTo: to });
};

const activeFilters = computed(() => {
    const filters = [];
    if (activeCategory.value) {
        filters.push({
            key: 'category',
            label: `分类: ${activeCategory.value}`,
            onRemove: () => { activeCategory.value = ''; currentPage.value = 1; syncRoute({ page: 1 }); }
        });
    }
    if (activeTag.value) {
        filters.push({
            key: 'tag',
            label: `标签: ${activeTag.value}`,
            onRemove: () => { activeTag.value = ''; currentPage.value = 1; syncRoute({ page: 1 }); }
        });
    }
    if (!isDefaultArticleSort(activeSort.value)) {
        const sortItem = ARTICLE_SORT_ITEMS.find(s => s.value === activeSort.value);
        if (sortItem) {
            filters.push({
                key: 'sort',
                label: `排序: ${sortItem.label}`,
                onRemove: () => { activeSort.value = ARTICLE_SORT_LATEST; currentPage.value = 1; syncRoute({ page: 1 }); }
            });
        }
    }
    if (authorKeyword.value) {
        filters.push({
            key: 'author',
            label: `作者: ${authorKeyword.value}`,
            onRemove: () => { authorKeyword.value = ''; currentPage.value = 1; syncRoute({ page: 1 }); }
        });
    }
    if (dateFrom.value || dateTo.value) {
        const label = dateFrom.value && dateTo.value
            ? `${dateFrom.value} ~ ${dateTo.value}`
            : (dateFrom.value || dateTo.value);
        filters.push({
            key: 'date',
            label: `日期: ${label}`,
            onRemove: () => { dateFrom.value = ''; dateTo.value = ''; currentPage.value = 1; syncRoute({ page: 1 }); }
        });
    }
    if (followingOnly.value) {
        filters.push({
            key: 'following',
            label: '仅看关注',
            onRemove: () => { followingOnly.value = false; currentPage.value = 1; syncRoute({ page: 1 }); }
        });
    }
    return filters;
});

const activeFilterCount = computed(() => activeFilters.value.length);

const clearAllFilters = () => {
    activeCategory.value = '';
    activeTag.value = '';
    activeSort.value = ARTICLE_SORT_LATEST;
    authorKeyword.value = '';
    dateFrom.value = '';
    dateTo.value = '';
    followingOnly.value = false;
    currentPage.value = 1;
    syncRoute({ page: 1 });
};

const MAX_VISIBLE_TAGS = 10; // 大约两行的数量

const categoryOptions = computed(() => ['全部', ...(bootstrap.value?.categories || []).map((item) => item.name)]);
const tagOptions = computed(() => ['全部', ...(bootstrap.value?.tags || []).map((item) => item.name)]);
const visibleCategories = computed(() => {
    if (categoriesExpanded.value) {
        return categoryOptions.value;
    }
    return categoryOptions.value.slice(0, MAX_VISIBLE_TAGS);
});
const visibleTags = computed(() => {
    if (tagsExpanded.value) {
        return tagOptions.value;
    }
    return tagOptions.value.slice(0, MAX_VISIBLE_TAGS);
});
const hasMoreCategories = computed(() => categoryOptions.value.length > MAX_VISIBLE_TAGS);
const hasMoreTags = computed(() => tagOptions.value.length > MAX_VISIBLE_TAGS);
const hotKeywords = computed(() => bootstrap.value?.hotKeywords || []);
const showKeywordChips = computed(() =>
    !keyword.value.trim() &&
    articles.value.length === 0 &&
    users.value.length === 0 &&
    columns.value.length === 0
);
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
    const response = await runStableRequest(
        () => listArticlesApi({
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
        }),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '搜索失败，请稍后重试',
            refreshErrorMessage: '搜索结果刷新失败，请稍后重试'
        }
    );
    if (response?.ignored || response?.error) {
        return;
    }
    const pageResult = response.result || {};
    articles.value = pageResult.items || [];
    articleTotal.value = pageResult.total || 0;
};

const fetchUsers = async () => {
    const response = await runStableRequest(
        () => searchUsersApi({
            keyword: keyword.value.trim(),
            page: currentPage.value,
            pageSize
        }),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '搜索失败，请稍后重试',
            refreshErrorMessage: '搜索结果刷新失败，请稍后重试'
        }
    );
    if (response?.ignored || response?.error) {
        return;
    }
    const pageResult = response.result || {};
    users.value = pageResult.items || [];
    userTotal.value = pageResult.total || 0;
};

const fetchColumns = async () => {
    const response = await runStableRequest(
        () => searchColumnsApi({
            keyword: keyword.value.trim(),
            page: currentPage.value,
            pageSize
        }),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '搜索失败，请稍后重试',
            refreshErrorMessage: '搜索结果刷新失败，请稍后重试'
        }
    );
    if (response?.ignored || response?.error) {
        return;
    }
    const pageResult = response.result || {};
    columns.value = pageResult.items || [];
    columnTotal.value = pageResult.total || 0;
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
    resetStableRequest();
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

const handleFollowChange = (user, followed) => {
    user.followed = followed;
    user.followerCount = Math.max(0, (user.followerCount || 0) + (followed ? 1 : -1));
};

const handleSubscribeChange = (column, subscribed) => {
    column.subscribed = subscribed;
    column.subscriberCount = Math.max(0, (column.subscriberCount || 0) + (subscribed ? 1 : -1));
};

watch(
    () => route.query,
    (query) => {
        const newTab = String(query.tab || 'articles');
        if (newTab !== activeTab.value) {
            resetStableRequest();
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
            <div v-if="showKeywordChips && (recentKeywords.length || hotKeywords.length)" class="keyword-section">
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

            <!-- Mobile filter toggle -->
            <button
                v-if="activeTab === 'articles'"
                class="mobile-filter-toggle"
                type="button"
                @click="filtersExpanded = !filtersExpanded"
            >
                <span>
                    筛选条件
                    <span v-if="activeFilterCount" class="filter-count-badge">{{ activeFilterCount }}</span>
                </span>
                <span class="filter-toggle-arrow" :class="{ open: filtersExpanded }">▾</span>
            </button>

            <!-- Article Filters -->
            <div v-if="activeTab === 'articles'" class="search-filters" :class="{ collapsed: !filtersExpanded }">
                <div class="filter-group sort-row">
                    <span>排序方式</span>
                    <div class="sort-buttons">
                        <button
                            v-for="item in ARTICLE_SORT_ITEMS"
                            :key="item.value"
                            type="button"
                            :class="{ active: activeSort === item.value }"
                            @click="changeSort(item.value)"
                        >
                            {{ item.label }}
                        </button>
                    </div>
                </div>
                <div class="filter-group">
                    <span>分类</span>
                    <div class="tag-row">
                        <button
                            v-for="category in visibleCategories"
                            :key="category"
                            type="button"
                            :class="{ active: (activeCategory || '全部') === category }"
                            @click="changeCategory(category)"
                        >
                            {{ category }}
                        </button>
                        <button
                            v-if="hasMoreCategories"
                            type="button"
                            class="expand-btn"
                            @click="categoriesExpanded = !categoriesExpanded"
                        >
                            {{ categoriesExpanded ? '收起' : `+${categoryOptions.length - MAX_VISIBLE_TAGS}个` }}
                        </button>
                    </div>
                </div>
                <div class="filter-group">
                    <span>标签</span>
                    <div class="tag-row">
                        <button
                            v-for="tag in visibleTags"
                            :key="tag"
                            type="button"
                            :class="{ active: (activeTag || '全部') === tag }"
                            @click="changeTag(tag)"
                        >
                            {{ tag }}
                        </button>
                        <button
                            v-if="hasMoreTags"
                            type="button"
                            class="expand-btn"
                            @click="tagsExpanded = !tagsExpanded"
                        >
                            {{ tagsExpanded ? '收起' : `+${tagOptions.length - MAX_VISIBLE_TAGS}个` }}
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
                    <span>发布日期</span>
                    <div class="date-presets">
                        <button
                            v-for="preset in datePresets"
                            :key="preset.label"
                            type="button"
                            :class="{ active: isPresetActive(preset) }"
                            @click="applyDatePreset(preset)"
                        >
                            {{ preset.label }}
                        </button>
                    </div>
                    <div class="date-range">
                        <input
                            v-model="dateFrom"
                            type="date"
                            class="filter-input"
                            @change="changeDateFrom"
                        >
                        <span class="date-range-sep">至</span>
                        <input
                            v-model="dateTo"
                            type="date"
                            class="filter-input"
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

            <!-- Active filter chips -->
            <div v-if="activeTab === 'articles' && activeFilters.length" class="active-filter-bar">
                <span class="active-filter-label">已筛选：</span>
                <span
                    v-for="f in activeFilters"
                    :key="f.key"
                    class="active-filter-chip"
                >
                    {{ f.label }}
                    <button type="button" class="chip-remove" @click="f.onRemove">&times;</button>
                </span>
                <button type="button" class="chip-clear-all" @click="clearAllFilters">清空全部</button>
            </div>

            <p class="result-note">
                <template v-if="activeTab === 'articles'">共找到 {{ articleTotal }} 篇文章</template>
                <template v-else-if="activeTab === 'users'">共找到 {{ userTotal }} 位作者</template>
                <template v-else>共找到 {{ columnTotal }} 个专栏</template>
            </p>

            <AdBanner :slot-code="'search_top'" class="search-ad-banner" />
        </section>

        <!-- Article Results -->
        <ArticleFeed
            v-if="activeTab === 'articles'"
            hide-sort
            :articles="articles"
            :page="currentPage"
            :page-size="pageSize"
            :total="articleTotal"
            :loading="loading"
            :initial-loading="initialLoading"
            :refreshing="refreshing"
            :has-loaded-once="hasLoadedOnce"
            :error-message="errorMessage"
            :inline-error-message="inlineError"
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
            <div v-if="refreshing && users.length" class="refresh-state">正在更新作者结果...</div>
            <div v-if="inlineError" class="error-state search-state-panel">{{ inlineError }}</div>
            <div v-if="initialLoading && !users.length" class="loading-state">加载中...</div>
            <div v-else-if="errorMessage && !users.length" class="error-state">{{ errorMessage }}</div>
            <div v-else-if="!refreshing && hasLoadedOnce && users.length === 0" class="empty-state">暂无匹配作者</div>
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
                    <AuthorFollowButton
                        :user-id="user.id"
                        :followed="user.followed"
                        compact
                        @click.stop
                        @change="(followed) => handleFollowChange(user, followed)"
                    />
                </div>
            </div>
            <div v-if="userTotal > pageSize" class="pagination">
                <button
                    v-if="currentPage > 1"
                    type="button"
                    :disabled="loading"
                    @click="changePage(currentPage - 1)"
                >
                    上一页
                </button>
                <span class="page-info">{{ currentPage }} / {{ Math.ceil(userTotal / pageSize) }}</span>
                <button
                    v-if="currentPage * pageSize < userTotal"
                    type="button"
                    :disabled="loading"
                    @click="changePage(currentPage + 1)"
                >
                    下一页
                </button>
            </div>
        </div>

        <!-- Column Results -->
        <div v-else-if="activeTab === 'columns'" class="column-results">
            <div v-if="refreshing && columns.length" class="refresh-state">正在更新专栏结果...</div>
            <div v-if="inlineError" class="error-state search-state-panel">{{ inlineError }}</div>
            <div v-if="initialLoading && !columns.length" class="loading-state">加载中...</div>
            <div v-else-if="errorMessage && !columns.length" class="error-state">{{ errorMessage }}</div>
            <div v-else-if="!refreshing && hasLoadedOnce && columns.length === 0" class="empty-state">暂无匹配专栏</div>
            <div v-else class="column-list">
                <div
                    v-for="column in columns"
                    :key="column.id"
                    class="column-card"
                    @click="goToColumn(column.id)"
                >
                    <img :src="column.coverUrl || 'https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=300&q=80'" :alt="column.title" class="column-cover">
                    <div class="column-info">
                        <div class="column-name">{{ column.title }}</div>
                        <div class="column-summary">{{ column.summary || column.description || '' }}</div>
                        <div class="column-author" @click.stop="goToUser(column.author?.id)">
                            <img
                                v-if="column.author?.avatarUrl"
                                :src="column.author.avatarUrl"
                                :alt="column.author?.nickname"
                                class="column-author-avatar"
                            >
                            <span>{{ column.author?.nickname || '' }}</span>
                        </div>
                        <div class="column-stats">
                            <span>订阅 {{ column.subscriberCount || 0 }}</span>
                            <span>文章 {{ column.articleCount || 0 }}</span>
                        </div>
                    </div>
                    <ColumnSubscribeButton
                        :column-id="column.id"
                        :subscribed="column.subscribed"
                        compact
                        @click.stop
                        @change="(subscribed) => handleSubscribeChange(column, subscribed)"
                    />
                </div>
            </div>
            <div v-if="columnTotal > pageSize" class="pagination">
                <button
                    v-if="currentPage > 1"
                    type="button"
                    :disabled="loading"
                    @click="changePage(currentPage - 1)"
                >
                    上一页
                </button>
                <span class="page-info">{{ currentPage }} / {{ Math.ceil(columnTotal / pageSize) }}</span>
                <button
                    v-if="currentPage * pageSize < columnTotal"
                    type="button"
                    :disabled="loading"
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
    gap: 16px;
    background: transparent;
}

.search-panel .eyebrow {
    margin: 0;
}

.search-panel h1 {
    margin: 0;
}

.keyword-section {
    display: grid;
    gap: 12px;
    padding: 14px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
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
    min-height: 28px;
    padding: 0 10px;
    border-radius: var(--radius-sm);
    background: var(--surface);
    border: 1px solid var(--line);
    font-size: 13px;
    cursor: pointer;
    transition: color 0.12s, border-color 0.12s;
}

.keyword-chip:hover {
    color: var(--brand);
    border-color: var(--brand);
}

.keyword-chip.hot {
    background: #fff3e0;
    border-color: #ffb74d;
    color: #e65100;
}

.keyword-clear {
    min-height: 28px;
    padding: 0 10px;
    border-radius: var(--radius-sm);
    background: transparent;
    border: 1px solid var(--line);
    font-size: 13px;
    color: var(--muted);
    cursor: pointer;
}

.keyword-clear:hover {
    color: var(--error);
    border-color: var(--error);
}

.expand-btn {
    min-height: 28px;
    padding: 0 10px;
    border-radius: var(--radius-sm);
    background: transparent;
    border: 1px dashed var(--line);
    font-size: 13px;
    color: var(--muted);
    cursor: pointer;
}

.expand-btn:hover {
    color: var(--brand);
    border-color: var(--brand);
    border-style: solid;
}

.search-tabs {
    display: flex;
    gap: 8px;
    margin-top: 4px;
    padding-bottom: 4px;
    border-bottom: 1px solid var(--border);
}

.search-tabs button {
    position: relative;
    padding: 10px 4px;
    border: none;
    background: transparent;
    cursor: pointer;
    margin-right: 20px;
    color: var(--muted);
    font-size: 15px;
    font-weight: 600;
}

.search-tabs button.active {
    color: var(--brand-strong);
}

.search-tabs button.active::after {
    position: absolute;
    right: 0;
    bottom: -5px;
    left: 0;
    height: 2px;
    content: "";
    background: var(--brand);
    border-radius: 0;
}

.search-filters {
    display: grid;
    gap: 14px;
    padding: 16px;
    margin-top: 4px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
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
    min-height: 34px;
    padding: 0 10px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    font-size: 13px;
    width: 100%;
    max-width: 240px;
    background: var(--surface);
}

.filter-input:focus {
    outline: none;
    border-color: var(--primary);
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
    margin-top: 6px;
}

.search-panel :deep(.result-note) {
    margin-top: 0;
}

.user-results, .column-results {
    margin-top: 20px;
}

.loading-state {
    text-align: center;
    padding: 40px;
    color: var(--muted);
}

.refresh-state {
    margin-bottom: 12px;
    padding: 8px 12px;
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
    background: var(--brand-soft);
    border: 1px solid rgba(37, 99, 235, 0.14);
    border-radius: var(--radius-sm);
}

.search-state-panel {
    margin-top: 8px;
}

.user-list, .column-list {
    display: grid;
    gap: 16px;
}

.user-card, .column-card {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: 14px;
    padding: 14px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    cursor: pointer;
    background: var(--surface);
    box-shadow: none;
    transition: background 0.12s;
}

.user-card:hover, .column-card:hover {
    background: var(--surface-soft);
    border-color: var(--line-strong);
}

.user-avatar {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    object-fit: cover;
    box-shadow: none;
}

.column-cover {
    width: 96px;
    height: 68px;
    border-radius: var(--radius-sm);
    object-fit: cover;
    box-shadow: none;
}

.user-info, .column-info {
    display: grid;
    gap: 6px;
    min-width: 0;
}

.user-name, .column-name {
    color: var(--text-strong);
    font-weight: 700;
    font-size: 18px;
    line-height: 1.25;
}

.user-bio, .column-summary {
    font-size: 14px;
    color: var(--muted);
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.column-author {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--muted);
    cursor: pointer;
}

.column-author:hover {
    color: var(--brand-strong);
}

.column-author-avatar {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    object-fit: cover;
}

.user-stats, .column-stats {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    font-size: 12px;
    color: var(--muted);
}

.user-stats span, .column-stats span {
    display: inline-flex;
    align-items: center;
    min-height: 22px;
    padding: 0 8px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
    margin-top: 20px;
}

.pagination button {
    min-height: 30px;
    padding: 0 12px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    font-size: 13px;
    cursor: pointer;
    transition: color 0.12s, border-color 0.12s;
}

.pagination button:hover {
    border-color: var(--primary);
}

.page-info {
    font-size: 13px;
    color: var(--muted);
}

/* Mobile filter toggle */
.mobile-filter-toggle {
    display: none;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    min-height: 40px;
    padding: 0 14px;
    margin-top: 4px;
    color: var(--text);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.mobile-filter-toggle:hover {
    border-color: var(--primary);
}

.filter-toggle-arrow {
    font-size: 12px;
    transition: transform 0.18s ease;
}

.filter-toggle-arrow.open {
    transform: rotate(180deg);
}

@media (max-width: 720px) {
    .mobile-filter-toggle {
        display: flex;
    }

    .search-filters.collapsed {
        display: none;
    }

    .search-filters {
        padding: 14px;
        gap: 12px;
    }

    .filter-input {
        max-width: none;
        width: 100%;
    }

    .user-card, .column-card {
        grid-template-columns: auto minmax(0, 1fr);
    }

    .follow-btn, .subscribe-btn {
        grid-column: 1 / -1;
        width: fit-content;
    }
}

.active-filter-bar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.active-filter-label {
    font-size: 13px;
    color: var(--muted);
    white-space: nowrap;
}

.active-filter-chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 3px 8px;
    font-size: 13px;
    background: var(--brand-soft);
    color: var(--brand);
    border: 1px solid var(--brand);
    border-radius: var(--radius-sm);
    white-space: nowrap;
}

.chip-remove {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    padding: 0;
    border: none;
    background: transparent;
    color: var(--brand);
    font-size: 14px;
    line-height: 1;
    cursor: pointer;
    border-radius: var(--radius-sm);
}

.chip-remove:hover {
    background: var(--brand-hover);
}

.chip-clear-all {
    padding: 3px 8px;
    font-size: 13px;
    color: var(--muted);
    background: transparent;
    border: 1px dashed var(--line);
    border-radius: var(--radius-sm);
    cursor: pointer;
}

.chip-clear-all:hover {
    color: var(--error);
    border-color: var(--error);
}

.sort-row .sort-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}

.sort-row .sort-buttons button {
    min-height: 30px;
    padding: 0 10px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    font-size: 13px;
    cursor: pointer;
    color: var(--text);
    transition: color 0.12s, border-color 0.12s, background 0.12s;
}

.sort-row .sort-buttons button.active {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
    font-weight: 600;
}

.sort-row .sort-buttons button:hover:not(.active) {
    color: var(--brand);
    border-color: var(--brand);
}

.date-presets {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 6px;
}

.date-presets button {
    min-height: 26px;
    padding: 0 8px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    font-size: 12px;
    cursor: pointer;
    color: var(--text);
    transition: color 0.12s, border-color 0.12s, background 0.12s;
}

.date-presets button.active {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
    font-weight: 600;
}

.date-presets button:hover:not(.active) {
    color: var(--brand);
    border-color: var(--brand);
}

.filter-count-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    margin-left: 4px;
    font-size: 11px;
    font-weight: 700;
    color: #ffffff;
    background: var(--brand);
    border-radius: var(--radius-sm);
    line-height: 1;
}
</style>
