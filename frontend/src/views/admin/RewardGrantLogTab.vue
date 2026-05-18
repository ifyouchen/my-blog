<script setup>
import {onMounted, reactive, ref} from 'vue';
import {getAdminRewardGrantLogsApi} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';

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

        <div class="toolbar">
            <input
                v-model="filters.userId"
                type="text"
                placeholder="用户 ID / 用户名 / 邮箱"
                class="ag-input toolbar-input"
                @keydown.enter="search"
            />
            <select v-model="filters.rewardType" class="ag-input toolbar-select" @change="search">
                <option v-for="option in REWARD_TYPE_OPTIONS" :key="option.value" :value="option.value">
                    {{ option.label }}
                </option>
            </select>
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
}
</style>
