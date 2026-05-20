<script setup>
import {onMounted, reactive, ref} from 'vue';
import {
    backfillAdminGrowthRewardsApi,
    getAdminRewardGrantLogsApi
} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';
import AdminSelect from '@/components/admin/AdminSelect.vue';

const REWARD_TYPE_OPTIONS = [
    {value: '', label: '全部奖励类型'},
    {value: 'LEVEL_UP', label: '等级奖励'},
    {value: 'CUMULATIVE_SIGN', label: '累计签到里程碑'},
    {value: 'CONSECUTIVE_SIGN', label: '连续签到奖励'}
];

const filters = reactive({
    userId: '',
    rewardType: ''
});
const backfillState = reactive({
    userId: '',
    loading: false,
    feedback: '',
    feedbackType: 'success',
    result: null
});

const pageState = reactive({
    items: [],
    page: 1,
    size: 10,
    total: 0,
    loading: false,
    error: ''
});

const hasPrev = () => pageState.page > 1;
const hasNext = () => pageState.page * pageState.size < pageState.total;

const loadLogs = async (page = 1) => {
    pageState.loading = true;
    pageState.error = '';
    try {
        const userKeyword = filters.userId.trim();
        const result = await getAdminRewardGrantLogsApi({
            userId: /^\d+$/.test(userKeyword) ? userKeyword : '',
            userKeyword: /^\d+$/.test(userKeyword) ? '' : userKeyword,
            rewardType: filters.rewardType,
            page,
            size: pageState.size
        });
        pageState.items = result.items || [];
        pageState.page = result.page || page;
        pageState.total = result.total || 0;
    } catch (e) {
        pageState.items = [];
        pageState.total = 0;
        pageState.error = e.message || '奖励领取记录加载失败';
    } finally {
        pageState.loading = false;
    }
};

const search = () => {
    loadLogs(1);
};

const runBackfill = async (mode) => {
    if (backfillState.loading) {
        return;
    }
    const targetUserId = String(backfillState.userId || '').trim();
    if (mode === 'USER' && !targetUserId) {
        backfillState.feedback = '请先输入需要补偿的用户 ID';
        backfillState.feedbackType = 'error';
        return;
    }
    backfillState.loading = true;
    backfillState.feedback = '';
    try {
        backfillState.result = await backfillAdminGrowthRewardsApi({
            mode,
            userId: mode === 'USER' ? Number(targetUserId) : undefined
        });
        backfillState.feedback = mode === 'ALL'
            ? '全量补偿已执行完成'
            : `用户 ${targetUserId} 补偿已执行完成`;
        backfillState.feedbackType = 'success';
        await loadLogs(1);
    } catch (error) {
        backfillState.result = null;
        backfillState.feedback = error.message || '补偿执行失败';
        backfillState.feedbackType = 'error';
    } finally {
        backfillState.loading = false;
    }
};

onMounted(() => {
    loadLogs(1);
});
</script>

<template>
    <section class="ag-section">
        <div class="ag-section-head">
            <h2 class="ag-section-title">奖励领取记录</h2>
            <span class="ag-section-subtitle">查看哪些用户已经拿到等级奖励或签到里程碑奖励</span>
        </div>

        <div class="backfill-panel">
            <div class="backfill-copy">
                <strong>成长奖励与徽章补偿</strong>
                <p>用于老用户补齐注册奖励、等级权益、等级徽章、签到徽章和年度候选徽章，重复执行不会重复写入。</p>
            </div>
            <div class="backfill-actions">
                <button
                    type="button"
                    class="ag-btn primary"
                    :disabled="backfillState.loading"
                    @click="runBackfill('ALL')"
                >
                    {{ backfillState.loading ? '执行中...' : '全量补偿' }}
                </button>
                <div class="input-with-btn backfill-user-action">
                    <input
                        v-model.trim="backfillState.userId"
                        class="ag-input"
                        type="number"
                        min="1"
                        placeholder="输入用户 ID"
                        @keydown.enter="runBackfill('USER')"
                    >
                    <button
                        type="button"
                        class="ag-btn secondary"
                        :disabled="backfillState.loading"
                        @click="runBackfill('USER')"
                    >
                        按用户补偿
                    </button>
                </div>
            </div>
        </div>

        <p
            v-if="backfillState.feedback"
            :class="backfillState.feedbackType === 'error' ? 'ag-error' : 'ag-success'"
        >
            {{ backfillState.feedback }}
        </p>

        <div v-if="backfillState.result" class="ag-result-card backfill-result">
            <div class="result-item">
                <span class="result-label">模式</span>
                <strong class="result-val">{{ backfillState.result.mode }}</strong>
            </div>
            <div v-if="backfillState.result.userId" class="result-item">
                <span class="result-label">用户 ID</span>
                <strong class="result-val">{{ backfillState.result.userId }}</strong>
            </div>
            <div class="result-item">
                <span class="result-label">注册奖励</span>
                <strong class="result-val highlight">{{ backfillState.result.registerBonusFixed || 0 }}</strong>
            </div>
            <div class="result-item">
                <span class="result-label">等级奖励</span>
                <strong class="result-val highlight">{{ backfillState.result.levelRewardFixed || 0 }}</strong>
            </div>
            <div class="result-item">
                <span class="result-label">等级权益</span>
                <strong class="result-val highlight">{{ backfillState.result.privilegeFixed || 0 }}</strong>
            </div>
            <div class="result-item">
                <span class="result-label">徽章</span>
                <strong class="result-val highlight">{{ backfillState.result.badgeFixed || 0 }}</strong>
            </div>
        </div>

        <div class="toolbar">
            <input
                v-model="filters.userId"
                type="text"
                placeholder="用户 ID / 用户名 / 邮箱"
                class="ag-input toolbar-input"
                @keydown.enter="search"
            />
            <AdminSelect
                v-model="filters.rewardType"
                class="toolbar-select"
                :options="REWARD_TYPE_OPTIONS"
                @change="search"
            />
            <button type="button" class="ag-btn primary" :disabled="pageState.loading" @click="search">
                {{ pageState.loading ? '加载中...' : '查询' }}
            </button>
        </div>

        <p class="ag-hint">
            连续签到奖励会并入签到积分，不会出现在这里；本页主要展示等级升级奖励与累计签到里程碑奖励。
        </p>
        <div v-if="pageState.error" class="ag-error">{{ pageState.error }}</div>
        <div v-if="pageState.loading" class="ag-table-empty">奖励记录加载中...</div>
        <div v-else-if="!pageState.items.length" class="ag-table-empty">暂无奖励领取记录</div>
        <div v-else class="ag-table-wrap">
            <table class="ag-table reward-log-table">
                <thead>
                    <tr>
                        <th>时间</th>
                        <th>用户 ID</th>
                        <th>奖励类型</th>
                        <th>奖励名称</th>
                        <th>发放积分</th>
                        <th>配置 ID</th>
                        <th>备注</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="row in pageState.items" :key="row.id">
                        <td class="time-cell"><span>{{ formatAdminDateTime(row.grantedAt) }}</span></td>
                        <td>{{ row.userId }}</td>
                        <td>{{ row.rewardTypeLabel || row.rewardType }}</td>
                        <td>{{ row.rewardName || '-' }}</td>
                        <td class="delta plus">+{{ row.pointsGranted }}</td>
                        <td>#{{ row.rewardId }}</td>
                        <td class="remark-cell">{{ row.remark || '-' }}</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div v-if="pageState.total > pageState.size" class="ag-pagination">
            <button
                type="button"
                class="ag-btn secondary small"
                :disabled="!hasPrev() || pageState.loading"
                @click="loadLogs(pageState.page - 1)"
            >
                上一页
            </button>
            <span>第 {{ pageState.page }} 页 / 共 {{ pageState.total }} 条</span>
            <button
                type="button"
                class="ag-btn secondary small"
                :disabled="!hasNext() || pageState.loading"
                @click="loadLogs(pageState.page + 1)"
            >
                下一页
            </button>
        </div>
    </section>
</template>

<style scoped>
.toolbar {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    align-items: center;
    margin-bottom: 14px;
}

.toolbar-input {
    max-width: 180px;
}

.toolbar-select {
    max-width: 180px;
}

.backfill-panel {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 16px;
    align-items: center;
    padding: 14px;
    margin-bottom: 14px;
    background: #f8fafc;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
}

.backfill-copy {
    min-width: 0;
}

.backfill-copy strong {
    display: block;
    color: #111827;
    font-size: 14px;
}

.backfill-copy p {
    margin: 4px 0 0;
    color: #6b7280;
    font-size: 12px;
    line-height: 1.6;
}

.backfill-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    justify-content: flex-end;
}

.backfill-user-action {
    width: 300px;
}

.backfill-result {
    margin-bottom: 14px;
}

.reward-log-table {
    min-width: 880px;
}

.remark-cell {
    max-width: 240px;
    color: #6b7280;
}

@media (max-width: 760px) {
    .toolbar {
        flex-direction: column;
        align-items: stretch;
    }

    .toolbar-input,
    .toolbar-select {
        max-width: none;
        width: 100%;
    }

    .backfill-panel {
        grid-template-columns: 1fr;
    }

    .backfill-actions,
    .backfill-user-action {
        width: 100%;
    }
}
</style>
