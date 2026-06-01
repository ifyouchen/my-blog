<script setup>
import {inject, nextTick, onMounted, reactive, ref} from 'vue';
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
import {createPagedState, formatAdminDateTime, useAdminRefresh} from '@/views/admin/adminShared';
import {useConfirmDialog} from '@/composables/useConfirmDialog';
import {sanitizeAnnouncementHtml} from '@/utils/markdown';

const toast = inject('toast', { success: () => {}, error: () => {} });
const {confirmDialog, openConfirmDialog, closeConfirmDialog, executeConfirmDialog} = useConfirmDialog();

const TARGET_OPTIONS = [
    { value: 'ALL', label: '所有用户' },
    { value: 'USER', label: '普通用户' },
    { value: 'ADMIN', label: '管理员' }
];

const COLOR_OPTIONS = [
    { value: '#2563eb', label: 'blue' },
    { value: '#16a34a', label: 'green' },
    { value: '#dc2626', label: 'red' },
    { value: '#9333ea', label: 'purple' },
    { value: '#111827', label: 'black' }
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
const createContentInput = ref(null);
const editContentInput = ref(null);
const linkUrlInput = ref(null);
const linkDialog = reactive({
    visible: false,
    mode: 'create',
    url: 'https://',
    text: '',
    selection: null
});

const resetForm = () => Object.assign(form, emptyForm());

const getSelection = (inputRef, content = '') => {
    const input = inputRef.value;
    const length = content.length;
    if (!input) {
        return { start: length, end: length, selected: '' };
    }
    const start = typeof input.selectionStart === 'number' ? input.selectionStart : length;
    const end = typeof input.selectionEnd === 'number' ? input.selectionEnd : start;
    return {
        start,
        end,
        selected: content.slice(start, end)
    };
};

const insertContentMarkup = async (targetForm, inputRef, before, after, fallback, selection = null, text = null) => {
    const content = targetForm.content || '';
    const range = selection || getSelection(inputRef, content);
    const selected = text || range.selected || fallback;
    targetForm.content = content.slice(0, range.start) + before + selected + after + content.slice(range.end);
    await nextTick();
    if (inputRef.value) {
        const selectionStart = range.start + before.length;
        const selectionEnd = selectionStart + selected.length;
        inputRef.value.focus();
        inputRef.value.setSelectionRange(selectionStart, selectionEnd);
    }
};

const applyBold = (targetForm, inputRef) => {
    insertContentMarkup(targetForm, inputRef, '<strong>', '</strong>', '加粗文本');
};

const getLinkTarget = () => (
    linkDialog.mode === 'edit'
        ? { targetForm: editForm, inputRef: editContentInput }
        : { targetForm: form, inputRef: createContentInput }
);

const openLinkDialog = async (mode) => {
    const inputRef = mode === 'edit' ? editContentInput : createContentInput;
    const targetForm = mode === 'edit' ? editForm : form;
    const selection = getSelection(inputRef, targetForm.content || '');
    Object.assign(linkDialog, {
        visible: true,
        mode,
        url: 'https://',
        text: selection.selected || '',
        selection
    });
    await nextTick();
    if (linkUrlInput.value) {
        linkUrlInput.value.focus();
        linkUrlInput.value.select();
    }
};

const closeLinkDialog = () => {
    linkDialog.visible = false;
};

const normalizeLinkUrl = (url = '') => {
    const value = url.trim();
    if (!value) {
        return '';
    }
    if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(value) || /^(#|\/)/.test(value)) {
        return value;
    }
    return `https://${value}`;
};

const escapeAttribute = (value = '') => String(value)
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;');

const confirmLinkDialog = async () => {
    const href = normalizeLinkUrl(linkDialog.url);
    if (!href) {
        toast.error('链接地址不能为空');
        return;
    }
    const { targetForm, inputRef } = getLinkTarget();
    await insertContentMarkup(
        targetForm,
        inputRef,
        `<a href="${escapeAttribute(href)}">`,
        '</a>',
        '链接文字',
        linkDialog.selection,
        linkDialog.text.trim() || null
    );
    closeLinkDialog();
};

const applyColor = (targetForm, inputRef, color) => {
    insertContentMarkup(targetForm, inputRef, `<span style="color:${color}">`, '</span>', '彩色文本');
};

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
        tone: 'danger',
        onConfirm: async () => {
            state.actionLoadingId = item.id;
            try {
                await deleteAdminAnnouncementApi(item.id);
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
                    <div class="content-editor">
                        <div class="content-toolbar" aria-label="公告内容工具栏">
                            <button
                                class="toolbar-btn toolbar-btn-bold"
                                type="button"
                                title="加粗"
                                aria-label="加粗"
                                :disabled="state.submitting"
                                @click="applyBold(form, createContentInput)"
                            >B</button>
                            <button
                                class="toolbar-btn"
                                type="button"
                                title="链接"
                                aria-label="链接"
                                :disabled="state.submitting"
                                @click="openLinkDialog('create')"
                            >
                                <svg viewBox="0 0 24 24" aria-hidden="true">
                                    <path d="M9 7h-1a5 5 0 0 0 0 10h3"></path>
                                    <path d="M15 7h1a5 5 0 0 1 0 10h-3"></path>
                                    <path d="M8 12h8"></path>
                                </svg>
                            </button>
                            <div class="toolbar-divider" aria-hidden="true"></div>
                            <button
                                v-for="color in COLOR_OPTIONS"
                                :key="color.value"
                                class="color-swatch"
                                type="button"
                                :style="{ backgroundColor: color.value }"
                                :title="color.label"
                                :aria-label="`颜色 ${color.label}`"
                                :disabled="state.submitting"
                                @click="applyColor(form, createContentInput, color.value)"
                            ></button>
                        </div>
                        <textarea
                            id="ann-content"
                            ref="createContentInput"
                            v-model="form.content"
                            class="form-textarea"
                            rows="3"
                            placeholder="公告内容"
                            maxlength="2000"
                            :disabled="state.submitting"
                        ></textarea>
                    </div>
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
                        创建公告
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

            <p v-if="state.error && !state.hasLoadedOnce" class="backend-state-text error-text">{{ state.error }}</p>
            <p v-if="state.items.length === 0 && state.hasLoadedOnce" class="backend-state-text">暂无公告</p>

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
                                    <div
                                        class="td-content-preview"
                                        v-html="sanitizeAnnouncementHtml(item.content)"
                                    ></div>
                                </td>
                                <td>{{ TARGET_OPTIONS.find(o => o.value === item.target)?.label || item.target }}</td>
                                <td>
                                    <span :class="item.published ? 'status-badge published' : 'status-badge draft'">
                                        {{ item.published ? '已发布' : '草稿' }}
                                    </span>
                                </td>
                                <td>{{ formatAdminDateTime(item.publishedAt) }}</td>
                                <td>{{ formatAdminDateTime(item.expiresAt, '永不过期') }}</td>
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
                                            <div class="content-editor">
                                                <div class="content-toolbar" aria-label="公告内容工具栏">
                                                    <button
                                                        class="toolbar-btn toolbar-btn-bold"
                                                        type="button"
                                                        title="加粗"
                                                        aria-label="加粗"
                                                        :disabled="state.submitting"
                                                        @click="applyBold(editForm, editContentInput)"
                                                    >B</button>
                                                    <button
                                                        class="toolbar-btn"
                                                        type="button"
                                                        title="链接"
                                                        aria-label="链接"
                                                        :disabled="state.submitting"
                                                        @click="openLinkDialog('edit')"
                                                    >
                                                        <svg viewBox="0 0 24 24" aria-hidden="true">
                                                            <path d="M9 7h-1a5 5 0 0 0 0 10h3"></path>
                                                            <path d="M15 7h1a5 5 0 0 1 0 10h-3"></path>
                                                            <path d="M8 12h8"></path>
                                                        </svg>
                                                    </button>
                                                    <div class="toolbar-divider" aria-hidden="true"></div>
                                                    <button
                                                        v-for="color in COLOR_OPTIONS"
                                                        :key="color.value"
                                                        class="color-swatch"
                                                        type="button"
                                                        :style="{ backgroundColor: color.value }"
                                                        :title="color.label"
                                                        :aria-label="`颜色 ${color.label}`"
                                                        :disabled="state.submitting"
                                                        @click="applyColor(editForm, editContentInput, color.value)"
                                                    ></button>
                                                </div>
                                                <textarea
                                                    ref="editContentInput"
                                                    v-model="editForm.content"
                                                    class="form-textarea"
                                                    rows="3"
                                                    maxlength="2000"
                                                    :disabled="state.submitting"
                                                ></textarea>
                                            </div>
                                        </div>
                                        <div class="form-row form-row-inline">
                                            <div class="form-field">
                                                <label class="form-label">目标用户</label>
                                                <select
                                                    v-model="editForm.target"
                                                    class="form-select"
                                                    :disabled="state.submitting"
                                                >
                                                    <option
                                                        v-for="opt in TARGET_OPTIONS"
                                                        :key="opt.value"
                                                        :value="opt.value"
                                                    >
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
                                                保存
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
        v-bind="confirmDialog"
        :title="confirmDialog.title"
        :message="confirmDialog.message"
        :confirm-text="confirmDialog.confirmText"
        :cancel-text="confirmDialog.cancelText"
        :tone="confirmDialog.tone"
        :loading="confirmDialog.loading"
        @confirm="executeConfirmDialog"
        @close="closeConfirmDialog"
    />

    <div
        v-if="linkDialog.visible"
        class="link-dialog-backdrop"
        @click.self="closeLinkDialog"
    >
        <form class="link-dialog" role="dialog" aria-modal="true" @submit.prevent="confirmLinkDialog">
            <div class="link-dialog-header">
                <h3>添加链接</h3>
                <button class="dialog-close-btn" type="button" aria-label="关闭" @click="closeLinkDialog">×</button>
            </div>
            <label class="form-row">
                <span class="form-label">链接文字</span>
                <input
                    v-model="linkDialog.text"
                    class="form-input"
                    type="text"
                    maxlength="100"
                    placeholder="链接文字"
                >
            </label>
            <label class="form-row">
                <span class="form-label">链接地址</span>
                <input
                    ref="linkUrlInput"
                    v-model="linkDialog.url"
                    class="form-input"
                    type="text"
                    maxlength="500"
                    placeholder="https://example.com"
                >
            </label>
            <div class="link-dialog-actions">
                <button class="secondary-action" type="button" @click="closeLinkDialog">取消</button>
                <button class="primary-action" type="submit">插入链接</button>
            </div>
        </form>
    </div>
</template>

<style scoped src="@/styles/views/admin/AdminAnnouncementsView.part-1.css"></style>
<style scoped src="@/styles/views/admin/AdminAnnouncementsView.part-2.css"></style>
