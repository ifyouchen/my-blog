<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    getAdminRevenueSharesApi,
    retryRevenueShareSettlementApi,
} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';
import AdminSelect from '@/components/admin/AdminSelect.vue';

const toast = useToast();

const revenueFilters = reactive({authorId: '', settlementStatus: ''});
const revenueState = reactive({items: [], page: 1, pageSize: 10, total: 0, loading: false, error: ''});
const retryingOrderNo = ref('');

const REVENUE_STATUS_META = {
    PENDING: {label: '待结算', className: 'pending'},
    SETTLED: {label: '已入账', className: 'settled'},
    FAILED: {label: '失败待处理', className: 'failed'},
};
const REVENUE_STATUS_OPTIONS = [
    {value: '', label: '全部状态'},
    {value: 'PENDING', label: '待结算'},
    {value: 'SETTLED', label: '已入账'},
    {value: 'FAILED', label: '失败待处理'},
];

const revenueStatusMeta = (status) => REVENUE_STATUS_META[status] || {label: status || '未知', className: 'pending'};
const revenueTotalPages = computed(() => Math.max(1, Math.ceil(revenueState.total / revenueState.pageSize)));
const canRetryRevenue = (row) => ['PENDING', 'FAILED'].includes(row?.settlementStatus);

const loadRevenueShares = async () => {
    revenueState.loading = true;
    revenueState.error = '';
    try {
        const authorKeyword = String(revenueFilters.authorId || '').trim();
        const result = await getAdminRevenueSharesApi({
            authorId: /^\d+$/.test(authorKeyword) ? authorKeyword : '',
            authorKeyword: /^\d+$/.test(authorKeyword) ? '' : authorKeyword,
            settlementStatus: revenueFilters.settlementStatus,
            page: revenueState.page, size: revenueState.pageSize,
        });
        revenueState.items = result.items || [];
        revenueState.total = result.total || revenueState.items.length;
    } catch (e) {
        revenueState.error = e.message || '分账流水加载失败';
        revenueState.items = [];
        revenueState.total = 0;
    } finally {
        revenueState.loading = false;
    }
};

const searchRevenueShares = () => { revenueState.page = 1; loadRevenueShares(); };
const resetRevenueFilters = () => { revenueFilters.authorId = ''; revenueFilters.settlementStatus = ''; searchRevenueShares(); };
const prevRevenuePage = () => { if (revenueState.page <= 1) return; revenueState.page -= 1; loadRevenueShares(); };
const nextRevenuePage = () => { if (revenueState.page >= revenueTotalPages.value) return; revenueState.page += 1; loadRevenueShares(); };

const retryRevenueShare = async (row) => {
    if (!row?.orderNo || retryingOrderNo.value) return;
    retryingOrderNo.value = row.orderNo;
    try {
        const result = await retryRevenueShareSettlementApi(row.orderNo);
        toast.success(result?.message || '已触发分账重试');
        await loadRevenueShares();
    } catch (e) {
        toast.error(e.message || '分账重试失败');
    } finally {
        retryingOrderNo.value = '';
    }
};

onMounted(() => { loadRevenueShares(); });
</script>

<template>
    <section class="ag-section">
        <div class="ag-section-head">
            <h2 class="ag-section-title">分账结算</h2>
            <span class="ag-section-subtitle">异步入账，失败可人工重试</span>
        </div>
        <div class="revenue-toolbar">
            <input v-model="revenueFilters.authorId" type="text" placeholder="作者 ID / 用户名 / 邮箱" class="ag-input revenue-author-input" @keydown.enter="searchRevenueShares" />
            <AdminSelect
                v-model="revenueFilters.settlementStatus"
                class="revenue-status-select"
                :options="REVENUE_STATUS_OPTIONS"
            />
            <button type="button" class="ag-btn primary" :disabled="revenueState.loading" @click="searchRevenueShares">
                {{ revenueState.loading ? '查询中...' : '查询' }}
            </button>
            <button type="button" class="ag-btn secondary" :disabled="revenueState.loading" @click="resetRevenueFilters">重置</button>
        </div>
        <div v-if="revenueState.error" class="ag-error">{{ revenueState.error }}</div>
        <div v-if="revenueState.loading" class="ag-table-empty">加载中...</div>
        <div v-else-if="revenueState.items.length === 0" class="ag-table-empty">暂无分账记录</div>
        <div v-else class="ag-table-wrap">
            <table class="ag-table revenue-table">
                <thead>
                    <tr>
                        <th>订单号</th>
                        <th>作者</th>
                        <th>文章</th>
                        <th>总积分</th>
                        <th>平台</th>
                        <th>作者分成</th>
                        <th>状态</th>
                        <th>作者流水号</th>
                        <th>重试</th>
                        <th>错误</th>
                        <th>创建/结算时间</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="row in revenueState.items" :key="row.id || row.orderNo">
                        <td class="biz-no" :title="row.orderNo">{{ row.orderNo }}</td>
                        <td>{{ row.authorId }}</td>
                        <td>
                            <div>{{ row.articleId }}</div>
                            <div v-if="row.articleTitle" class="article-title">{{ row.articleTitle }}</div>
                        </td>
                        <td>{{ row.totalPoints }}</td>
                        <td>{{ row.platformPoints }}</td>
                        <td class="delta plus">+{{ row.authorPoints }}</td>
                        <td>
                            <span :class="['status-chip', revenueStatusMeta(row.settlementStatus).className]">
                                {{ revenueStatusMeta(row.settlementStatus).label }}
                            </span>
                        </td>
                        <td class="biz-no" :title="row.pointJournalBizNo">{{ row.pointJournalBizNo || '-' }}</td>
                        <td>{{ row.retryCount || 0 }}</td>
                        <td class="error-cell" :title="row.lastError">{{ row.lastError || '-' }}</td>
                        <td class="time-cell">
                            <span>{{ formatAdminDateTime(row.createdAt) }}</span>
                            <span v-if="row.settledAt">入账 {{ formatAdminDateTime(row.settledAt) }}</span>
                        </td>
                        <td>
                            <button type="button" class="ag-btn secondary small"
                                :disabled="!canRetryRevenue(row) || retryingOrderNo === row.orderNo"
                                @click="retryRevenueShare(row)">
                                {{ retryingOrderNo === row.orderNo ? '重试中' : '重试' }}
                            </button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div v-if="revenueState.total > revenueState.pageSize" class="ag-pagination">
            <button type="button" class="ag-btn secondary small" :disabled="revenueState.page <= 1" @click="prevRevenuePage">上一页</button>
            <span>第 {{ revenueState.page }} / {{ revenueTotalPages }} 页，共 {{ revenueState.total }} 条</span>
            <button type="button" class="ag-btn secondary small" :disabled="revenueState.page >= revenueTotalPages" @click="nextRevenuePage">下一页</button>
        </div>
    </section>
</template>

<style scoped>
.article-title {
    font-size: 0.85em;
    color: #666;
    line-height: 1.4;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
</style>
