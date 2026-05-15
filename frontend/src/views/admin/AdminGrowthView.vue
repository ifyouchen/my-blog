<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    adminAdjustPointsApi,
    adminGetPointAccountApi,
    getAdminRevenueSharesApi,
    retryRevenueShareSettlementApi,
} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';

const toast = useToast();

// ── 查询用户积分 ─────────────────────────────────────────────────
const queryUserId = ref('');
const queryResult = ref(null);
const querying = ref(false);
const queryError = ref('');

const queryAccount = async () => {
    const uid = String(queryUserId.value || '').trim();
    if (!uid) {
        queryError.value = '请输入用户 ID';
        return;
    }
    querying.value = true;
    queryError.value = '';
    queryResult.value = null;
    try {
        queryResult.value = await adminGetPointAccountApi(uid);
    } catch (e) {
        queryError.value = e.message || '查询失败';
    } finally {
        querying.value = false;
    }
};

// ── 调整积分 ─────────────────────────────────────────────────────
const adjustForm = reactive({
    targetUserId: '',
    delta: '',
    reason: '',
    bizNo: '',
});
const adjusting = ref(false);
const adjustResult = ref(null);
const adjustError = ref('');
const adjustHistory = ref([]);

const validateAdjust = () => {
    if (!adjustForm.targetUserId) return '目标用户 ID 不能为空';
    const delta = Number(adjustForm.delta);
    if (!adjustForm.delta || isNaN(delta) || delta === 0) return '变动积分不能为空且不能为 0';
    if (!adjustForm.reason.trim()) return '调整原因不能为空';
    if (!adjustForm.bizNo.trim()) return '业务流水号不能为空';
    return '';
};

const generateBizNo = () => {
    adjustForm.bizNo = `ADJ-${Date.now()}-${Math.random().toString(36).slice(2, 6).toUpperCase()}`;
};

const doAdjust = async () => {
    const errMsg = validateAdjust();
    if (errMsg) {
        adjustError.value = errMsg;
        return;
    }
    adjusting.value = true;
    adjustError.value = '';
    adjustResult.value = null;
    try {
        const payload = {
            targetUserId: Number(adjustForm.targetUserId),
            delta: Number(adjustForm.delta),
            reason: adjustForm.reason.trim(),
            bizNo: adjustForm.bizNo.trim(),
        };
        const result = await adminAdjustPointsApi(payload);
        adjustResult.value = result;
        adjustHistory.value.unshift({
            ...payload,
            balanceAfter: result?.balanceAfter,
            time: new Date().toISOString(),
        });
        toast.success(`积分调整成功！用户 ${result?.targetUserId} 余额：${result?.balanceAfter}`);
        // 清空表单（保留 userId 方便连续操作）
        adjustForm.delta = '';
        adjustForm.reason = '';
        adjustForm.bizNo = '';
    } catch (e) {
        adjustError.value = e.message || '调整失败';
        toast.error(e.message || '积分调整失败');
    } finally {
        adjusting.value = false;
    }
};

// ── 分账结算管理 ─────────────────────────────────────────────────
const revenueFilters = reactive({
    authorId: '',
    settlementStatus: '',
});
const revenueState = reactive({
    items: [],
    page: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    error: '',
});
const retryingOrderNo = ref('');

const REVENUE_STATUS_META = {
    PENDING: {label: '待结算', className: 'pending'},
    SETTLED: {label: '已入账', className: 'settled'},
    FAILED: {label: '失败待处理', className: 'failed'},
};

const revenueStatusMeta = (status) => REVENUE_STATUS_META[status] || {
    label: status || '未知',
    className: 'pending',
};

const revenueTotalPages = computed(() => Math.max(1, Math.ceil(revenueState.total / revenueState.pageSize)));

const canRetryRevenue = (row) => ['PENDING', 'FAILED'].includes(row?.settlementStatus);

const loadRevenueShares = async () => {
    revenueState.loading = true;
    revenueState.error = '';
    try {
        const result = await getAdminRevenueSharesApi({
            authorId: String(revenueFilters.authorId || '').trim(),
            settlementStatus: revenueFilters.settlementStatus,
            page: revenueState.page,
            size: revenueState.pageSize,
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

const searchRevenueShares = () => {
    revenueState.page = 1;
    loadRevenueShares();
};

const resetRevenueFilters = () => {
    revenueFilters.authorId = '';
    revenueFilters.settlementStatus = '';
    searchRevenueShares();
};

const prevRevenuePage = () => {
    if (revenueState.page <= 1) return;
    revenueState.page -= 1;
    loadRevenueShares();
};

const nextRevenuePage = () => {
    if (revenueState.page >= revenueTotalPages.value) return;
    revenueState.page += 1;
    loadRevenueShares();
};

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

onMounted(() => {
    loadRevenueShares();
});
</script>

<template>
    <div class="admin-growth">
        <!-- 查询用户积分 -->
        <section class="ag-section">
            <h2 class="ag-section-title">查询用户积分</h2>
            <div class="ag-query-row">
                <input
                    v-model="queryUserId"
                    type="text"
                    placeholder="输入用户 ID"
                    class="ag-input"
                    @keydown.enter="queryAccount"
                />
                <button type="button" class="ag-btn primary" :disabled="querying" @click="queryAccount">
                    {{ querying ? '查询中...' : '查询' }}
                </button>
            </div>
            <div v-if="queryError" class="ag-error">{{ queryError }}</div>
            <div v-if="queryResult" class="ag-result-card">
                <div class="result-item">
                    <span class="result-label">用户 ID</span>
                    <span class="result-val">{{ queryResult.userId }}</span>
                </div>
                <div class="result-item">
                    <span class="result-label">当前余额</span>
                    <span class="result-val highlight">{{ queryResult.balance }} 积分</span>
                </div>
                <div class="result-item">
                    <span class="result-label">累计获得</span>
                    <span class="result-val">{{ queryResult.totalEarned }}</span>
                </div>
                <div class="result-item">
                    <span class="result-label">累计消耗</span>
                    <span class="result-val">{{ queryResult.totalSpent }}</span>
                </div>
            </div>
        </section>

        <!-- 调整积分 -->
        <section class="ag-section">
            <h2 class="ag-section-title">调整用户积分</h2>
            <p class="ag-hint">
                <strong>正数</strong>为加积分，<strong>负数</strong>为扣积分。
                bizNo 需全局唯一，可点击「生成」自动生成。
            </p>

            <div class="ag-form">
                <div class="form-row">
                    <label class="form-label">目标用户 ID <em>*</em></label>
                    <input v-model="adjustForm.targetUserId" type="text" placeholder="用户 ID（数字）" class="ag-input" />
                </div>
                <div class="form-row">
                    <label class="form-label">变动积分 <em>*</em></label>
                    <input
                        v-model="adjustForm.delta"
                        type="number"
                        placeholder="正数加分，负数扣分（如 100 或 -50）"
                        class="ag-input"
                    />
                </div>
                <div class="form-row">
                    <label class="form-label">调整原因 <em>*</em></label>
                    <input v-model="adjustForm.reason" type="text" placeholder="请填写调整原因（将记录到审计日志）" class="ag-input" />
                </div>
                <div class="form-row">
                    <label class="form-label">业务流水号 <em>*</em></label>
                    <div class="input-with-btn">
                        <input v-model="adjustForm.bizNo" type="text" placeholder="唯一幂等键" class="ag-input" />
                        <button type="button" class="ag-btn secondary small" @click="generateBizNo">生成</button>
                    </div>
                </div>

                <div v-if="adjustError" class="ag-error">{{ adjustError }}</div>
                <div v-if="adjustResult" class="ag-success">
                    ✓ 调整成功！用户 {{ adjustResult.targetUserId }} 余额变为 <strong>{{ adjustResult.balanceAfter }}</strong> 积分
                </div>

                <button
                    type="button"
                    class="ag-btn primary submit-btn"
                    :disabled="adjusting"
                    @click="doAdjust"
                >
                    {{ adjusting ? '提交中...' : '提交调整' }}
                </button>
            </div>
        </section>

        <!-- 分账结算 -->
        <section class="ag-section">
            <div class="ag-section-head">
                <h2 class="ag-section-title">分账结算</h2>
                <span class="ag-section-subtitle">异步入账，失败可人工重试</span>
            </div>

            <div class="revenue-toolbar">
                <input
                    v-model="revenueFilters.authorId"
                    type="text"
                    placeholder="作者用户 ID"
                    class="ag-input revenue-author-input"
                    @keydown.enter="searchRevenueShares"
                />
                <select v-model="revenueFilters.settlementStatus" class="ag-input revenue-status-select">
                    <option value="">全部状态</option>
                    <option value="PENDING">待结算</option>
                    <option value="SETTLED">已入账</option>
                    <option value="FAILED">失败待处理</option>
                </select>
                <button type="button" class="ag-btn primary" :disabled="revenueState.loading" @click="searchRevenueShares">
                    {{ revenueState.loading ? '查询中...' : '查询' }}
                </button>
                <button type="button" class="ag-btn secondary" :disabled="revenueState.loading" @click="resetRevenueFilters">
                    重置
                </button>
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
                            <td>{{ row.articleId }}</td>
                            <td>{{ row.totalPoints }}</td>
                            <td>{{ row.platformPoints }}</td>
                            <td class="delta plus">+{{ row.authorPoints }}</td>
                            <td>
                                <span :class="['status-chip', revenueStatusMeta(row.settlementStatus).className]">
                                    {{ revenueStatusMeta(row.settlementStatus).label }}
                                </span>
                            </td>
                            <td class="biz-no" :title="row.pointJournalBizNo">
                                {{ row.pointJournalBizNo || '-' }}
                            </td>
                            <td>{{ row.retryCount || 0 }}</td>
                            <td class="error-cell" :title="row.lastError">{{ row.lastError || '-' }}</td>
                            <td class="time-cell">
                                <span>{{ formatAdminDateTime(row.createdAt) }}</span>
                                <span v-if="row.settledAt">入账 {{ formatAdminDateTime(row.settledAt) }}</span>
                            </td>
                            <td>
                                <button
                                    type="button"
                                    class="ag-btn secondary small"
                                    :disabled="!canRetryRevenue(row) || retryingOrderNo === row.orderNo"
                                    @click="retryRevenueShare(row)"
                                >
                                    {{ retryingOrderNo === row.orderNo ? '重试中' : '重试' }}
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="revenueState.total > revenueState.pageSize" class="ag-pagination">
                <button type="button" class="ag-btn secondary small" :disabled="revenueState.page <= 1" @click="prevRevenuePage">
                    上一页
                </button>
                <span>第 {{ revenueState.page }} / {{ revenueTotalPages }} 页，共 {{ revenueState.total }} 条</span>
                <button
                    type="button"
                    class="ag-btn secondary small"
                    :disabled="revenueState.page >= revenueTotalPages"
                    @click="nextRevenuePage"
                >
                    下一页
                </button>
            </div>
        </section>

        <!-- 本次操作历史 -->
        <section v-if="adjustHistory.length > 0" class="ag-section">
            <h2 class="ag-section-title">本页操作记录（会话内）</h2>
            <table class="ag-table">
                <thead>
                    <tr>
                        <th>目标用户 ID</th>
                        <th>变动</th>
                        <th>调整后余额</th>
                        <th>原因</th>
                        <th>流水号</th>
                        <th>时间</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="(row, idx) in adjustHistory" :key="idx">
                        <td>{{ row.targetUserId }}</td>
                        <td :class="['delta', row.delta > 0 ? 'plus' : 'minus']">
                            {{ row.delta > 0 ? '+' : '' }}{{ row.delta }}
                        </td>
                        <td>{{ row.balanceAfter ?? '-' }}</td>
                        <td>{{ row.reason }}</td>
                        <td class="biz-no">{{ row.bizNo }}</td>
                        <td>{{ formatAdminDateTime(row.time) }}</td>
                    </tr>
                </tbody>
            </table>
        </section>
    </div>
</template>

<style scoped>
.admin-growth {
    display: flex;
    flex-direction: column;
    gap: 28px;
}

.ag-section {
    background: var(--admin-surface, #fff);
    border: 1px solid var(--admin-line, #e5e7eb);
    border-radius: 10px;
    padding: 24px;
}

.ag-section-title {
    font-size: 16px;
    font-weight: 700;
    color: var(--admin-text, #111827);
    margin: 0 0 16px;
}

.ag-section-head {
    display: flex;
    align-items: baseline;
    gap: 10px;
    margin-bottom: 16px;
}

.ag-section-head .ag-section-title {
    margin-bottom: 0;
}

.ag-section-subtitle {
    font-size: 12px;
    color: #6b7280;
}

.ag-hint {
    font-size: 13px;
    color: var(--admin-muted, #6b7280);
    margin: 0 0 16px;
    line-height: 1.6;
}

/* 查询行 */
.ag-query-row {
    display: flex;
    gap: 10px;
    align-items: center;
}

.ag-input {
    flex: 1;
    height: 36px;
    padding: 0 10px;
    border: 1px solid var(--admin-line, #d1d5db);
    border-radius: 6px;
    font-size: 14px;
    color: var(--admin-text, #111827);
    background: #fff;
    outline: none;
    transition: border-color 0.15s;
}

.ag-input:focus { border-color: #6366f1; }

/* 按钮 */
.ag-btn {
    height: 36px;
    padding: 0 18px;
    border-radius: 6px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    border: none;
    transition: opacity 0.15s;
    white-space: nowrap;
}

.ag-btn.primary { background: #6366f1; color: #fff; }
.ag-btn.secondary { background: #f3f4f6; color: #374151; border: 1px solid #d1d5db; }
.ag-btn.small { height: 32px; padding: 0 12px; font-size: 13px; }
.ag-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ag-btn:not(:disabled):hover { opacity: 0.88; }

.revenue-toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
    margin-bottom: 16px;
}

.revenue-author-input {
    max-width: 180px;
}

.revenue-status-select {
    max-width: 160px;
}

/* 结果卡 */
.ag-result-card {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 12px;
    margin-top: 16px;
    padding: 16px;
    background: #f9fafb;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
}

.result-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.result-label {
    font-size: 12px;
    color: #6b7280;
}

.result-val {
    font-size: 15px;
    font-weight: 600;
    color: #111827;
}

.result-val.highlight { color: #4f46e5; font-size: 18px; }

/* 调整表单 */
.ag-form {
    display: flex;
    flex-direction: column;
    gap: 14px;
    max-width: 560px;
}

.form-row {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.form-label {
    font-size: 13px;
    font-weight: 500;
    color: #374151;
}

.form-label em {
    color: #ef4444;
    font-style: normal;
    margin-left: 2px;
}

.input-with-btn {
    display: flex;
    gap: 8px;
    align-items: center;
}

.input-with-btn .ag-input { flex: 1; }

.ag-error {
    padding: 10px 14px;
    background: #fef2f2;
    border: 1px solid #fca5a5;
    border-radius: 6px;
    color: #dc2626;
    font-size: 13px;
}

.ag-success {
    padding: 10px 14px;
    background: #f0fdf4;
    border: 1px solid #86efac;
    border-radius: 6px;
    color: #16a34a;
    font-size: 13px;
}

.submit-btn { align-self: flex-start; margin-top: 4px; }

/* 操作历史表格 */
.ag-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
}

.ag-table-wrap {
    width: 100%;
    overflow-x: auto;
}

.ag-table th {
    text-align: left;
    padding: 8px 10px;
    font-size: 12px;
    font-weight: 600;
    color: #6b7280;
    border-bottom: 1px solid #e5e7eb;
}

.ag-table td {
    padding: 10px 10px;
    border-bottom: 1px solid #f3f4f6;
    color: #374151;
    vertical-align: middle;
}

.ag-table tr:last-child td { border-bottom: none; }
.ag-table tr:hover td { background: #f9fafb; }

.ag-table-empty {
    padding: 28px 12px;
    text-align: center;
    color: #6b7280;
    font-size: 13px;
}

.revenue-table {
    min-width: 1080px;
}

.delta { font-weight: 700; }
.delta.plus { color: #16a34a; }
.delta.minus { color: #dc2626; }

.status-chip {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
}

.status-chip.pending {
    background: #fff7ed;
    color: #c2410c;
}

.status-chip.settled {
    background: #ecfdf5;
    color: #047857;
}

.status-chip.failed {
    background: #fef2f2;
    color: #b91c1c;
}

.biz-no {
    font-family: monospace;
    font-size: 11px;
    color: #6b7280;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.error-cell {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #9ca3af;
}

.time-cell {
    min-width: 140px;
    color: #6b7280;
}

.time-cell span {
    display: block;
    white-space: nowrap;
}

.ag-pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin-top: 16px;
    color: #6b7280;
    font-size: 13px;
}
</style>

