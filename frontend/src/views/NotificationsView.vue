<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import EmptyState from '@/components/EmptyState.vue';
import {
    getActiveAnnouncementsApi,
    getNotificationsApi,
    markAllNotificationsReadApi,
    markNotificationReadApi
} from '@/api/notifications';
import {
    formatNotificationTime,
    getNotificationDetail,
    getNotificationTargetUrl,
    getNotificationText
} from '@/utils/notifications';
import {sanitizeAnnouncementHtml} from '@/utils/markdown';

const route = useRoute();
const router = useRouter();

const validFilters = ['all', 'unread', 'read', 'system'];
const pageSize = 10;
const fallbackAvatarUrl = 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d'
    + '?auto=format&fit=crop&w=96&q=80';

const notifications = ref([]);
const total = ref(0);
const currentFilter = ref(validFilters.includes(route.query.filter) ? route.query.filter : 'all');
const currentPage = ref(Number.parseInt(route.query.page || '1', 10) || 1);
const initialLoading = ref(true);
const refreshing = ref(false);
const errorMessage = ref('');
const actionError = ref('');
let hasLoadedOnce = false;

// 系统公告
const announcements = ref([]);
const announcementsLoading = ref(false);
const announcementsError = ref('');

const loadAnnouncements = async () => {
    announcementsLoading.value = true;
    announcementsError.value = '';
    try {
        const result = await getActiveAnnouncementsApi();
        announcements.value = Array.isArray(result) ? result : [];
    } catch (e) {
        announcementsError.value = e.message || '加载公告失败';
    } finally {
        announcementsLoading.value = false;
    }
};

onMounted(() => {
    if (currentFilter.value === 'system') {
        loadAnnouncements();
    }
});

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

const totalPages = computed(() => (total.value > 0 ? Math.ceil(total.value / pageSize) : 1));
const showMarkAllRead = computed(() =>
    ['all', 'unread'].includes(currentFilter.value) && total.value > 0
);

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
    if (filter === 'system') {
        loadAnnouncements();
    }
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

const markNotificationReadLocally = (notification) => {
    notification.read = true;
    if (currentFilter.value === 'unread') {
        notifications.value = notifications.value.filter((item) => item.id !== notification.id);
        total.value = Math.max(0, total.value - 1);
        normalizeAfterUnreadMutation();
    }
};

const handleNotificationClick = (notification) => {
    actionError.value = '';
    const targetUrl = getNotificationTargetUrl(notification);
    if (!notification.read) {
        markNotificationReadLocally(notification);
        markNotificationReadApi(notification.id)
            .then(() => {
                window.dispatchEvent(new CustomEvent('notifications:refresh'));
            })
            .catch((error) => {
                actionError.value = error.message || '标记通知已读失败，请稍后重试';
                void fetchNotifications({ silent: true });
            });
    }
    router.push(targetUrl);
};

const applyAllReadLocally = () => {
    if (currentFilter.value === 'unread') {
        notifications.value = [];
        total.value = 0;
        currentPage.value = 1;
        return;
    }
    notifications.value = notifications.value.map((item) => ({
        ...item,
        read: true
    }));
};

const markingAllRead = ref(false);

const handleMarkAllRead = async () => {
    if (markingAllRead.value || refreshing.value || !showMarkAllRead.value) {
        return;
    }
    markingAllRead.value = true;
    actionError.value = '';
    try {
        await markAllNotificationsReadApi();
        applyAllReadLocally();
        if (currentFilter.value === 'unread') {
            await normalizeAfterUnreadMutation();
        }
        window.dispatchEvent(new CustomEvent('notifications:refresh'));
    } catch (error) {
        actionError.value = error.message || '全部标记已读失败，请稍后重试';
        await fetchNotifications({ silent: true });
    } finally {
        markingAllRead.value = false;
    }
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
                        <button
                            :class="{ active: currentFilter === 'system' }"
                            type="button"
                            :disabled="refreshing"
                            @click="changeFilter('system')"
                        >
                            系统通知
                        </button>
                    </div>
                    <div class="mark-all-read-slot">
                        <button
                            v-if="showMarkAllRead"
                            class="mark-all-read"
                            type="button"
                            :disabled="markingAllRead || refreshing"
                            @click="handleMarkAllRead"
                        >
                            {{ markingAllRead ? '处理中...' : '全部已读' }}
                        </button>
                    </div>
                </div>
            </header>

            <section class="notifications-panel">
                <!-- 系统通知（公告）面板 -->
                <template v-if="currentFilter === 'system'">
                    <div v-if="announcementsLoading" class="panel-status panel-status-refreshing">正在加载系统公告...</div>
                    <div v-else-if="announcementsError" class="panel-status panel-status-error">
                        {{ announcementsError }}
                    </div>
                    <EmptyState
                        v-else-if="announcements.length === 0"
                        eyebrow="系统通知"
                        title="暂无系统公告"
                        description="平台公告与重要通知将在此展示。"
                        compact
                        class="notifications-state"
                    />
                    <div v-else class="notifications-list">
                        <article
                            v-for="ann in announcements"
                            :key="ann.id"
                            class="notification-item announcement-item"
                        >
                            <div class="announcement-icon" aria-hidden="true">📢</div>
                            <div class="notification-content">
                                <div class="notification-text">
                                    <span class="notification-actor">官方公告</span>
                                    <span class="notification-action">· {{ ann.title }}</span>
                                </div>
                                <div class="notification-detail" v-html="sanitizeAnnouncementHtml(ann.content)"></div>
                                <div class="notification-time">{{ ann.publishedAt || ann.createdAt }}</div>
                            </div>
                        </article>
                    </div>
                </template>

                <template v-else>
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
                         decoding="async">
                        <img
                            v-else
                            :src="fallbackAvatarUrl"
                            class="notification-avatar"
                            alt=""
                            loading="lazy"
                         decoding="async">
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
                            <div class="notification-time">{{ formatNotificationTime(notification.createdAt) }}</div>
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
                </template>
            </section>
        </section>
    </main>
</template>

<style scoped src="@/styles/views/NotificationsView.part-1.css"></style>
<style scoped src="@/styles/views/NotificationsView.part-2.css"></style>
