<script setup>
import {onMounted, ref} from 'vue';
import {
    backfillAdminGrowthRewardsApi,
    getAdminAnnualCreatorCandidatesApi
} from '@/api/growth';

const items = ref([]);
const loading = ref(false);
const feedback = ref('');
const feedbackType = ref('success');
const backfillLoading = ref(false);
const targetUserId = ref('');
const backfillResult = ref(null);
const candidateKeyword = ref('');

const loadCandidates = async () => {
    loading.value = true;
    try {
        items.value = await getAdminAnnualCreatorCandidatesApi({
            userKeyword: candidateKeyword.value.trim()
        });
    } catch (error) {
        items.value = [];
        feedback.value = error.message || '加载年度候选失败';
        feedbackType.value = 'error';
    } finally {
        loading.value = false;
    }
};

const runBackfill = async (mode) => {
    if (backfillLoading.value) {
        return;
    }
    if (mode === 'USER' && !String(targetUserId.value || '').trim()) {
        feedback.value = '请先输入需要补偿的用户 ID';
        feedbackType.value = 'error';
        return;
    }
    backfillLoading.value = true;
    feedback.value = '';
    try {
        backfillResult.value = await backfillAdminGrowthRewardsApi({
            mode,
            userId: mode === 'USER' ? Number(targetUserId.value) : undefined
        });
        feedback.value = mode === 'ALL' ? '全量补偿已执行完成' : `用户 ${targetUserId.value} 补偿已执行完成`;
        feedbackType.value = 'success';
        await loadCandidates();
    } catch (error) {
        backfillResult.value = null;
        feedback.value = error.message || '补偿执行失败';
        feedbackType.value = 'error';
    } finally {
        backfillLoading.value = false;
    }
};

onMounted(loadCandidates);
</script>

<template>
    <section class="ag-section">
        <div class="ag-section-head">
            <h2 class="ag-section-title">年度创作者候选</h2>
            <span class="ag-section-subtitle">查看已获得 Lv.8 年度候选资格的创作者</span>
        </div>
        <p class="ag-hint">这里展示真实 entitlement 候选池，同时提供注册奖励、等级权益与徽章补偿入口。</p>

        <div class="candidate-toolbar">
            <input
                v-model.trim="candidateKeyword"
                class="ag-input candidate-search"
                type="text"
                placeholder="用户名 / 邮箱"
                @keydown.enter="loadCandidates"
            >
            <button type="button" class="ag-btn primary" :disabled="loading" @click="loadCandidates">
                {{ loading ? '查询中...' : '查询候选' }}
            </button>
            <button
                type="button"
                class="ag-btn secondary"
                :disabled="loading"
                @click="candidateKeyword = ''; loadCandidates()"
            >
                重置
            </button>
        </div>

        <div class="ag-form annual-tools">
            <div class="form-row">
                <span class="form-label">补偿修复</span>
                <div class="ag-action-row">
                    <button type="button" class="ag-btn primary" :disabled="backfillLoading" @click="runBackfill('ALL')">
                        {{ backfillLoading ? '执行中...' : '全量补偿' }}
                    </button>
                    <div class="input-with-btn">
                        <input
                            v-model.trim="targetUserId"
                            class="ag-input"
                            type="number"
                            min="1"
                            placeholder="输入用户 ID 定向补偿"
                        >
                        <button
                            type="button"
                            class="ag-btn secondary"
                            :disabled="backfillLoading"
                            @click="runBackfill('USER')"
                        >
                            按用户补偿
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <p v-if="feedback" :class="feedbackType === 'error' ? 'ag-error' : 'ag-success'">{{ feedback }}</p>

        <div v-if="backfillResult" class="ag-result-card">
            <div class="result-item">
                <span class="result-label">模式</span>
                <strong class="result-val">{{ backfillResult.mode }}</strong>
            </div>
            <div v-if="backfillResult.userId" class="result-item">
                <span class="result-label">用户 ID</span>
                <strong class="result-val">{{ backfillResult.userId }}</strong>
            </div>
            <div class="result-item">
                <span class="result-label">补发注册奖励</span>
                <strong class="result-val highlight">{{ backfillResult.registerBonusFixed || 0 }}</strong>
            </div>
            <div class="result-item">
                <span class="result-label">补发等级奖励</span>
                <strong class="result-val highlight">{{ backfillResult.levelRewardFixed || 0 }}</strong>
            </div>
            <div class="result-item">
                <span class="result-label">补齐等级权益</span>
                <strong class="result-val highlight">{{ backfillResult.privilegeFixed || 0 }}</strong>
            </div>
            <div class="result-item">
                <span class="result-label">补发徽章</span>
                <strong class="result-val highlight">{{ backfillResult.badgeFixed || 0 }}</strong>
            </div>
        </div>

        <div class="ag-table-wrap">
            <table class="ag-table">
                <thead>
                    <tr>
                        <th>用户 ID</th>
                        <th>用户</th>
                        <th>当前等级</th>
                        <th>当前经验</th>
                        <th>获得资格时间</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-if="loading">
                        <td colspan="5" class="ag-table-empty">加载中...</td>
                    </tr>
                    <tr v-else-if="!items.length">
                        <td colspan="5" class="ag-table-empty">暂无年度创作者候选</td>
                    </tr>
                    <tr v-for="item in items" v-else :key="item.userId">
                        <td>{{ item.userId }}</td>
                        <td>
                            <strong>{{ item.nickname || item.username || `用户 ${item.userId}` }}</strong>
                            <small>{{ item.username || '-' }}</small>
                        </td>
                        <td>Lv.{{ item.currentLevel || 1 }}</td>
                        <td>{{ item.currentExp || 0 }}</td>
                        <td class="time-cell"><span>{{ item.grantedAt || '-' }}</span></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </section>
</template>

<style scoped src="@/styles/views/admin/AnnualCreatorCandidateTab.css"></style>
