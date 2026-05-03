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

<style scoped>
.access-denied-page {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: calc(100vh - 60px);
    padding: 24px;
}

.access-denied-card {
    display: grid;
    gap: 16px;
    max-width: 420px;
    padding: 40px 32px;
    text-align: center;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow);
}

.access-denied-icon {
    display: flex;
    justify-content: center;
    color: var(--accent);
}

.access-denied-card h1 {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: var(--text-strong);
}

.access-denied-card p {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
    line-height: 1.7;
}

.access-denied-actions {
    display: flex;
    gap: 10px;
    justify-content: center;
    margin-top: 8px;
}

.secondary-action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 34px;
    padding: 0 16px;
    color: var(--text);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    transition: color 0.12s, border-color 0.12s;
}

.secondary-action:hover {
    color: var(--brand);
    border-color: var(--brand);
}
</style>
