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

<style scoped>
.history-page {
    display: grid;
    gap: 16px;
}

.history-head {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto auto;
    gap: 16px;
    align-items: end;
    padding: 20px 0 10px;
    border-bottom: 1px solid var(--line);
}

.history-title h1 {
    margin: 4px 0 0;
    color: var(--text-strong);
    font-size: 28px;
    line-height: 1.25;
}

.history-stats {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    justify-content: flex-end;
}

.history-stats span {
    display: inline-grid;
    min-width: 72px;
    gap: 2px;
    padding: 8px 10px;
    color: var(--muted);
    font-size: 12px;
    text-align: center;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.history-stats strong {
    color: var(--text-strong);
    font-size: 16px;
    line-height: 1;
}

.history-clear-btn,
.history-resume-action,
.history-actions button,
.history-pagination button,
.history-open,
.history-remove {
    min-height: 34px;
    padding: 0 14px;
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: border-color 0.12s, color 0.12s, background 0.12s;
}

.history-clear-btn,
.history-remove {
    color: var(--accent);
    border-color: rgba(220, 38, 38, 0.32);
}

.history-clear-btn:hover,
.history-remove:hover {
    color: #fff;
    background: var(--accent);
    border-color: var(--accent);
}

.history-resume {
    display: grid;
    grid-template-columns: 140px minmax(0, 1fr) auto;
    gap: 16px;
    align-items: center;
    padding: 14px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.history-resume-cover {
    display: block;
    aspect-ratio: 16 / 9;
    overflow: hidden;
    color: var(--brand);
    font-size: 13px;
    font-weight: 700;
    text-decoration: none;
    background: var(--brand-soft);
    border-radius: var(--radius-sm);
}

.history-resume-cover img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.history-resume-cover span {
    display: inline-flex;
    width: 100%;
    height: 100%;
    align-items: center;
    justify-content: center;
}

.history-resume-body {
    display: grid;
    gap: 5px;
    min-width: 0;
}

.history-resume-label {
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
}

.history-resume-title {
    overflow: hidden;
    color: var(--text-strong);
    font-size: 18px;
    font-weight: 800;
    line-height: 1.35;
    text-decoration: none;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.history-resume-title:hover {
    color: var(--brand);
}

.history-resume-body p {
    display: -webkit-box;
    margin: 0;
    overflow: hidden;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}

.history-resume-meta,
.history-item-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    color: var(--muted);
    font-size: 12px;
}

.history-resume-action {
    display: inline-flex;
    align-items: center;
    color: #fff;
    text-decoration: none;
    background: var(--brand);
    border-color: var(--brand);
}

.history-tools {
    display: grid;
    grid-template-columns: minmax(220px, 1fr) auto 136px 136px auto;
    gap: 10px;
    align-items: end;
    padding: 12px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.history-search,
.history-date {
    display: grid;
    gap: 5px;
}

.history-search span,
.history-date span {
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
}

.history-search input,
.history-date input {
    width: 100%;
    min-height: 34px;
    padding: 0 10px;
    color: var(--text);
    font-size: 14px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-sizing: border-box;
    outline: 0;
}

.history-search input:focus,
.history-date input:focus {
    border-color: var(--brand);
}

.history-range {
    display: flex;
    gap: 4px;
    padding: 3px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.history-range button {
    min-height: 28px;
    padding: 0 10px;
    color: var(--muted);
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    background: transparent;
    border: 0;
    border-radius: var(--radius-sm);
    cursor: pointer;
}

.history-range button.active {
    color: var(--brand-strong);
    background: var(--surface);
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}

.history-actions {
    display: flex;
    gap: 8px;
}

.history-actions .primary {
    color: #fff;
    background: var(--brand);
    border-color: var(--brand);
}

.history-actions button:hover:not(.primary),
.history-pagination button:hover:not(:disabled),
.history-resume-action:hover,
.history-open:hover {
    color: var(--brand);
    border-color: var(--brand);
}

.history-actions .primary:hover {
    filter: brightness(1.04);
}

.history-result-head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    align-items: center;
    color: var(--muted);
    font-size: 13px;
}

.history-result-head strong {
    color: var(--text);
    font-weight: 600;
}

.history-result-head span {
    color: var(--brand);
    font-weight: 700;
}

.history-groups {
    display: grid;
    gap: 16px;
}

.history-group {
    display: grid;
    gap: 8px;
}

.history-group h2 {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
    font-weight: 700;
}

.history-item {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 10px;
    align-items: center;
    padding: 10px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: border-color 0.15s, background 0.15s;
}

.history-item:hover {
    border-color: var(--brand-hover);
    background: var(--surface-soft);
}

.history-item-link {
    display: grid;
    grid-template-columns: 80px minmax(0, 1fr);
    gap: 12px;
    align-items: center;
    min-width: 0;
    color: inherit;
    user-select: text;
}

.history-thumb-link {
    display: block;
    width: 80px;
    height: 54px;
    overflow: hidden;
    color: var(--brand);
    text-decoration: none;
    border-radius: var(--radius-sm);
}

.history-thumb-link img,
.history-fallback {
    width: 80px;
    height: 54px;
    border-radius: var(--radius-sm);
}

.history-thumb-link img {
    object-fit: cover;
    transition: transform 0.2s ease;
}

.history-thumb-link:hover img {
    transform: scale(1.05);
}

.history-fallback {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
    background: var(--brand-soft);
}

.history-item-body {
    display: grid;
    gap: 4px;
    min-width: 0;
    user-select: text;
}

.history-item-title {
    display: block;
    overflow: hidden;
    color: var(--text);
    font-size: 15px;
    font-weight: 700;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-decoration: none;
    transition: color 0.12s;
}

.history-item-title:hover {
    color: var(--brand);
}

.history-item-body p {
    display: -webkit-box;
    margin: 0;
    overflow: hidden;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.5;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
}

.history-item-actions {
    display: flex;
    gap: 8px;
    align-items: center;
}

.history-open {
    display: inline-flex;
    align-items: center;
    color: var(--brand);
    text-decoration: none;
    border-color: rgba(37, 99, 235, 0.26);
}

.history-remove {
    min-width: 58px;
}

.history-empty {
    display: grid;
    justify-items: center;
    gap: 8px;
    padding: 42px 16px;
    color: var(--muted);
    text-align: center;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.history-empty strong {
    color: var(--text-strong);
    font-size: 16px;
}

.history-empty p {
    margin: 0;
    font-size: 14px;
}

.history-empty a {
    display: inline-flex;
    min-height: 34px;
    align-items: center;
    padding: 0 14px;
    color: #fff;
    font-size: 13px;
    font-weight: 700;
    text-decoration: none;
    background: var(--brand);
    border-radius: var(--radius-sm);
}

.history-pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    padding: 10px 0 16px;
}

.history-pagination button:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.history-pagination span {
    color: var(--muted);
    font-size: 13px;
}

@media (max-width: 1080px) {
    .history-tools {
        grid-template-columns: minmax(0, 1fr) 1fr 1fr;
    }

    .history-range,
    .history-actions {
        grid-column: span 2;
    }
}

@media (max-width: 760px) {
    .history-head {
        grid-template-columns: 1fr;
        align-items: start;
    }

    .history-stats {
        justify-content: stretch;
    }

    .history-stats span {
        flex: 1;
    }

    .history-resume {
        grid-template-columns: 96px minmax(0, 1fr);
    }

    .history-resume-action {
        grid-column: 1 / -1;
        justify-content: center;
    }

    .history-tools {
        grid-template-columns: 1fr;
    }

    .history-range,
    .history-actions {
        grid-column: auto;
    }

    .history-range {
        overflow-x: auto;
    }

    .history-actions button {
        flex: 1;
    }

    .history-item {
        grid-template-columns: 1fr;
    }

    .history-item-link {
        grid-template-columns: 64px minmax(0, 1fr);
    }

    .history-thumb-link,
    .history-thumb-link img,
    .history-fallback {
        width: 64px;
        height: 46px;
    }

    .history-item-actions {
        justify-content: stretch;
    }

    .history-open,
    .history-remove {
        flex: 1;
        justify-content: center;
    }
}
</style>
