<script setup>
import { RouterLink } from 'vue-router';

const navItems = [
    {
        label: '创作概览',
        path: '/dashboard/overview'
    },
    {
        label: '我的文章',
        path: '/dashboard/articles'
    },
    {
        label: '我的收藏',
        path: '/dashboard/favorites'
    }
];
</script>

<template>
    <aside class="dashboard-nav">
        <p class="eyebrow">创作者后台</p>
        <RouterLink
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
        >
            {{ item.label }}
        </RouterLink>
    </aside>
</template>
