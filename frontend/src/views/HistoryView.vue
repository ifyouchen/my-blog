<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';

const RECENT_READING_KEY = 'my-blog:recent-reading';
const PAGE_SIZE = 8;
const DAY_MS = 86400000;
const QUICK_RANGES = [
    {value: 'all', label: '全部'},
    {value: 'today', label: '今天'},
    {value: 'week', label: '近7天'},
    {value: 'month', label: '近30天'}
];

const route = useRoute();
const router = useRouter();
const allItems = ref([]);
const keyword = ref('');
const dateFrom = ref('');
const dateTo = ref('');
const activeRange = ref('all');
const currentPage = ref(1);

const normalizeItems = (items) => (Array.isArray(items) ? items : [])
    .filter((item) => item?.id && item?.title)
    .map((item) => ({
        ...item,
        readAt: Number(item.readAt) || 0
    }))
    .sort((a, b) => b.readAt - a.readAt);

const persistHistory = (items) => {
    if (items.length) {
        localStorage.setItem(RECENT_READING_KEY, JSON.stringify(items));
        return;
    }
    localStorage.removeItem(RECENT_READING_KEY);
};

const loadHistory = () => {
    try {
        const cached = JSON.parse(localStorage.getItem(RECENT_READING_KEY) || '[]');
        allItems.value = normalizeItems(cached);
    } catch {
        localStorage.removeItem(RECENT_READING_KEY);
        allItems.value = [];
    }
};

const getQueryValue = (value) => (Array.isArray(value) ? value[0] : value) || '';

const getStartOfToday = () => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
};

const getRangeStart = (range) => {
    const today = getStartOfToday();
    if (range === 'today') return today;
    if (range === 'week') return today - 6 * DAY_MS;
    if (range === 'month') return today - 29 * DAY_MS;
    return 0;
};

const isValidRange = (range) => QUICK_RANGES.some((item) => item.value === range);

const invalidDateRange = computed(() => {
    if (!dateFrom.value || !dateTo.value) return false;
    return new Date(dateFrom.value).getTime() > new Date(dateTo.value).getTime();
});

const hasFilters = computed(() => Boolean(
    keyword.value.trim()
    || dateFrom.value
    || dateTo.value
    || activeRange.value !== 'all'
));

const latestItem = computed(() => allItems.value[0] || null);

const todayCount = computed(() => {
    const today = getStartOfToday();
    return allItems.value.filter((item) => item.readAt >= today).length;
});

const weekCount = computed(() => {
    const weekStart = getRangeStart('week');
    return allItems.value.filter((item) => item.readAt >= weekStart).length;
});

const filteredItems = computed(() => {
    if (invalidDateRange.value) {
        return [];
    }

    let items = allItems.value;
    const kw = keyword.value.trim().toLowerCase();
    if (kw) {
        items = items.filter((item) => [
            item.title,
            item.summary,
            item.authorName,
            item.category
        ].filter(Boolean).some((value) => String(value).toLowerCase().includes(kw)));
    }

    const quickFrom = getRangeStart(activeRange.value);
    if (quickFrom) {
        items = items.filter((item) => item.readAt >= quickFrom);
    }

    if (dateFrom.value) {
        const from = new Date(dateFrom.value).getTime();
        items = items.filter((item) => item.readAt >= from);
    }
    if (dateTo.value) {
        const to = new Date(dateTo.value).getTime() + DAY_MS;
        items = items.filter((item) => item.readAt < to);
    }
    return items;
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / PAGE_SIZE)));

const pagedItems = computed(() => {
    const start = (currentPage.value - 1) * PAGE_SIZE;
    return filteredItems.value.slice(start, start + PAGE_SIZE);
});

const groupedPagedItems = computed(() => {
    const groups = [];
    pagedItems.value.forEach((item) => {
        const label = formatDateGroup(item.readAt);
        let group = groups.find((entry) => entry.label === label);
        if (!group) {
            group = {label, items: []};
            groups.push(group);
        }
        group.items.push(item);
    });
    return groups;
});

const pageStart = computed(() => {
    if (!filteredItems.value.length) return 0;
    return (currentPage.value - 1) * PAGE_SIZE + 1;
});
const pageEnd = computed(() => Math.min(currentPage.value * PAGE_SIZE, filteredItems.value.length));

const resultSummary = computed(() => {
    if (invalidDateRange.value) return '结束日期需要晚于开始日期';
    if (!filteredItems.value.length) return allItems.value.length ? '没有符合条件的记录' : '暂无阅读记录';
    return `显示 ${pageStart.value}-${pageEnd.value}，共 ${filteredItems.value.length} 条`;
});

const syncRoute = () => {
    const query = {};
    if (keyword.value.trim()) query.keyword = keyword.value.trim();
    if (activeRange.value !== 'all') query.range = activeRange.value;
    if (dateFrom.value) query.from = dateFrom.value;
    if (dateTo.value) query.to = dateTo.value;
    if (currentPage.value > 1) query.page = String(currentPage.value);
    router.replace({query});
};

const applyRoute = () => {
    keyword.value = getQueryValue(route.query.keyword);
    dateFrom.value = getQueryValue(route.query.from);
    dateTo.value = getQueryValue(route.query.to);
    const range = getQueryValue(route.query.range) || 'all';
    activeRange.value = isValidRange(range) ? range : 'all';
    currentPage.value = Math.max(1, Number(getQueryValue(route.query.page)) || 1);
};

const doSearch = () => {
    currentPage.value = 1;
    syncRoute();
};

const resetFilters = () => {
    keyword.value = '';
    dateFrom.value = '';
    dateTo.value = '';
    activeRange.value = 'all';
    currentPage.value = 1;
    syncRoute();
};

const setQuickRange = (range) => {
    activeRange.value = range;
    dateFrom.value = '';
    dateTo.value = '';
    currentPage.value = 1;
    syncRoute();
};

const applyDateFilter = () => {
    activeRange.value = 'all';
    currentPage.value = 1;
    syncRoute();
};

const goToPage = (page) => {
    if (page < 1 || page > totalPages.value) return;
    currentPage.value = page;
    syncRoute();
};

const formatReadTime = (timestamp) => {
    if (!timestamp) return '';
    const d = new Date(timestamp);
    const pad = (n) => String(n).padStart(2, '0');
    const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    const today = getStartOfToday();
    const target = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
    const diffDays = Math.floor((today - target) / DAY_MS);
    if (diffDays === 0) return `今天 ${time}`;
    if (diffDays === 1) return `昨天 ${time}`;
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}`;
};

function formatDateGroup(timestamp) {
    if (!timestamp) return '更早';
    const d = new Date(timestamp);
    const today = getStartOfToday();
    const target = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
    const diffDays = Math.floor((today - target) / DAY_MS);
    if (diffDays === 0) return '今天';
    if (diffDays === 1) return '昨天';
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return `${d.getMonth() + 1}月${d.getDate()}日 ${weekdays[d.getDay()]}`;
}

const removeItem = (target) => {
    if (!confirm(`从阅读记录中移除《${target.title}》吗？`)) {
        return;
    }
    const next = allItems.value.filter((item) => !(
        String(item.id) === String(target.id)
        && Number(item.readAt) === Number(target.readAt)
    ));
    allItems.value = normalizeItems(next);
    persistHistory(allItems.value);
    if (currentPage.value > totalPages.value) {
        currentPage.value = totalPages.value;
    }
    syncRoute();
};

const clearAll = () => {
    if (confirm('确定清空所有阅读历史吗？此操作不可撤销。')) {
        localStorage.removeItem(RECENT_READING_KEY);
        allItems.value = [];
        currentPage.value = 1;
        syncRoute();
    }
};

onMounted(() => {
    loadHistory();
    applyRoute();
});

watch(() => route.query, () => {
    applyRoute();
});

watch(filteredItems, () => {
    if (currentPage.value > totalPages.value) {
        currentPage.value = totalPages.value;
        syncRoute();
    }
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell history-page" data-testid="history-page">
        <section class="history-head">
            <div class="history-title">
                <p class="eyebrow">阅读记录</p>
                <h1>最近读过</h1>
            </div>
            <div class="history-stats" aria-label="阅读记录统计">
                <span><strong>{{ allItems.length }}</strong>全部</span>
                <span><strong>{{ todayCount }}</strong>今天</span>
                <span><strong>{{ weekCount }}</strong>近7天</span>
            </div>
            <button v-if="allItems.length" type="button" class="history-clear-btn" @click="clearAll">清空</button>
        </section>

        <section v-if="latestItem" class="history-resume" aria-label="继续阅读">
            <RouterLink class="history-resume-cover" :to="`/articles/${latestItem.id}`">
                <img
                    v-if="latestItem.cover"
                    :src="latestItem.cover"
                    :alt="`${latestItem.title} 封面`"
                    loading="lazy"
                    decoding="async"
                >
                <span v-else>{{ latestItem.category || '文章' }}</span>
            </RouterLink>
            <div class="history-resume-body">
                <span class="history-resume-label">继续阅读</span>
                <RouterLink class="history-resume-title" :to="`/articles/${latestItem.id}`">
                    {{ latestItem.title }}
                </RouterLink>
                <p>{{ latestItem.summary || latestItem.category || latestItem.authorName || '最近打开的文章' }}</p>
                <div class="history-resume-meta">
                    <span v-if="latestItem.authorName">{{ latestItem.authorName }}</span>
                    <span>{{ formatReadTime(latestItem.readAt) }}</span>
                </div>
            </div>
            <RouterLink class="history-resume-action" :to="`/articles/${latestItem.id}`">继续</RouterLink>
        </section>

        <form class="history-tools" @submit.prevent="doSearch">
            <label class="history-search">
                <span>关键词</span>
                <input
                    v-model.trim="keyword"
                    type="search"
                    placeholder="标题、作者、分类"
                    @keydown.enter.prevent="doSearch"
                >
            </label>
            <div class="history-range" aria-label="时间范围">
                <button
                    v-for="range in QUICK_RANGES"
                    :key="range.value"
                    type="button"
                    :class="{ active: activeRange === range.value }"
                    @click="setQuickRange(range.value)"
                >
                    {{ range.label }}
                </button>
            </div>
            <label class="history-date">
                <span>开始</span>
                <input v-model="dateFrom" type="date" @change="applyDateFilter">
            </label>
            <label class="history-date">
                <span>结束</span>
                <input v-model="dateTo" type="date" @change="applyDateFilter">
            </label>
            <div class="history-actions">
                <button type="submit" class="primary">搜索</button>
                <button type="button" @click="resetFilters">重置</button>
            </div>
        </form>

        <header v-if="allItems.length" class="history-result-head">
            <strong>{{ resultSummary }}</strong>
            <span v-if="hasFilters">筛选中</span>
        </header>

        <div v-if="pagedItems.length" class="history-groups">
            <section v-for="group in groupedPagedItems" :key="group.label" class="history-group">
                <h2>{{ group.label }}</h2>
                <article
                    v-for="item in group.items"
                    :key="item.id + '-' + item.readAt"
                    class="history-item"
                >
                    <div class="history-item-link">
                        <RouterLink class="history-thumb-link" :to="`/articles/${item.id}`">
                            <img
                                v-if="item.cover"
                                :src="item.cover"
                                :alt="`${item.title} 封面`"
                                loading="lazy"
                                decoding="async"
                            >
                            <span v-else class="history-fallback">{{ item.category || '文章' }}</span>
                        </RouterLink>
                        <div class="history-item-body">
                            <RouterLink class="history-item-title" :to="`/articles/${item.id}`">{{ item.title }}</RouterLink>
                            <p v-if="item.summary">{{ item.summary }}</p>
                            <div class="history-item-meta">
                                <span v-if="item.authorName">{{ item.authorName }}</span>
                                <span v-if="item.category">{{ item.category }}</span>
                                <span>{{ formatReadTime(item.readAt) }}</span>
                            </div>
                        </div>
                    </div>
                    <div class="history-item-actions">
                        <RouterLink class="history-open" :to="`/articles/${item.id}`">打开</RouterLink>
                        <button
                            type="button"
                            class="history-remove"
                            :title="`移除 ${item.title}`"
                            @click="removeItem(item)"
                        >
                            移除
                        </button>
                    </div>
                </article>
            </section>
        </div>
        <section v-else class="history-empty">
            <strong>{{ resultSummary }}</strong>
            <p v-if="invalidDateRange">调整日期范围后再试一次。</p>
            <p v-else-if="allItems.length">换个关键词或时间范围看看。</p>
            <p v-else>读过的文章会自动出现在这里。</p>
            <RouterLink v-if="!allItems.length" to="/explore">去发现</RouterLink>
        </section>

        <nav v-if="totalPages > 1" class="history-pagination" aria-label="阅读记录分页">
            <button type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">上一页</button>
            <span>{{ currentPage }} / {{ totalPages }}</span>
            <button type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
        </nav>
    </main>
</template>

<style scoped src="@/styles/views/HistoryView.part-1.css"></style>
<style scoped src="@/styles/views/HistoryView.part-2.css"></style>
