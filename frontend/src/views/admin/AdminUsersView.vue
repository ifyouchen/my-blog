<script setup>
import { reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import { exportAdminUsersApi, getAdminUsersApi, updateAdminUserStatusApi } from '@/api/admin';
import {
    createPagedState,
    readPositiveInt,
    readQueryText,
    resolveAdminOverflowPage,
    syncAdminQuery,
    useAdminRefresh
} from '@/views/admin/adminShared';
import { useConfirmDialog } from '@/composables/useConfirmDialog';

const route = useRoute();
const router = useRouter();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const state = reactive({
    ...createPagedState(),
    status: '',
    keyword: '',
    feedback: '',
    feedbackType: 'success',
    actionLoadingId: null
});

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.status = readQueryText(route, 'status');
    state.keyword = readQueryText(route, 'keyword');
    state.jumpPage = String(state.page);
};

const loadUsers = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminUsersApi(
            state.page,
            state.pageSize,
            state.status || null,
            state.keyword || null
        );
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
        state.error = error.message || '用户列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        status: patch.status ?? (state.status || undefined),
        keyword: patch.keyword ?? (state.keyword || undefined)
    });
};

const submitFilters = async () => {
    state.page = 1;
    await syncQuery({
        page: undefined,
        status: state.status || undefined,
        keyword: state.keyword || undefined
    });
};

const resetFilters = async () => {
    state.status = '';
    state.keyword = '';
    state.page = 1;
    await syncQuery({
        page: undefined,
        status: undefined,
        keyword: undefined
    });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({
        page: targetPage > 1 ? String(targetPage) : undefined
    });
};

const toggleUserStatus = async (user) => {
    const nextStatus = user.status === 'NORMAL' ? 'DISABLED' : 'NORMAL';
    openConfirmDialog({
        eyebrow: '用户状态确认',
        title: nextStatus === 'DISABLED' ? '禁用用户' : '启用用户',
        message: nextStatus === 'DISABLED'
            ? `确定禁用用户 ${user.username} 吗？禁用后该账号将无法正常使用站内能力。`
            : `确定启用用户 ${user.username} 吗？启用后该账号会恢复正常访问权限。`,
        confirmText: nextStatus === 'DISABLED' ? '确认禁用' : '确认启用',
        tone: nextStatus === 'DISABLED' ? 'warning' : 'primary',
        onConfirm: async () => {
            state.actionLoadingId = user.id;
            try {
                await updateAdminUserStatusApi(user.id, nextStatus);
                state.feedback = nextStatus === 'DISABLED' ? '用户已禁用' : '用户已启用';
                state.feedbackType = 'success';
                await loadUsers();
            } catch (error) {
                state.feedback = error.message || '用户状态更新失败';
                state.feedbackType = 'error';
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

useAdminRefresh(loadUsers);

watch(
    () => [route.query.page, route.query.status, route.query.keyword],
    () => {
        applyRouteState();
        loadUsers();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel">
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar" @submit.prevent="submitFilters">
                <label>
                    <span>用户状态</span>
                    <select v-model="state.status">
                        <option value="">全部</option>
                        <option value="NORMAL">正常</option>
                        <option value="DISABLED">禁用</option>
                    </select>
                </label>
                <label class="admin-filter-grow">
                    <span>关键词</span>
                    <input v-model.trim="state.keyword" type="text" placeholder="搜索用户名、昵称或邮箱">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit">查询</button>
                    <button type="button" @click="resetFilters">重置</button>
                    <button type="button" class="btn-export" @click="exportAdminUsersApi" title="导出所有用户为 CSV">导出 CSV</button>
                </div>
            </form>
            <p v-if="state.feedback" :class="['backend-state-text', state.feedbackType === 'error' ? 'error-text' : 'success-text']">
                {{ state.feedback }}
            </p>
        </div>

        <div class="admin-table-shell">
            <p v-if="state.loading && state.items.length" class="backend-state-text subtle">
                正在更新用户数据...
            </p>
            <p v-if="state.error && state.items.length" class="backend-state-text error-text subtle">
                {{ state.error }}
            </p>
            <p v-if="state.loading && !state.items.length" class="backend-state-text">
                用户数据加载中...
            </p>
            <p v-else-if="state.error && !state.items.length" class="backend-state-text error-text">
                {{ state.error }}
            </p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-users-table">
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>用户</th>
                                <th>邮箱</th>
                                <th>角色</th>
                                <th>状态</th>
                                <th>注册时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="user in state.items" :key="user.id">
                                <td>#{{ user.id }}</td>
                                <td>
                                    <strong>{{ user.nickname || user.username }}</strong>
                                    <p class="admin-subtext">@{{ user.username }}</p>
                                </td>
                                <td>{{ user.email }}</td>
                                <td>{{ user.role }}</td>
                                <td>
                                    <span class="status-pill" :class="{ danger: user.status !== 'NORMAL' }">
                                        {{ user.status === 'NORMAL' ? '正常' : '禁用' }}
                                    </span>
                                </td>
                                <td>{{ user.createdAt }}</td>
                                <td class="table-actions">
                                    <button type="button" :disabled="state.actionLoadingId === user.id" @click="toggleUserStatus(user)">
                                        {{ state.actionLoadingId === user.id ? '处理中...' : (user.status === 'NORMAL' ? '禁用' : '启用') }}
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有用户</p>
            </template>
        </div>

        <AdminPagination :state="state" label="用户分页" @page-change="changePage" />
        <ConfirmDialog
            :visible="confirmDialog.visible"
            :eyebrow="confirmDialog.eyebrow"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :confirm-text="confirmDialog.confirmText"
            :tone="confirmDialog.tone"
            :loading="confirmDialog.loading"
            @close="closeConfirmDialog"
            @confirm="executeConfirmDialog"
        />
    </section>
</template>
