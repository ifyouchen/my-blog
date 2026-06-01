<script setup>
import {computed, defineAsyncComponent, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {getAdminGrowthRulesApi, getAdminPointRulesApi, getAdminRevenueSharesApi} from '@/api/growth';

const route = useRoute();
const router = useRouter();

const GrowthRuleTab = defineAsyncComponent(() => import('@/views/admin/GrowthRuleTab.vue'));
const LevelThresholdTab = defineAsyncComponent(() => import('@/views/admin/LevelThresholdTab.vue'));
const LevelRewardTab = defineAsyncComponent(() => import('@/views/admin/LevelRewardTab.vue'));
const PointRuleTab = defineAsyncComponent(() => import('@/views/admin/PointRuleTab.vue'));
const PointAccountTab = defineAsyncComponent(() => import('@/views/admin/PointAccountTab.vue'));
const RevenueShareTab = defineAsyncComponent(() => import('@/views/admin/RevenueShareTab.vue'));
const RewardGrantLogTab = defineAsyncComponent(() => import('@/views/admin/RewardGrantLogTab.vue'));
const RecommendationApplicationTab = defineAsyncComponent(() => import('@/views/admin/RecommendationApplicationTab.vue'));
const AnnualCreatorCandidateTab = defineAsyncComponent(() => import('@/views/admin/AnnualCreatorCandidateTab.vue'));
const SignInRewardTab = defineAsyncComponent(() => import('@/views/admin/SignInRewardTab.vue'));

const TABS = [
    {key: 'dashboard', label: '概览'},
    {key: 'rules', label: '经验规则'},
    {key: 'thresholds', label: '等级配置'},
    {key: 'level-rewards', label: '等级奖励'},
    {key: 'sign-in-rewards', label: '签到奖励'},
    {key: 'point-rules', label: '积分规则'},
    {key: 'points', label: '积分账户'},
    {key: 'reward-logs', label: '奖励记录'},
    {key: 'recommendation-applications', label: '推荐申请'},
    {key: 'annual-candidates', label: '年度候选'},
    {key: 'revenue', label: '分账流水'},
];

const activeTab = ref('dashboard');
const TAB_COMPONENTS = {
    rules: GrowthRuleTab,
    thresholds: LevelThresholdTab,
    'level-rewards': LevelRewardTab,
    'sign-in-rewards': SignInRewardTab,
    'point-rules': PointRuleTab,
    points: PointAccountTab,
    'reward-logs': RewardGrantLogTab,
    'recommendation-applications': RecommendationApplicationTab,
    'annual-candidates': AnnualCreatorCandidateTab,
    revenue: RevenueShareTab
};
const activeTabComponent = computed(() => TAB_COMPONENTS[activeTab.value] || null);

watch(() => route.query.tab, (tab) => {
    if (tab && TABS.some(t => t.key === tab)) {
        activeTab.value = tab;
    }
}, {immediate: true});

const switchTab = (key) => {
    activeTab.value = key;
    router.replace({query: {...route.query, tab: key}});
    if (key === 'dashboard') {
        loadDashboard();
    }
};

// ── 仪表盘统计 ────────────────────────────────────────────────────
const dashboardData = ref({
    growthRulesCount: 0,
    pointRulesTotal: 0,
    pointRulesEnabled: 0,
    revenuePending: 0,
    revenueFailed: 0,
});
const dashboardLoading = ref(false);

const loadDashboard = async () => {
    dashboardLoading.value = true;
    try {
        const [growthRules, pointRules] = await Promise.all([
            getAdminGrowthRulesApi().catch(() => []),
            getAdminPointRulesApi().catch(() => []),
        ]);
        const pointTotal = pointRules.length || 0;
        const pointEnabled = pointRules.filter(r => !r.deleted && r.enabled).length || 0;
        const growthCount = growthRules.filter(r => r.enabled).length || 0;

        let revenuePending = 0;
        let revenueFailed = 0;
        try {
            const pendingResult = await getAdminRevenueSharesApi({settlementStatus: 'PENDING', page: 1, size: 1});
            revenuePending = pendingResult.total || 0;
        } catch {}
        try {
            const failedResult = await getAdminRevenueSharesApi({settlementStatus: 'FAILED', page: 1, size: 1});
            revenueFailed = failedResult.total || 0;
        } catch {}

        dashboardData.value = {
            growthRulesCount: growthCount,
            pointRulesTotal: pointTotal,
            pointRulesEnabled: pointEnabled,
            revenuePending,
            revenueFailed,
        };
    } catch {} finally {
        dashboardLoading.value = false;
    }
};

// ── 生命周期 ───────────────────────────────────────────────────────
onMounted(() => {
    if (activeTab.value === 'dashboard') loadDashboard();
});
</script>

<template>
    <div class="admin-growth">
        <!-- Tabs 栏 -->
        <div class="ag-tabs">
            <button
                v-for="tab in TABS"
                :key="tab.key"
                type="button"
                :class="['ag-tab', {active: activeTab === tab.key}]"
                @click="switchTab(tab.key)"
            >
                {{ tab.label }}
            </button>
        </div>

        <!-- ==================== 概览 ==================== -->
        <section v-if="activeTab === 'dashboard'" class="ag-section">
            <div class="ag-section-head">
                <h2 class="ag-section-title">成长系统概览</h2>
                <span class="ag-section-subtitle">当前系统运行状态一览</span>
            </div>
            <div v-if="dashboardLoading" class="ag-table-empty">加载中...</div>
            <div v-else class="dashboard-grid">
                <div class="dashboard-card">
                    <span class="dashboard-card-icon rules-icon">⚙</span>
                    <div class="dashboard-card-body">
                        <span class="dashboard-card-value">{{ dashboardData.growthRulesCount }}</span>
                        <span class="dashboard-card-label">已启用经验规则</span>
                    </div>
                </div>
                <div class="dashboard-card">
                    <span class="dashboard-card-icon point-icon">★</span>
                    <div class="dashboard-card-body">
                        <span class="dashboard-card-value">{{ dashboardData.pointRulesEnabled }}/{{ dashboardData.pointRulesTotal }}</span>
                        <span class="dashboard-card-label">积分规则（启用/总数）</span>
                    </div>
                </div>
                <div class="dashboard-card warning">
                    <span class="dashboard-card-icon pending-icon">⏳</span>
                    <div class="dashboard-card-body">
                        <span class="dashboard-card-value">{{ dashboardData.revenuePending }}</span>
                        <span class="dashboard-card-label">待结算分账</span>
                    </div>
                </div>
                <div class="dashboard-card danger">
                    <span class="dashboard-card-icon failed-icon">⚠</span>
                    <div class="dashboard-card-body">
                        <span class="dashboard-card-value">{{ dashboardData.revenueFailed }}</span>
                        <span class="dashboard-card-label">失败待处理分账</span>
                    </div>
                </div>
            </div>
        </section>

        <KeepAlive v-else>
            <component :is="activeTabComponent" :key="activeTab" />
        </KeepAlive>
    </div>
</template>

<style scoped src="@/styles/views/admin/AdminGrowthView.part-1.css"></style>
<style scoped src="@/styles/views/admin/AdminGrowthView.part-2.css"></style>
<style scoped src="@/styles/views/admin/AdminGrowthView.part-3.css"></style>
<style src="@/styles/views/admin/AdminGrowthView.global-2.css"></style>
