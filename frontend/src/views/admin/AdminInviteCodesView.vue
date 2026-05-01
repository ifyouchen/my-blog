<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { getAdminInviteCodesApi, deleteAdminInviteCodeApi } from '@/api/admin.js';
import { formatAdminDateTime } from '@/views/admin/adminShared';

const list = ref([]);
const total = ref(0);
const loading = ref(false);
const keyword = ref('');
const page = ref(1);
const pageSize = ref(20);

const statusMap = {
    ACTIVE: { label: '有效', cls: 'status-active' },
    USED: { label: '已使用', cls: 'status-used' },
    EXPIRED: { label: '已过期', cls: 'status-expired' }
};

async function fetchList() {
    loading.value = true;
    try {
        const res = await getAdminInviteCodesApi(page.value, pageSize.value, keyword.value);
        if (res && res.code === 0) {
            list.value = res.data.items || [];
            total.value = res.data.total || 0;
        }
    } finally {
        loading.value = false;
    }
}

async function handleDelete(id) {
    if (!confirm('确认删除该邀请码？')) return;
    const res = await deleteAdminInviteCodeApi(id);
    if (res && res.code === 0) {
        alert('删除成功');
        fetchList();
    } else {
        alert(res?.message || '删除失败');
    }
}

function handleSearch() {
    page.value = 1;
    fetchList();
}

function prevPage() {
    if (page.value > 1) { page.value--; fetchList(); }
}

function nextPage() {
    if (page.value * pageSize.value < total.value) { page.value++; fetchList(); }
}

function formatDate(dt) {
    return formatAdminDateTime(dt);
}

onMounted(fetchList);
</script>

<template>
    <div class="invite-codes-page">
        <div class="toolbar">
            <input v-model="keyword" placeholder="搜索邀请码..." @keyup.enter="handleSearch" class="search-input" />
            <button class="btn-primary" @click="handleSearch">搜索</button>
        </div>

        <div v-if="loading" class="loading-tip">加载中...</div>

        <table v-else class="data-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>邀请码</th>
                <th>创建者ID</th>
                <th>使用者ID</th>
                <th>使用时间</th>
                <th>过期时间</th>
                <th>使用次数</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-if="list.length === 0">
                <td colspan="10" class="empty-td">暂无数据</td>
            </tr>
            <tr v-for="row in list" :key="row.id">
                <td>{{ row.id }}</td>
                <td class="code-cell">{{ row.code }}</td>
                <td>{{ row.creatorId }}</td>
                <td>{{ row.usedBy || '-' }}</td>
                <td>{{ formatDate(row.usedAt) }}</td>
                <td>{{ formatDate(row.expiredAt) }}</td>
                <td>{{ row.useCount }}/{{ row.maxUses }}</td>
                <td>
                    <span :class="['status-badge', statusMap[row.status]?.cls]">
                        {{ statusMap[row.status]?.label || row.status }}
                    </span>
                </td>
                <td>{{ formatDate(row.createdAt) }}</td>
                <td>
                    <button class="btn-danger-sm" @click="handleDelete(row.id)">删除</button>
                </td>
            </tr>
            </tbody>
        </table>

        <div class="pagination" v-if="total > 0">
            <button @click="prevPage" :disabled="page <= 1">上一页</button>
            <span>第 {{ page }} 页 / 共 {{ Math.ceil(total / pageSize) }} 页 ({{ total }} 条)</span>
            <button @click="nextPage" :disabled="page * pageSize >= total">下一页</button>
        </div>
    </div>
</template>

<style scoped>
.invite-codes-page { padding: 0; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.search-input { flex: 1; max-width: 320px; padding: 8px 12px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 14px; }
.btn-primary { padding: 8px 16px; background: #2563eb; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; }
.btn-primary:hover { background: #1d4ed8; }
.loading-tip { text-align: center; padding: 40px; color: #6b7280; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { background: #f9fafb; padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; color: #374151; font-weight: 600; }
.data-table td { padding: 10px 12px; border-bottom: 1px solid #f3f4f6; color: #374151; }
.data-table tr:hover td { background: #f9fafb; }
.empty-td { text-align: center; color: #9ca3af; padding: 40px; }
.code-cell { font-family: monospace; font-weight: 600; letter-spacing: 1px; color: #1d4ed8; }
.status-badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.status-active { background: #dcfce7; color: #16a34a; }
.status-used { background: #f3f4f6; color: #6b7280; }
.status-expired { background: #fef9c3; color: #854d0e; }
.btn-danger-sm { padding: 4px 10px; background: #ef4444; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; }
.btn-danger-sm:hover { background: #dc2626; }
.pagination { display: flex; align-items: center; gap: 12px; margin-top: 16px; justify-content: center; }
.pagination button { padding: 6px 14px; border: 1px solid #d1d5db; border-radius: 6px; background: #fff; cursor: pointer; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { color: #6b7280; font-size: 14px; }
</style>

