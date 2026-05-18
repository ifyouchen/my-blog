<script setup>
import {reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    adminAdjustPointsApi,
    adminGetPointAccountApi,
    adminGetPointJournalsApi,
} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';
import AdminSelect from '@/components/admin/AdminSelect.vue';

const toast = useToast();

const SOURCE_TYPE_OPTIONS = [
    {value: '', label: '全部类型'},
    {value: 'SIGN_IN', label: '签到'},
    {value: 'INVITE', label: '邀请'},
    {value: 'RECHARGE', label: '充值'},
    {value: 'PUBLISH', label: '发布奖励'},
    {value: 'UNLOCK', label: '文章解锁'},
    {value: 'REVENUE_SHARE', label: '文章解锁分成'},
    {value: 'ADMIN_ADJUST', label: '管理员调整'},
];

const sourceTypeLabel = (sourceType) =>
    SOURCE_TYPE_OPTIONS.find(option => option.value === sourceType)?.label || sourceType || '-';

const QUERY_TYPE_OPTIONS = [
    {value: 'id', label: '用户 ID'},
    {value: 'username', label: '用户名/邮箱'},
];

const queryType = ref('id');
const queryUserId = ref('');
const queryResult = ref(null);
const querying = ref(false);
const queryError = ref('');
const journalSourceType = ref('');
const journals = ref([]);
const journalsTotal = ref(0);
const journalsPage = ref(1);
const journalsPageSize = 10;
const journalsLoading = ref(false);
const journalsError = ref('');

const hasJournalPrev = () => journalsPage.value > 1;
const hasJournalNext = () => journalsPage.value * journalsPageSize < journalsTotal.value;

const loadJournals = async (page = 1) => {
    const uid = queryResult.value?.userId;
    if (!uid) return;
    journalsLoading.value = true;
    journalsError.value = '';
    try {
        const result = await adminGetPointJournalsApi({
            userId: uid,
            sourceType: journalSourceType.value,
            page,
            size: journalsPageSize,
        });
        journals.value = result.items || [];
        journalsTotal.value = result.total || 0;
        journalsPage.value = result.page || page;
    } catch (e) {
        journals.value = [];
        journalsTotal.value = 0;
        journalsError.value = e.message || '积分流水加载失败';
    } finally {
        journalsLoading.value = false;
    }
};

const queryAccount = async () => {
    const uid = String(queryUserId.value || '').trim();
    if (!uid) { queryError.value = queryType.value === 'id' ? '请输入用户 ID' : '请输入用户名或邮箱'; return; }
    if (queryType.value === 'id' && !/^\d+$/.test(uid)) {
        queryError.value = '用户 ID 只能输入数字';
        return;
    }
    querying.value = true;
    queryError.value = '';
    queryResult.value = null;
    journals.value = [];
    journalsTotal.value = 0;
    try {
        queryResult.value = await adminGetPointAccountApi({
            queryType: queryType.value,
            value: uid
        });
        await loadJournals(1);
    } catch (e) {
        queryError.value = e.message || '查询失败';
    } finally {
        querying.value = false;
    }
};

const adjustForm = reactive({targetUserId: '', delta: '', reason: '', bizNo: ''});
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
    if (errMsg) { adjustError.value = errMsg; return; }
    adjusting.value = true;
    adjustError.value = '';
    adjustResult.value = null;
    try {
        const payload = {targetUserId: Number(adjustForm.targetUserId), delta: Number(adjustForm.delta),
            reason: adjustForm.reason.trim(), bizNo: adjustForm.bizNo.trim()};
        const result = await adminAdjustPointsApi(payload);
        adjustResult.value = result;
        adjustHistory.value.unshift({...payload, balanceAfter: result?.balanceAfter, time: new Date().toISOString()});
        toast.success(`积分调整成功！用户 ${result?.targetUserId} 余额：${result?.balanceAfter}`);
        if (String(payload.targetUserId) === String(queryResult.value?.userId || queryUserId.value || '').trim()) {
            await queryAccount();
        }
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

const changeJournalSourceType = () => {
    if (queryResult.value) {
        loadJournals(1);
    }
};

const changeQueryType = () => {
    queryError.value = '';
    journalsError.value = '';
};
</script>

<template>
    <section class="ag-section">
        <h2 class="ag-section-title">查询用户积分</h2>
        <div class="ag-query-row">
            <AdminSelect
                v-model="queryType"
                class="query-type-select"
                :options="QUERY_TYPE_OPTIONS"
                @change="changeQueryType"
            />
            <input
                v-model="queryUserId"
                type="text"
                :placeholder="queryType === 'id' ? '输入用户 ID' : '输入用户名或邮箱'"
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

        <section v-if="queryResult" class="journal-panel">
            <div class="journal-head">
                <div>
                    <h2 class="ag-section-title">积分流水</h2>
                    <p class="ag-hint">共 {{ journalsTotal }} 条记录，按创建时间倒序展示。</p>
                </div>
                <AdminSelect
                    v-model="journalSourceType"
                    class="source-filter"
                    :options="SOURCE_TYPE_OPTIONS"
                    @change="changeJournalSourceType"
                />
            </div>
            <div v-if="journalsError" class="ag-error">{{ journalsError }}</div>
            <div v-if="journalsLoading" class="ag-table-empty">积分流水加载中...</div>
            <div v-else-if="!journals.length" class="ag-table-empty">暂无积分流水</div>
            <div v-else class="ag-table-wrap">
                <table class="ag-table point-journal-table">
                    <thead>
                        <tr>
                            <th>时间</th>
                            <th>类型</th>
                            <th>变动</th>
                            <th>变更后余额</th>
                            <th>业务流水号</th>
                            <th>备注</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in journals" :key="row.id">
                            <td class="time-cell"><span>{{ formatAdminDateTime(row.createdAt) }}</span></td>
                            <td>{{ sourceTypeLabel(row.sourceType) }}</td>
                            <td :class="['delta', row.delta >= 0 ? 'plus' : 'minus']">
                                {{ row.delta > 0 ? '+' : '' }}{{ row.delta }}
                            </td>
                            <td>{{ row.balanceAfter }}</td>
                            <td class="biz-no">{{ row.bizNo }}</td>
                            <td class="remark-cell">{{ row.remark || '-' }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div v-if="journalsTotal > journalsPageSize" class="ag-pagination">
                <button
                    type="button"
                    class="ag-btn secondary small"
                    :disabled="!hasJournalPrev() || journalsLoading"
                    @click="loadJournals(journalsPage - 1)"
                >
                    上一页
                </button>
                <span>第 {{ journalsPage }} 页</span>
                <button
                    type="button"
                    class="ag-btn secondary small"
                    :disabled="!hasJournalNext() || journalsLoading"
                    @click="loadJournals(journalsPage + 1)"
                >
                    下一页
                </button>
            </div>
        </section>

        <h2 class="ag-section-title" style="margin-top:20px">调整用户积分</h2>
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
                <input v-model="adjustForm.delta" type="number" placeholder="正数加分，负数扣分（如 100 或 -50）" class="ag-input" />
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
            <button type="button" class="ag-btn primary submit-btn" :disabled="adjusting" @click="doAdjust">
                {{ adjusting ? '提交中...' : '提交调整' }}
            </button>
        </div>

        <section v-if="adjustHistory.length > 0" style="margin-top:24px">
            <h2 class="ag-section-title">本页操作记录（会话内）</h2>
            <div class="ag-table-wrap">
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
                            <td :class="['delta', row.delta > 0 ? 'plus' : 'minus']">{{ row.delta > 0 ? '+' : '' }}{{ row.delta }}</td>
                            <td>{{ row.balanceAfter ?? '-' }}</td>
                            <td>{{ row.reason }}</td>
                            <td class="biz-no">{{ row.bizNo }}</td>
                            <td>{{ formatAdminDateTime(row.time) }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>
    </section>
</template>

<style scoped>
.journal-panel {
    margin-top: 20px;
}

.journal-head {
    display: flex;
    gap: 12px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 12px;
}

.journal-head .ag-section-title {
    margin-bottom: 4px;
}

.journal-head .ag-hint {
    margin-bottom: 0;
}

.source-filter {
    max-width: 180px;
    flex: 0 0 180px;
}

.query-type-select {
    max-width: 150px;
    flex: 0 0 150px;
}

.point-journal-table {
    min-width: 860px;
}

.remark-cell {
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #6b7280;
}

@media (max-width: 760px) {
    .journal-head {
        flex-direction: column;
    }

    .source-filter {
        max-width: none;
        width: 100%;
        flex-basis: auto;
    }

    .query-type-select {
        max-width: none;
        width: 100%;
        flex-basis: auto;
    }
}
</style>
