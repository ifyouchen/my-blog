<script setup>
import {computed, inject, reactive, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
    getAdminReportDetailApi,
    getAdminReportsApi,
    resolveAdminReportApi
} from '@/api/admin';
import {
    createPagedState,
    formatAdminDateTime,
    readPositiveInt,
    readQueryText,
    resolveAdminOverflowPage,
    syncAdminQuery,
    useAdminRefresh
} from '@/views/admin/adminShared';
import {useConfirmDialog} from '@/composables/useConfirmDialog';

const route = useRoute();
const router = useRouter();
const toast = inject('toast', { success: () => {}, error: () => {} });
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const state = reactive({
    ...createPagedState(),
    status: 'PENDING',
    targetType: '',
    reasonType: '',
    selectedReport: null,
    resolveTargetReport: null,
    detailLoading: false,
    detailError: '',
    actionLoadingId: null,
    resolveAction: 'REJECT',
    handleNote: ''
});

const targetTypeLabels = {
    ARTICLE: '文章',
    COMMENT: '评论',
    USER: '用户'
};

const reasonTypeLabels = {
    SPAM: '垃圾内容',
    INFRINGEMENT: '侵权',
    ABUSE: '辱骂骚扰',
    ILLEGAL: '违法违规',
    OTHER: '其他'
};

const statusLabels = {
    PENDING: '待处理',
    RESOLVED: '已处理',
    REJECTED: '已驳回'
};

const actionLabels = {
    REJECT: '驳回举报',
    OFFLINE_ARTICLE: '下架文章',
    DELETE_COMMENT: '删除评论',
    DISABLE_USER: '禁用用户'
};

const actionSuccessMessages = {
    REJECT: '举报已驳回',
    OFFLINE_ARTICLE: '文章已下架，举报已处理',
    DELETE_COMMENT: '评论已删除，举报已处理',
    DISABLE_USER: '用户已禁用，举报已处理'
};

const actionOptions = computed(() => {
    const report = state.resolveTargetReport || state.selectedReport;
    if (!report) {
        return [{ value: 'REJECT', label: actionLabels.REJECT }];
    }
    if (report.targetType === 'ARTICLE') {
        return [
            { value: 'REJECT', label: actionLabels.REJECT },
            { value: 'OFFLINE_ARTICLE', label: actionLabels.OFFLINE_ARTICLE }
        ];
    }
    if (report.targetType === 'COMMENT') {
        return [
            { value: 'REJECT', label: actionLabels.REJECT },
            { value: 'DELETE_COMMENT', label: actionLabels.DELETE_COMMENT }
        ];
    }
    return [
        { value: 'REJECT', label: actionLabels.REJECT },
        { value: 'DISABLE_USER', label: actionLabels.DISABLE_USER }
    ];
});

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.status = readQueryText(route, 'status', 'PENDING');
    state.targetType = readQueryText(route, 'targetType');
    state.reasonType = readQueryText(route, 'reasonType');
    state.jumpPage = String(state.page);
};

const loadReports = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminReportsApi(state.page, state.pageSize, {
            status: state.status || null,
            targetType: state.targetType || null,
            reasonType: state.reasonType || null
        });
        const overflowPage = resolveAdminOverflowPage(state, result);
        if (overflowPage) {
            state.page = overflowPage;
            state.jumpPage = String(overflowPage);
            await syncQuery({
                page: overflowPage > 1 ? String(overflowPage) : undefined
            });
            return;
        }
        state.items = result.items || [];
        state.total = result.total || 0;
        state.page = result.page || state.page;
        state.jumpPage = String(state.page);
    } catch (error) {
        state.error = error.message || '举报列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        status: patch.status ?? (state.status || undefined),
        targetType: patch.targetType ?? (state.targetType || undefined),
        reasonType: patch.reasonType ?? (state.reasonType || undefined)
    });
};

const submitFilters = async () => {
    state.page = 1;
    await syncQuery({
        page: undefined,
        status: state.status || undefined,
        targetType: state.targetType || undefined,
        reasonType: state.reasonType || undefined
    });
};

const resetFilters = async () => {
    state.status = 'PENDING';
    state.targetType = '';
    state.reasonType = '';
    state.page = 1;
    await syncQuery({ page: undefined, status: 'PENDING', targetType: undefined, reasonType: undefined });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({
        page: targetPage > 1 ? String(targetPage) : undefined
    });
};

const openDetail = async (report) => {
    state.selectedReport = report;
    state.detailLoading = true;
    state.detailError = '';
    try {
        state.selectedReport = await getAdminReportDetailApi(report.id);
    } catch (error) {
        state.detailError = error.message || '举报详情加载失败';
    } finally {
        state.detailLoading = false;
    }
};

const closeDetail = () => {
    state.selectedReport = null;
    state.detailError = '';
};

const openResolveDialog = (report, defaultAction = 'REJECT') => {
    state.resolveTargetReport = report;
    state.resolveAction = defaultAction;
    state.handleNote = '';
    openConfirmDialog({
        eyebrow: '举报处理',
        title: actionLabels[defaultAction] || '处理举报',
        message: `处理举报 #${report.id}`,
        confirmText: '确认处理',
        tone: defaultAction === 'REJECT' ? 'primary' : 'warning',
        onConfirm: async () => {
            state.actionLoadingId = report.id;
            try {
                const result = await resolveAdminReportApi(report.id, {
                    action: state.resolveAction,
                    handleNote: state.handleNote.trim()
                });
                if (state.selectedReport && String(state.selectedReport.id) === String(result.id)) {
                    state.selectedReport = result;
                }
                await loadReports();
                state.resolveTargetReport = null;
            } catch (error) {
                toast.error(error.message || '举报处理失败');
                throw error;
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

const closeResolveDialog = () => {
    closeConfirmDialog();
    if (!confirmDialog.loading) {
        state.resolveTargetReport = null;
    }
};

const targetRoute = (report) => {
    if (report.targetType === 'ARTICLE') {
        return `/articles/${report.targetId}`;
    }
    if (report.targetType === 'USER') {
        return `/users/${report.targetId}`;
    }
    return '';
};

const formatTarget = (report) => {
    return `${targetTypeLabels[report.targetType] || report.targetType} #${report.targetId}`;
};

const targetSummaryRows = computed(() => {
    const summary = state.selectedReport?.targetSummary;
    if (!summary) {
        return [];
    }
    const rows = [];
    if (summary.available === false) {
        rows.push(['状态', '目标已不可见或已删除']);
        return rows;
    }
    Object.keys(summary).forEach((key) => {
        if (key === 'available' || key === 'type' || key === 'id') {
            return;
        }
        rows.push([key, summary[key] || '-']);
    });
    return rows;
});

useAdminRefresh(loadReports);

watch(
    () => [route.query.page, route.query.status, route.query.targetType, route.query.reasonType],
    () => {
        applyRouteState();
        loadReports();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel">
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar" @submit.prevent="submitFilters">
                <label>
                    <span>处理状态</span>
                    <select v-model="state.status">
                        <option value="">全部</option>
                        <option value="PENDING">待处理</option>
                        <option value="RESOLVED">已处理</option>
                        <option value="REJECTED">已驳回</option>
                    </select>
                </label>
                <label>
                    <span>目标类型</span>
                    <select v-model="state.targetType">
                        <option value="">全部目标</option>
                        <option value="ARTICLE">文章</option>
                        <option value="COMMENT">评论</option>
                        <option value="USER">用户</option>
                    </select>
                </label>
                <label>
                    <span>举报原因</span>
                    <select v-model="state.reasonType">
                        <option value="">全部原因</option>
                        <option value="SPAM">垃圾内容</option>
                        <option value="INFRINGEMENT">侵权</option>
                        <option value="ABUSE">辱骂骚扰</option>
                        <option value="ILLEGAL">违法违规</option>
                        <option value="OTHER">其他</option>
                    </select>
                </label>
                <div class="admin-filter-actions">
                    <button type="submit">查询</button>
                    <button type="button" @click="resetFilters">重置</button>
                </div>
            </form>
        </div>

        <div class="admin-table-shell">
            <p v-if="state.error && state.items.length" class="backend-state-text error-text subtle">
                {{ state.error }}
            </p>
            <p v-else-if="state.error && !state.items.length" class="backend-state-text error-text">
                {{ state.error }}
            </p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-reports-table">
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>状态</th>
                                <th>举报目标</th>
                                <th>原因</th>
                                <th>举报人</th>
                                <th>提交时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="report in state.items" :key="report.id">
                                <td>#{{ report.id }}</td>
                                <td>
                                    <span
                                        class="status-pill"
                                        :class="{ warning: report.status === 'PENDING', danger: report.status === 'REJECTED' }"
                                    >
                                        {{ statusLabels[report.status] || report.status }}
                                    </span>
                                </td>
                                <td>
                                    <RouterLink v-if="targetRoute(report)" :to="targetRoute(report)">
                                        {{ formatTarget(report) }}
                                    </RouterLink>
                                    <span v-else>{{ formatTarget(report) }}</span>
                                </td>
                                <td>
                                    <strong>{{ reasonTypeLabels[report.reasonType] || report.reasonType }}</strong>
                                    <p class="admin-subtext">{{ report.reasonDetail || '无补充说明' }}</p>
                                </td>
                                <td>
                                    <strong>{{ report.reporterNickname || report.reporterUsername || `#${report.reporterUserId}` }}</strong>
                                    <p class="admin-subtext">@{{ report.reporterUsername || report.reporterUserId }}</p>
                                </td>
                                <td>{{ formatAdminDateTime(report.createdAt) }}</td>
                                <td class="table-actions">
                                    <button type="button" @click="openDetail(report)">详情</button>
                                    <button
                                        v-if="report.status === 'PENDING'"
                                        type="button"
                                        :disabled="state.actionLoadingId === report.id"
                                        @click="openResolveDialog(report, 'REJECT')"
                                    >
                                        驳回
                                    </button>
                                    <button
                                        v-if="report.status === 'PENDING' && report.targetType === 'ARTICLE'"
                                        type="button"
                                        :disabled="state.actionLoadingId === report.id"
                                        @click="openResolveDialog(report, 'OFFLINE_ARTICLE')"
                                    >
                                        下架
                                    </button>
                                    <button
                                        v-if="report.status === 'PENDING' && report.targetType === 'COMMENT'"
                                        type="button"
                                        class="danger-link"
                                        :disabled="state.actionLoadingId === report.id"
                                        @click="openResolveDialog(report, 'DELETE_COMMENT')"
                                    >
                                        删除
                                    </button>
                                    <button
                                        v-if="report.status === 'PENDING' && report.targetType === 'USER'"
                                        type="button"
                                        class="danger-link"
                                        :disabled="state.actionLoadingId === report.id"
                                        @click="openResolveDialog(report, 'DISABLE_USER')"
                                    >
                                        禁用
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有举报</p>
            </template>
        </div>

        <AdminPagination :state="state" label="举报分页" @page-change="changePage" />

        <Teleport to="body">
            <div v-if="state.selectedReport" class="report-drawer-overlay" @click.self="closeDetail">
                <aside class="report-drawer" data-testid="admin-report-detail">
                    <button type="button" class="report-drawer-close" aria-label="关闭详情" @click="closeDetail">×</button>
                    <p class="eyebrow">举报详情</p>
                    <h2>#{{ state.selectedReport.id }}</h2>
                    <p v-if="state.detailLoading" class="backend-state-text">详情加载中...</p>
                    <p v-else-if="state.detailError" class="backend-state-text error-text">{{ state.detailError }}</p>
                    <template v-else>
                        <dl class="report-detail-list">
                            <div>
                                <dt>目标</dt>
                                <dd>{{ formatTarget(state.selectedReport) }}</dd>
                            </div>
                            <div>
                                <dt>状态</dt>
                                <dd>{{ statusLabels[state.selectedReport.status] || state.selectedReport.status }}</dd>
                            </div>
                            <div>
                                <dt>原因</dt>
                                <dd>{{ reasonTypeLabels[state.selectedReport.reasonType] || state.selectedReport.reasonType }}</dd>
                            </div>
                            <div>
                                <dt>说明</dt>
                                <dd>{{ state.selectedReport.reasonDetail || '无补充说明' }}</dd>
                            </div>
                            <div>
                                <dt>举报人</dt>
                                <dd>{{ state.selectedReport.reporterNickname || state.selectedReport.reporterUsername || `#${state.selectedReport.reporterUserId}` }}</dd>
                            </div>
                            <div v-if="state.selectedReport.handlerUserId">
                                <dt>处理人</dt>
                                <dd>{{ state.selectedReport.handlerNickname || state.selectedReport.handlerUsername || `#${state.selectedReport.handlerUserId}` }}</dd>
                            </div>
                            <div v-if="state.selectedReport.handleNote">
                                <dt>处理备注</dt>
                                <dd>{{ state.selectedReport.handleNote }}</dd>
                            </div>
                        </dl>

                        <section class="report-target-summary">
                            <h3>目标摘要</h3>
                            <dl class="report-detail-list compact">
                                <div v-for="row in targetSummaryRows" :key="row[0]">
                                    <dt>{{ row[0] }}</dt>
                                    <dd>{{ row[1] }}</dd>
                                </div>
                            </dl>
                        </section>

                        <div v-if="state.selectedReport.status === 'PENDING'" class="report-drawer-actions">
                            <button type="button" @click="openResolveDialog(state.selectedReport, 'REJECT')">驳回</button>
                            <button
                                v-if="state.selectedReport.targetType === 'ARTICLE'"
                                type="button"
                                @click="openResolveDialog(state.selectedReport, 'OFFLINE_ARTICLE')"
                            >
                                下架文章
                            </button>
                            <button
                                v-if="state.selectedReport.targetType === 'COMMENT'"
                                type="button"
                                class="danger-link"
                                @click="openResolveDialog(state.selectedReport, 'DELETE_COMMENT')"
                            >
                                删除评论
                            </button>
                            <button
                                v-if="state.selectedReport.targetType === 'USER'"
                                type="button"
                                class="danger-link"
                                @click="openResolveDialog(state.selectedReport, 'DISABLE_USER')"
                            >
                                禁用用户
                            </button>
                        </div>
                    </template>
                </aside>
            </div>
        </Teleport>

        <ConfirmDialog
            :visible="confirmDialog.visible"
            :eyebrow="confirmDialog.eyebrow"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :confirm-text="confirmDialog.confirmText"
            :tone="confirmDialog.tone"
            :loading="confirmDialog.loading"
            @close="closeResolveDialog"
            @confirm="executeConfirmDialog"
        >
            <div class="report-resolve-form">
                <label>
                    <span>处理动作</span>
                    <select v-model="state.resolveAction" :disabled="confirmDialog.loading">
                        <option v-for="option in actionOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                        </option>
                    </select>
                </label>
                <label>
                    <span>处理备注</span>
                    <textarea
                        v-model="state.handleNote"
                        rows="3"
                        maxlength="500"
                        :disabled="confirmDialog.loading"
                        placeholder="填写处理依据或说明"
                    ></textarea>
                </label>
            </div>
        </ConfirmDialog>
    </section>
</template>

<style scoped>
.report-drawer-overlay {
    position: fixed;
    inset: 0;
    z-index: 1150;
    display: flex;
    justify-content: flex-end;
    background: rgba(15, 23, 42, 0.28);
}

.report-drawer {
    position: relative;
    display: grid;
    align-content: start;
    gap: 18px;
    width: min(520px, 100%);
    height: 100%;
    padding: 28px;
    overflow-y: auto;
    background: var(--surface);
    border-left: 1px solid var(--line);
    box-shadow: var(--shadow-md);
}

.report-drawer-close {
    position: absolute;
    top: 14px;
    right: 14px;
    width: 30px;
    height: 30px;
    color: var(--muted);
    font-size: 20px;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.report-drawer h2,
.report-target-summary h3 {
    margin: 0;
    color: var(--text);
}

.report-detail-list {
    display: grid;
    gap: 12px;
    margin: 0;
}

.report-detail-list div {
    display: grid;
    gap: 4px;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--line);
}

.report-detail-list dt {
    color: var(--muted);
    font-size: 12px;
    font-weight: 700;
}

.report-detail-list dd {
    margin: 0;
    color: var(--text);
    line-height: 1.6;
    overflow-wrap: anywhere;
}

.report-detail-list.compact {
    margin-top: 10px;
}

.report-drawer-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.report-drawer-actions button {
    min-height: 34px;
    padding: 0 12px;
    color: var(--text);
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.report-resolve-form {
    display: grid;
    gap: 12px;
    margin-top: 18px;
}

.report-resolve-form label {
    display: grid;
    gap: 8px;
}

.report-resolve-form span {
    color: var(--muted);
    font-size: 13px;
    font-weight: 700;
}

.report-resolve-form select,
.report-resolve-form textarea {
    width: 100%;
    padding: 10px 12px;
    color: var(--text);
    font: inherit;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.report-resolve-form textarea {
    resize: vertical;
}

@media (max-width: 640px) {
    .report-drawer {
        padding: 24px 18px;
    }
}
</style>
