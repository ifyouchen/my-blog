<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { getAdminLogsApi, getAdminStatsApi } from '@/api/admin';
import { useAdminRefresh } from '@/views/admin/adminShared';

const loading = ref(false);
const error = ref('');
const stats = ref(null);
const recentLogs = ref([]);

const statCards = computed(() => {
    const data = stats.value || {};
    return [
        { label: '用户总数', value: data.totalUsers || 0, helper: `今日新增 ${data.todayNewUsers || 0}` },
        { label: '正常用户', value: data.normalUsers || 0, helper: `已禁用 ${data.disabledUsers || 0}` },
        { label: '文章总数', value: data.totalArticles || 0, helper: `近 7 天 ${data.sevenDayArticles || 0}` },
        { label: '已发布文章', value: data.publishedArticles || 0, helper: `待下架关注 ${data.offlineArticles || 0}` },
        { label: '评论总数', value: data.totalComments || 0, helper: `今日新增 ${data.todayNewComments || 0}` },
        { label: '近 7 天用户', value: data.sevenDayUsers || 0, helper: `近 7 天评论 ${data.sevenDayComments || 0}` }
    ];
});

const quickLinks = [
    { title: '管理用户状态', description: '快速处理禁用、恢复和账号检索。', to: '/admin/users' },
    { title: '管理文章状态', description: '筛选已发布、草稿和下架文章。', to: '/admin/articles' },
    { title: '处理评论与审计', description: '查看评论删除和管理员操作记录。', to: '/admin/comments' }
];

const loadOverview = async () => {
    loading.value = true;
    error.value = '';
    try {
        const [statsResult, logsResult] = await Promise.all([
            getAdminStatsApi(),
            getAdminLogsApi(1, 5)
        ]);
        stats.value = statsResult;
        recentLogs.value = logsResult.items || [];
    } catch (loadError) {
        error.value = loadError.message || '管理概览加载失败';
    } finally {
        loading.value = false;
    }
};

useAdminRefresh(loadOverview);

onMounted(loadOverview);
</script>

<template>
    <section class="admin-page-grid">
        <section class="dashboard-content-panel">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">概览</p>
                    <h2>站点运行概况</h2>
                </div>
            </div>

            <p v-if="loading" class="backend-state-text">概览数据加载中...</p>
            <p v-else-if="error" class="backend-state-text error-text">{{ error }}</p>
            <div v-else class="admin-overview-grid" data-testid="admin-overview-stats">
                <article v-for="card in statCards" :key="card.label" class="admin-overview-card">
                    <span>{{ card.label }}</span>
                    <strong>{{ card.value }}</strong>
                    <p>{{ card.helper }}</p>
                </article>
            </div>
        </section>

        <section class="dashboard-content-panel">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">快捷入口</p>
                    <h2>今天先处理什么</h2>
                </div>
            </div>

            <div class="admin-quick-grid">
                <RouterLink
                    v-for="link in quickLinks"
                    :key="link.to"
                    :to="link.to"
                    class="admin-quick-card"
                >
                    <strong>{{ link.title }}</strong>
                    <span>{{ link.description }}</span>
                </RouterLink>
            </div>
        </section>

        <section class="dashboard-content-panel">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">最近操作</p>
                    <h2>管理员日志摘要</h2>
                </div>
                <RouterLink class="text-link" to="/admin/logs">查看全部</RouterLink>
            </div>

            <div v-if="recentLogs.length" class="admin-log-list">
                <article v-for="log in recentLogs" :key="log.id" class="admin-log-item">
                    <div>
                        <strong>{{ log.operation }}</strong>
                        <p>{{ log.detail }}</p>
                    </div>
                    <span>{{ log.createdAt }}</span>
                </article>
            </div>
            <p v-else-if="!loading && !error" class="backend-state-text">最近还没有管理员操作记录</p>
        </section>
    </section>
</template>
