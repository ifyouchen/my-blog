<script setup>
import {inject, onMounted, reactive, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {createAdminAdApi, deleteAdminAdApi, getAdminAdsApi, getAdminAdStatsApi, updateAdminAdApi} from '@/api/ads';
import {createPagedState, formatAdminDateTime, readPositiveInt, readQueryText, resolveAdminOverflowPage, syncAdminQuery, useAdminRefresh} from '@/views/admin/adminShared';
import {useConfirmDialog} from '@/composables/useConfirmDialog';

const route = useRoute();
const router = useRouter();
const toast = inject('toast', { success: () => {}, error: () => {} });
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const SLOT_OPTIONS = [
    { value: 'home_sidebar', label: '首页右侧栏' },
    { value: 'article_sidebar', label: '文章详情右侧' },
    { value: 'search_top', label: '搜索结果顶部' }
];

const state = reactive({
    ...createPagedState(10),
    slotCode: '',
    enabled: '',
    submitting: false,
    editingId: null,
    actionLoadingId: null
});

const form = reactive({
    slotCode: 'home_sidebar',
    title: '',
    imageUrl: '',
    targetUrl: '',
    label: '广告',
    startAt: '',
    endAt: '',
    enabled: true,
    sortOrder: 0
});

const editForm = reactive({
    slotCode: 'home_sidebar',
    title: '',
    imageUrl: '',
    targetUrl: '',
    label: '广告',
    startAt: '',
    endAt: '',
    enabled: true,
    sortOrder: 0
});

const resetForm = () => {
    form.slotCode = 'home_sidebar';
    form.title = '';
    form.imageUrl = '';
    form.targetUrl = '';
    form.label = '广告';
    form.startAt = '';
    form.endAt = '';
    form.enabled = true;
    form.sortOrder = 0;
};

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.slotCode = readQueryText(route, 'slotCode');
    state.enabled = readQueryText(route, 'enabled');
    state.jumpPage = String(state.page);
};

const loadAds = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminAdsApi(state.page, state.pageSize, {
            slotCode: state.slotCode || null,
            enabled: state.enabled !== '' ? state.enabled : undefined
        });
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
        state.error = error.message || '广告列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        slotCode: patch.slotCode ?? (state.slotCode || undefined),
        enabled: patch.enabled ?? (state.enabled !== '' ? state.enabled : undefined)
    });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({ page: targetPage > 1 ? String(targetPage) : undefined });
};

const submitFilters = async () => {
    state.page = 1;
    await syncQuery({
        page: undefined,
        slotCode: state.slotCode || undefined,
        enabled: state.enabled !== '' ? state.enabled : undefined
    });
};

const resetFilters = async () => {
    state.slotCode = '';
    state.enabled = '';
    state.page = 1;
    await syncQuery({ page: undefined, slotCode: undefined, enabled: undefined });
};

const submitCreate = async () => {
    if (!form.title.trim()) {
        toast.error('广告标题不能为空');
        return;
    }
    if (!form.targetUrl.trim()) {
        toast.error('跳转链接不能为空');
        return;
    }
    state.submitting = true;
    try {
        await createAdminAdApi({
            slotCode: form.slotCode,
            title: form.title.trim(),
            imageUrl: form.imageUrl.trim() || null,
            targetUrl: form.targetUrl.trim(),
            label: form.label.trim() || '广告',
            startAt: form.startAt || null,
            endAt: form.endAt || null,
            enabled: form.enabled,
            sortOrder: Number(form.sortOrder) || 0
        });
        resetForm();
        await loadAds();
    } catch (error) {
        toast.error(error.message || '广告创建失败');
    } finally {
        state.submitting = false;
    }
};

const startEdit = (ad) => {
    state.editingId = ad.id;
    editForm.slotCode = ad.slotCode || 'home_sidebar';
    editForm.title = ad.title || '';
    editForm.imageUrl = ad.imageUrl || '';
    editForm.targetUrl = ad.targetUrl || '';
    editForm.label = ad.label || '广告';
    editForm.startAt = ad.startAt || '';
    editForm.endAt = ad.endAt || '';
    editForm.enabled = ad.enabled !== false;
    editForm.sortOrder = ad.sortOrder || 0;
};

const cancelEdit = () => {
    state.editingId = null;
};

const submitEdit = async (ad) => {
    if (!editForm.title.trim()) {
        toast.error('广告标题不能为空');
        return;
    }
    if (!editForm.targetUrl.trim()) {
        toast.error('跳转链接不能为空');
        return;
    }
    state.actionLoadingId = ad.id;
    try {
        await updateAdminAdApi(ad.id, {
            slotCode: editForm.slotCode,
            title: editForm.title.trim(),
            imageUrl: editForm.imageUrl.trim() || null,
            targetUrl: editForm.targetUrl.trim(),
            label: editForm.label.trim() || '广告',
            startAt: editForm.startAt || null,
            endAt: editForm.endAt || null,
            enabled: editForm.enabled,
            sortOrder: Number(editForm.sortOrder) || 0
        });
        state.editingId = null;
        await loadAds();
    } catch (error) {
        toast.error(error.message || '广告更新失败');
    } finally {
        state.actionLoadingId = null;
    }
};

const confirmDelete = (ad) => {
    openConfirmDialog({
        eyebrow: '广告操作',
        title: '删除广告',
        message: `确定删除广告「${ad.title}」吗？删除后不可恢复。`,
        confirmText: '确认删除',
        tone: 'danger',
        onConfirm: async () => {
            state.actionLoadingId = ad.id;
            try {
                await deleteAdminAdApi(ad.id);
                await loadAds();
            } catch (error) {
                toast.error(error.message || '删除失败');
                throw error;
            } finally {
                state.actionLoadingId = null;
            }
        }
    });
};

const getSlotLabel = (code) => {
    const opt = SLOT_OPTIONS.find((item) => item.value === code);
    return opt ? opt.label : code;
};

const formatDate = (d) => formatAdminDateTime(d, '—');

// ======== 统计概览 ========
const stats = ref(null);
const statsLoading = ref(false);

const loadStats = async () => {
    statsLoading.value = true;
    try {
        stats.value = await getAdminAdStatsApi();
    } catch (e) {
        // 统计加载失败不影响列表
    } finally {
        statsLoading.value = false;
    }
};

onMounted(loadStats);

useAdminRefresh(loadAds);

watch(
    () => [route.query.page, route.query.slotCode, route.query.enabled],
    () => {
        applyRouteState();
        loadAds();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel">
        <!-- 统计概览 -->
        <div class="admin-stats-overview" data-testid="admin-ads-stats">
            <div class="admin-stat-card">
                <span class="stat-label">广告总数</span>
                <span class="stat-value">{{ statsLoading ? '…' : (stats ? stats.totalCampaigns : '—') }}</span>
            </div>
            <div class="admin-stat-card">
                <span class="stat-label">启用中</span>
                <span class="stat-value enabled">{{ statsLoading ? '…' : (stats ? stats.enabledCampaigns : '—') }}</span>
            </div>
            <div class="admin-stat-card">
                <span class="stat-label">总曝光</span>
                <span class="stat-value">{{ statsLoading ? '…' : (stats ? stats.totalImpressions : '—') }}</span>
            </div>
            <div class="admin-stat-card">
                <span class="stat-label">总点击</span>
                <span class="stat-value">{{ statsLoading ? '…' : (stats ? stats.totalClicks : '—') }}</span>
            </div>
        </div>

        <!-- 广告位明细统计 -->
        <div v-if="stats && stats.slotStats && stats.slotStats.length" class="admin-slot-stats">
            <p class="admin-slot-stats-title">各广告位统计</p>
            <div class="admin-slot-stats-grid">
                <div
                    v-for="slot in stats.slotStats"
                    :key="slot.slotCode"
                    class="admin-slot-stat-card"
                >
                    <span class="slot-name">{{ getSlotLabel(slot.slotCode) }}</span>
                    <div class="slot-numbers">
                        <span>{{ slot.campaignCount }} 条投放</span>
                        <span>{{ slot.impressions }} 曝光</span>
                        <span>{{ slot.clicks }} 点击</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 创建广告表单 -->
        <div class="admin-create-form admin-form-card">
            <p class="eyebrow">新增广告</p>
            <h3>创建广告投放</h3>
            <form class="admin-form-grid" @submit.prevent="submitCreate">
                <label>
                    <span>广告位</span>
                    <select v-model="form.slotCode" :disabled="state.submitting">
                        <option v-for="opt in SLOT_OPTIONS" :key="opt.value" :value="opt.value">
                            {{ opt.label }}
                        </option>
                    </select>
                </label>
                <label>
                    <span>广告标题 *</span>
                    <input
                        v-model="form.title"
                        type="text"
                        placeholder="广告标题"
                        maxlength="200"
                        :disabled="state.submitting"
                    >
                </label>
                <label>
                    <span>图片地址</span>
                    <input
                        v-model="form.imageUrl"
                        type="text"
                        placeholder="https://..."
                        :disabled="state.submitting"
                    >
                </label>
                <label>
                    <span>跳转链接 *</span>
                    <input
                        v-model="form.targetUrl"
                        type="text"
                        placeholder="https://..."
                        :disabled="state.submitting"
                    >
                </label>
                <label>
                    <span>标签</span>
                    <input
                        v-model="form.label"
                        type="text"
                        placeholder="广告"
                        maxlength="50"
                        :disabled="state.submitting"
                    >
                </label>
                <label>
                    <span>排序值（小到前）</span>
                    <input
                        v-model.number="form.sortOrder"
                        type="number"
                        min="0"
                        :disabled="state.submitting"
                    >
                </label>
                <label>
                    <span>开始时间</span>
                    <input v-model="form.startAt" type="datetime-local" :disabled="state.submitting">
                </label>
                <label>
                    <span>结束时间</span>
                    <input v-model="form.endAt" type="datetime-local" :disabled="state.submitting">
                </label>
                <label class="admin-form-toggle">
                    <input v-model="form.enabled" type="checkbox" :disabled="state.submitting">
                    <span>立即启用</span>
                </label>
                <div class="admin-form-actions">
                    <button type="submit" :disabled="state.submitting">
                        创建广告
                    </button>
                </div>
            </form>
        </div>

        <!-- 筛选栏 -->
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar" @submit.prevent="submitFilters">
                <label>
                    <span>广告位</span>
                    <select v-model="state.slotCode">
                        <option value="">全部广告位</option>
                        <option v-for="opt in SLOT_OPTIONS" :key="opt.value" :value="opt.value">
                            {{ opt.label }}
                        </option>
                    </select>
                </label>
                <label>
                    <span>启用状态</span>
                    <select v-model="state.enabled">
                        <option value="">全部状态</option>
                        <option value="true">已启用</option>
                        <option value="false">已禁用</option>
                    </select>
                </label>
                <div class="admin-filter-actions">
                    <button type="submit">查询</button>
                    <button type="button" @click="resetFilters">重置</button>
                </div>
            </form>
        </div>

        <!-- 列表 -->
        <div class="admin-table-shell">
            <p v-if="state.error && state.items.length" class="backend-state-text error-text subtle">
                {{ state.error }}
            </p>
            <p v-else-if="state.error && !state.items.length" class="backend-state-text error-text">
                {{ state.error }}
            </p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-ads-table">
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>广告位</th>
                                <th>标题</th>
                                <th>标签</th>
                                <th>状态</th>
                                <th>时间范围</th>
                                <th>曝光</th>
                                <th>点击</th>
                                <th>排序</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <template v-for="ad in state.items" :key="ad.id">
                                <tr v-if="state.editingId !== ad.id">
                                    <td>#{{ ad.id }}</td>
                                    <td>{{ getSlotLabel(ad.slotCode) }}</td>
                                    <td>
                                        <strong>{{ ad.title }}</strong>
                                        <p v-if="ad.imageUrl" class="admin-subtext">{{ ad.imageUrl }}</p>
                                    </td>
                                    <td>
                                        <span class="ad-label-badge">{{ ad.label || '广告' }}</span>
                                    </td>
                                    <td>
                                        <span class="status-pill" :class="{ success: ad.enabled, danger: !ad.enabled }">
                                            {{ ad.enabled ? '已启用' : '已禁用' }}
                                        </span>
                                    </td>
                                    <td>
                                        <small>{{ formatDate(ad.startAt) }}</small>
                                        <small v-if="ad.endAt"> ~ {{ formatDate(ad.endAt) }}</small>
                                        <small v-else> ~ 不限</small>
                                    </td>
                                    <td>{{ ad.impressionCount || 0 }}</td>
                                    <td>{{ ad.clickCount || 0 }}</td>
                                    <td>{{ ad.sortOrder }}</td>
                                    <td class="table-actions">
                                        <button type="button" @click="startEdit(ad)">编辑</button>
                                        <button
                                            type="button"
                                            class="danger-link"
                                            :disabled="state.actionLoadingId === ad.id"
                                            @click="confirmDelete(ad)"
                                        >
                                            删除
                                        </button>
                                    </td>
                                </tr>
                                <tr v-else class="ad-edit-row">
                                    <td colspan="10">
                                        <form class="ad-inline-edit" @submit.prevent="submitEdit(ad)">
                                            <div class="ad-inline-edit-grid">
                                                <label>
                                                    <span>广告位</span>
                                                    <select v-model="editForm.slotCode">
                                                        <option v-for="opt in SLOT_OPTIONS" :key="opt.value" :value="opt.value">
                                                            {{ opt.label }}
                                                        </option>
                                                    </select>
                                                </label>
                                                <label>
                                                    <span>标题 *</span>
                                                    <input v-model="editForm.title" type="text" maxlength="200" required>
                                                </label>
                                                <label>
                                                    <span>图片地址</span>
                                                    <input v-model="editForm.imageUrl" type="text" placeholder="https://...">
                                                </label>
                                                <label>
                                                    <span>跳转链接 *</span>
                                                    <input v-model="editForm.targetUrl" type="text" required>
                                                </label>
                                                <label>
                                                    <span>标签</span>
                                                    <input v-model="editForm.label" type="text" maxlength="50">
                                                </label>
                                                <label>
                                                    <span>排序值</span>
                                                    <input v-model.number="editForm.sortOrder" type="number" min="0">
                                                </label>
                                                <label>
                                                    <span>开始时间</span>
                                                    <input v-model="editForm.startAt" type="datetime-local">
                                                </label>
                                                <label>
                                                    <span>结束时间</span>
                                                    <input v-model="editForm.endAt" type="datetime-local">
                                                </label>
                                                <label class="admin-form-toggle">
                                                    <input v-model="editForm.enabled" type="checkbox">
                                                    <span>启用</span>
                                                </label>
                                            </div>
                                            <div class="ad-inline-actions">
                                                <button
                                                    type="submit"
                                                    :disabled="state.actionLoadingId === ad.id"
                                                >
                                                    保存
                                                </button>
                                                <button type="button" @click="cancelEdit">取消</button>
                                            </div>
                                        </form>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有广告</p>
            </template>
        </div>

        <AdminPagination :state="state" label="广告分页" @page-change="changePage" />

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
.admin-form-card {
    padding: 20px;
    margin-bottom: 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.admin-form-card h3 {
    margin: 4px 0 16px;
    color: var(--text);
    font-size: 15px;
}

.admin-form-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    align-items: end;
}

.admin-form-grid label {
    display: grid;
    gap: 6px;
}

.admin-form-grid label span {
    color: var(--muted);
    font-size: 12px;
    font-weight: 700;
}

.admin-form-grid input,
.admin-form-grid select {
    padding: 8px 10px;
    color: var(--text);
    font: inherit;
    font-size: 13px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    width: 100%;
}

.admin-form-toggle {
    display: flex !important;
    flex-direction: row !important;
    gap: 8px !important;
    align-items: center;
}

.admin-form-toggle input[type="checkbox"] {
    width: 16px;
    height: 16px;
    flex: none;
}

.admin-form-actions {
    grid-column: 1 / -1;
    display: flex;
    gap: 10px;
}

.admin-form-actions button {
    padding: 8px 16px;
    color: #fff;
    font: inherit;
    font-size: 13px;
    font-weight: 600;
    background: var(--brand);
    border: 1px solid var(--brand);
    border-radius: var(--radius-sm);
    cursor: pointer;
}


.ad-label-badge {
    padding: 2px 8px;
    font-size: 11px;
    font-weight: 700;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 4px;
}

.ad-edit-row td {
    background: var(--surface-soft);
    padding: 16px;
}

.ad-inline-edit-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
    margin-bottom: 12px;
}

.ad-inline-edit-grid label {
    display: grid;
    gap: 4px;
}

.ad-inline-edit-grid label span {
    color: var(--muted);
    font-size: 12px;
    font-weight: 700;
}

.ad-inline-edit-grid input,
.ad-inline-edit-grid select {
    padding: 6px 8px;
    font: inherit;
    font-size: 13px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    width: 100%;
}

.ad-inline-edit .admin-form-toggle {
    flex-direction: row;
    gap: 6px;
    align-items: center;
}

.ad-inline-edit .admin-form-toggle input {
    width: 16px;
    height: 16px;
    flex: none;
}

.ad-inline-actions {
    display: flex;
    gap: 10px;
}

.ad-inline-actions button {
    padding: 6px 14px;
    font: inherit;
    font-size: 13px;
    cursor: pointer;
    border-radius: var(--radius-sm);
    border: 1px solid var(--line);
    background: var(--surface);
    color: var(--text);
}

.ad-inline-actions button[type="submit"] {
    color: #fff;
    background: var(--brand);
    border-color: var(--brand);
}

.admin-stats-overview {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 20px;
}

.admin-stat-card {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 16px 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.stat-label {
    color: var(--muted);
    font-size: 12px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
}

.stat-value {
    font-size: 28px;
    font-weight: 700;
    color: var(--text);
    line-height: 1;
}

.stat-value.enabled {
    color: var(--brand);
}

.admin-slot-stats {
    margin-bottom: 20px;
}

.admin-slot-stats-title {
    margin: 0 0 10px;
    color: var(--muted);
    font-size: 12px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
}

.admin-slot-stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
}

.admin-slot-stat-card {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 12px 16px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.slot-name {
    font-size: 13px;
    font-weight: 700;
    color: var(--text);
}

.slot-numbers {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.slot-numbers span {
    font-size: 12px;
    color: var(--muted);
}

@media (max-width: 760px) {
    .admin-stats-overview {
        grid-template-columns: repeat(2, 1fr);
    }

    .admin-slot-stats-grid,
    .admin-form-grid,
    .ad-inline-edit-grid {
        grid-template-columns: 1fr;
    }
}
</style>

