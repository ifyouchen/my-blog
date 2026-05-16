<script setup>
import { reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
    disableAdminUserApi,
    exportAdminUsersApi,
    getAdminUsersApi,
    recommendAdminUserApi,
    unrecommendAdminUserApi,
    updateAdminUserRoleApi,
    updateAdminUserStatusApi
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
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useToast } from '@/composables/useToast';
import { useSession } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const {state: sessionState} = useSession();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const ROLE_OPTIONS = [
    {value: 'USER', label: '普通用户'},
    {value: 'ADMIN', label: '管理员'},
];

const roleLabel = (role) => ROLE_OPTIONS.find((item) => item.value === role)?.label || role || '-';
const isCurrentUser = (user) => String(user.id) === String(sessionState.user?.id || '');

const state = reactive({
    ...createPagedState(),
    status: '',
    keyword: '',
    actionLoadingId: null,
    disableTargetId: null,
    disableReason: {}
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
    if (user.status === 'NORMAL') {
        openDisableDialog(user);
    } else {
        openConfirmDialog({
            eyebrow: '用户状态确认',
            title: '启用用户',
            message: `确定启用用户 ${user.username} 吗？启用后该账号会恢复正常访问权限。`,
            confirmText: '确认启用',
            tone: 'primary',
            onConfirm: async () => {
                state.actionLoadingId = user.id;
                try {
                    await updateAdminUserStatusApi(user.id, 'NORMAL');
                    await loadUsers();
                    toast.success('用户已启用');
                } catch (error) {
                    toast.error(error.message || '用户状态更新失败');
                } finally {
                    state.actionLoadingId = null;
                }
            }
        });
    }
};

const changeUserRole = (user, event) => {
    const nextRole = event.target.value;
    event.target.value = user.role;
    if (!nextRole || nextRole === user.role) return;
    if (isCurrentUser(user)) {
        toast.error('不能修改当前登录管理员自己的角色');
        return;
    }
    openConfirmDialog({
        eyebrow: '用户角色确认',
        title: '修改用户角色',
        message: `确定将用户 ${user.username} 的角色从「${roleLabel(user.role)}」改为「${roleLabel(nextRole)}」吗？`,
        confirmText: '确认修改',
        tone: 'primary',
        onConfirm: async () => {
            state.actionLoadingId = user.id;
            try {
                const result = await updateAdminUserRoleApi(user.id, nextRole);
                user.role = result?.role || nextRole;
                await loadUsers();
                toast.success('用户角色已更新');
            } catch (error) {
                toast.error(error.message || '用户角色更新失败');
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

const openDisableDialog = (user) => {
    state.disableTargetId = user.id;
    state.disableReason = { ...state.disableReason, [user.id]: '' };
    openConfirmDialog({
        eyebrow: '用户禁用确认',
        title: '禁用用户',
        message: `确定禁用用户 ${user.username} 吗？禁用后该账号将无法正常使用站内能力。`,
        confirmText: '确认禁用',
        tone: 'warning',
        onConfirm: async () => {
            state.actionLoadingId = user.id;
            try {
                const reason = state.disableReason[user.id] || '';
                await disableAdminUserApi(user.id, reason);
                await loadUsers();
                toast.success('用户已禁用');
            } catch (error) {
                toast.error(error.message || '用户禁用失败');
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

const exportUsers = async () => {
    try {
        await exportAdminUsersApi({
            status: state.status || undefined,
            keyword: state.keyword || undefined
        });
    } catch (error) {
        toast.error(error.message || '用户导出失败');
    }
};

const toggleRecommended = async (user) => {
    const newRecommended = !user.recommended;
    state.actionLoadingId = user.id;
    try {
        if (newRecommended) {
            await recommendAdminUserApi(user.id);
        } else {
            await unrecommendAdminUserApi(user.id);
        }
        user.recommended = newRecommended;
        toast.success(newRecommended ? '已推荐' : '已取消推荐');
    } catch (error) {
        toast.error(error.message || '推荐操作失败');
    } finally {
        state.actionLoadingId = null;
    }
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
                    <button type="button" class="btn-export" @click="exportUsers" title="导出所有用户为 CSV">导出 CSV</button>
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
                <div class="admin-table-wrap" data-testid="admin-users-table">
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>用户</th>
                                <th>邮箱</th>
                                <th>角色</th>
                                <th>状态</th>
                                <th>推荐</th>
                                <th>禁用原因</th>
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
                                <td>
                                    <div class="role-control">
                                        <span class="role-badge" :class="{admin: user.role === 'ADMIN'}">
                                            {{ roleLabel(user.role) }}
                                        </span>
                                        <select
                                            :value="user.role"
                                            :disabled="state.actionLoadingId === user.id || isCurrentUser(user)"
                                            :title="isCurrentUser(user) ? '不能修改当前登录管理员自己的角色' : '修改角色'"
                                            @change="changeUserRole(user, $event)"
                                        >
                                            <option
                                                v-for="option in ROLE_OPTIONS"
                                                :key="option.value"
                                                :value="option.value"
                                            >
                                                {{ option.label }}
                                            </option>
                                        </select>
                                    </div>
                                </td>
                                <td>
                                    <span class="status-pill" :class="{ danger: user.status !== 'NORMAL' }">
                                        {{ user.status === 'NORMAL' ? '正常' : '禁用' }}
                                    </span>
                                </td>
                                <td>
                                    <span v-if="user.recommended" class="status-pill">推荐</span>
                                    <span v-else class="admin-subtext">--</span>
                                </td>
                                <td>{{ user.disableReason || '-' }}</td>
                                <td>{{ formatAdminDateTime(user.createdAt) }}</td>
                                <td class="table-actions">
                                    <button type="button" :disabled="state.actionLoadingId === user.id" @click="toggleRecommended(user)">
                                        {{ user.recommended ? '取消推荐' : '推荐' }}
                                    </button>
                                    <button type="button" :disabled="state.actionLoadingId === user.id" @click="toggleUserStatus(user)">
                                        {{ user.status === 'NORMAL' ? '禁用' : '启用' }}
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
        >
            <div v-if="confirmDialog.title === '禁用用户' && confirmDialog.visible" class="disable-reason-field">
                <label>
                    <span>禁用原因（可选）</span>
                    <input
                        v-model="state.disableReason[state.disableTargetId]"
                        type="text"
                        placeholder="请输入禁用原因"
                        maxlength="200"
                    >
                </label>
            </div>
        </ConfirmDialog>
    </section>
</template>

<style scoped>
.disable-reason-field {
    margin-top: 16px;
}
.disable-reason-field label {
    display: flex;
    flex-direction: column;
    gap: 6px;
}
.disable-reason-field span {
    font-size: 13px;
    font-weight: 600;
    color: var(--muted);
}
.disable-reason-field input {
    padding: 8px 12px;
    font-size: 14px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    color: var(--text);
}

.role-control {
    display: flex;
    align-items: center;
    gap: 8px;
}

.role-badge {
    display: inline-flex;
    align-items: center;
    height: 24px;
    padding: 0 8px;
    border-radius: 4px;
    background: #f3f4f6;
    color: #4b5563;
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
}

.role-badge.admin {
    background: #eef2ff;
    color: #4338ca;
}

.role-control select {
    min-width: 96px;
    height: 30px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    background: var(--surface);
    color: var(--text);
    font-size: 13px;
}

.role-control select:disabled {
    cursor: not-allowed;
    opacity: 0.55;
}
</style>
