<script setup>import {computed, inject, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import {navItems} from '@/data/home';
import {useSession} from '@/stores/session';
import {markAllNotificationsReadApi, markNotificationReadApi} from '@/api/notifications';
import {useHeaderNotifications} from '@/composables/useHeaderNotifications';
import {formatNotificationTime, getNotificationDetail, getNotificationText} from '@/utils/notifications';

const route = useRoute();
const router = useRouter();
const keyword = ref('');
const userMenuOpen = ref(false);
// 在 editor 页面时隐藏 header 里的「写文章」（编辑器内已有导航）
const hideWriteButton = computed(() =>
    route.path.startsWith('/editor')
);
const mobileMenuOpen = ref(false);
const mobileSearchOpen = ref(false);
const mobileSearchInputRef = ref(null);
const userMenuRef = ref(null);
const notificationOpen = ref(false);
const notificationRef = ref(null);
const { isLoggedIn, logout, state } = useSession();
const loginModal = inject('loginModal', { requireLogin: () => false });
const toast = inject('toast', { error: () => {} });
const displayName = computed(() => state.user?.nickname || state.user?.username || '用户');
const avatarUrl = computed(() => (
    state.user?.avatar
    || state.user?.avatarUrl
    || 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80'
));
const {
    displayMessageUnreadCount,
    displayUnreadCount,
    fetchRecentNotifications,
    markingAllRead,
    messageUnreadCount,
    notificationError,
    notificationsLoading,
    recentNotifications,
    refreshUnreadCounts,
    unreadCount
} = useHeaderNotifications(isLoggedIn);

const submitSearch = () => {
    const q = keyword.value.trim();
    mobileSearchOpen.value = false;
    mobileMenuOpen.value = false;
    router.push({
        path: '/search',
        query: q ? { keyword: q } : {}
    });
};

const openMobileSearch = () => {
    userMenuOpen.value = false;
    notificationOpen.value = false;
    mobileMenuOpen.value = false;
    mobileSearchOpen.value = true;
    nextTick(() => mobileSearchInputRef.value?.focus());
};

const closeMobileSearch = () => {
    mobileSearchOpen.value = false;
};

const handleSearchKeydown = (event) => {
    if (event.key === 'Escape') {
        closeMobileSearch();
    }
};

watch(mobileSearchOpen, (open) => {
    if (open) {
        document.body.classList.add('mobile-search-open');
        document.addEventListener('keydown', handleSearchKeydown);
    } else {
        document.body.classList.remove('mobile-search-open');
        document.removeEventListener('keydown', handleSearchKeydown);
    }
});

watch(() => route.fullPath, () => {
    mobileSearchOpen.value = false;
    mobileMenuOpen.value = false;
    notificationOpen.value = false;
    userMenuOpen.value = false;
    if (route.path === '/search' && route.query.keyword) {
        keyword.value = route.query.keyword;
    }
});

const logoutAndGoHome = () => {
    userMenuOpen.value = false;
    logout();
    router.push('/');
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
            toast.error(e.message || '标记通知已读失败，请稍后重试');
            return;
        }
    }
    notificationOpen.value = false;
    router.push(notification.targetUrl);
};

const handleMarkAllRead = async () => {
    if (markingAllRead.value) {
        return;
    }
    markingAllRead.value = true;
    try {
        await markAllNotificationsReadApi();
        unreadCount.value = 0;
        recentNotifications.value = [];
    } catch (e) {
        toast.error(e.message || '全部标记已读失败，请稍后重试');
    } finally {
        markingAllRead.value = false;
    }
};

onMounted(() => {
    document.addEventListener('click', handleDocumentClick);
    window.addEventListener('notifications:refresh', handleNotificationsRefresh);
    window.addEventListener('messages:refresh', handleMessagesRefresh);
});

onUnmounted(() => {
    document.removeEventListener('click', handleDocumentClick);
    document.removeEventListener('keydown', handleSearchKeydown);
    document.body.classList.remove('mobile-search-open');
    window.removeEventListener('notifications:refresh', handleNotificationsRefresh);
    window.removeEventListener('messages:refresh', handleMessagesRefresh);
});

const handleNotificationsRefresh = () => {
    refreshUnreadCounts();
    if (notificationOpen.value) {
        fetchRecentNotifications();
    }
};

const handleMessagesRefresh = () => {
    refreshUnreadCounts();
};
</script>

<template>
    <header class="site-header" data-testid="site-header">
        <div class="header-inner header-inner--article">
            <RouterLink class="brand" to="/" aria-label="DevNotes 首页" data-testid="site-brand">
                <span class="brand-mark">M</span>
                <span class="brand-name">DevNotes</span>
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
                    class="mobile-search-toggle"
                    type="button"
                    aria-label="搜索"
                    @click="openMobileSearch"
                >
                    <svg width="18" height="18" viewBox="0 0 20 20" fill="none">
                        <circle cx="8.5" cy="8.5" r="5.5" stroke="currentColor" stroke-width="1.6"/>
                        <path d="M13 13l4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
                    </svg>
                </button>
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
                    <RouterLink class="text-link" to="/history" data-testid="header-history-link">阅读记录</RouterLink>
                    <RouterLink v-if="state.user?.role === 'ADMIN'" class="text-link" to="/admin" data-testid="header-admin-link">后台</RouterLink>
                    <RouterLink class="message-btn" to="/messages" aria-label="私信" data-testid="header-message-btn">
                        <span class="message-icon" aria-hidden="true">
                            <svg viewBox="0 0 20 20" fill="none">
                                <path d="M2.5 4.5A1.5 1.5 0 0 1 4 3h12a1.5 1.5 0 0 1 1.5 1.5v7A1.5 1.5 0 0 1 16 13H7l-3.5 3V4.5Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                                <path d="M5.5 7.5h9M5.5 10h6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                            </svg>
                        </span>
                        <span
                            class="unread-badge"
                            :class="{ visible: Boolean(displayMessageUnreadCount) }"
                        >{{ displayMessageUnreadCount || '0' }}</span>
                    </RouterLink>
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
                            <span class="bell-icon" aria-hidden="true">
                                <svg viewBox="0 0 20 20" fill="none">
                                    <path d="M6.75 7.5a3.25 3.25 0 1 1 6.5 0v1.42c0 .63.2 1.24.58 1.74l.67.9c.52.7.03 1.69-.84 1.69H5.34c-.87 0-1.36-.99-.84-1.69l.67-.9c.38-.5.58-1.11.58-1.74V7.5Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                                    <path d="M8.25 15a1.75 1.75 0 0 0 3.5 0" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                                </svg>
                            </span>
                            <span
                                class="unread-badge"
                                :class="{ visible: Boolean(displayUnreadCount) }"
                            >{{ displayUnreadCount || '0' }}</span>
                        </button>
                        <div v-if="notificationOpen" class="notification-dropdown" data-testid="header-notification-dropdown">
                            <div class="notification-header">
                                <div class="notification-header-copy">
                                    <span class="notification-title">最近通知</span>
                                    <span class="notification-subtitle">
                                        {{ unreadCount > 0 ? `${displayUnreadCount} 条未读` : '保持关注最新互动' }}
                                    </span>
                                </div>
                                <button
                                    v-if="unreadCount > 0"
                                    class="mark-all-read"
                                    type="button"
                                    :disabled="markingAllRead"
                                    @click.stop="handleMarkAllRead"
                                >
                                    {{ markingAllRead ? '处理中...' : '全部已读' }}
                                </button>
                            </div>
                            <div v-if="notificationsLoading" class="notification-empty notification-empty-loading">
                                正在同步通知...
                            </div>
                            <div v-else-if="notificationError" class="notification-empty notification-empty-error">
                                {{ notificationError }}
                            </div>
                            <div v-else-if="recentNotifications.length === 0" class="notification-empty">
                                暂无通知
                            </div>
                            <div v-else class="notification-list">
                                <article
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
                                        loading="lazy"
                                        decoding="async"
                                    >
                                    <img
                                        v-else
                                        src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80"
                                        class="notification-avatar"
                                        alt=""
                                        loading="lazy"
                                        decoding="async"
                                    >
                                    <div class="notification-content">
                                        <div class="notification-text">
                                            <span class="notification-actor">{{ notification.actor?.nickname || notification.actor?.username }}</span>
                                            <span class="notification-action">{{ getNotificationText(notification.type) }}</span>
                                        </div>
                                        <div class="notification-detail">{{ getNotificationDetail(notification) }}</div>
                                        <div class="notification-time">{{ formatNotificationTime(notification.createdAt) }}</div>
                                    </div>
                                    <div class="notification-meta">
                                        <span v-if="!notification.read" class="unread-dot"></span>
                                        <span class="notification-arrow">›</span>
                                    </div>
                                </article>
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
                    >
                        <div class="user-menu-trigger-group" @click.stop="toggleUserMenu">
                            <button
                                class="user-chip user-home-link"
                                type="button"
                                data-testid="header-user-home"
                                :aria-expanded="userMenuOpen"
                                aria-haspopup="true"
                            >
                                <img :src="avatarUrl" alt="用户头像" loading="lazy" decoding="async">
                                <span>{{ displayName }}</span>
                            </button>
                        </div>
                        <div v-if="userMenuOpen" class="user-menu-panel" data-testid="header-user-menu" @click.stop>
                            <button class="user-menu-item" type="button" data-testid="header-user-home-link" @click="goToUserHome">
                                个人主页
                            </button>
                            <button class="user-menu-item" type="button" data-testid="header-profile-link" @click="goToProfile">
                                个人设置
                            </button>
                            <button
                                class="user-menu-item user-menu-item-danger"
                                type="button"
                                data-testid="header-logout-button"
                                @click="logoutAndGoHome"
                            >
                                退出登录
                            </button>
                        </div>
                    </div>
                </template>
                <RouterLink v-else class="text-link" to="/login" data-testid="header-login-link">登录</RouterLink>
                <button
                    v-if="!hideWriteButton"
                    class="primary-action desktop-write-action"
                    type="button"
                    data-testid="header-write-article"
                    @click="writeArticle"
                >写文章</button>
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

            <!-- 已登录：用户信息区 -->
            <div v-if="isLoggedIn" class="mobile-user-info">
                <img :src="avatarUrl" class="mobile-user-avatar" alt="用户头像" loading="lazy" decoding="async">
                <div class="mobile-user-detail">
                    <span class="mobile-user-name">{{ displayName }}</span>
                    <span v-if="unreadCount > 0" class="mobile-unread-hint">{{ displayUnreadCount }} 条未读通知</span>
                </div>
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
                <button class="primary-action block" type="button" @click="mobileMenuOpen = false; writeArticle()">写文章</button>
                <template v-if="isLoggedIn">
                    <RouterLink class="mobile-menu-link" :to="`/users/${state.user?.id}`" @click="mobileMenuOpen = false">个人主页</RouterLink>
                    <RouterLink class="mobile-menu-link" to="/notifications" @click="mobileMenuOpen = false">
                        通知
                        <span v-if="unreadCount > 0" class="mobile-badge">{{ displayUnreadCount }}</span>
                    </RouterLink>
                    <RouterLink class="mobile-menu-link" to="/messages" @click="mobileMenuOpen = false">
                        私信
                        <span v-if="messageUnreadCount > 0" class="mobile-badge">{{ displayMessageUnreadCount }}</span>
                    </RouterLink>
                    <RouterLink class="mobile-menu-link" to="/dashboard/articles" @click="mobileMenuOpen = false">创作台</RouterLink>
                    <RouterLink class="mobile-menu-link" to="/history" @click="mobileMenuOpen = false">阅读记录</RouterLink>
                    <RouterLink v-if="state.user?.role === 'ADMIN'" class="mobile-menu-link" to="/admin" @click="mobileMenuOpen = false">后台管理</RouterLink>
                    <RouterLink class="mobile-menu-link" to="/settings/profile" @click="mobileMenuOpen = false">个人设置</RouterLink>
                    <button class="mobile-menu-link mobile-menu-link-danger" type="button" @click="logoutAndGoHome">
                        退出登录
                    </button>
                </template>
                <RouterLink v-else class="mobile-menu-link" to="/login" @click="mobileMenuOpen = false">登录</RouterLink>
            </div>
        </div>
    </Teleport>

    <!-- Mobile search panel -->
    <Teleport to="body">
        <div v-if="mobileSearchOpen" class="mobile-search-overlay" @click="closeMobileSearch"></div>
        <div v-if="mobileSearchOpen" class="mobile-search-panel">
            <form class="mobile-search-form" @submit.prevent="submitSearch">
                <button type="button" class="mobile-search-cancel" @click="closeMobileSearch">取消</button>
                <input
                    ref="mobileSearchInputRef"
                    v-model="keyword"
                    type="search"
                    placeholder="搜文章、作者、专栏、技术问题"
                    class="mobile-search-input"
                    data-testid="mobile-site-search-input"
                >
                <button type="submit" class="mobile-search-submit" data-testid="mobile-site-search-submit">搜索</button>
            </form>
        </div>
    </Teleport>
</template>
