<script setup>
import {onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import GrowthRuleTab from '@/views/admin/GrowthRuleTab.vue';
import LevelThresholdTab from '@/views/admin/LevelThresholdTab.vue';
import LevelRewardTab from '@/views/admin/LevelRewardTab.vue';
import PointRuleTab from '@/views/admin/PointRuleTab.vue';
import PointAccountTab from '@/views/admin/PointAccountTab.vue';
import RevenueShareTab from '@/views/admin/RevenueShareTab.vue';
import RewardGrantLogTab from '@/views/admin/RewardGrantLogTab.vue';
import SignInRewardTab from '@/views/admin/SignInRewardTab.vue';
import {getAdminGrowthRulesApi, getAdminPointRulesApi, getAdminRevenueSharesApi} from '@/api/growth';

const route = useRoute();
const router = useRouter();

const TABS = [
    {key: 'dashboard', label: '概览'},
    {key: 'rules', label: '经验规则'},
    {key: 'thresholds', label: '等级配置'},
    {key: 'level-rewards', label: '等级奖励'},
    {key: 'sign-in-rewards', label: '签到奖励'},
    {key: 'point-rules', label: '积分规则'},
    {key: 'points', label: '积分账户'},
    {key: 'reward-logs', label: '奖励记录'},
    {key: 'revenue', label: '分账流水'},
];

const activeTab = ref('dashboard');

watch(() => route.query.tab, (tab) => {
    if (tab && TABS.some(t => t.key === tab)) {
        activeTab.value = tab;
    }
}, {immediate: true});

const switchTab = (key) => {
    activeTab.value = key;
    router.replace({query: {...route.query, tab: key}});
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

        <!-- ==================== 经验规则 ==================== -->
        <GrowthRuleTab v-if="activeTab === 'rules'" />

        <!-- ==================== 等级阈值 ==================== -->
        <LevelThresholdTab v-if="activeTab === 'thresholds'" />

        <!-- ==================== 等级奖励 ==================== -->
        <LevelRewardTab v-if="activeTab === 'level-rewards'" />

        <!-- ==================== 签到奖励 ==================== -->
        <SignInRewardTab v-if="activeTab === 'sign-in-rewards'" />

        <!-- ==================== 积分规则 ==================== -->
        <PointRuleTab v-if="activeTab === 'point-rules'" />

        <!-- ==================== 积分账户 ==================== -->
        <PointAccountTab v-if="activeTab === 'points'" />

        <!-- ==================== 奖励记录 ==================== -->
        <RewardGrantLogTab v-if="activeTab === 'reward-logs'" />

        <!-- ==================== 分账流水 ==================== -->
        <RevenueShareTab v-if="activeTab === 'revenue'" />
    </div>
</template>

<style scoped>
.admin-growth {
    display: flex;
    flex-direction: column;
    gap: 0;
    min-width: 0;
    max-width: 100%;
}

/* ── Tabs ── */
.ag-tabs {
    display: flex;
    gap: 0;
    min-width: 0;
    max-width: 100%;
    border-bottom: 2px solid #e5e7eb;
    margin-bottom: 24px;
    background: #fff;
    position: sticky;
    top: 0;
    z-index: 10;
}

.ag-tab {
    padding: 12px 20px;
    font-size: 14px;
    font-weight: 600;
    color: #6b7280;
    background: none;
    border: none;
    border-bottom: 2px solid transparent;
    margin-bottom: -2px;
    cursor: pointer;
    transition: all 0.15s;
    white-space: nowrap;
}

.ag-tab:hover {
    color: #374151;
    background: #f9fafb;
}

.ag-tab.active {
    color: #6366f1;
    border-bottom-color: #6366f1;
}

/* ── Dashboard ── */
.dashboard-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
    min-width: 0;
}

.dashboard-card {
    display: flex;
    align-items: center;
    gap: 16px;
    min-width: 0;
    padding: 20px;
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    transition: box-shadow 0.15s;
}

.dashboard-card:hover {
    box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.dashboard-card-icon {
    font-size: 28px;
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    flex-shrink: 0;
}

.rules-icon {
    background: #ecfdf5;
    color: #059669;
}
.point-icon {
    background: #eff6ff;
    color: #2563eb;
}
.pending-icon {
    background: #fff7ed;
    color: #c2410c;
}
.failed-icon {
    background: #fef2f2;
    color: #dc2626;
}

.dashboard-card-body {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
}

.dashboard-card-value {
    font-size: 24px;
    font-weight: 700;
    color: #111827;
    line-height: 1.2;
}

.dashboard-card.warning .dashboard-card-value {
    color: #c2410c;
}

.dashboard-card.danger .dashboard-card-value {
    color: #dc2626;
}

.dashboard-card-label {
    font-size: 12px;
    color: #6b7280;
    overflow-wrap: anywhere;
}

/* ── Sections (shared by tab components) ── */
:deep(.ag-section) {
    min-width: 0;
    max-width: 100%;
    overflow: hidden;
    background: var(--admin-surface, #fff);
    border: 1px solid var(--admin-line, #e5e7eb);
    border-radius: 10px;
    padding: 24px;
}

:deep(.ag-section-title) {
    font-size: 16px;
    font-weight: 700;
    color: var(--admin-text, #111827);
    margin: 0 0 16px;
}

:deep(.ag-section-head) {
    display: flex;
    align-items: baseline;
    gap: 10px;
    margin-bottom: 16px;
}

:deep(.ag-section-head .ag-section-title) {
    margin-bottom: 0;
}

:deep(.ag-section-subtitle) {
    font-size: 12px;
    color: #6b7280;
}

:deep(.ag-hint) {
    font-size: 13px;
    color: var(--admin-muted, #6b7280);
    margin: 0 0 16px;
    line-height: 1.6;
}

:deep(.rule-config-grid) {
    display: grid;
    grid-template-columns: minmax(260px, 360px) minmax(0, 1fr);
    gap: 18px;
    align-items: start;
    min-width: 0;
}

:deep(.rule-form) {
    max-width: none;
    padding: 16px;
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
}

:deep(.compact-row) {
    max-width: 180px;
}

:deep(.ag-toggle) {
    display: inline-flex;
    gap: 8px;
    align-items: center;
    width: fit-content;
    color: #374151;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
}

:deep(.ag-toggle input) {
    width: 16px;
    height: 16px;
    accent-color: #6366f1;
}

:deep(.ag-action-row) {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
}

:deep(.rules-table td strong) {
    display: block;
    color: #111827;
    font-size: 13px;
}

:deep(.rules-table td small) {
    display: block;
    margin-top: 3px;
    color: #9ca3af;
    font-size: 11px;
}

:deep(.action-cell) {
    display: flex;
    gap: 6px;
    align-items: center;
}

:deep(.threshold-actions) {
    margin-top: 14px;
}

:deep(.cell-input) {
    width: 110px;
    min-width: 0;
}

:deep(.cell-input.wide) {
    width: min(280px, 32vw);
}

:deep(.ag-query-row) {
    display: flex;
    gap: 10px;
    align-items: center;
}

:deep(.ag-input) {
    flex: 1;
    height: 36px;
    padding: 0 10px;
    border: 1px solid var(--admin-line, #d1d5db);
    border-radius: 6px;
    font-size: 14px;
    color: var(--admin-text, #111827);
    background: #fff;
    outline: none;
    transition: border-color 0.15s;
}

:deep(.ag-input:focus) { border-color: #6366f1; }

:deep(.ag-btn) {
    height: 36px;
    padding: 0 18px;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    border: none;
    transition: opacity 0.15s;
    white-space: nowrap;
}

:deep(.ag-btn.primary) { background: #6366f1; color: #fff; }
:deep(.ag-btn.secondary) { background: #f3f4f6; color: #374151; border: 1px solid #d1d5db; }
:deep(.ag-btn.danger) { background: #fef2f2; color: #dc2626; border: 1px solid #fca5a5; }
:deep(.ag-btn.small) { height: 32px; padding: 0 12px; font-size: 13px; }
:deep(.ag-btn:disabled) { opacity: 0.5; cursor: not-allowed; }
:deep(.ag-btn:not(:disabled):hover) { opacity: 0.88; }

:deep(.revenue-toolbar) {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
    margin-bottom: 16px;
}

:deep(.revenue-author-input) {
    max-width: 180px;
}

:deep(.revenue-status-select) {
    max-width: 160px;
}

:deep(.ag-result-card) {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 12px;
    min-width: 0;
    margin-top: 16px;
    padding: 16px;
    background: #f9fafb;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
}

:deep(.result-item) {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

:deep(.result-label) {
    font-size: 12px;
    color: #6b7280;
}

:deep(.result-val) {
    font-size: 15px;
    font-weight: 600;
    color: #111827;
}

:deep(.result-val.highlight) { color: #4f46e5; font-size: 18px; }

:deep(.ag-form) {
    display: flex;
    flex-direction: column;
    gap: 14px;
    max-width: 560px;
}

:deep(.form-row) {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

:deep(.form-label) {
    font-size: 13px;
    font-weight: 500;
    color: #374151;
}

:deep(.form-label em) {
    color: #ef4444;
    font-style: normal;
    margin-left: 2px;
}

:deep(.input-with-btn) {
    display: flex;
    gap: 8px;
    align-items: center;
}

:deep(.input-with-btn .ag-input) { flex: 1; }

:deep(.ag-error) {
    padding: 10px 14px;
    background: #fef2f2;
    border: 1px solid #fca5a5;
    border-radius: 6px;
    color: #dc2626;
    font-size: 13px;
}

:deep(.ag-success) {
    padding: 10px 14px;
    background: #f0fdf4;
    border: 1px solid #86efac;
    border-radius: 6px;
    color: #16a34a;
    font-size: 13px;
}

:deep(.submit-btn) { align-self: flex-start; margin-top: 4px; }

:deep(.ag-table) {
    width: 100%;
    max-width: 100%;
    border-collapse: collapse;
    font-size: 13px;
}

:deep(.ag-table-wrap) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow-x: auto;
    overscroll-behavior-x: contain;
}

:deep(.ag-table th) {
    text-align: left;
    padding: 8px 10px;
    font-size: 12px;
    font-weight: 600;
    color: #6b7280;
    border-bottom: 1px solid #e5e7eb;
}

:deep(.ag-table td) {
    padding: 10px 10px;
    border-bottom: 1px solid #f3f4f6;
    color: #374151;
    vertical-align: middle;
}

:deep(.ag-table tr:last-child td) { border-bottom: none; }
:deep(.ag-table tr:hover td) { background: #f9fafb; }
:deep(.ag-table tr.row-deleted td) { opacity: 0.55; text-decoration: line-through; }
:deep(.ag-table tr.row-deleted td.action-cell) { opacity: 1; text-decoration: none; }

:deep(.ag-table-empty) {
    padding: 28px 12px;
    text-align: center;
    color: #6b7280;
    font-size: 13px;
}

:deep(.revenue-table) {
    min-width: 1080px;
}

:deep(.delta) { font-weight: 700; }
:deep(.delta.plus) { color: #16a34a; }
:deep(.delta.minus) { color: #dc2626; }

:deep(.status-chip) {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
}

:deep(.status-chip.pending) { background: #fff7ed; color: #c2410c; }
:deep(.status-chip.settled) { background: #ecfdf5; color: #047857; }
:deep(.status-chip.failed) { background: #fef2f2; color: #b91c1c; }

:deep(.biz-no) {
    font-family: monospace;
    font-size: 11px;
    color: #6b7280;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

:deep(.error-cell) {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #9ca3af;
}

:deep(.time-cell) {
    min-width: 140px;
    color: #6b7280;
}

:deep(.time-cell span) {
    display: block;
    white-space: nowrap;
}

:deep(.ag-pagination) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin-top: 16px;
    color: #6b7280;
    font-size: 13px;
}

@media (max-width: 980px) {
    :deep(.rule-config-grid) {
        grid-template-columns: 1fr;
    }
    :deep(.compact-row) {
        max-width: none;
    }
}

@media (max-width: 640px) {
    .admin-growth {
        overflow-x: hidden;
    }

    .ag-tabs {
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
        scrollbar-width: none;
        gap: 0;
    }

    .ag-tabs::-webkit-scrollbar {
        display: none;
    }

    .ag-tab {
        padding: 10px 14px;
        font-size: 13px;
        flex-shrink: 0;
    }

    .dashboard-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
    }

    .dashboard-card {
        align-items: flex-start;
        gap: 10px;
        padding: 12px;
        border-radius: 8px;
    }

    .dashboard-card-icon {
        width: 38px;
        height: 38px;
        border-radius: 8px;
        font-size: 21px;
    }

    .dashboard-card-value {
        font-size: 20px;
    }

    .dashboard-card-label {
        line-height: 1.35;
    }

    :deep(.ag-section) {
        padding: 16px;
        border-radius: 8px;
    }

    :deep(.ag-section-head) {
        flex-direction: column;
        align-items: flex-start;
        gap: 4px;
    }

    :deep(.revenue-toolbar) {
        flex-direction: column;
        align-items: stretch;
    }

    :deep(.revenue-author-input),
    :deep(.revenue-status-select) {
        max-width: none;
        width: 100%;
    }

    :deep(.ag-query-row) {
        flex-direction: column;
        align-items: stretch;
    }

    :deep(.ag-query-row .ag-btn) {
        width: 100%;
    }

    :deep(.ag-input) {
        width: 100%;
    }

    :deep(.ag-form) {
        max-width: none;
    }

    :deep(.input-with-btn) {
        flex-direction: column;
        align-items: stretch;
    }

    :deep(.input-with-btn .ag-btn) {
        width: 100%;
    }

    :deep(.ag-action-row) {
        flex-direction: column;
        align-items: stretch;
    }

    :deep(.ag-action-row .ag-btn) {
        width: 100%;
    }

    :deep(.submit-btn) {
        align-self: stretch;
        width: 100%;
    }

    :deep(.ag-table th),
    :deep(.ag-table td) {
        padding: 6px 6px;
        font-size: 12px;
    }

    :deep(.ag-table-wrap) {
        margin: 0 -16px;
        width: calc(100% + 32px);
        padding: 0 2px;
    }

    :deep(.revenue-table) {
        min-width: 800px;
    }

    :deep(.ag-table) {
        font-size: 12px;
    }

    :deep(.template-btns) {
        flex-wrap: wrap;
        gap: 6px;
    }

    :deep(.template-btns .ag-btn) {
        font-size: 12px;
        padding: 4px 10px;
        height: 30px;
    }

    :deep(.ag-drawer) {
        width: 100vw !important;
        max-width: 100vw !important;
    }

    :deep(.rules-table) {
        min-width: 600px;
    }

    :deep(.threshold-table) {
        min-width: 500px;
    }

    :deep(.threshold-table input),
    :deep(.threshold-table .cell-input) {
        width: 100%;
        max-width: none;
        box-sizing: border-box;
    }

    :deep(.ag-hint) {
        font-size: 12px;
    }

    :deep(.result-item) {
        gap: 2px;
    }

    :deep(.ag-section) + :deep(.ag-section) {
        margin-top: 16px;
    }

    :deep(.ag-pagination) {
        flex-wrap: wrap;
        gap: 8px;
    }

    :deep(.ag-pagination button) {
        font-size: 12px;
        padding: 6px 12px;
    }

    :deep(.cell-input.wide) {
        width: 100%;
        max-width: none;
    }

    :deep(.ag-action-row) {
        flex-direction: column;
        align-items: stretch;
    }
}
</style>
