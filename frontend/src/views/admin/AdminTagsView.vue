<script setup>
import { reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
    createTagApi,
    deleteTagApi,
    getAdminTagsApi,
    getAdminTagGroupsApi,
    renameTagGroupApi,
    deleteTagGroupApi,
    updateTagApi
} from '@/api/admin';
import {
    createPagedState,
    readPositiveInt,
    readQueryBooleanText,
    readQueryText,
    resolveAdminOverflowPage,
    syncAdminQuery,
    useAdminRefresh
} from '@/views/admin/adminShared';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useToast } from '@/composables/useToast';

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
    groupName: ''
});

const state = reactive({
    ...createPagedState(8),
    enabled: '',
    keyword: '',
    submitting: false,
    editingId: null,
    editForm: {
        name: '',
        description: '',
        groupName: '',
        enabled: true
    },
    groups: [],
    showGroupPanel: false,
    renameGroup: { oldName: '', newName: '' },
    deleteGroupName: ''
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
    state.keyword = readQueryText(route, 'keyword');
    state.jumpPage = String(state.page);
};

const loadTags = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminTagsApi(
            state.page,
            state.pageSize,
            normalizeEnabledFilter(state.enabled),
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
        state.error = error.message || '标签列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    return await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        enabled: patch.enabled ?? (state.enabled || undefined),
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
        keyword: state.keyword || undefined
    });
    if (!changed) {
        await loadTags();
    }
};

const resetFilters = async () => {
    state.enabled = '';
    state.keyword = '';
    state.page = 1;
    const changed = await syncQuery({
        page: undefined,
        enabled: undefined,
        keyword: undefined
    });
    if (!changed) {
        await loadTags();
    }
};

const submitTag = async () => {
    state.submitting = true;
    try {
        await createTagApi({
            name: form.name,
            description: form.description,
            groupName: form.groupName || undefined
        });
        form.name = '';
        form.description = '';
        form.groupName = '';
        await loadTags();
    } catch (error) {
        toast.error(error.message || '标签创建失败');
    } finally {
        state.submitting = false;
    }
};

const startEdit = (tag) => {
    state.editingId = tag.id;
    state.editForm.name = tag.name || '';
    state.editForm.description = tag.description || '';
    state.editForm.groupName = tag.groupName || '';
    state.editForm.enabled = Boolean(tag.enabled);
};

const cancelEdit = () => {
    state.editingId = null;
};

const saveEdit = async (tagId) => {
    state.submitting = true;
    try {
        await updateTagApi(tagId, {
            name: state.editForm.name,
            description: state.editForm.description,
            groupName: state.editForm.groupName || undefined,
            enabled: Boolean(state.editForm.enabled)
        });
        state.editingId = null;
        await loadTags();
    } catch (error) {
        toast.error(error.message || '标签更新失败');
    } finally {
        state.submitting = false;
    }
};

const toggleTag = async (tag) => {
    state.submitting = true;
    try {
        await updateTagApi(tag.id, {
            name: tag.name,
            description: tag.description,
            enabled: !tag.enabled
        });
        await loadTags();
    } catch (error) {
        toast.error(error.message || '标签更新失败');
    } finally {
        state.submitting = false;
    }
};

const removeTag = async (tag) => {
    openConfirmDialog({
        eyebrow: '标签治理确认',
        title: '删除标签',
        message: `确定删除标签「${tag.name}」吗？删除后相关文章的标签检索和编辑器候选会同步受影响。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            state.submitting = true;
            try {
                await deleteTagApi(tag.id);
                await loadTags();
            } catch (error) {
                toast.error(error.message || '标签删除失败');
            } finally {
                state.submitting = false;
            }
        }
    });
};

const loadGroups = async () => {
    try {
        state.groups = await getAdminTagGroupsApi();
    } catch {
        state.groups = [];
    }
};

const submitRenameGroup = async () => {
    if (!state.renameGroup.oldName || !state.renameGroup.newName) return;
    try {
        await renameTagGroupApi(state.renameGroup.oldName, state.renameGroup.newName);
        toast.success('标签大类重命名成功');
        state.renameGroup = { oldName: '', newName: '' };
        await loadGroups();
    } catch (error) {
        toast.error(error.message || '重命名失败');
    }
};

const submitDeleteGroup = async () => {
    if (!state.deleteGroupName) return;
    const name = state.deleteGroupName;
    openConfirmDialog({
        eyebrow: '标签大类确认',
        title: '删除标签大类',
        message: `确定删除标签大类「${name}」吗？该大类下所有标签的分类信息将被清空，但标签本身不会删除。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            try {
                await deleteTagGroupApi(name);
                toast.success('标签大类已删除');
                state.deleteGroupName = '';
                await loadGroups();
                await loadTags();
            } catch (error) {
                toast.error(error.message || '删除失败');
            }
        }
    });
};

useAdminRefresh(loadTags);

watch(
    () => [route.query.page, route.query.enabled, route.query.keyword],
    () => {
        applyRouteState();
        loadTags();
    },
    { immediate: true }
);

watch(() => state.showGroupPanel, (v) => { if (v) loadGroups(); });
</script>

<template>
    <section class="dashboard-content-panel admin-resource-page">
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar admin-create-toolbar" @submit.prevent="submitTag">
                <label>
                    <span>标签名称</span>
                    <input v-model.trim="form.name" type="text" placeholder="标签名称" required>
                </label>
                <label>
                    <span>所属大类</span>
                    <input v-model.trim="form.groupName" type="text" placeholder="如：Java、数据库">
                </label>
                <label class="admin-filter-grow">
                    <span>标签说明</span>
                    <input v-model.trim="form.description" type="text" placeholder="标签说明">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit" :disabled="state.submitting">新增标签</button>
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
                <label class="admin-filter-grow">
                    <span>关键词</span>
                    <input v-model.trim="state.keyword" type="text" placeholder="搜索标签名称或说明">
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
                <div class="admin-table-wrap" data-testid="admin-tags-table">
                    <table class="admin-table">
                        <colgroup>
                            <col style="width: 8%">
                            <col style="width: 18%">
                            <col style="width: 22%">
                            <col style="width: 16%">
                            <col style="width: 14%">
                            <col style="width: 22%">
                        </colgroup>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>名称</th>
                                <th>说明</th>
                                <th>所属大类</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="tag in state.items" :key="tag.id">
                                <td>#{{ tag.id }}</td>
                                <td v-if="state.editingId === tag.id" class="admin-edit-cell">
                                    <input v-model.trim="state.editForm.name" class="admin-edit-input" type="text">
                                </td>
                                <td v-else><span class="admin-cell-text">{{ tag.name }}</span></td>
                                <td v-if="state.editingId === tag.id" class="admin-edit-cell">
                                    <input v-model.trim="state.editForm.description" class="admin-edit-input" type="text">
                                </td>
                                <td v-else><span class="admin-cell-text muted">{{ tag.description || '-' }}</span></td>
                                <td v-if="state.editingId === tag.id" class="admin-edit-cell">
                                    <input v-model.trim="state.editForm.groupName" class="admin-edit-input" type="text" placeholder="如：Java">
                                </td>
                                <td v-else><span class="admin-cell-text muted">{{ tag.groupName || '-' }}</span></td>
                                <td>
                                    <template v-if="state.editingId === tag.id">
                                        <select v-model="state.editForm.enabled" class="admin-edit-select">
                                            <option :value="true">启用</option>
                                            <option :value="false">禁用</option>
                                        </select>
                                    </template>
                                    <span v-else class="status-pill" :class="{ danger: !tag.enabled }">
                                        {{ tag.enabled ? '启用' : '禁用' }}
                                    </span>
                                </td>
                                <td class="table-actions">
                                    <template v-if="state.editingId === tag.id">
                                        <button type="button" class="admin-edit-btn primary" :disabled="state.submitting" @click="saveEdit(tag.id)">
                                            保存
                                        </button>
                                        <button type="button" class="admin-edit-btn secondary" :disabled="state.submitting" @click="cancelEdit">取消</button>
                                    </template>
                                    <template v-else>
                                        <button type="button" :disabled="state.submitting" @click="startEdit(tag)">编辑</button>
                                        <button type="button" :disabled="state.submitting" @click="toggleTag(tag)">
                                            {{ tag.enabled ? '禁用' : '启用' }}
                                        </button>
                                        <button type="button" class="danger-link" :disabled="state.submitting" @click="removeTag(tag)">
                                            删除
                                        </button>
                                    </template>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有标签</p>
            </template>
        </div>

        <AdminPagination :state="state" label="标签分页" @page-change="changePage" />

        <div class="admin-section-actions">
            <button type="button" class="admin-ghost-btn" @click="state.showGroupPanel = !state.showGroupPanel">
                {{ state.showGroupPanel ? '收起' : '管理' }}标签大类
            </button>
        </div>

        <div v-if="state.showGroupPanel" class="admin-group-panel">
            <h4 class="admin-group-panel-title">标签大类列表</h4>
            <div v-if="state.groups.length" class="admin-group-cards">
                <div v-for="g in state.groups" :key="g.name" class="admin-group-card">
                    <div class="admin-group-card-header">
                        <strong class="admin-group-card-name">{{ g.name }}</strong>
                        <span class="admin-group-card-count">{{ g.tagCount }} 个标签</span>
                        <div class="admin-group-card-actions">
                            <button type="button" @click="state.renameGroup.oldName = g.name; state.renameGroup.newName = g.name">
                                重命名
                            </button>
                            <button type="button" class="danger-link" @click="state.deleteGroupName = g.name; submitDeleteGroup()">
                                删除
                            </button>
                        </div>
                    </div>
                    <div v-if="g.tagNames" class="admin-group-card-items">
                        <span v-for="name in g.tagNames.split(', ')" :key="name" class="admin-group-item-tag">{{ name }}</span>
                    </div>
                </div>
            </div>
            <p v-else class="backend-state-text">暂无标签大类</p>

            <div v-if="state.renameGroup.oldName" class="admin-group-rename-form">
                <label>
                    <span>新名称</span>
                    <input v-model.trim="state.renameGroup.newName" type="text" placeholder="输入新大类名称">
                </label>
                <button type="button" :disabled="!state.renameGroup.newName" @click="submitRenameGroup">确认重命名</button>
                <button type="button" @click="state.renameGroup = { oldName: '', newName: '' }">取消</button>
            </div>
        </div>

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
    min-width: 180px;
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

.admin-group-panel-title {
    margin: 0 0 12px;
    font-size: 1rem;
    font-weight: 600;
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
</style>
