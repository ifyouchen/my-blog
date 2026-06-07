<script setup>
import {onMounted, reactive, ref} from 'vue';
import {getAdminInviteRecordsApi} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';
import AdminSelect from '@/components/admin/AdminSelect.vue';

const REWARD_STATUS_OPTIONS = [
    {value: '', label: '全部状态'},
    {value: 'PENDING', label: '待发放'},
    {value: 'GRANTED', label: '已发放'},
    {value: 'SKIPPED', label: '已跳过'},
];

const REWARD_STATUS_META = {
    PENDING: {label: '待发放', className: 'pending'},
    GRANTED: {label: '已发放', className: 'granted'},
    SKIPPED: {label: '已跳过', className: 'skipped'},
};

const rewardStatusMeta = (status) => REWARD_STATUS_META[status] || {label: status || '未知', className: 'pending'};

const filters = reactive({
    keyword: '',
    rewardStatus: ''
});

const pageState = reactive({
    items: [],
    page: 1,
    size: 10,
    total: 0,
    loading: false,
    error: '',
    /** 防闪烁：保留旧 items 直到新数据到达 */
    hasData: false
});

const hasPrev = () => pageState.page > 1;
const hasNext = () => pageState.page * pageState.size < pageState.total;

const loadRecords = async (page = 1) => {
    pageState.loading = true;
    pageState.error = '';
    try {
        const result = await getAdminInviteRecordsApi({
            keyword: filters.keyword.trim() || undefined,
            rewardStatus: filters.rewardStatus || undefined,
            page,
            size: pageState.size
        });
        pageState.items = result.items || [];
        pageState.page = result.page || page;
        pageState.total = result.total || 0;
        pageState.hasData = true;
    } catch (e) {
        if (pageState.items.length === 0) {
            pageState.hasData = false;
        }
        pageState.total = 0;
        pageState.error = e.message || '邀请记录加载失败';
    } finally {
        pageState.loading = false;
    }
};

const search = () => {
    loadRecords(1);
};

onMounted(() => {
    loadRecords(1);
});
</script>

<template>
    <section class="ag-section">
        <div class="ag-section-head">
            <h2 class="ag-section-title">邀请记录</h2>
            <span class="ag-section-subtitle">查看用户邀请关系，支持按用户名或邀请码搜索</span>
        </div>

        <!-- 筛选栏 -->
        <div class="invite-toolbar">
            <input
                v-model="filters.keyword"
                type="text"
                placeholder="邀请人 / 被邀请人用户名或邀请码"
                class="ag-input invite-keyword-input"
                @keydown.enter="search"
            />
            <AdminSelect
                v-model="filters.rewardStatus"
                class="invite-status-select"
                :options="REWARD_STATUS_OPTIONS"
                @change="search"
            />
            <button type="button" class="ag-btn primary" :disabled="pageState.loading" @click="search">
                {{ pageState.loading ? '查询中...' : '查询' }}
            </button>
        </div>

        <!-- 首次加载（无数据时） -->
        <template v-if="!pageState.hasData && !pageState.items.length">
            <div v-if="pageState.error" class="ag-error">{{ pageState.error }}</div>
            <div v-if="pageState.loading" class="ag-table-empty">加载中...</div>
            <div v-else class="ag-table-empty">暂无邀请记录</div>
        </template>

        <!-- 表格（有数据后保留旧数据防闪烁） -->
        <template v-else>
            <div class="ag-table-outer-wrap">
                <div v-if="pageState.loading" class="ag-table-loading-overlay">
                    <span>查询中...</span>
                </div>
                <div v-if="pageState.error" class="ag-error">{{ pageState.error }}</div>
                <div class="ag-table-wrap">
                    <table class="ag-table invite-table">
                        <thead>
                            <tr>
                                <th>时间</th>
                                <th>邀请人</th>
                                <th>被邀请人</th>
                                <th>邀请码</th>
                                <th>奖励状态</th>
                                <th>业务单号</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="row in pageState.items" :key="row.id">
                                <td class="time-cell">{{ formatAdminDateTime(row.createdAt) }}</td>
                                <td>
                                    <div>{{ row.inviterUserId }}</div>
                                    <div v-if="row.inviterUsername" class="user-name">{{ row.inviterUsername }}</div>
                                </td>
                                <td>
                                    <div>{{ row.inviteeUserId }}</div>
                                    <div v-if="row.inviteeUsername" class="user-name">{{ row.inviteeUsername }}</div>
                                </td>
                                <td>
                                    <code v-if="row.inviteCode" class="invite-code-tag">{{ row.inviteCode }}</code>
                                    <span v-else class="no-value">-</span>
                                </td>
                                <td>
                                    <span :class="['status-chip', rewardStatusMeta(row.rewardStatus).className]">
                                        {{ rewardStatusMeta(row.rewardStatus).label }}
                                    </span>
                                </td>
                                <td class="biz-no" :title="row.triggerBizNo">{{ row.triggerBizNo || '-' }}</td>
                            </tr>
                            <tr v-if="!pageState.items.length && !pageState.loading">
                                <td colspan="6" class="ag-table-empty">暂无邀请记录</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div v-if="pageState.total > pageState.size" class="ag-pagination">
                <button
                    type="button"
                    class="ag-btn secondary small"
                    :disabled="!hasPrev() || pageState.loading"
                    @click="loadRecords(pageState.page - 1)"
                >
                    上一页
                </button>
                <span>第 {{ pageState.page }} 页 / 共 {{ pageState.total }} 条</span>
                <button
                    type="button"
                    class="ag-btn secondary small"
                    :disabled="!hasNext() || pageState.loading"
                    @click="loadRecords(pageState.page + 1)"
                >
                    下一页
                </button>
            </div>
        </template>
    </section>
</template>

<style scoped src="@/styles/views/admin/InviteRecordsTab.css"></style>
