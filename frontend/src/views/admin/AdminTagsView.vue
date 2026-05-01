<script setup>
import { reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
    createTagApi,
    deleteTagApi,
    getAdminTagsApi,
    updateTagApi
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
    description: ''
});

const state = reactive({
    ...createPagedState(8),
    enabled: '',
    submitting: false,
    editingId: null,
    editForm: {
        name: '',
        description: '',
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
    state.jumpPage = String(state.page);
};

const loadTags = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminTagsApi(
            state.page,
            state.pageSize,
            normalizeEnabledFilter(state.enabled)
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
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        enabled: patch.enabled ?? (state.enabled || undefined)
    });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({ page: targetPage > 1 ? String(targetPage) : undefined });
};

const changeFilter = async () => {
    state.page = 1;
    await syncQuery({
        page: undefined,
        enabled: state.enabled || undefined
    });
};

const submitTag = async () => {
    state.submitting = true;
    try {
        await createTagApi({
            name: form.name,
            description: form.description
        });
        form.name = '';
        form.description = '';
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

useAdminRefresh(loadTags);

watch(
    () => [route.query.page, route.query.enabled],
    () => {
        applyRouteState();
        loadTags();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel admin-resource-page">
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar admin-create-toolbar" @submit.prevent="submitTag">
                <label>
                    <span>标签名称</span>
                    <input v-model.trim="form.name" type="text" placeholder="标签名称" required>
                </label>
                <label class="admin-filter-grow">
                    <span>标签说明</span>
                    <input v-model.trim="form.description" type="text" placeholder="标签说明">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit" :disabled="state.submitting">新增标签</button>
                </div>
            </form>

            <div class="admin-filter-toolbar">
                <label>
                    <span>状态筛选</span>
                    <select v-model="state.enabled" @change="changeFilter">
                        <option value="">全部</option>
                        <option value="true">仅启用</option>
                        <option value="false">仅禁用</option>
                    </select>
                </label>
            </div>
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
                            <col style="width: 12%">
                            <col style="width: 24%">
                            <col style="width: 30%">
                            <col style="width: 14%">
                            <col style="width: 20%">
                        </colgroup>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>名称</th>
                                <th>说明</th>
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
</style>
