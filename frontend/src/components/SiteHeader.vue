<script setup>
import { computed, inject, onMounted, onUnmounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import { useRoute, useRouter } from 'vue-router';
import { navItems } from '@/data/home';
import { useSession } from '@/stores/session';
import {
    getNotificationUnreadCountApi,
    getRecentNotificationsApi,
    markNotificationReadApi,
    markAllNotificationsReadApi
} from '@/api/notifications';

const route = useRoute();
const router = useRouter();
const keyword = ref('');
const userMenuOpen = ref(false);
const mobileMenuOpen = ref(false);
const userMenuRef = ref(null);
const notificationOpen = ref(false);
const notificationRef = ref(null);
const { isLoggedIn, logout, state } = useSession();
const loginModal = inject('loginModal', { requireLogin: () => false });
const displayName = computed(() => state.user?.nickname || state.user?.username || '用户');
const avatarUrl = computed(() => (
    state.user?.avatar
    || state.user?.avatarUrl
    || 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80'
));

const unreadCount = ref(0);
const recentNotifications = ref([]);
let notificationPollInterval = null;

const submitSearch = () => {
    router.push({
        path: '/search',
        query: keyword.value ? { keyword: keyword.value } : {}
    });
};

const logoutAndGoHome = () => {
    userMenuOpen.value = false;
    logout();
    router.push('/');
};

const openUserMenu = () => {
    userMenuOpen.value = true;
};

const closeUserMenu = () => {
    userMenuOpen.value = false;
};

const toggleUserMenu = () => {
    userMenuOpen.value = !userMenuOpen.value;
};

const goToProfile = () => {
    userMenuOpen.value = false;
    router.push('/settings/profile');
};

const goToUserHome = () => {
    userMenuOpen.value = false;
    if (state.user?.id) {
        router.push(`/users/${state.user.id}`);
    }
};

const writeArticle = () => {
    const canContinue = loginModal.requireLogin(() => router.push('/editor/new'), {
        title: '登录后开始创作',
        message: '登录后可以保存草稿、发布文章，并在个人中心管理内容。',
        actionText: '登录并写文章'
    });
    if (canContinue) {
        router.push('/editor/new');
    }
};

const handleDocumentClick = (event) => {
    if (!userMenuRef.value?.contains(event.target)) {
        userMenuOpen.value = false;
    }
    if (!notificationRef.value?.contains(event.target)) {
        notificationOpen.value = false;
    }
};

const fetchUnreadCount = async () => {
    if (!isLoggedIn.value) {
        unreadCount.value = 0;
        return;
    }
    try {
        const result = await getNotificationUnreadCountApi();
        unreadCount.value = result.count || 0;
    } catch (e) {
        console.error('Failed to fetch unread count:', e);
    }
};

const fetchRecentNotifications = async () => {
    if (!isLoggedIn.value) {
        recentNotifications.value = [];
        return;
    }
    try {
        const result = await getRecentNotificationsApi(5);
        recentNotifications.value = result || [];
    } catch (e) {
        console.error('Failed to fetch recent notifications:', e);
    }
};

const toggleNotifications = () => {
    notificationOpen.value = !notificationOpen.value;
    if (notificationOpen.value) {
        fetchRecentNotifications();
    }
};

const goToNotifications = () => {
    notificationOpen.value = false;
    router.push('/notifications');
};

const handleNotificationClick = async (notification) => {
    if (!notification.read) {
        try {
            await markNotificationReadApi(notification.id);
            unreadCount.value = Math.max(0, unreadCount.value - 1);
            const index = recentNotifications.value.findIndex(n => n.id === notification.id);
            if (index !== -1) {
                recentNotifications.value.splice(index, 1);
            }
        } catch (e) {
            console.error('Failed to mark notification as read:', e);
        }
    }
    notificationOpen.value = false;
    router.push(notification.targetUrl);
};

const handleMarkAllRead = async () => {
    try {
        await markAllNotificationsReadApi();
        unreadCount.value = 0;
        recentNotifications.value = [];
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

const displayUnreadCount = computed(() => {
    if (unreadCount.value <= 0) return '';
    if (unreadCount.value > 99) return '99+';
    return unreadCount.value;
});

watch(isLoggedIn, (loggedIn) => {
    if (loggedIn) {
        fetchUnreadCount();
        if (notificationPollInterval) {
            clearInterval(notificationPollInterval);
        }
        notificationPollInterval = setInterval(fetchUnreadCount, 30000);
    } else {
        unreadCount.value = 0;
        recentNotifications.value = [];
        if (notificationPollInterval) {
            clearInterval(notificationPollInterval);
            notificationPollInterval = null;
        }
    }
}, { immediate: true });

onMounted(() => {
    document.addEventListener('click', handleDocumentClick);
    window.addEventListener('notifications:refresh', handleNotificationsRefresh);
});

onUnmounted(() => {
    document.removeEventListener('click', handleDocumentClick);
    if (notificationPollInterval) {
        clearInterval(notificationPollInterval);
    }
    window.removeEventListener('notifications:refresh', handleNotificationsRefresh);
});

const handleNotificationsRefresh = () => {
    fetchUnreadCount();
    if (notificationOpen.value) {
        fetchRecentNotifications();
    }
};
</script>

<template>
    <header class="site-header" data-testid="site-header">
        <div class="header-inner">
            <RouterLink class="brand" to="/" aria-label="my-blog 首页" data-testid="site-brand">
                <span class="brand-mark">M</span>
                <span class="brand-name">my-blog 社区</span>
            </RouterLink>

            <nav class="main-nav" aria-label="主导航">
                <RouterLink
                    v-for="item in navItems"
                    :key="item.label"
                    :to="item.path"
                    :class="{ active: item.path === '/'
                        ? route.path === '/'
                        : route.path === item.path || route.path.startsWith(`${item.path}/`) }"
                >
                    {{ item.label }}
                </RouterLink>
            </nav>

            <form class="search-box" role="search" data-testid="site-search-form" @submit.prevent="submitSearch">
                <label for="site-search" class="sr-only">搜索文章</label>
                <input
                    id="site-search"
                    v-model="keyword"
                    type="search"
                    placeholder="搜文章、作者、专栏、技术问题"
                    data-testid="site-search-input"
                >
                <button type="submit" data-testid="site-search-submit">搜索</button>
            </form>

            <div class="header-actions">
                <button
                    class="mobile-menu-toggle"
                    type="button"
                    aria-label="展开导航菜单"
                    :aria-expanded="mobileMenuOpen"
                    @click="mobileMenuOpen = !mobileMenuOpen"
                >
                    <span class="hamburger-line"></span>
                    <span class="hamburger-line"></span>
                    <span class="hamburger-line"></span>
                </button>
                <template v-if="isLoggedIn">
                    <RouterLink class="text-link" to="/dashboard/articles" data-testid="header-dashboard-link">创作台</RouterLink>
                    <RouterLink v-if="state.user?.role === 'ADMIN'" class="text-link" to="/admin" data-testid="header-admin-link">后台</RouterLink>
                    <div
                        ref="notificationRef"
                        class="notification-wrapper"
                    >
                        <button
                            class="notification-bell"
                            type="button"
                            aria-label="通知"
                            data-testid="header-notification-bell"
                            @click.stop="toggleNotifications"
                        >
                            <span class="bell-icon">🔔</span>
                            <span v-if="displayUnreadCount" class="unread-badge">{{ displayUnreadCount }}</span>
                        </button>
                        <div v-if="notificationOpen" class="notification-dropdown" data-testid="header-notification-dropdown">
                            <div class="notification-header">
                                <span class="notification-title">通知</span>
                                <button
                                    v-if="unreadCount > 0"
                                    class="mark-all-read"
                                    type="button"
                                    @click.stop="handleMarkAllRead"
                                >
                                    全部已读
                                </button>
                            </div>
                            <div v-if="recentNotifications.length === 0" class="notification-empty">
                                暂无通知
                            </div>
                            <div v-else class="notification-list">
                                <div
                                    v-for="notification in recentNotifications"
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
                            <div class="notification-footer">
                                <button class="view-all-btn" type="button" @click.stop="goToNotifications">
                                    查看全部通知
                                </button>
                            </div>
                        </div>
                    </div>
                    <div
                        ref="userMenuRef"
                        class="user-menu"
                        @mouseenter="openUserMenu"
                        @mouseleave="closeUserMenu"
                    >
                        <div class="user-menu-trigger-group">
                            <button
                                class="user-chip user-home-link"
                                type="button"
                                data-testid="header-user-home"
                                @click="goToUserHome"
                            >
                                <img :src="avatarUrl" alt="用户头像">
                                <span>{{ displayName }}</span>
                            </button>
                            <button
                                class="user-menu-toggle"
                                type="button"
                                aria-label="展开用户菜单"
                                :aria-expanded="userMenuOpen"
                                data-testid="header-user-menu-toggle"
                                @click.stop="toggleUserMenu"
                            >
                                <span
                                    class="user-trigger-arrow"
                                    :class="{ open: userMenuOpen }"
                                    aria-hidden="true"
                                >
                                    ▾
                                </span>
                            </button>
                        </div>
                        <div v-if="userMenuOpen" class="user-menu-panel" data-testid="header-user-menu">
                            <button class="user-menu-item" type="button" data-testid="header-profile-link" @click="goToProfile">
                                个人资料
                            </button>
                            <button
                                class="user-menu-item user-menu-item-danger"
                                type="button"
                                data-testid="header-logout-button"
                                @click="logoutAndGoHome"
                            >
                                退出
                            </button>
                        </div>
                    </div>
                </template>
                <RouterLink v-else class="text-link" to="/login" data-testid="header-login-link">登录</RouterLink>
                <button class="primary-action" type="button" data-testid="header-write-article" @click="writeArticle">写文章</button>
            </div>
        </div>
    </header>

    <!-- Mobile nav drawer -->
    <Teleport to="body">
        <div v-if="mobileMenuOpen" class="mobile-menu-overlay" @click="mobileMenuOpen = false"></div>
        <div class="mobile-menu-drawer" :class="{ open: mobileMenuOpen }">
            <div class="mobile-menu-header">
                <button
                    class="mobile-menu-close"
                    type="button"
                    aria-label="关闭导航菜单"
                    @click="mobileMenuOpen = false"
                >×</button>
            </div>
            <nav class="mobile-nav" aria-label="移动端导航">
                <RouterLink
                    v-for="item in navItems"
                    :key="item.label"
                    :to="item.path"
                    :class="{ active: item.path === '/'
                        ? route.path === '/'
                        : route.path === item.path || route.path.startsWith(`${item.path}/`) }"
                    @click="mobileMenuOpen = false"
                >
                    {{ item.label }}
                </RouterLink>
            </nav>
            <div class="mobile-menu-actions">
                <template v-if="isLoggedIn">
                    <RouterLink class="mobile-menu-link" to="/dashboard/articles" @click="mobileMenuOpen = false">创作台</RouterLink>
                    <RouterLink v-if="state.user?.role === 'ADMIN'" class="mobile-menu-link" to="/admin" @click="mobileMenuOpen = false">后台</RouterLink>
                    <button class="mobile-menu-link mobile-menu-link-danger" type="button" @click="logoutAndGoHome; mobileMenuOpen = false">
                        退出
                    </button>
                </template>
                <RouterLink v-else class="mobile-menu-link" to="/login" @click="mobileMenuOpen = false">登录</RouterLink>
                <button class="primary-action" type="button" @click="writeArticle; mobileMenuOpen = false">写文章</button>
            </div>
        </div>
    </Teleport>
</template>
