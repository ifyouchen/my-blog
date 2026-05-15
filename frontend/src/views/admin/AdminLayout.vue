<script setup>import {computed} from 'vue';
import {RouterLink, RouterView, useRoute} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import {useSession} from '@/stores/session';

const route = useRoute();
const { state } = useSession();

const navItems = [
    { to: '/admin/overview', label: '概览', icon: '总览' },
    { to: '/admin/users', label: '用户', icon: '用户' },
    { to: '/admin/articles', label: '文章', icon: '文章' },
    { to: '/admin/comments', label: '评论', icon: '评论' },
    { to: '/admin/reports', label: '举报', icon: '举报' },
    { to: '/admin/categories', label: '分类', icon: '分类' },
    { to: '/admin/tags', label: '标签', icon: '标签' },
    { to: '/admin/columns', label: '专栏', icon: '专栏' },
    { to: '/admin/topics', label: '专题', icon: '专题' },
    { to: '/admin/ads', label: '广告', icon: '广告' },
    { to: '/admin/announcements', label: '公告', icon: '公告' },
    { to: '/admin/sensitive-words', label: '敏感词', icon: '敏感' },
    { to: '/admin/logs', label: '日志', icon: '日志' },
    { to: '/admin/growth', label: '积分管理', icon: '积分' }
];

const currentTitle = computed(() => route.meta.adminTitle || '管理后台');
const currentDescription = computed(() => route.meta.adminDescription || '统一管理站点内容、用户与审核动作。');
const canAccessAdmin = computed(() => state.ready && state.user?.role === 'ADMIN');

const requestRefresh = () => {
    window.dispatchEvent(new CustomEvent('admin-refresh-request'));
};
</script>

<template>
    <SiteHeader />
    <main class="page-shell admin-shell" data-testid="admin-layout">
        <aside class="admin-sidebar" data-testid="admin-sidebar">
            <div class="admin-sidebar-head">
                <p class="eyebrow">管理后台</p>
                <h2>运营工作台</h2>
                <p>统一处理账号、内容、评论和审计日志。</p>
            </div>
            <nav class="admin-nav" aria-label="管理后台导航" data-testid="admin-nav">
                <RouterLink
                    v-for="item in navItems"
                    :key="item.to"
                    :to="item.to"
                    class="admin-nav-link"
                >
                    <span class="admin-nav-icon">{{ item.icon }}</span>
                    <span>{{ item.label }}</span>
                </RouterLink>
            </nav>
        </aside>

        <section class="admin-workspace" data-testid="admin-workspace">
            <header class="admin-workspace-head">
                <div>
                    <p class="eyebrow">管理后台</p>
                    <h1>{{ currentTitle }}</h1>
                    <p>{{ currentDescription }}</p>
                </div>
                <button type="button" class="admin-refresh-button" @click="requestRefresh">刷新当前页</button>
            </header>

            <section v-if="!state.ready" class="dashboard-content-panel admin-empty-panel">
                <p class="eyebrow">正在准备</p>
                <h2>正在校验你的登录状态</h2>
                <p>马上就好，管理端数据准备完成后会自动显示。</p>
            </section>

            <section v-else-if="!canAccessAdmin" class="dashboard-content-panel admin-empty-panel">
                <p class="eyebrow">访问受限</p>
                <h2>当前账号不能访问管理后台</h2>
                <p>这里仅对管理员开放。你可以返回首页继续浏览内容，或切换到管理员账号后再进入。</p>
                <div class="admin-empty-actions">
                    <RouterLink class="primary-action" to="/">返回首页</RouterLink>
                </div>
            </section>

            <RouterView v-else />
        </section>
    </main>
</template>

<style scoped>
.admin-sidebar-head h2 {
    color: var(--text-strong);
    font-size: 28px;
    line-height: 1.16;
}

.admin-sidebar-head p:not(.eyebrow),
.admin-workspace-head p:not(.eyebrow) {
    max-width: 620px;
}

.admin-workspace-head {
    background: var(--surface);
}

.admin-refresh-button {
    border-radius: var(--radius-sm);
}

.admin-empty-panel {
    background: var(--surface);
}

@media (max-width: 760px) {
    .admin-workspace-head {
        align-items: stretch;
        flex-direction: column;
    }

    .admin-refresh-button {
        width: fit-content;
    }
}
</style>
