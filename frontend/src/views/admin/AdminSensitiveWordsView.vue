<script setup>
import { onMounted, reactive, ref } from 'vue';
import {
    createAdminSensitiveWordApi,
    deleteAdminSensitiveWordApi,
    getAdminSensitiveWordsApi,
    updateAdminSensitiveWordApi
} from '@/api/admin';
import { useAdminRefresh } from '@/views/admin/adminShared';

const loading = ref(false);
const error = ref('');
const items = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = 20;

const filters = reactive({ keyword: '', category: '', enabled: '' });

// ===== 弹窗 =====
const showModal = ref(false);
const saving = ref(false);
const editingId = ref(null);
const form = reactive({ word: '', category: '', level: 'WARN', enabled: true });
const formError = ref('');

const LEVELS = [
    { value: 'WARN', label: '警告（显示提示）' },
    { value: 'BLOCK', label: '拦截（禁止发布）' }
];

const CATEGORIES = ['政治', '色情', '广告', '暴力', '谣言', '其他'];

const load = async () => {
    loading.value = true;
    error.value = '';
    try {
        const enabledParam = filters.enabled === '' ? null : filters.enabled === 'true';
        const res = await getAdminSensitiveWordsApi(
            page.value, pageSize,
            filters.keyword || null,
            filters.category || null,
            enabledParam
        );
        items.value = res.items || [];
        total.value = res.total || 0;
    } catch (e) {
        error.value = e.message || '加载失败';
    } finally {
        loading.value = false;
    }
};

const search = () => { page.value = 1; load(); };
const goPage = (p) => { page.value = p; load(); };
const totalPages = () => Math.ceil(total.value / pageSize) || 1;

const openCreate = () => {
    editingId.value = null;
    form.word = '';
    form.category = '';
    form.level = 'WARN';
    form.enabled = true;
    formError.value = '';
    showModal.value = true;
};

const openEdit = (item) => {
    editingId.value = item.id;
    form.word = item.word;
    form.category = item.category || '';
    form.level = item.level || 'WARN';
    form.enabled = item.enabled !== false;
    formError.value = '';
    showModal.value = true;
};

const closeModal = () => { showModal.value = false; };

const save = async () => {
    if (!form.word.trim()) { formError.value = '敏感词不能为空'; return; }
    saving.value = true;
    formError.value = '';
    try {
        const payload = {
            word: form.word.trim(),
            category: form.category || null,
            level: form.level,
            enabled: form.enabled
        };
        if (editingId.value) {
            await updateAdminSensitiveWordApi(editingId.value, payload);
        } else {
            await createAdminSensitiveWordApi(payload);
        }
        showModal.value = false;
        await load();
    } catch (e) {
        formError.value = e.message || '保存失败';
    } finally {
        saving.value = false;
    }
};

const remove = async (item) => {
    if (!confirm(`确认删除敏感词「${item.word}」？`)) return;
    try {
        await deleteAdminSensitiveWordApi(item.id);
        await load();
    } catch (e) {
        alert(e.message || '删除失败');
    }
};

const toggleEnabled = async (item) => {
    try {
        await updateAdminSensitiveWordApi(item.id, { enabled: !item.enabled });
        item.enabled = !item.enabled;
    } catch (e) {
        alert(e.message || '操作失败');
    }
};

useAdminRefresh(load);
onMounted(load);
</script>

<template>
    <section class="admin-page-grid">
        <section class="dashboard-content-panel">
            <!-- 工具栏 -->
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">内容安全</p>
                    <h2>敏感词管理</h2>
                </div>
                <button class="primary-action" @click="openCreate">+ 新增敏感词</button>
            </div>

            <!-- 筛选栏 -->
            <form class="admin-filter-bar" @submit.prevent="search">
                <input v-model="filters.keyword" class="admin-input" placeholder="搜索敏感词…" />
                <select v-model="filters.category" class="admin-input">
                    <option value="">全部分类</option>
                    <option v-for="c in CATEGORIES" :key="c" :value="c">{{ c }}</option>
                </select>
                <select v-model="filters.enabled" class="admin-input">
                    <option value="">全部状态</option>
                    <option value="true">已启用</option>
                    <option value="false">已禁用</option>
                </select>
                <button type="submit" class="admin-btn-sm">搜索</button>
            </form>

            <p v-if="loading" class="backend-state-text">加载中...</p>
            <p v-else-if="error" class="backend-state-text error-text">{{ error }}</p>

            <!-- 表格 -->
            <div v-else-if="items.length" class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>敏感词</th>
                            <th>分类</th>
                            <th>等级</th>
                            <th>状态</th>
                            <th>创建时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="item in items" :key="item.id">
                            <td><strong>{{ item.word }}</strong></td>
                            <td>{{ item.category || '-' }}</td>
                            <td>
                                <span :class="['level-badge', item.level === 'BLOCK' ? 'level-block' : 'level-warn']">
                                    {{ item.level === 'BLOCK' ? '拦截' : '警告' }}
                                </span>
                            </td>
                            <td>
                                <button
                                    class="toggle-btn"
                                    :class="item.enabled ? 'enabled' : 'disabled'"
                                    @click="toggleEnabled(item)"
                                >{{ item.enabled ? '启用' : '禁用' }}</button>
                            </td>
                            <td class="text-sm">{{ item.createdAt ? String(item.createdAt).slice(0, 10) : '-' }}</td>
                            <td>
                                <div class="action-btns">
                                    <button class="admin-btn-sm" @click="openEdit(item)">编辑</button>
                                    <button class="admin-btn-sm danger" @click="remove(item)">删除</button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <p v-else class="backend-state-text">暂无敏感词数据</p>

            <!-- 分页 -->
            <div v-if="total > pageSize" class="admin-pagination">
                <button :disabled="page <= 1" class="admin-btn-sm" @click="goPage(page - 1)">上一页</button>
                <span>第 {{ page }} / {{ totalPages() }} 页，共 {{ total }} 条</span>
                <button :disabled="page >= totalPages()" class="admin-btn-sm" @click="goPage(page + 1)">下一页</button>
            </div>
        </section>
    </section>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
        <div v-if="showModal" class="sw-modal-overlay" @click.self="closeModal">
            <div class="sw-modal" role="dialog" aria-modal="true">
                <h3>{{ editingId ? '编辑敏感词' : '新增敏感词' }}</h3>

                <label class="sw-field">
                    <span>敏感词 <em>*</em></span>
                    <input v-model="form.word" class="admin-input" placeholder="输入敏感词" />
                </label>

                <label class="sw-field">
                    <span>分类</span>
                    <select v-model="form.category" class="admin-input">
                        <option value="">不分类</option>
                        <option v-for="c in CATEGORIES" :key="c" :value="c">{{ c }}</option>
                    </select>
                </label>

                <label class="sw-field">
                    <span>拦截等级</span>
                    <select v-model="form.level" class="admin-input">
                        <option v-for="l in LEVELS" :key="l.value" :value="l.value">{{ l.label }}</option>
                    </select>
                </label>

                <label class="sw-field sw-field-inline">
                    <input type="checkbox" v-model="form.enabled" />
                    <span>启用该敏感词</span>
                </label>

                <p v-if="formError" class="error-text sw-error">{{ formError }}</p>

                <div class="sw-modal-footer">
                    <button class="admin-btn-sm" @click="closeModal">取消</button>
                    <button class="primary-action" :disabled="saving" @click="save">
                        {{ saving ? '保存中...' : '保存' }}
                    </button>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.admin-filter-bar {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 16px;
}
.admin-input {
    padding: 6px 10px;
    border: 1px solid var(--border-color, #e5e7eb);
    border-radius: var(--radius-sm, 6px);
    font-size: 13px;
    background: var(--surface);
    color: var(--text-primary);
    min-width: 120px;
}
.admin-table-wrap { overflow-x: auto; }
.admin-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
}
.admin-table th,
.admin-table td {
    padding: 10px 12px;
    border-bottom: 1px solid var(--border-color, #e5e7eb);
    text-align: left;
    white-space: nowrap;
}
.admin-table th { font-weight: 600; color: var(--text-secondary); }
.admin-table tbody tr:hover { background: var(--surface-hover, #f9fafb); }
.level-badge {
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 600;
}
.level-warn { background: #fef3c7; color: #92400e; }
.level-block { background: #fee2e2; color: #991b1b; }
.toggle-btn {
    padding: 2px 10px;
    border-radius: 12px;
    font-size: 12px;
    border: none;
    cursor: pointer;
    font-weight: 600;
}
.toggle-btn.enabled { background: #d1fae5; color: #065f46; }
.toggle-btn.disabled { background: #f3f4f6; color: #6b7280; }
.action-btns { display: flex; gap: 6px; }
.admin-btn-sm {
    padding: 4px 10px;
    border: 1px solid var(--border-color, #e5e7eb);
    border-radius: var(--radius-sm, 6px);
    background: var(--surface);
    font-size: 12px;
    cursor: pointer;
    color: var(--text-primary);
}
.admin-btn-sm:hover { background: var(--surface-hover, #f3f4f6); }
.admin-btn-sm.danger { color: #dc2626; border-color: #fca5a5; }
.admin-btn-sm.danger:hover { background: #fee2e2; }
.admin-btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.admin-pagination {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 16px;
    font-size: 13px;
    color: var(--text-secondary);
}
.text-sm { font-size: 12px; color: var(--text-secondary); }

/* 弹窗 */
.sw-modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,.45);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}
.sw-modal {
    background: var(--surface);
    border-radius: 12px;
    padding: 28px 32px;
    width: 420px;
    max-width: 96vw;
    box-shadow: 0 20px 60px rgba(0,0,0,.18);
}
.sw-modal h3 {
    margin: 0 0 20px;
    font-size: 16px;
    font-weight: 700;
}
.sw-field {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 14px;
    font-size: 13px;
}
.sw-field em { color: #dc2626; font-style: normal; }
.sw-field-inline { flex-direction: row; align-items: center; gap: 8px; }
.sw-error { font-size: 12px; color: #dc2626; margin-top: 4px; }
.sw-modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 20px;
}
</style>

