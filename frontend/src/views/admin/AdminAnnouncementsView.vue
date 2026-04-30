<script setup>
import {inject, onMounted, reactive} from 'vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import {
  createAdminAnnouncementApi,
  deleteAdminAnnouncementApi,
  getAdminAnnouncementsApi,
  publishAdminAnnouncementApi,
  unpublishAdminAnnouncementApi,
  updateAdminAnnouncementApi
} from '@/api/admin';
import {createPagedState, useAdminRefresh} from '@/views/admin/adminShared';
import {useConfirmDialog} from '@/composables/useConfirmDialog';

const toast = inject('toast', { success: () => {}, error: () => {} });
const {confirmDialog, openConfirmDialog, closeConfirmDialog, executeConfirmDialog} = useConfirmDialog();

const TARGET_OPTIONS = [
    { value: 'ALL', label: '所有用户' },
    { value: 'USER', label: '普通用户' },
    { value: 'ADMIN', label: '管理员' }
];

const state = reactive({
    ...createPagedState(10),
    refreshing: false,
    hasLoadedOnce: false,
    submitting: false,
    editingId: null,
    actionLoadingId: null
});

const emptyForm = () => ({
    title: '',
    content: '',
    target: 'ALL',
    expiresAt: ''
});

const form = reactive(emptyForm());
const editForm = reactive(emptyForm());

const resetForm = () => Object.assign(form, emptyForm());

const loadList = async (silent = false) => {
    if (!silent) {
        state.loading = true;
        state.error = '';
    } else {
        state.refreshing = true;
    }
    try {
        const result = await getAdminAnnouncementsApi(state.page, state.pageSize);
        state.items = result.items || [];
        state.total = result.total || 0;
        const maxPage = Math.max(1, Math.ceil(state.total / state.pageSize));
        if (state.page > maxPage) {
            state.page = maxPage;
        }
    } catch (e) {
        state.error = e.message || '加载公告列表失败';
    } finally {
        state.loading = false;
        state.refreshing = false;
        state.hasLoadedOnce = true;
    }
};

const changePage = (page) => {
    if (page === state.page || state.loading) return;
    state.page = page;
    loadList(true);
};

const handleCreate = async () => {
    if (!form.title.trim()) {
        toast.error('公告标题不能为空');
        return;
    }
    if (!form.content.trim()) {
        toast.error('公告内容不能为空');
        return;
    }
    state.submitting = true;
    try {
        await createAdminAnnouncementApi({
            title: form.title.trim(),
            content: form.content.trim(),
            target: form.target,
            expiresAt: form.expiresAt || undefined
        });
        toast.success('公告创建成功');
        resetForm();
        await loadList(true);
    } catch (e) {
        toast.error(e.message || '创建公告失败');
    } finally {
        state.submitting = false;
    }
};

const startEdit = (item) => {
    state.editingId = item.id;
    Object.assign(editForm, {
        title: item.title || '',
        content: item.content || '',
        target: item.target || 'ALL',
        expiresAt: item.expiresAt || ''
    });
};

const cancelEdit = () => {
    state.editingId = null;
};

const handleUpdate = async (id) => {
    if (!editForm.title.trim()) {
        toast.error('公告标题不能为空');
        return;
    }
    state.submitting = true;
    try {
        await updateAdminAnnouncementApi(id, {
            title: editForm.title.trim(),
            content: editForm.content.trim(),
            target: editForm.target,
            expiresAt: editForm.expiresAt || undefined
        });
        toast.success('公告更新成功');
        state.editingId = null;
        await loadList(true);
    } catch (e) {
        toast.error(e.message || '更新公告失败');
    } finally {
        state.submitting = false;
    }
};

const handlePublish = async (item) => {
    state.actionLoadingId = item.id;
    try {
        await publishAdminAnnouncementApi(item.id);
        toast.success('公告已发布');
        await loadList(true);
    } catch (e) {
        toast.error(e.message || '发布公告失败');
    } finally {
        state.actionLoadingId = null;
    }
};

const handleUnpublish = async (item) => {
    state.actionLoadingId = item.id;
    try {
        await unpublishAdminAnnouncementApi(item.id);
        toast.success('公告已撤回');
        await loadList(true);
    } catch (e) {
        toast.error(e.message || '撤回公告失败');
    } finally {
        state.actionLoadingId = null;
    }
};

const handleDelete = (item) => {
    openConfirmDialog({
        title: '确认删除公告',
        message: `确认要删除公告「${item.title}」吗？此操作不可恢复。`,
        confirmText: '删除',
        cancelText: '取消',
        onConfirm: async () => {
            state.actionLoadingId = item.id;
            try {
                await deleteAdminAnnouncementApi(item.id);
                toast.success('公告已删除');
                await loadList(true);
            } catch (e) {
                toast.error(e.message || '删除公告失败');
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

useAdminRefresh(() => loadList(true));
onMounted(() => loadList());
</script>

<template>
    <section class="admin-page-grid">
        <!-- 创建公告表单 -->
        <section class="dashboard-content-panel">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">公告管理</p>
                    <h2>创建新公告</h2>
                </div>
            </div>
            <form class="announcement-form" @submit.prevent="handleCreate">
                <div class="form-row">
                    <label class="form-label" for="ann-title">标题 <span class="required">*</span></label>
                    <input
                        id="ann-title"
                        v-model="form.title"
                        class="form-input"
                        type="text"
                        placeholder="公告标题"
                        maxlength="100"
                        :disabled="state.submitting"
                    >
                </div>
                <div class="form-row">
                    <label class="form-label" for="ann-content">内容 <span class="required">*</span></label>
                    <textarea
                        id="ann-content"
                        v-model="form.content"
                        class="form-textarea"
                        rows="3"
                        placeholder="公告内容"
                        maxlength="500"
                        :disabled="state.submitting"
                    ></textarea>
                </div>
                <div class="form-row form-row-inline">
                    <div class="form-field">
                        <label class="form-label" for="ann-target">目标用户</label>
                        <select id="ann-target" v-model="form.target" class="form-select" :disabled="state.submitting">
                            <option v-for="opt in TARGET_OPTIONS" :key="opt.value" :value="opt.value">
                                {{ opt.label }}
                            </option>
                        </select>
                    </div>
                    <div class="form-field">
                        <label class="form-label" for="ann-expires">过期时间（可选）</label>
                        <input
                            id="ann-expires"
                            v-model="form.expiresAt"
                            class="form-input"
                            type="datetime-local"
                            :disabled="state.submitting"
                        >
                    </div>
                </div>
                <div class="form-actions">
                    <button class="primary-action" type="submit" :disabled="state.submitting">
                        {{ state.submitting ? '创建中...' : '创建公告' }}
                    </button>
                </div>
            </form>
        </section>

        <!-- 公告列表 -->
        <section class="dashboard-content-panel">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">公告列表</p>
                    <h2>所有公告 <span v-if="state.total > 0" class="count-badge">{{ state.total }}</span></h2>
                </div>
            </div>

            <p v-if="state.loading && !state.hasLoadedOnce" class="backend-state-text">加载中...</p>
            <p v-else-if="state.error && !state.hasLoadedOnce" class="backend-state-text error-text">{{ state.error }}</p>
            <p v-else-if="state.items.length === 0 && state.hasLoadedOnce" class="backend-state-text">暂无公告</p>

            <div v-if="state.items.length > 0" class="announcement-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>标题</th>
                            <th>目标</th>
                            <th>状态</th>
                            <th>发布时间</th>
                            <th>过期时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <template v-for="item in state.items" :key="item.id">
                            <tr :class="{ 'editing-row': state.editingId === item.id }">
                                <td class="td-title">
                                    <span>{{ item.title }}</span>
                                    <p class="td-content-preview">{{ item.content }}</p>
                                </td>
                                <td>{{ TARGET_OPTIONS.find(o => o.value === item.target)?.label || item.target }}</td>
                                <td>
                                    <span :class="item.published ? 'status-badge published' : 'status-badge draft'">
                                        {{ item.published ? '已发布' : '草稿' }}
                                    </span>
                                </td>
                                <td>{{ item.publishedAt || '-' }}</td>
                                <td>{{ item.expiresAt || '永不过期' }}</td>
                                <td class="td-actions">
                                    <button
                                        v-if="!item.published"
                                        class="action-btn action-btn-primary"
                                        type="button"
                                        :disabled="state.actionLoadingId === item.id"
                                        @click="handlePublish(item)"
                                    >发布</button>
                                    <button
                                        v-else
                                        class="action-btn"
                                        type="button"
                                        :disabled="state.actionLoadingId === item.id"
                                        @click="handleUnpublish(item)"
                                    >撤回</button>
                                    <button
                                        class="action-btn"
                                        type="button"
                                        :disabled="state.submitting"
                                        @click="startEdit(item)"
                                    >编辑</button>
                                    <button
                                        class="action-btn action-btn-danger"
                                        type="button"
                                        :disabled="state.actionLoadingId === item.id"
                                        @click="handleDelete(item)"
                                    >删除</button>
                                </td>
                            </tr>
                            <!-- 内联编辑行 -->
                            <tr v-if="state.editingId === item.id" class="edit-row">
                                <td colspan="6">
                                    <div class="edit-form">
                                        <div class="form-row">
                                            <label class="form-label">标题</label>
                                            <input
                                                v-model="editForm.title"
                                                class="form-input"
                                                type="text"
                                                maxlength="100"
                                                :disabled="state.submitting"
                                            >
                                        </div>
                                        <div class="form-row">
                                            <label class="form-label">内容</label>
                                            <textarea
                                                v-model="editForm.content"
                                                class="form-textarea"
                                                rows="3"
                                                maxlength="500"
                                                :disabled="state.submitting"
                                            ></textarea>
                                        </div>
                                        <div class="form-row form-row-inline">
                                            <div class="form-field">
                                                <label class="form-label">目标用户</label>
                                                <select v-model="editForm.target" class="form-select" :disabled="state.submitting">
                                                    <option v-for="opt in TARGET_OPTIONS" :key="opt.value" :value="opt.value">
                                                        {{ opt.label }}
                                                    </option>
                                                </select>
                                            </div>
                                            <div class="form-field">
                                                <label class="form-label">过期时间</label>
                                                <input
                                                    v-model="editForm.expiresAt"
                                                    class="form-input"
                                                    type="datetime-local"
                                                    :disabled="state.submitting"
                                                >
                                            </div>
                                        </div>
                                        <div class="edit-form-actions">
                                            <button
                                                class="primary-action"
                                                type="button"
                                                :disabled="state.submitting"
                                                @click="handleUpdate(item.id)"
                                            >
                                                {{ state.submitting ? '保存中...' : '保存' }}
                                            </button>
                                            <button
                                                class="secondary-action"
                                                type="button"
                                                :disabled="state.submitting"
                                                @click="cancelEdit"
                                            >取消</button>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        </template>
                    </tbody>
                </table>
            </div>

            <AdminPagination
                v-if="state.total > state.pageSize"
                :page="state.page"
                :page-size="state.pageSize"
                :total="state.total"
                @page-change="changePage"
            />
        </section>
    </section>

    <ConfirmDialog
        v-if="confirmDialog.visible"
        :title="confirmDialog.title"
        :message="confirmDialog.message"
        :confirm-text="confirmDialog.confirmText"
        :cancel-text="confirmDialog.cancelText"
        @confirm="executeConfirmDialog"
        @cancel="closeConfirmDialog"
    />
</template>

<style scoped>
.announcement-form {
    display: grid;
    gap: 16px;
}

.form-row {
    display: grid;
    gap: 6px;
}

.form-row-inline {
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}

.form-field {
    display: grid;
    gap: 6px;
}

.form-label {
    color: var(--text-strong);
    font-size: 13px;
    font-weight: 600;
}

.form-label .required {
    color: var(--accent);
}

.form-input,
.form-select,
.form-textarea {
    width: 100%;
    padding: 8px 12px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-family: inherit;
    transition: border-color 0.12s;
    box-sizing: border-box;
}

.form-textarea {
    resize: vertical;
    min-height: 80px;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
    border-color: var(--brand);
    outline: none;
}

.form-input:disabled,
.form-select:disabled,
.form-textarea:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.form-actions {
    display: flex;
    gap: 12px;
}

.announcement-table-wrap {
    overflow-x: auto;
}

.admin-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 14px;
}

.admin-table th {
    padding: 10px 12px;
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
    text-align: left;
    background: var(--surface-soft);
    border-bottom: 1px solid var(--line);
}

.admin-table td {
    padding: 12px 12px;
    border-bottom: 1px solid var(--line);
    vertical-align: top;
}

.td-title {
    max-width: 240px;
}

.td-title span {
    display: block;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.td-content-preview {
    margin: 4px 0 0;
    color: var(--muted);
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.status-badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 600;
}

.status-badge.published {
    color: #065f46;
    background: #d1fae5;
}

.status-badge.draft {
    color: #92400e;
    background: #fef3c7;
}

.td-actions {
    display: flex;
    gap: 6px;
    align-items: center;
    flex-wrap: wrap;
}

.action-btn {
    padding: 4px 10px;
    color: var(--text);
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    font-size: 12px;
    font-weight: 600;
    transition: color 0.12s, border-color 0.12s, background 0.12s;
}

.action-btn:hover:not(:disabled) {
    color: var(--brand);
    border-color: var(--brand);
}

.action-btn-primary {
    color: #fff;
    background: var(--brand);
    border-color: var(--brand);
}

.action-btn-primary:hover:not(:disabled) {
    color: #fff;
    background: var(--brand-strong);
    border-color: var(--brand-strong);
}

.action-btn-danger:hover:not(:disabled) {
    color: var(--accent);
    border-color: var(--accent);
}

.action-btn:disabled {
    cursor: not-allowed;
    opacity: 0.5;
}

.editing-row {
    background: var(--brand-soft);
}

.edit-row td {
    padding: 0;
    background: var(--surface-soft);
    border-bottom: 2px solid var(--brand);
}

.edit-form {
    display: grid;
    gap: 14px;
    padding: 16px;
}

.edit-form-actions {
    display: flex;
    gap: 10px;
}

.count-badge {
    display: inline-block;
    padding: 1px 8px;
    color: var(--muted);
    font-size: 13px;
    font-weight: 400;
}

@media (max-width: 600px) {
    .form-row-inline {
        grid-template-columns: 1fr;
    }
}
</style>

