<script setup>
import { computed, inject, onMounted, onUnmounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { useRoute, useRouter } from 'vue-router';
import { navItems } from '@/data/home';
import { useSession } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const keyword = ref('');
const userMenuOpen = ref(false);
const userMenuRef = ref(null);
const { isLoggedIn, logout, state } = useSession();
const loginModal = inject('loginModal', { requireLogin: () => false });
const displayName = computed(() => state.user?.nickname || state.user?.username || '用户');
const avatarUrl = computed(() => (
    state.user?.avatar
    || state.user?.avatarUrl
    || 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80'
));

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
};

onMounted(() => {
    document.addEventListener('click', handleDocumentClick);
});

onUnmounted(() => {
    document.removeEventListener('click', handleDocumentClick);
});
</script>

<template>
    <header class="site-header">
        <div class="header-inner">
            <RouterLink class="brand" to="/" aria-label="my-blog 首页">
                <span class="brand-mark">M</span>
                <span class="brand-name">my-blog</span>
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

            <form class="search-box" role="search" @submit.prevent="submitSearch">
                <label for="site-search" class="sr-only">搜索文章</label>
                <input
                    id="site-search"
                    v-model="keyword"
                    type="search"
                    placeholder="搜索 Java、Spring Boot、Vue、MySQL"
                >
                <button type="submit">搜索</button>
            </form>

            <div class="header-actions">
                <template v-if="isLoggedIn">
                    <RouterLink class="text-link" to="/dashboard/articles">创作台</RouterLink>
                    <RouterLink v-if="state.user?.role === 'ADMIN'" class="text-link" to="/admin">后台</RouterLink>
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
                        <div v-if="userMenuOpen" class="user-menu-panel">
                            <button class="user-menu-item" type="button" @click="goToProfile">
                                个人资料
                            </button>
                            <button
                                class="user-menu-item user-menu-item-danger"
                                type="button"
                                @click="logoutAndGoHome"
                            >
                                退出
                            </button>
                        </div>
                    </div>
                </template>
                <RouterLink v-else class="text-link" to="/login">登录</RouterLink>
                <button class="primary-action" type="button" @click="writeArticle">写文章</button>
            </div>
        </div>
    </header>
</template>
