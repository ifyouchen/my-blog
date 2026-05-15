<script setup>
import {reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    adminAdjustPointsApi,
    adminGetPointAccountApi,
} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';

const toast = useToast();

const queryUserId = ref('');
const queryResult = ref(null);
const querying = ref(false);
const queryError = ref('');

const queryAccount = async () => {
    const uid = String(queryUserId.value || '').trim();
    if (!uid) { queryError.value = '请输入用户 ID'; return; }
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
</script>

<template>
    <section class="ag-section">
        <h2 class="ag-section-title">查询用户积分</h2>
        <div class="ag-query-row">
            <input v-model="queryUserId" type="text" placeholder="输入用户 ID" class="ag-input" @keydown.enter="queryAccount" />
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
        </section>
    </section>
</template>
