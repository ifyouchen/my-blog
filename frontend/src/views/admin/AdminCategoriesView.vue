<script setup>
import { reactive, watch } from 'vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
    createCategoryApi,
    deleteCategoryApi,
    getAdminCategoriesApi,
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
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const form = reactive({
    name: '',
    description: '',
    sortOrder: 0
});

const state = reactive({
    ...createPagedState(8),
    enabled: '',
    feedback: '',
    feedbackType: 'success',
    submitting: false,
    editingId: null,
    editForm: {
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

const setFeedback = (message, type = 'success') => {
    state.feedback = message;
    state.feedbackType = type;
};

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.enabled = readQueryBooleanText(route, 'enabled');
    state.jumpPage = String(state.page);
};

const loadCategories = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminCategoriesApi(
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
        state.items = [];
        state.total = 0;
        state.error = error.message || '分类列表加载失败';
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

const submitCategory = async () => {
    state.submitting = true;
    try {
        await createCategoryApi({
            name: form.name,
            description: form.description,
            sortOrder: Number(form.sortOrder || 0)
        });
        form.name = '';
        form.description = '';
        form.sortOrder = 0;
        setFeedback('分类已创建');
        await loadCategories();
    } catch (error) {
        setFeedback(error.message || '分类创建失败', 'error');
    } finally {
        state.submitting = false;
    }
};

const startEdit = (category) => {
    state.editingId = category.id;
    state.editForm.name = category.name || '';
    state.editForm.description = category.description || '';
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
            sortOrder: Number(state.editForm.sortOrder || 0),
            enabled: Boolean(state.editForm.enabled)
        });
        state.editingId = null;
        setFeedback('分类已更新');
        await loadCategories();
    } catch (error) {
        setFeedback(error.message || '分类更新失败', 'error');
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
            sortOrder: category.sortOrder || 0,
            enabled: !category.enabled
        });
        setFeedback(category.enabled ? '分类已禁用' : '分类已启用');
        await loadCategories();
    } catch (error) {
        setFeedback(error.message || '分类更新失败', 'error');
    } finally {
        state.submitting = false;
    }
};

const removeCategory = async (category) => {
    if (!window.confirm(`确定删除分类「${category.name}」吗？`)) {
        return;
    }
    state.submitting = true;
    try {
        await deleteCategoryApi(category.id);
        setFeedback('分类已删除');
        await loadCategories();
    } catch (error) {
        setFeedback(error.message || '分类删除失败', 'error');
    } finally {
        state.submitting = false;
    }
};

useAdminRefresh(loadCategories);

watch(
    () => [route.query.page, route.query.enabled],
    () => {
        applyRouteState();
        loadCategories();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel admin-resource-page">
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar admin-create-toolbar" @submit.prevent="submitCategory">
                <label>
                    <span>分类名称</span>
                    <input v-model.trim="form.name" type="text" placeholder="分类名称" required>
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
                    <button type="submit" :disabled="state.submitting">{{ state.submitting ? '提交中...' : '新增分类' }}</button>
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

        <p v-if="state.feedback" :class="['backend-state-text', state.feedbackType === 'error' ? 'error-text' : 'success-text']">
            {{ state.feedback }}
        </p>

        <div class="admin-table-shell">
            <p v-if="state.loading" class="backend-state-text">分类数据加载中...</p>
            <p v-else-if="state.error" class="backend-state-text error-text">{{ state.error }}</p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-categories-table">
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>名称</th>
                                <th>说明</th>
                                <th>排序</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="category in state.items" :key="category.id">
                                <td>#{{ category.id }}</td>
                                <td v-if="state.editingId === category.id">
                                    <input v-model.trim="state.editForm.name" type="text">
                                </td>
                                <td v-else>{{ category.name }}</td>
                                <td v-if="state.editingId === category.id">
                                    <input v-model.trim="state.editForm.description" type="text">
                                </td>
                                <td v-else>{{ category.description || '-' }}</td>
                                <td v-if="state.editingId === category.id">
                                    <input v-model.number="state.editForm.sortOrder" type="number">
                                </td>
                                <td v-else>{{ category.sortOrder ?? 0 }}</td>
                                <td>
                                    <template v-if="state.editingId === category.id">
                                        <select v-model="state.editForm.enabled">
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
                                        <button type="button" :disabled="state.submitting" @click="saveEdit(category.id)">
                                            {{ state.submitting ? '保存中...' : '保存' }}
                                        </button>
                                        <button type="button" :disabled="state.submitting" @click="cancelEdit">取消</button>
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
    </section>
</template>
