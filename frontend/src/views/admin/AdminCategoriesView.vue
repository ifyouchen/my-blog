<script setup>
import { reactive, watch } from 'vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
    createCategoryApi,
    createCategoryGroupApi,
    deleteCategoryApi,
    getAdminCategoriesApi,
    getCategoryGroupsApi,
    deleteCategoryGroupApi,
    updateCategoryGroupApi,
    updateCategoryApi
} from '@/api/admin';
import {
    createPagedState,
    readPositiveInt,
    readQueryBooleanText,
    resolveAdminOverflowPage,
    syncAdminQuery,
    useAdminRefresh
} from '@/views/admin/adminShared';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useToast } from '@/composables/useToast';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const form = reactive({
    name: '',
    description: '',
    groupId: '',
    sortOrder: 0
});

const state = reactive({
    ...createPagedState(8),
    enabled: '',
    groupId: '',
    keyword: '',
    submitting: false,
    editingId: null,
    editForm: {
        name: '',
        description: '',
        groupId: '',
        sortOrder: 0,
        enabled: true
    },
    groups: [],
    groupForm: {
        name: '',
        description: '',
        sortOrder: 0
    },
    groupEditingId: null,
    groupEditForm: {
        name: '',
        description: '',
        sortOrder: 0,
        enabled: true
    }
});

const normalizeEnabledFilter = (value) => {
    if (value === 'true') {
        return true;
    }
    if (value === 'false') {
        return false;
    }
    return null;
};

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.enabled = readQueryBooleanText(route, 'enabled');
    state.groupId = typeof route.query.groupId === 'string' ? route.query.groupId : '';
    state.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : '';
    state.jumpPage = String(state.page);
};

const loadCategories = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminCategoriesApi(
            state.page,
            state.pageSize,
            normalizeEnabledFilter(state.enabled),
            state.groupId || null,
            state.keyword || null
        );
        const overflowPage = resolveAdminOverflowPage(state, result);
        if (overflowPage) {
            state.page = overflowPage;
            state.jumpPage = String(overflowPage);
            await syncQuery({ page: overflowPage > 1 ? String(overflowPage) : undefined });
            return;
        }
        state.items = result.items || [];
        state.total = result.total || 0;
        state.page = result.page || state.page;
        state.jumpPage = String(state.page);
    } catch (error) {
        state.error = error.message || '分类列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        enabled: patch.enabled ?? (state.enabled || undefined),
        groupId: patch.groupId ?? (state.groupId || undefined),
        keyword: patch.keyword ?? (state.keyword || undefined)
    });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({ page: targetPage > 1 ? String(targetPage) : undefined });
};

const submitFilters = async () => {
    state.page = 1;
    const changed = await syncQuery({
        page: undefined,
        enabled: state.enabled || undefined,
        groupId: state.groupId || undefined,
        keyword: state.keyword || undefined
    });
    if (!changed) {
        await loadCategories();
    }
};

const resetFilters = async () => {
    state.enabled = '';
    state.groupId = '';
    state.keyword = '';
    state.page = 1;
    const changed = await syncQuery({
        page: undefined,
        enabled: undefined,
        groupId: undefined,
        keyword: undefined
    });
    if (!changed) {
        await loadCategories();
    }
};

const loadGroups = async () => {
    try {
        state.groups = await getCategoryGroupsApi();
    } catch {
        state.groups = [];
    }
};

const submitGroup = async () => {
    state.submitting = true;
    try {
        await createCategoryGroupApi({
            name: state.groupForm.name,
            description: state.groupForm.description,
            sortOrder: Number(state.groupForm.sortOrder || 0)
        });
        state.groupForm.name = '';
        state.groupForm.description = '';
        state.groupForm.sortOrder = 0;
        toast.success('分类组创建成功');
        await loadGroups();
    } catch (error) {
        toast.error(error.message || '分类组创建失败');
    } finally {
        state.submitting = false;
    }
};

const startGroupEdit = (group) => {
    state.groupEditingId = group.id;
    state.groupEditForm.name = group.name || '';
    state.groupEditForm.description = group.description || '';
    state.groupEditForm.sortOrder = group.sortOrder || 0;
    state.groupEditForm.enabled = Boolean(group.enabled);
};

const cancelGroupEdit = () => {
    state.groupEditingId = null;
};

const saveGroupEdit = async (groupId) => {
    state.submitting = true;
    try {
        await updateCategoryGroupApi(groupId, {
            name: state.groupEditForm.name,
            description: state.groupEditForm.description,
            sortOrder: Number(state.groupEditForm.sortOrder || 0),
            enabled: Boolean(state.groupEditForm.enabled)
        });
        state.groupEditingId = null;
        toast.success('分类组更新成功');
        await loadGroups();
        await loadCategories();
    } catch (error) {
        toast.error(error.message || '分类组更新失败');
    } finally {
        state.submitting = false;
    }
};

const toggleGroup = async (group) => {
    state.submitting = true;
    try {
        await updateCategoryGroupApi(group.id, {
            name: group.name,
            description: group.description,
            sortOrder: Number(group.sortOrder || 0),
            enabled: !group.enabled
        });
        await loadGroups();
        await loadCategories();
    } catch (error) {
        toast.error(error.message || '分类组更新失败');
    } finally {
        state.submitting = false;
    }
};

const removeGroup = async (group) => {
    openConfirmDialog({
        eyebrow: '分类大类确认',
        title: '删除分类组',
        message: `确定删除分类组「${group.name}」吗？只有空分类组可以删除，非空分类组请先迁移或删除小分类。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            try {
                await deleteCategoryGroupApi(group.id);
                toast.success('分类组已删除');
                await loadGroups();
                await loadCategories();
            } catch (error) {
                toast.error(error.message || '删除失败');
            }
        }
    });
};

const submitCategory = async () => {
    state.submitting = true;
    try {
        await createCategoryApi({
            name: form.name,
            description: form.description,
            groupId: form.groupId ? Number(form.groupId) : undefined,
            sortOrder: Number(form.sortOrder || 0)
        });
        form.name = '';
        form.description = '';
        form.groupId = '';
        form.sortOrder = 0;
        await loadCategories();
    } catch (error) {
        toast.error(error.message || '分类创建失败');
    } finally {
        state.submitting = false;
    }
};

const startEdit = (category) => {
    state.editingId = category.id;
    state.editForm.name = category.name || '';
    state.editForm.description = category.description || '';
    state.editForm.groupId = category.groupId ? String(category.groupId) : '';
    state.editForm.sortOrder = category.sortOrder || 0;
    state.editForm.enabled = Boolean(category.enabled);
};

const cancelEdit = () => {
    state.editingId = null;
};

const saveEdit = async (categoryId) => {
    state.submitting = true;
    try {
        await updateCategoryApi(categoryId, {
            name: state.editForm.name,
            description: state.editForm.description,
            groupId: state.editForm.groupId ? Number(state.editForm.groupId) : undefined,
            sortOrder: Number(state.editForm.sortOrder || 0),
            enabled: Boolean(state.editForm.enabled)
        });
        state.editingId = null;
        await loadCategories();
    } catch (error) {
        toast.error(error.message || '分类更新失败');
    } finally {
        state.submitting = false;
    }
};

const toggleCategory = async (category) => {
    state.submitting = true;
    try {
        await updateCategoryApi(category.id, {
            name: category.name,
            description: category.description,
            groupId: category.groupId,
            sortOrder: category.sortOrder || 0,
            enabled: !category.enabled
        });
        await loadCategories();
    } catch (error) {
        toast.error(error.message || '分类更新失败');
    } finally {
        state.submitting = false;
    }
};

const removeCategory = async (category) => {
    openConfirmDialog({
        eyebrow: '分类治理确认',
        title: '删除分类',
        message: `确定删除分类「${category.name}」吗？删除后相关文章的分类展示会受到影响。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            state.submitting = true;
            try {
                await deleteCategoryApi(category.id);
                await loadCategories();
            } catch (error) {
                toast.error(error.message || '分类删除失败');
            } finally {
                state.submitting = false;
            }
        }
    });
};

useAdminRefresh(loadCategories);

watch(
    () => [route.query.page, route.query.enabled, route.query.groupId, route.query.keyword],
    () => {
        applyRouteState();
        loadCategories();
    },
    { immediate: true }
);

loadGroups();
</script>

<template>
    <section class="dashboard-content-panel admin-resource-page">
        <div class="admin-group-panel">
            <div class="admin-group-panel-head">
                <h4 class="admin-group-panel-title">分类组</h4>
                <form class="admin-group-create-form" @submit.prevent="submitGroup">
                    <input v-model.trim="state.groupForm.name" type="text" placeholder="分类组名称" required>
                    <input v-model.trim="state.groupForm.description" type="text" placeholder="说明">
                    <input v-model.number="state.groupForm.sortOrder" type="number" placeholder="排序">
                    <button type="submit" :disabled="state.submitting">新增分类组</button>
                </form>
            </div>
            <div v-if="state.groups.length" class="admin-group-cards">
                <div
                    v-for="g in state.groups"
                    :key="g.id"
                    class="admin-group-card"
                    :class="{ disabled: !g.enabled }"
                >
                    <template v-if="state.groupEditingId === g.id">
                        <div class="admin-group-edit-grid">
                            <input v-model.trim="state.groupEditForm.name" class="admin-edit-input" type="text">
                            <input v-model.trim="state.groupEditForm.description" class="admin-edit-input" type="text">
                            <input v-model.number="state.groupEditForm.sortOrder" class="admin-edit-input" type="number">
                            <select v-model="state.groupEditForm.enabled" class="admin-edit-select">
                                <option :value="true">启用</option>
                                <option :value="false">禁用</option>
                            </select>
                            <button type="button" class="admin-edit-btn primary" :disabled="state.submitting" @click="saveGroupEdit(g.id)">
                                保存
                            </button>
                            <button type="button" class="admin-edit-btn secondary" :disabled="state.submitting" @click="cancelGroupEdit">
                                取消
                            </button>
                        </div>
                    </template>
                    <template v-else>
                        <div class="admin-group-card-header">
                            <strong class="admin-group-card-name">{{ g.name }}</strong>
                            <span class="admin-group-card-count">{{ g.categoryCount || 0 }} 个小分类</span>
                            <span class="status-pill" :class="{ danger: !g.enabled }">
                                {{ g.enabled ? '启用' : '禁用' }}
                            </span>
                            <div class="admin-group-card-actions">
                                <button type="button" :disabled="state.submitting" @click="startGroupEdit(g)">编辑</button>
                                <button type="button" :disabled="state.submitting" @click="toggleGroup(g)">
                                    {{ g.enabled ? '禁用' : '启用' }}
                                </button>
                                <button type="button" class="danger-link" :disabled="state.submitting || Number(g.categoryCount || 0) > 0" @click="removeGroup(g)">
                                    删除
                                </button>
                            </div>
                        </div>
                        <p v-if="g.description" class="admin-group-card-desc">{{ g.description }}</p>
                    </template>
                </div>
            </div>
            <p v-else class="backend-state-text">暂无分类组，请先新增分类组或执行初始化迁移</p>
        </div>

        <div class="admin-toolbar">
            <form class="admin-filter-toolbar admin-create-toolbar" @submit.prevent="submitCategory">
                <label>
                    <span>分类名称</span>
                    <input v-model.trim="form.name" type="text" placeholder="分类名称" required>
                </label>
                <label>
                    <span>所属分类组</span>
                    <select v-model="form.groupId" required>
                        <option value="" disabled>选择分类组</option>
                        <option v-for="group in state.groups" :key="group.id" :value="String(group.id)" :disabled="!group.enabled">
                            {{ group.name }}
                        </option>
                    </select>
                </label>
                <label>
                    <span>分类说明</span>
                    <input v-model.trim="form.description" type="text" placeholder="分类说明">
                </label>
                <label>
                    <span>排序值</span>
                    <input v-model.number="form.sortOrder" type="number" placeholder="排序值">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit" :disabled="state.submitting">新增分类</button>
                </div>
            </form>

            <form class="admin-filter-toolbar" @submit.prevent="submitFilters">
                <label>
                    <span>状态筛选</span>
                    <select v-model="state.enabled">
                        <option value="">全部</option>
                        <option value="true">仅启用</option>
                        <option value="false">仅禁用</option>
                    </select>
                </label>
                <label>
                    <span>分类组</span>
                    <select v-model="state.groupId">
                        <option value="">全部分类组</option>
                        <option v-for="group in state.groups" :key="group.id" :value="String(group.id)">
                            {{ group.name }}
                        </option>
                    </select>
                </label>
                <label class="admin-filter-grow">
                    <span>关键词</span>
                    <input v-model.trim="state.keyword" type="text" placeholder="搜索分类名称、说明或分类组">
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
                <div class="admin-table-wrap" data-testid="admin-categories-table">
                    <table class="admin-table">
                        <colgroup>
                            <col style="width: 8%">
                            <col style="width: 16%">
                            <col style="width: 18%">
                            <col style="width: 14%">
                            <col style="width: 10%">
                            <col style="width: 12%">
                            <col style="width: 22%">
                        </colgroup>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>名称</th>
                                <th>说明</th>
                                <th>所属大类</th>
                                <th>排序</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="category in state.items" :key="category.id">
                                <td>#{{ category.id }}</td>
                                <td v-if="state.editingId === category.id" class="admin-edit-cell">
                                    <input v-model.trim="state.editForm.name" class="admin-edit-input" type="text">
                                </td>
                                <td v-else><span class="admin-cell-text">{{ category.name }}</span></td>
                                <td v-if="state.editingId === category.id" class="admin-edit-cell">
                                    <input v-model.trim="state.editForm.description" class="admin-edit-input" type="text">
                                </td>
                                <td v-else><span class="admin-cell-text muted">{{ category.description || '-' }}</span></td>
                                <td v-if="state.editingId === category.id" class="admin-edit-cell">
                                    <select v-model="state.editForm.groupId" class="admin-edit-select">
                                        <option value="" disabled>选择分类组</option>
                                        <option v-for="group in state.groups" :key="group.id" :value="String(group.id)" :disabled="!group.enabled">
                                            {{ group.name }}
                                        </option>
                                    </select>
                                </td>
                                <td v-else><span class="admin-cell-text muted">{{ category.groupName || '-' }}</span></td>
                                <td v-if="state.editingId === category.id" class="admin-edit-cell admin-edit-cell-narrow">
                                    <input v-model.number="state.editForm.sortOrder" class="admin-edit-input" type="number">
                                </td>
                                <td v-else>{{ category.sortOrder ?? 0 }}</td>
                                <td>
                                    <template v-if="state.editingId === category.id">
                                        <select v-model="state.editForm.enabled" class="admin-edit-select">
                                            <option :value="true">启用</option>
                                            <option :value="false">禁用</option>
                                        </select>
                                    </template>
                                    <span v-else class="status-pill" :class="{ danger: !category.enabled }">
                                        {{ category.enabled ? '启用' : '禁用' }}
                                    </span>
                                </td>
                                <td class="table-actions">
                                    <template v-if="state.editingId === category.id">
                                        <button type="button" class="admin-edit-btn primary" :disabled="state.submitting" @click="saveEdit(category.id)">
                                            保存
                                        </button>
                                        <button type="button" class="admin-edit-btn secondary" :disabled="state.submitting" @click="cancelEdit">取消</button>
                                    </template>
                                    <template v-else>
                                        <button type="button" :disabled="state.submitting" @click="startEdit(category)">编辑</button>
                                        <button type="button" :disabled="state.submitting" @click="toggleCategory(category)">
                                            {{ category.enabled ? '禁用' : '启用' }}
                                        </button>
                                        <button type="button" class="danger-link" :disabled="state.submitting" @click="removeCategory(category)">
                                            删除
                                        </button>
                                    </template>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有分类</p>
            </template>
        </div>

        <AdminPagination :state="state" label="分类分页" @page-change="changePage" />

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

<style scoped>
.admin-table {
    table-layout: fixed;
}

.admin-edit-cell {
    min-width: 160px;
}

.admin-edit-cell-narrow {
    min-width: 96px;
}

.admin-edit-input,
.admin-edit-select {
    width: 100%;
    min-height: 38px;
    padding: 0 12px;
    color: var(--text);
    font: inherit;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    outline: 0;
    box-sizing: border-box;
}

.admin-edit-input:focus,
.admin-edit-select:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.admin-edit-btn {
    min-width: 72px;
}

.admin-edit-btn.primary {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.admin-edit-btn.primary:hover:not(:disabled) {
    color: #ffffff;
    background: var(--brand-strong);
    border-color: var(--brand-strong);
}

.admin-edit-btn.primary:focus-visible {
    color: #ffffff;
}

.admin-edit-btn.secondary {
    color: var(--text);
    background: var(--surface);
    border-color: var(--line);
}

.admin-edit-btn.secondary:hover:not(:disabled) {
    color: var(--brand);
    border-color: var(--brand);
}

.admin-cell-text {
    display: -webkit-box;
    overflow: hidden;
    color: var(--text);
    line-height: 1.6;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    word-break: break-word;
}

.admin-cell-text.muted {
    color: var(--muted);
}

.admin-section-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
    margin: 16px 0 8px;
}

.admin-ghost-btn {
    color: var(--brand);
    cursor: pointer;
    background: transparent;
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    padding: 6px 16px;
    font: inherit;
    font-size: 0.875rem;
}

.admin-ghost-btn:hover {
    color: var(--brand-strong);
    border-color: var(--brand);
}

.admin-group-panel {
    margin: 12px 0 24px;
    padding: 16px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.admin-group-panel-head {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr);
    gap: 12px;
    align-items: end;
    margin-bottom: 12px;
}

.admin-group-panel-title {
    margin: 0;
    font-size: 1rem;
    font-weight: 600;
}

.admin-group-create-form {
    display: grid;
    grid-template-columns: minmax(120px, 0.8fr) minmax(160px, 1fr) 88px auto;
    gap: 8px;
    align-items: center;
}

.admin-group-create-form input,
.admin-group-create-form button {
    min-height: 36px;
    box-sizing: border-box;
}

.admin-group-create-form input {
    width: 100%;
    padding: 0 12px;
    color: var(--text);
    font: inherit;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.admin-group-create-form button {
    padding: 0 14px;
    color: #fff;
    cursor: pointer;
    background: var(--brand);
    border: 0;
    border-radius: var(--radius-md);
    font: inherit;
    white-space: nowrap;
}

.admin-group-cards {
    display: grid;
    gap: 12px;
    margin-bottom: 16px;
}

.admin-group-card {
    padding: 12px 16px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.admin-group-card.disabled {
    opacity: 0.72;
}

.admin-group-card-header {
    display: flex;
    align-items: center;
    gap: 12px;
}

.admin-group-card-name {
    font-size: 0.95rem;
    min-width: 80px;
}

.admin-group-card-count {
    color: var(--muted);
    font-size: 0.8rem;
}

.admin-group-card-actions {
    margin-left: auto;
    display: flex;
    gap: 8px;
}

.admin-group-card-actions button {
    min-height: 32px;
    padding: 0 12px;
    color: var(--text);
    font-size: 13px;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    transition: color 0.12s, border-color 0.12s;
}

.admin-group-card-actions button:hover:not(:disabled) {
    color: var(--brand);
    border-color: var(--brand);
}

.admin-group-card-desc {
    margin: 8px 0 0;
    color: var(--muted);
    font-size: 0.85rem;
    line-height: 1.6;
}

.admin-group-edit-grid {
    display: grid;
    grid-template-columns: minmax(120px, 0.8fr) minmax(160px, 1fr) 88px 96px auto auto;
    gap: 8px;
    align-items: center;
}

.admin-group-card-items {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid var(--line);
}

.admin-group-item-tag {
    padding: 2px 8px;
    font-size: 0.8rem;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.admin-group-rename-form {
    display: flex;
    gap: 8px;
    align-items: flex-end;
    padding-top: 12px;
    border-top: 1px solid var(--line);
}

.admin-group-rename-form label {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 4px;
    font-size: 0.875rem;
    color: var(--muted);
}

.admin-group-rename-form input {
    min-height: 36px;
    padding: 0 12px;
    color: var(--text);
    font: inherit;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.admin-group-rename-form input:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
    outline: 0;
}

.admin-group-rename-form button {
    height: 36px;
    padding: 0 16px;
    color: #fff;
    cursor: pointer;
    background: var(--brand);
    border: none;
    border-radius: var(--radius-md);
    font: inherit;
    font-size: 0.875rem;
    white-space: nowrap;
}

.admin-group-rename-form button:last-child {
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
}

.admin-group-rename-form button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

@media (max-width: 900px) {
    .admin-group-panel-head,
    .admin-group-create-form,
    .admin-group-edit-grid {
        grid-template-columns: 1fr;
    }

    .admin-group-card-header {
        align-items: flex-start;
        flex-wrap: wrap;
    }

    .admin-group-card-actions {
        width: 100%;
        margin-left: 0;
        flex-wrap: wrap;
    }
}
</style>
