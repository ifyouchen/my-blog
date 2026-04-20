<script setup>
import { computed, inject, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { useRoute, useRouter } from 'vue-router';
import { navItems } from '@/data/home';
import { useSession } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const keyword = ref('');
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
    logout();
    router.push('/');
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
                    :class="{ active: route.path === item.path }"
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
                    <RouterLink class="user-chip" to="/dashboard/articles">
                        <img :src="avatarUrl" alt="用户头像">
                        <span>{{ displayName }}</span>
                    </RouterLink>
                    <button class="text-button" type="button" @click="logoutAndGoHome">退出</button>
                </template>
                <RouterLink v-else class="text-link" to="/login">登录</RouterLink>
                <button class="primary-action" type="button" @click="writeArticle">写文章</button>
            </div>
        </div>
    </header>
</template>
