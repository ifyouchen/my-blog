<script setup>
import {reactive, watch} from 'vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {createAdminColumnApi, deleteAdminColumnApi, getAdminColumnsApi, updateAdminColumnApi} from '@/api/admin';
import {createPagedState, readPositiveInt, resolveAdminOverflowPage, syncAdminQuery, useAdminRefresh} from '@/views/admin/adminShared';
import {useRoute, useRouter} from 'vue-router';
import { useConfirmDialog } from '@/composables/useConfirmDialog';

const route = useRoute();
const router = useRouter();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const form = reactive({
    authorId: '',
    title: '',
    summary: '',
    coverUrl: '',
    sortOrder: 0
});

const state = reactive({
    ...createPagedState(10),
    keyword: '',
    feedback: '',
    feedbackType: 'success',
    submitting: false,
    editingId: null,
    editForm: {
        title: '',
        summary: '',
        coverUrl: '',
        sortOrder: 0,
        status: 'PUBLISHED'
    }
});

const setFeedback = (message, type = 'success') => {
    state.feedback = message;
    state.feedbackType = type;
};

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.keyword = route.query.keyword || '';
    state.jumpPage = String(state.page);
};

const loadColumns = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminColumnsApi(
            state.page,
            state.pageSize,
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
        state.error = error.message || '专栏列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        keyword: patch.keyword ?? (state.keyword || undefined)
    });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({ page: targetPage > 1 ? String(targetPage) : undefined });
};

const changeFilter = async () => {
    state.page = 1;
    await syncQuery({ page: undefined, keyword: state.keyword || undefined });
};

const submitColumn = async () => {
    if (!form.authorId) {
        setFeedback('请填写作者 ID', 'error');
        return;
    }
    state.submitting = true;
    try {
        await createAdminColumnApi({
            authorId: Number(form.authorId),
            title: form.title,
            summary: form.summary,
            coverUrl: form.coverUrl,
            sortOrder: Number(form.sortOrder || 0)
        });
        form.authorId = '';
        form.title = '';
        form.summary = '';
        form.coverUrl = '';
        form.sortOrder = 0;
        setFeedback('专栏已创建');
        await loadColumns();
    } catch (error) {
        setFeedback(error.message || '专栏创建失败', 'error');
    } finally {
        state.submitting = false;
    }
};

const startEdit = (column) => {
    state.editingId = column.id;
    state.editForm.title = column.title || '';
    state.editForm.summary = column.summary || '';
    state.editForm.coverUrl = column.coverUrl || '';
    state.editForm.sortOrder = column.sortOrder ?? 0;
    state.editForm.status = column.status || 'PUBLISHED';
};

const cancelEdit = () => {
    state.editingId = null;
};

const saveEdit = async (columnId) => {
    state.submitting = true;
    try {
        await updateAdminColumnApi(columnId, {
            title: state.editForm.title,
            summary: state.editForm.summary,
            coverUrl: state.editForm.coverUrl,
            sortOrder: Number(state.editForm.sortOrder || 0),
            status: state.editForm.status
        });
        state.editingId = null;
        setFeedback('专栏已更新');
        await loadColumns();
    } catch (error) {
        setFeedback(error.message || '专栏更新失败', 'error');
    } finally {
        state.submitting = false;
    }
};

const removeColumn = async (column) => {
    openConfirmDialog({
        eyebrow: '专栏治理确认',
        title: '删除专栏',
        message: `确定删除专栏「${column.title}」吗？删除后该专栏将不再对外展示。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            state.submitting = true;
            try {
                await deleteAdminColumnApi(column.id);
                setFeedback('专栏已删除');
                await loadColumns();
            } catch (error) {
                setFeedback(error.message || '专栏删除失败', 'error');
            } finally {
                state.submitting = false;
            }
        }
    });
};

useAdminRefresh(loadColumns);

watch(
    () => [route.query.page, route.query.keyword],
    () => {
        applyRouteState();
        loadColumns();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel admin-resource-page">
        <div class="admin-toolbar">
            <!-- 新增专栏表单 -->
            <form class="admin-filter-toolbar admin-create-toolbar" @submit.prevent="submitColumn">
                <label>
                    <span>作者 ID</span>
                    <input v-model.trim="form.authorId" type="number" placeholder="用户 ID" required>
                </label>
                <label>
                    <span>专栏标题</span>
                    <input v-model.trim="form.title" type="text" placeholder="专栏标题" required>
                </label>
                <label>
                    <span>简介</span>
                    <input v-model.trim="form.summary" type="text" placeholder="专栏简介">
                </label>
                <label>
                    <span>封面 URL</span>
                    <input v-model.trim="form.coverUrl" type="text" placeholder="封面图片 URL">
                </label>
                <label>
                    <span>排序值</span>
                    <input v-model.number="form.sortOrder" type="number" placeholder="排序值">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit" :disabled="state.submitting">
                        {{ state.submitting ? '提交中...' : '新增专栏' }}
                    </button>
                </div>
            </form>

            <!-- 关键字搜索 -->
            <div class="admin-filter-toolbar">
                <label>
                    <span>关键字搜索</span>
                    <input v-model.trim="state.keyword" type="text" placeholder="标题/简介关键字"
                           @keyup.enter="changeFilter">
                </label>
                <div class="admin-filter-actions">
                    <button type="button" @click="changeFilter">搜索</button>
                </div>
            </div>
        </div>

        <p v-if="state.feedback"
           :class="['backend-state-text', state.feedbackType === 'error' ? 'error-text' : 'success-text']">
            {{ state.feedback }}
        </p>

        <div class="admin-table-shell">
            <p v-if="state.loading && state.items.length" class="backend-state-text subtle">
                正在更新专栏数据...
            </p>
            <p v-if="state.error && state.items.length" class="backend-state-text error-text subtle">
                {{ state.error }}
            </p>
            <p v-if="state.loading && !state.items.length" class="backend-state-text">
                专栏数据加载中...
            </p>
            <p v-else-if="state.error && !state.items.length" class="backend-state-text error-text">
                {{ state.error }}
            </p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-columns-table">
                    <table class="admin-table">
                        <colgroup>
                            <col style="width: 8%">
                            <col style="width: 14%">
                            <col style="width: 18%">
                            <col style="width: 9%">
                            <col style="width: 9%">
                            <col style="width: 10%">
                            <col style="width: 10%">
                            <col style="width: 10%">
                            <col style="width: 12%">
                        </colgroup>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>标题</th>
                                <th>简介</th>
                                <th>作者ID</th>
                                <th>排序</th>
                                <th>订阅数</th>
                                <th>文章数</th>
                                <th>状态</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="column in state.items" :key="column.id">
                                <td>#{{ column.id }}</td>

                                <!-- 标题 -->
                                <td v-if="state.editingId === column.id" class="admin-edit-cell">
                                    <input v-model.trim="state.editForm.title" class="admin-edit-input" type="text">
                                </td>
                                <td v-else><span class="admin-cell-text">{{ column.title }}</span></td>

                                <!-- 简介 -->
                                <td v-if="state.editingId === column.id" class="admin-edit-cell admin-edit-cell-wide">
                                    <input v-model.trim="state.editForm.summary" class="admin-edit-input" type="text">
                                </td>
                                <td v-else class="summary-cell">{{ column.summary || '-' }}</td>

                                <!-- 作者 ID -->
                                <td>{{ column.authorId }}</td>

                                <!-- 排序 -->
                                <td v-if="state.editingId === column.id" class="admin-edit-cell admin-edit-cell-narrow">
                                    <input v-model.number="state.editForm.sortOrder" class="admin-edit-input" type="number">
                                </td>
                                <td v-else>{{ column.sortOrder ?? 0 }}</td>

                                <td>{{ column.subscriberCount }}</td>
                                <td>{{ column.articleCount }}</td>

                                <!-- 状态 -->
                                <td v-if="state.editingId === column.id">
                                    <select v-model="state.editForm.status" class="admin-edit-select">
                                        <option value="PUBLISHED">已发布</option>
                                        <option value="DRAFT">草稿</option>
                                    </select>
                                </td>
                                <td v-else>
                                    <span class="status-pill" :class="{ danger: column.status !== 'PUBLISHED' }">
                                        {{ column.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                                    </span>
                                </td>

                                <!-- 操作 -->
                                <td class="table-actions">
                                    <template v-if="state.editingId === column.id">
                                        <button type="button" class="admin-edit-btn primary" :disabled="state.submitting"
                                                @click="saveEdit(column.id)">
                                            {{ state.submitting ? '保存中...' : '保存' }}
                                        </button>
                                        <button type="button" class="admin-edit-btn secondary" :disabled="state.submitting"
                                                @click="cancelEdit">取消</button>
                                    </template>
                                    <template v-else>
                                        <button type="button" :disabled="state.submitting"
                                                @click="startEdit(column)">编辑</button>
                                        <button type="button" class="danger-link" :disabled="state.submitting"
                                                @click="removeColumn(column)">删除</button>
                                    </template>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有专栏</p>
            </template>
        </div>

        <AdminPagination :state="state" label="专栏分页" @page-change="changePage" />
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

.summary-cell {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.admin-edit-cell {
    min-width: 160px;
}

.admin-edit-cell-wide {
    min-width: 220px;
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
</style>

