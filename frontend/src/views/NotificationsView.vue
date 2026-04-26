<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import {
    getNotificationsApi,
    markNotificationReadApi,
    markAllNotificationsReadApi
} from '@/api/notifications';

const route = useRoute();
const router = useRouter();

const currentFilter = ref(route.query.filter || 'all');
const validFilters = ['all', 'unread', 'read'];
if (!validFilters.includes(currentFilter.value)) {
    currentFilter.value = 'all';
}
const currentPage = ref(Number.parseInt(route.query.page || '1', 10) || 1);
const pageSize = 10;
const notifications = ref([]);
const total = ref(0);
const initialLoading = ref(true);
const errorMessage = ref('');
let isInitialLoad = true;

const syncRoute = (overrides = {}) => {
    router.replace({
        path: '/notifications',
        query: {
            filter: overrides.filter ?? currentFilter.value,
            page: String(overrides.page ?? currentPage.value) === '1' ? undefined : String(overrides.page ?? currentPage.value)
        }
    });
};

const fetchNotifications = async () => {
    errorMessage.value = '';
    if (!isInitialLoad) {
        initialLoading.value = true;
        notifications.value = [];
    }
    try {
        const pageResult = await getNotificationsApi(currentPage.value, pageSize, currentFilter.value);
        notifications.value = (pageResult && pageResult.items) ? pageResult.items : [];
        total.value = (pageResult && pageResult.total) ? pageResult.total : 0;
    } catch (error) {
        notifications.value = [];
        total.value = 0;
        errorMessage.value = error.message || '加载通知失败，请稍后重试';
    } finally {
        initialLoading.value = false;
        isInitialLoad = false;
    }
};

const changeFilter = (filter) => {
    currentFilter.value = filter;
    currentPage.value = 1;
    syncRoute({ filter, page: 1 });
};

const changePage = (page) => {
    currentPage.value = page;
    syncRoute({ page });
};

const handleNotificationClick = async (notification) => {
    if (!notification.read) {
        try {
            await markNotificationReadApi(notification.id);
            notification.read = true;
            if (currentFilter.value === 'unread') {
                const index = notifications.value.findIndex(n => n.id === notification.id);
                if (index !== -1) {
                    notifications.value.splice(index, 1);
                }
                total.value = Math.max(0, total.value - 1);
            }
            window.dispatchEvent(new CustomEvent('notifications:refresh'));
        } catch (e) {
            console.error('Failed to mark notification as read:', e);
        }
    }
    router.push(notification.targetUrl);
};

const handleMarkAllRead = async () => {
    try {
        await markAllNotificationsReadApi();
        if (currentFilter.value === 'unread') {
            notifications.value = [];
            total.value = 0;
        } else {
            notifications.value = notifications.value.map(n => ({ ...n, read: true }));
        }
        window.dispatchEvent(new CustomEvent('notifications:refresh'));
    } catch (e) {
        console.error('Failed to mark all as read:', e);
    }
};

const getNotificationText = (type) => {
    const textMap = {
        'ARTICLE_LIKE': '点赞了你的文章',
        'ARTICLE_FAVORITE': '收藏了你的文章',
        'ARTICLE_COMMENT': '评论了你的文章',
        'COMMENT_REPLY': '回复了你的评论',
        'COMMENT_LIKE': '点赞了你的评论',
        'USER_FOLLOW': '关注了你'
    };
    return textMap[type] || '有一条新通知';
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

const totalPages = computed(() => Math.ceil(total.value / pageSize));

const visiblePages = computed(() => {
    const pages = [];
    const total = totalPages.value;
    const current = currentPage.value;

    if (total <= 5) {
        for (let i = 1; i <= total; i++) {
            pages.push(i);
        }
    } else {
        let start = Math.max(1, current - 2);
        let end = Math.min(total, start + 4);
        if (end - start < 4) {
            start = Math.max(1, end - 4);
        }
        for (let i = start; i <= end; i++) {
            pages.push(i);
        }
    }
    return pages;
});

watch(
    () => route.query,
    (query) => {
        currentFilter.value = query.filter || 'all';
        currentPage.value = Number.parseInt(query.page || '1', 10) || 1;
        fetchNotifications();
    },
    { immediate: true }
);
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <div class="notifications-page">
            <div class="notifications-header">
                <h1>通知中心</h1>
                <div class="filter-tabs">
                    <button
                        :class="{ active: currentFilter === 'all' }"
                        type="button"
                        @click="changeFilter('all')"
                    >
                        全部
                    </button>
                    <button
                        :class="{ active: currentFilter === 'unread' }"
                        type="button"
                        @click="changeFilter('unread')"
                    >
                        未读
                    </button>
                    <button
                        :class="{ active: currentFilter === 'read' }"
                        type="button"
                        @click="changeFilter('read')"
                    >
                        已读
                    </button>
                </div>
                <button
                    v-if="currentFilter === 'unread' && total > 0"
                    class="mark-all-btn"
                    type="button"
                    @click="handleMarkAllRead"
                >
                    全部标记为已读
                </button>
            </div>

            <div v-if="initialLoading" class="notifications-loading">
                <p>加载中...</p>
            </div>

            <div v-else-if="errorMessage" class="notifications-error">
                <p>{{ errorMessage }}</p>
            </div>

            <div v-else-if="notifications.length === 0" class="notifications-empty">
                <p>暂无通知</p>
            </div>

            <div v-else class="notifications-list">
                <div
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
                    >
                    <img
                        v-else
                        src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80"
                        class="notification-avatar"
                        alt=""
                    >
                    <div class="notification-content">
                        <div class="notification-text">
                            <span class="notification-actor">{{ notification.actor?.nickname || notification.actor?.username }}</span>
                            {{ getNotificationText(notification.type) }}
                        </div>
                        <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
                    </div>
                    <span v-if="!notification.read" class="unread-dot"></span>
                </div>
            </div>

            <div v-if="totalPages > 1" class="pagination-bar">
                <p>共 {{ total }} 条通知，当前 {{ currentPage }} / {{ totalPages }} 页</p>
                <div class="pagination-actions">
                    <button
                        type="button"
                        :disabled="currentPage <= 1"
                        @click="changePage(1)"
                    >
                        首页
                    </button>
                    <button
                        type="button"
                        :disabled="currentPage <= 1"
                        @click="changePage(currentPage - 1)"
                    >
                        上一页
                    </button>
                    <button
                        v-for="page in visiblePages"
                        :key="page"
                        type="button"
                        :class="{ active: page === currentPage }"
                        @click="changePage(page)"
                    >
                        {{ page }}
                    </button>
                    <button
                        type="button"
                        :disabled="currentPage >= totalPages"
                        @click="changePage(currentPage + 1)"
                    >
                        下一页
                    </button>
                    <button
                        type="button"
                        :disabled="currentPage >= totalPages"
                        @click="changePage(totalPages)"
                    >
                        末页
                    </button>
                </div>
            </div>
        </div>
    </main>
</template>

<style scoped>
.notifications-page {
    max-width: 720px;
    margin: 0 auto;
    padding: 24px 0;
}

.notifications-header {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;
}

.notifications-header h1 {
    margin: 0;
    font-size: 28px;
}

.filter-tabs {
    display: flex;
    gap: 8px;
    padding: 4px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
}

.filter-tabs button {
    min-height: 34px;
    padding: 0 14px;
    color: var(--muted);
    cursor: pointer;
    background: transparent;
    border: 0;
    border-radius: 6px;
}

.filter-tabs button.active,
.filter-tabs button:hover {
    color: #ffffff;
    background: var(--ink-soft);
}

.mark-all-btn {
    min-height: 34px;
    padding: 0 14px;
    color: var(--brand-strong);
    cursor: pointer;
    background: transparent;
    border: 1px solid var(--line);
    border-radius: 8px;
}

.mark-all-btn:hover {
    border-color: var(--brand);
}

.notifications-loading,
.notifications-error,
.notifications-empty {
    padding: 48px 24px;
    text-align: center;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
}

.notifications-error p {
    color: var(--accent);
    margin: 0;
}

.notifications-empty p {
    color: var(--muted);
    margin: 0;
}

.notifications-list {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
    overflow: hidden;
}

.notification-item {
    display: grid;
    grid-template-columns: 52px minmax(0, 1fr) auto;
    gap: 16px;
    padding: 18px 20px;
    cursor: pointer;
    border-bottom: 1px solid var(--line);
    transition: background 0.15s ease;
}

.notification-item:last-child {
    border-bottom: 0;
}

.notification-item:hover {
    background: var(--surface-soft);
}

.notification-item.unread {
    background: rgba(15, 143, 117, 0.03);
}

.notification-avatar {
    width: 52px;
    height: 52px;
    object-fit: cover;
    border-radius: 50%;
}

.notification-content {
    min-width: 0;
}

.notification-text {
    font-size: 15px;
    line-height: 1.6;
    word-break: break-word;
}

.notification-actor {
    font-weight: 700;
}

.notification-time {
    margin-top: 4px;
    color: var(--muted);
    font-size: 13px;
}

.unread-dot {
    width: 10px;
    height: 10px;
    margin: auto 0;
    background: var(--brand);
    border-radius: 50%;
}

.pagination-bar {
    display: grid;
    gap: 12px;
    padding: 14px;
    margin-top: 18px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
}

.pagination-bar p {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
}

.pagination-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.pagination-actions button {
    min-width: 38px;
    min-height: 34px;
    padding: 0 11px;
    color: var(--text);
    cursor: pointer;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 8px;
}

.pagination-actions button:hover:not(:disabled) {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.pagination-actions button.active {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.pagination-actions button:disabled {
    cursor: not-allowed;
    opacity: 0.54;
}

.pagination-ellipsis {
    color: var(--muted);
    text-align: center;
}
</style>