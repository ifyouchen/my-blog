<script setup>
import {computed, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import EmptyState from '@/components/EmptyState.vue';
import {getNotificationsApi, markNotificationReadApi} from '@/api/notifications';

const route = useRoute();
const router = useRouter();

const validFilters = ['all', 'unread', 'read'];
const pageSize = 10;

const notifications = ref([]);
const total = ref(0);
const currentFilter = ref(validFilters.includes(route.query.filter) ? route.query.filter : 'all');
const currentPage = ref(Number.parseInt(route.query.page || '1', 10) || 1);
const initialLoading = ref(true);
const refreshing = ref(false);
const errorMessage = ref('');
const actionError = ref('');
let hasLoadedOnce = false;

const syncRoute = (overrides = {}) => {
    const nextFilter = overrides.filter ?? currentFilter.value;
    const nextPage = Number(overrides.page ?? currentPage.value) || 1;
    router.replace({
        path: '/notifications',
        query: {
            filter: nextFilter === 'all' ? undefined : nextFilter,
            page: nextPage === 1 ? undefined : String(nextPage)
        }
    });
};

const getEmptyText = () => {
    if (currentFilter.value === 'unread') {
        return '当前没有未读通知';
    }
    if (currentFilter.value === 'read') {
        return '暂无已读通知';
    }
    return '暂无通知';
};

const getNotificationText = (type) => {
    const textMap = {
        ARTICLE_LIKE: '点赞了你的文章',
        ARTICLE_FAVORITE: '收藏了你的文章',
        ARTICLE_COMMENT: '评论了你的文章',
        COMMENT_REPLY: '回复了你的评论',
        COMMENT_LIKE: '点赞了你的评论',
        USER_FOLLOW: '关注了你'
    };
    return textMap[type] || '有一条新通知';
};

const getNotificationDetail = (notification) => {
    const payload = notification.payload || {};
    if (payload.articleTitle) {
        return payload.articleTitle;
    }
    if (payload.commentExcerpt) {
        return payload.commentExcerpt;
    }
    return '点击查看详情';
};

const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    const now = new Date();
    const diff = now - date;
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
    if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`;
    return date.toLocaleDateString();
};

const totalPages = computed(() => (total.value > 0 ? Math.ceil(total.value / pageSize) : 1));

const visiblePages = computed(() => {
    const pages = [];
    const totalCount = totalPages.value;
    const current = currentPage.value;

    if (totalCount <= 5) {
        for (let i = 1; i <= totalCount; i++) {
            pages.push(i);
        }
        return pages;
    }

    let start = Math.max(1, current - 2);
    let end = Math.min(totalCount, start + 4);
    if (end - start < 4) {
        start = Math.max(1, end - 4);
    }
    for (let i = start; i <= end; i++) {
        pages.push(i);
    }
    return pages;
});

const fetchNotifications = async ({ silent = false } = {}) => {
    actionError.value = '';
    errorMessage.value = '';
    if (!hasLoadedOnce) {
        initialLoading.value = true;
    } else if (silent) {
        refreshing.value = true;
    }

    try {
        const pageResult = await getNotificationsApi(currentPage.value, pageSize, currentFilter.value);
        const nextItems = Array.isArray(pageResult?.items) ? pageResult.items : [];
        const nextTotal = Number(pageResult?.total || 0);
        const resolvedPages = nextTotal > 0 ? Math.ceil(nextTotal / pageSize) : 1;

        if (nextTotal === 0 && currentPage.value !== 1) {
            currentPage.value = 1;
            syncRoute({ page: 1 });
            return;
        }

        if (nextTotal > 0 && currentPage.value > resolvedPages) {
            currentPage.value = resolvedPages;
            syncRoute({ page: resolvedPages });
            return;
        }

        notifications.value = nextItems;
        total.value = nextTotal;
    } catch (error) {
        if (!hasLoadedOnce) {
            notifications.value = [];
            total.value = 0;
            errorMessage.value = error.message || '加载通知失败，请稍后重试';
        } else {
            actionError.value = error.message || '刷新通知失败，请稍后重试';
        }
    } finally {
        initialLoading.value = false;
        refreshing.value = false;
        hasLoadedOnce = true;
    }
};

const changeFilter = (filter) => {
    if (filter === currentFilter.value || refreshing.value) {
        return;
    }
    currentFilter.value = filter;
    currentPage.value = 1;
    syncRoute({ filter, page: 1 });
};

const changePage = (page) => {
    if (
        page === currentPage.value
        || page < 1
        || page > totalPages.value
        || refreshing.value
    ) {
        return;
    }
    currentPage.value = page;
    syncRoute({ page });
};

const normalizeAfterUnreadMutation = async () => {
    if (currentFilter.value !== 'unread') {
        return;
    }
    if (total.value === 0) {
        currentPage.value = 1;
        if (route.query.page) {
            syncRoute({ page: 1 });
        }
        return;
    }
    const resolvedPages = Math.ceil(total.value / pageSize);
    if (currentPage.value > resolvedPages) {
        currentPage.value = resolvedPages;
        syncRoute({ page: resolvedPages });
        return;
    }
    if (notifications.value.length === 0) {
        await fetchNotifications({ silent: true });
    }
};

const handleNotificationClick = async (notification) => {
    actionError.value = '';
    if (!notification.read) {
        try {
            await markNotificationReadApi(notification.id);
            notification.read = true;
            if (currentFilter.value === 'unread') {
                notifications.value = notifications.value.filter((item) => item.id !== notification.id);
                total.value = Math.max(0, total.value - 1);
                await normalizeAfterUnreadMutation();
            }
            window.dispatchEvent(new CustomEvent('notifications:refresh'));
        } catch (error) {
            actionError.value = error.message || '标记通知已读失败，请稍后重试';
            return;
        }
    }
    router.push(notification.targetUrl);
};

watch(
    () => route.query,
    (query) => {
        const nextFilter = validFilters.includes(query.filter) ? query.filter : 'all';
        const nextPage = Number.parseInt(query.page || '1', 10) || 1;
        currentFilter.value = nextFilter;
        currentPage.value = nextPage;
        fetchNotifications({ silent: hasLoadedOnce });
    },
    { immediate: true }
);
</script>

<template>
    <SiteHeader />
    <main class="page-shell notifications-shell">
        <section class="notifications-page">
            <header class="notifications-hero">
                <div class="hero-copy">
                    <span class="hero-kicker">消息中心</span>
                    <h1>通知中心</h1>
                    <p>查看互动、回复与关注动态，快速回到你关心的内容现场。</p>
                </div>
                <div class="hero-actions">
                    <div class="filter-tabs" role="tablist" aria-label="通知筛选">
                        <button
                            :class="{ active: currentFilter === 'all' }"
                            type="button"
                            :disabled="refreshing"
                            @click="changeFilter('all')"
                        >
                            全部
                        </button>
                        <button
                            :class="{ active: currentFilter === 'unread' }"
                            type="button"
                            :disabled="refreshing"
                            @click="changeFilter('unread')"
                        >
                            未读
                        </button>
                        <button
                            :class="{ active: currentFilter === 'read' }"
                            type="button"
                            :disabled="refreshing"
                            @click="changeFilter('read')"
                        >
                            已读
                        </button>
                    </div>
                </div>
            </header>

            <section class="notifications-panel">
                <div v-if="refreshing" class="panel-status panel-status-refreshing">刷新中...</div>
                <div v-else-if="actionError" class="panel-status panel-status-error">{{ actionError }}</div>

                <div v-if="initialLoading" class="notifications-skeleton" aria-hidden="true">
                    <div v-for="item in 4" :key="item" class="skeleton-item">
                        <div class="skeleton-avatar"></div>
                        <div class="skeleton-lines">
                            <span class="skeleton-line line-lg"></span>
                            <span class="skeleton-line line-md"></span>
                            <span class="skeleton-line line-sm"></span>
                        </div>
                    </div>
                </div>

                <EmptyState
                    v-else-if="errorMessage"
                    eyebrow="通知异常"
                    title="暂时无法加载通知"
                    :description="errorMessage"
                    tone="error"
                    compact
                    class="notifications-state"
                />

                <EmptyState
                    v-else-if="notifications.length === 0"
                    :eyebrow="currentFilter === 'unread' ? '未读动态' : currentFilter === 'read' ? '已读动态' : '消息中心'"
                    :title="getEmptyText()"
                    :description="currentFilter === 'unread'
                        ? '当你收到新的点赞、评论、回复或关注时，会优先显示在这里。'
                        : currentFilter === 'read'
                            ? '新的互动一旦处理完成，就会沉淀在已读列表中。'
                            : '去首页逛逛，等待新的互动回到消息中心。'"
                    compact
                    class="notifications-state"
                />

                <div
                    v-else
                    class="notifications-list"
                    :class="{ 'is-refreshing': refreshing }"
                >
                    <article
                        v-for="notification in notifications"
                        :key="notification.id"
                        class="notification-item"
                        :class="{ unread: !notification.read }"
                        @click="handleNotificationClick(notification)"
                    >
                        <img
                            v-if="notification.actor?.avatarUrl"
                            :src="notification.actor.avatarUrl"
                            class="notification-avatar"
                            alt=""
                            loading="lazy"
                        >
                        <img
                            v-else
                            src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80"
                            class="notification-avatar"
                            alt=""
                            loading="lazy"
                        >
                        <div class="notification-content">
                            <div class="notification-text">
                                <span class="notification-actor">
                                    {{ notification.actor?.nickname || notification.actor?.username }}
                                </span>
                                <span class="notification-action">{{ getNotificationText(notification.type) }}</span>
                            </div>
                            <div class="notification-detail">
                                {{ getNotificationDetail(notification) }}
                            </div>
                            <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
                        </div>
                        <div class="notification-meta">
                            <span v-if="!notification.read" class="unread-dot"></span>
                            <span class="notification-arrow">›</span>
                        </div>
                    </article>
                </div>

                <footer v-if="total > 0 && totalPages > 1" class="pagination-bar">
                    <p>共 {{ total }} 条通知，当前 {{ currentPage }} / {{ totalPages }} 页</p>
                    <div class="pagination-actions">
                        <button
                            type="button"
                            :disabled="currentPage <= 1 || refreshing"
                            @click="changePage(1)"
                        >
                            首页
                        </button>
                        <button
                            type="button"
                            :disabled="currentPage <= 1 || refreshing"
                            @click="changePage(currentPage - 1)"
                        >
                            上一页
                        </button>
                        <button
                            v-for="page in visiblePages"
                            :key="page"
                            type="button"
                            :class="{ active: page === currentPage }"
                            :disabled="refreshing"
                            @click="changePage(page)"
                        >
                            {{ page }}
                        </button>
                        <button
                            type="button"
                            :disabled="currentPage >= totalPages || refreshing"
                            @click="changePage(currentPage + 1)"
                        >
                            下一页
                        </button>
                        <button
                            type="button"
                            :disabled="currentPage >= totalPages || refreshing"
                            @click="changePage(totalPages)"
                        >
                            末页
                        </button>
                    </div>
                </footer>
            </section>
        </section>
    </main>
</template>

<style scoped>
.notifications-shell {
    padding-top: 28px;
    padding-bottom: 36px;
}

.notifications-page {
    display: grid;
    gap: 20px;
    max-width: 1320px;
    margin: 0 auto;
}

.notifications-hero {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
    padding: 20px 24px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
}

.hero-copy {
    display: grid;
    gap: 8px;
    max-width: 760px;
}

.hero-kicker {
    color: var(--brand-strong);
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0.08em;
}

.hero-copy h1 {
    margin: 0;
    color: #111827;
    font-size: 34px;
    line-height: 1.12;
}

.hero-copy p {
    margin: 0;
    color: #6b7280;
    font-size: 15px;
    line-height: 1.7;
}

.hero-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
    justify-content: flex-end;
}

.filter-tabs {
    display: flex;
    gap: 4px;
    padding: 3px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
}

.filter-tabs button {
    min-height: 30px;
    padding: 0 12px;
    color: var(--muted);
    cursor: pointer;
    background: transparent;
    border: 0;
    border-radius: var(--radius-sm);
    font-size: 13px;
    font-weight: 600;
    transition: background 0.12s, color 0.12s;
}

.filter-tabs button:hover:not(:disabled) {
    color: var(--text);
    background: var(--surface);
}

.filter-tabs button.active {
    color: #ffffff;
    background: var(--brand);
    box-shadow: none;
}

.filter-tabs button:disabled {
    cursor: not-allowed;
    opacity: 0.56;
}

.notifications-panel {
    overflow: hidden;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
}

.panel-status {
    padding: 10px 18px;
    font-size: 13px;
    font-weight: 600;
}

.panel-status-refreshing {
    color: var(--brand);
    background: var(--brand-soft);
    border-bottom: 1px solid var(--line);
}

.panel-status-error {
    color: var(--accent);
    background: #fff0f0;
    border-bottom: 1px solid var(--line);
}

.notifications-skeleton {
    display: grid;
    gap: 0;
    background: var(--line);
}

.skeleton-item {
    display: grid;
    grid-template-columns: 52px minmax(0, 1fr);
    gap: 16px;
    align-items: center;
    padding: 22px 24px;
    background: var(--surface);
}

.skeleton-avatar {
    width: 44px;
    height: 44px;
    background: linear-gradient(90deg, var(--surface-muted) 25%, var(--surface-soft) 50%, var(--surface-muted) 75%);
    background-size: 200% 100%;
    border-radius: 50%;
    animation: shimmer 1.4s ease-in-out infinite;
}

.skeleton-lines {
    display: grid;
    gap: 10px;
}

.skeleton-line {
    display: block;
    height: 12px;
    background: linear-gradient(90deg, var(--surface-muted) 25%, var(--surface-soft) 50%, var(--surface-muted) 75%);
    background-size: 200% 100%;
    border-radius: var(--radius-sm);
    animation: shimmer 1.4s ease-in-out infinite;
}

.line-lg {
    width: min(72%, 460px);
}

.line-md {
    width: min(54%, 320px);
}

.line-sm {
    width: 120px;
}

.notifications-state {
    min-height: 280px;
    margin: 0;
    border-radius: 0;
    border: 0;
    box-shadow: none;
}

.notifications-list {
    position: relative;
    display: grid;
    gap: 0;
    background: var(--line);
}

.notification-item {
    display: grid;
    grid-template-columns: 44px minmax(0, 1fr) auto;
    gap: 14px;
    align-items: center;
    padding: 14px 20px;
    cursor: pointer;
    background: var(--surface);
    transition: background 0.12s;
}

.notification-item:hover {
    background: var(--surface-soft);
}

.notification-item.unread {
    background: var(--brand-soft);
    border-left: 3px solid var(--brand);
}

.notification-avatar {
    width: 44px;
    height: 44px;
    object-fit: cover;
    border-radius: 50%;
    box-shadow: none;
}

.notification-content {
    display: grid;
    gap: 6px;
    min-width: 0;
}

.notification-text {
    color: #111827;
    font-size: 16px;
    line-height: 1.55;
}

.notification-actor {
    margin-right: 4px;
    font-weight: 700;
}

.notification-action {
    color: #374151;
}

.notification-detail {
    overflow: hidden;
    color: #6b7280;
    font-size: 14px;
    line-height: 1.6;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.notification-time {
    color: #9ca3af;
    font-size: 13px;
}

.notification-meta {
    display: flex;
    gap: 10px;
    align-items: center;
}

.unread-dot {
    width: 8px;
    height: 8px;
    background: var(--brand);
    border-radius: 50%;
    box-shadow: none;
}

.notification-arrow {
    color: #cbd5e1;
    font-size: 26px;
    line-height: 1;
}

.pagination-bar {
    display: grid;
    gap: 14px;
    padding: 16px 20px 20px;
    background: var(--surface);
    border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.pagination-bar p {
    margin: 0;
    color: #6b7280;
    font-size: 14px;
}

.pagination-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.pagination-actions button {
    min-width: 32px;
    min-height: 30px;
    padding: 0 8px;
    color: var(--text);
    font-size: 13px;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: color 0.12s, border-color 0.12s, background 0.12s;
}

.pagination-actions button:hover:not(:disabled) {
    color: var(--brand);
    border-color: var(--brand);
}

.pagination-actions button.active {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.pagination-actions button:disabled {
    cursor: not-allowed;
    opacity: 0.52;
}

@keyframes shimmer {
    0% {
        background-position: -180px 0;
    }
    100% {
        background-position: 180px 0;
    }
}

@media (max-width: 900px) {
    .notifications-shell {
        padding-top: 20px;
    }

    .notifications-hero,
    .notifications-panel {
        border-radius: var(--radius-sm);
    }

    .notifications-hero {
        padding: 16px;
    }

    .hero-copy h1 {
        font-size: 28px;
    }

    .notification-item,
    .skeleton-item {
        grid-template-columns: 46px minmax(0, 1fr);
        padding: 18px 18px;
    }

    .notification-meta {
        justify-content: flex-end;
    }

    .notification-arrow {
        display: none;
    }
}

@media (max-width: 640px) {
    .notifications-page {
        gap: 16px;
    }

    .hero-actions {
        width: 100%;
        justify-content: flex-start;
    }

    .filter-tabs {
        overflow-x: auto;
    }

    .notification-item {
        gap: 12px;
    }

    .notification-detail {
        white-space: normal;
    }

    .pagination-bar {
        padding: 14px 16px 18px;
    }
}
</style>
