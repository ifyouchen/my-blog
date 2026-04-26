<script setup>
import { reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AdminLogDrawer from '@/components/admin/AdminLogDrawer.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import { getAdminLogsApi } from '@/api/admin';
import {
    createPagedState,
    readPositiveInt,
    readQueryText,
    resolveAdminOverflowPage,
    syncAdminQuery,
    useAdminRefresh
} from '@/views/admin/adminShared';

const route = useRoute();
const router = useRouter();

const state = reactive({
    ...createPagedState(),
    actionType: '',
    resultStatus: '',
    dateFrom: '',
    dateTo: '',
    feedback: '',
    feedbackType: 'success'
});

const selectedLog = ref(null);
const drawerOpen = ref(false);

const actionOptions = [
    { value: '', label: '全部操作' },
    { value: 'UPDATE_USER_STATUS', label: '更新用户状态' },
    { value: 'UPDATE_ARTICLE_STATUS', label: '更新文章状态' },
    { value: 'DELETE_COMMENT', label: '删除评论' },
    { value: 'CREATE_CATEGORY', label: '创建分类' },
    { value: 'UPDATE_CATEGORY', label: '更新分类' },
    { value: 'DELETE_CATEGORY', label: '删除分类' },
    { value: 'CREATE_TAG', label: '创建标签' },
    { value: 'UPDATE_TAG', label: '更新标签' },
    { value: 'DELETE_TAG', label: '删除标签' },
    { value: 'DELETE_ARTICLE', label: '删除文章' }
];

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.actionType = readQueryText(route, 'actionType');
    state.resultStatus = readQueryText(route, 'resultStatus');
    state.dateFrom = readQueryText(route, 'dateFrom');
    state.dateTo = readQueryText(route, 'dateTo');
    state.jumpPage = String(state.page);
};

const loadLogs = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminLogsApi(state.page, state.pageSize, {
            actionType: state.actionType || null,
            resultStatus: state.resultStatus || null,
            dateFrom: state.dateFrom || null,
            dateTo: state.dateTo || null
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
        state.items = [];
        state.total = 0;
        state.error = error.message || '日志列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        actionType: patch.actionType ?? (state.actionType || undefined),
        resultStatus: patch.resultStatus ?? (state.resultStatus || undefined),
        dateFrom: patch.dateFrom ?? (state.dateFrom || undefined),
        dateTo: patch.dateTo ?? (state.dateTo || undefined)
    });
};

const submitFilters = async () => {
    if (state.dateFrom && state.dateTo && state.dateFrom > state.dateTo) {
        state.feedback = '开始日期不能晚于结束日期';
        state.feedbackType = 'error';
        return;
    }
    state.feedback = '';
    state.page = 1;
    await syncQuery({
        page: undefined,
        actionType: state.actionType || undefined,
        resultStatus: state.resultStatus || undefined,
        dateFrom: state.dateFrom || undefined,
        dateTo: state.dateTo || undefined
    });
};

const resetFilters = async () => {
    state.actionType = '';
    state.resultStatus = '';
    state.dateFrom = '';
    state.dateTo = '';
    state.feedback = '';
    state.page = 1;
    await syncQuery({
        page: undefined,
        actionType: undefined,
        resultStatus: undefined,
        dateFrom: undefined,
        dateTo: undefined
    });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({
        page: targetPage > 1 ? String(targetPage) : undefined
    });
};

const openDrawer = (log) => {
    selectedLog.value = log;
    drawerOpen.value = true;
};

const closeDrawer = () => {
    drawerOpen.value = false;
    selectedLog.value = null;
};

useAdminRefresh(loadLogs);

watch(
    () => [route.query.page, route.query.actionType, route.query.resultStatus, route.query.dateFrom, route.query.dateTo],
    () => {
        applyRouteState();
        loadLogs();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel">
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar" @submit.prevent="submitFilters">
                <label>
                    <span>操作类型</span>
                    <select v-model="state.actionType">
                        <option
                            v-for="option in actionOptions"
                            :key="option.value || 'all'"
                            :value="option.value"
                        >
                            {{ option.label }}
                        </option>
                    </select>
                </label>
                <label>
                    <span>结果状态</span>
                    <select v-model="state.resultStatus">
                        <option value="">全部</option>
                        <option value="SUCCESS">成功</option>
                        <option value="FAILED">失败</option>
                    </select>
                </label>
                <label>
                    <span>开始日期</span>
                    <input v-model="state.dateFrom" type="date">
                </label>
                <label>
                    <span>结束日期</span>
                    <input v-model="state.dateTo" type="date">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit">查询</button>
                    <button type="button" @click="resetFilters">重置</button>
                </div>
            </form>
            <p v-if="state.feedback" :class="['backend-state-text', state.feedbackType === 'error' ? 'error-text' : 'success-text']">
                {{ state.feedback }}
            </p>
        </div>

        <div class="admin-table-shell">
            <p v-if="state.loading" class="backend-state-text">日志数据加载中...</p>
            <p v-else-if="state.error" class="backend-state-text error-text">{{ state.error }}</p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-logs-table">
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>管理员</th>
                                <th>操作</th>
                                <th>请求</th>
                                <th>结果</th>
                                <th>目标</th>
                                <th>时间</th>
                                <th>详情</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="log in state.items" :key="log.id">
                                <td>#{{ log.id }}</td>
                                <td>{{ log.adminUsername || `#${log.adminUserId}` }}</td>
                                <td>{{ log.operation }}</td>
                                <td>{{ log.requestMethod || '-' }} {{ log.requestUri || '-' }}</td>
                                <td>
                                    <span class="status-pill" :class="{ danger: log.resultStatus !== 'SUCCESS' }">
                                        {{ log.resultStatus || '-' }}
                                    </span>
                                </td>
                                <td>{{ log.targetType }} #{{ log.targetId ?? '-' }}</td>
                                <td>{{ log.createdAt }}</td>
                                <td class="table-actions">
                                    <button type="button" @click="openDrawer(log)">查看</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有日志</p>
            </template>
        </div>

        <AdminPagination :state="state" label="日志分页" @page-change="changePage" />
        <AdminLogDrawer :open="drawerOpen" :log="selectedLog" @close="closeDrawer" />
    </section>
</template>
