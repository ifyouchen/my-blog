<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';

const RECENT_READING_KEY = 'my-blog:recent-reading';
const PAGE_SIZE = 10;

const route = useRoute();
const router = useRouter();
const allItems = ref([]);
const keyword = ref('');
const dateFrom = ref('');
const dateTo = ref('');
const currentPage = ref(1);

const loadHistory = () => {
    try {
        const cached = JSON.parse(localStorage.getItem(RECENT_READING_KEY) || '[]');
        allItems.value = Array.isArray(cached) ? cached.filter((item) => item?.id && item?.title) : [];
    } catch {
        localStorage.removeItem(RECENT_READING_KEY);
        allItems.value = [];
    }
};

const filteredItems = computed(() => {
    let items = allItems.value;
    const kw = keyword.value.trim().toLowerCase();
    if (kw) {
        items = items.filter((item) => item.title.toLowerCase().includes(kw));
    }
    if (dateFrom.value) {
        const from = new Date(dateFrom.value).getTime();
        items = items.filter((item) => item.readAt >= from);
    }
    if (dateTo.value) {
        const to = new Date(dateTo.value).getTime() + 86400000;
        items = items.filter((item) => item.readAt < to);
    }
    return items;
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / PAGE_SIZE)));

const pagedItems = computed(() => {
    const start = (currentPage.value - 1) * PAGE_SIZE;
    return filteredItems.value.slice(start, start + PAGE_SIZE);
});

const pageStart = computed(() => {
    if (!filteredItems.value.length) return 0;
    return (currentPage.value - 1) * PAGE_SIZE + 1;
});
const pageEnd = computed(() => Math.min(currentPage.value * PAGE_SIZE, filteredItems.value.length));

const syncRoute = () => {
    const query = {};
    if (keyword.value.trim()) query.keyword = keyword.value.trim();
    if (dateFrom.value) query.from = dateFrom.value;
    if (dateTo.value) query.to = dateTo.value;
    if (currentPage.value > 1) query.page = String(currentPage.value);
    router.replace({ query });
};

const applyRoute = () => {
    keyword.value = route.query.keyword || '';
    dateFrom.value = route.query.from || '';
    dateTo.value = route.query.to || '';
    currentPage.value = Math.max(1, Number(route.query.page) || 1);
};

const doSearch = () => {
    currentPage.value = 1;
    syncRoute();
};

const resetFilters = () => {
    keyword.value = '';
    dateFrom.value = '';
    dateTo.value = '';
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
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const target = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    const diffDays = Math.floor((today - target) / 86400000);
    if (diffDays === 0) return `今天 ${time}`;
    if (diffDays === 1) return `昨天 ${time}`;
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}`;
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
</script>

<template>
    <SiteHeader />
    <main class="page-shell history-page" data-testid="history-page">
        <section class="history-hero">
            <div>
                <p class="eyebrow">历史阅读</p>
                <h1>阅读记录</h1>
                <p class="history-hero-desc">回顾你读过的所有文章，按时间或标题筛选查找。</p>
            </div>
            <button v-if="allItems.length" type="button" class="history-clear-btn" @click="clearAll">清空记录</button>
        </section>

        <form class="history-filters" @submit.prevent="doSearch">
            <label class="history-filter-grow">
                <span>文章标题</span>
                <input v-model.trim="keyword" type="text" placeholder="搜索文章标题..." @keydown.enter.prevent="doSearch">
            </label>
            <label>
                <span>开始日期</span>
                <input v-model="dateFrom" type="date">
            </label>
            <label>
                <span>结束日期</span>
                <input v-model="dateTo" type="date">
            </label>
            <div class="admin-filter-actions">
                <button type="submit">查询</button>
                <button type="button" @click="resetFilters">重置</button>
            </div>
        </form>

        <div v-if="filteredItems.length" class="history-summary">
            共 {{ filteredItems.length }} 条记录，当前显示 {{ pageStart }}-{{ pageEnd }}
        </div>

        <div v-if="pagedItems.length" class="history-list">
            <RouterLink
                v-for="item in pagedItems"
                :key="item.id + '-' + item.readAt"
                class="history-item"
                :to="`/articles/${item.id}`"
            >
                <img
                    v-if="item.cover"
                    :src="item.cover"
                    :alt="`${item.title} 封面`"
                    loading="lazy"
                    decoding="async"
                >
                <span v-else class="history-fallback">{{ item.category || '文章' }}</span>
                <div class="history-item-body">
                    <strong>{{ item.title }}</strong>
                    <div class="history-item-meta">
                        <span v-if="item.authorName">{{ item.authorName }}</span>
                        <span v-if="item.category">{{ item.category }}</span>
                        <span>{{ formatReadTime(item.readAt) }}</span>
                    </div>
                </div>
            </RouterLink>
        </div>
        <p v-else-if="allItems.length" class="history-empty">没有符合条件的阅读记录</p>
        <p v-else class="history-empty">暂无阅读记录，去发现页面看看吧</p>

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
    gap: 20px;
}

.history-hero {
    display: flex;
    gap: 20px;
    align-items: flex-end;
    justify-content: space-between;
    padding: 30px 0 8px;
    border-bottom: 1px solid var(--line);
}

.history-hero h1 {
    margin: 4px 0 0;
    color: var(--text-strong);
    font-size: 34px;
    line-height: 1.18;
}

.history-hero-desc {
    max-width: 680px;
    margin: 10px 0 0;
    color: var(--muted);
    font-size: 15px;
    line-height: 1.8;
}

.history-clear-btn {
    min-height: 36px;
    padding: 0 14px;
    color: var(--accent);
    font-size: 13px;
    font-weight: 700;
    background: var(--surface);
    border: 1px solid var(--accent);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: background 0.12s, color 0.12s;
}

.history-clear-btn:hover {
    color: #fff;
    background: var(--accent);
}

.history-filters {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: flex-end;
}

.history-filters label {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.history-filters label span {
    font-size: 12px;
    font-weight: 600;
    color: var(--muted);
}

.history-filters input {
    min-height: 32px;
    padding: 0 10px;
    font-size: 14px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    color: var(--text);
}

.history-filter-grow {
    flex: 1;
    min-width: 180px;
}

.history-summary {
    color: var(--muted);
    font-size: 13px;
}

.history-list {
    display: grid;
    gap: 10px;
}

.history-item {
    display: grid;
    grid-template-columns: 48px minmax(0, 1fr);
    gap: 12px;
    align-items: center;
    min-height: 66px;
    padding: 10px 12px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    text-decoration: none;
    transition: border-color 0.15s, background 0.15s;
}

.history-item:hover {
    border-color: var(--brand-hover);
    background: var(--surface-soft);
}

.history-item img {
    width: 48px;
    height: 48px;
    object-fit: cover;
    border-radius: var(--radius-sm);
}

.history-fallback {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
    background: var(--brand-soft);
    border-radius: var(--radius-sm);
}

.history-item-body {
    display: grid;
    gap: 4px;
    min-width: 0;
}

.history-item-body strong {
    overflow: hidden;
    color: var(--text);
    font-size: 15px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.history-item-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    color: var(--muted);
    font-size: 12px;
}

.history-empty {
    margin: 0;
    padding: 40px 0;
    color: var(--muted);
    font-size: 14px;
    text-align: center;
}

.history-pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    padding: 16px 0;
}

.history-pagination button {
    min-height: 32px;
    padding: 0 14px;
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: border-color 0.12s, color 0.12s;
}

.history-pagination button:hover:not(:disabled) {
    color: var(--brand);
    border-color: var(--brand);
}

.history-pagination button:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.history-pagination span {
    color: var(--muted);
    font-size: 13px;
}

@media (max-width: 640px) {
    .history-hero {
        display: grid;
        align-items: start;
    }

    .history-filters {
        flex-direction: column;
    }

    .history-filter-grow {
        min-width: 0;
    }

    .history-filters label {
        width: 100%;
    }

    .history-filters input {
        width: 100%;
        box-sizing: border-box;
    }

    .admin-filter-actions {
        display: flex;
        gap: 8px;
        width: 100%;
    }

    .admin-filter-actions button {
        flex: 1;
    }
}
</style>
