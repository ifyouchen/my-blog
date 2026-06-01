<script setup>
import { useRouter, useRoute } from 'vue-router';
import { useSession } from '@/stores/session';

const router = useRouter();
const route = useRoute();
const { logout } = useSession();

const goHome = () => {
    router.push('/');
};

const switchAccount = () => {
    logout();
    router.push('/login');
};
</script>

<template>
    <div class="access-denied-page">
        <div class="access-denied-card">
            <div class="access-denied-icon">
                <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                    <circle cx="24" cy="24" r="20" stroke="currentColor" stroke-width="2.5"/>
                    <path d="M16 16l16 16M32 16L16 32" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
            </div>
            <h1>无权限访问</h1>
            <p>当前账号没有访问该页面的权限。如需访问，请联系管理员或切换账号。</p>
            <div class="access-denied-actions">
                <button class="primary-action" type="button" @click="goHome">返回首页</button>
                <button class="secondary-action" type="button" @click="switchAccount">切换账号</button>
            </div>
        </div>
    </div>
</template>

<style scoped src="@/styles/views/AccessDeniedView.css"></style>
