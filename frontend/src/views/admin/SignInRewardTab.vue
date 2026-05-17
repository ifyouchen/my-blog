<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    createAdminConsecutiveRewardApi,
    createAdminCumulativeRewardApi,
    deleteAdminConsecutiveRewardApi,
    deleteAdminCumulativeRewardApi,
    getAdminConsecutiveRewardsApi,
    getAdminCumulativeRewardsApi,
    updateAdminConsecutiveRewardApi,
    updateAdminCumulativeRewardApi
} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';
import ADrawer from '@/components/ADrawer.vue';

const toast = useToast();

const consecutiveRewards = ref([]);
const cumulativeRewards = ref([]);
const loading = ref(false);
const error = ref('');
const saving = ref(false);
const drawerVisible = ref(false);
const drawerType = ref('consecutive');
const drawerMode = ref('create');
const deletingKey = ref('');

const consecutiveForm = reactive({
    id: null,
    minDays: 1,
    maxDays: null,
    bonusPoints: 0,
    rewardTier: '',
    rewardDesc: '',
    enabled: true,
    version: 0
});

const cumulativeForm = reactive({
    id: null,
    milestoneDays: 7,
    rewardPoints: 0,
    rewardTitle: '',
    badgeCode: '',
    description: '',
    enabled: true,
    version: 0
});

const consecutiveEnabledCount = computed(() => consecutiveRewards.value.filter(item => item.enabled).length);
const cumulativeEnabledCount = computed(() => cumulativeRewards.value.filter(item => item.enabled).length);
const drawerTitle = computed(() => {
    if (drawerType.value === 'consecutive') {
        return drawerMode.value === 'create' ? '新增连续签到奖励' : '编辑连续签到奖励';
    }
    return drawerMode.value === 'create' ? '新增累计签到里程碑' : '编辑累计签到里程碑';
});
const configWarnings = computed(() => {
    const warnings = [];
    if (hasConsecutiveConflict(consecutiveRewards.value)) {
        warnings.push('连续签到奖励存在区间重叠，请调整后再新增或编辑。');
    }
    if (!isSorted(consecutiveRewards.value, (item) => Number(item.minDays || 0))) {
        warnings.push('连续签到奖励顺序异常，后端应按最小连续天数升序返回。');
    }
    if (hasDuplicateCumulative(cumulativeRewards.value)) {
        warnings.push('累计签到里程碑存在重复天数，请调整后再新增或编辑。');
    }
    if (!isSorted(cumulativeRewards.value, (item) => Number(item.milestoneDays || 0))) {
        warnings.push('累计签到里程碑顺序异常，后端应按天数升序返回。');
    }
    return warnings;
});

const rangeLabel = (row) => row.maxDays == null ? `${row.minDays} 天起` : `${row.minDays} - ${row.maxDays} 天`;

const loadRewards = async () => {
    loading.value = true;
    error.value = '';
    try {
        const [consecutive, cumulative] = await Promise.all([
            getAdminConsecutiveRewardsApi(),
            getAdminCumulativeRewardsApi()
        ]);
        consecutiveRewards.value = consecutive || [];
        cumulativeRewards.value = cumulative || [];
    } catch (e) {
        consecutiveRewards.value = [];
        cumulativeRewards.value = [];
        error.value = e.message || '签到奖励配置加载失败';
    } finally {
        loading.value = false;
    }
};

const openConsecutiveEdit = (row) => {
    drawerType.value = 'consecutive';
    drawerMode.value = 'edit';
    consecutiveForm.id = row.id;
    consecutiveForm.minDays = Number(row.minDays || 1);
    consecutiveForm.maxDays = row.maxDays == null ? null : Number(row.maxDays);
    consecutiveForm.bonusPoints = Number(row.bonusPoints || 0);
    consecutiveForm.rewardTier = row.rewardTier || '';
    consecutiveForm.rewardDesc = row.rewardDesc || '';
    consecutiveForm.enabled = Boolean(row.enabled);
    consecutiveForm.version = Number(row.version || 0);
    error.value = '';
    drawerVisible.value = true;
};

const openConsecutiveCreate = () => {
    drawerType.value = 'consecutive';
    drawerMode.value = 'create';
    consecutiveForm.id = null;
    consecutiveForm.minDays = nextConsecutiveMinDays();
    consecutiveForm.maxDays = null;
    consecutiveForm.bonusPoints = 0;
    consecutiveForm.rewardTier = '';
    consecutiveForm.rewardDesc = '';
    consecutiveForm.enabled = true;
    consecutiveForm.version = 0;
    error.value = '';
    drawerVisible.value = true;
};

const openCumulativeEdit = (row) => {
    drawerType.value = 'cumulative';
    drawerMode.value = 'edit';
    cumulativeForm.id = row.id;
    cumulativeForm.milestoneDays = Number(row.milestoneDays || 1);
    cumulativeForm.rewardPoints = Number(row.rewardPoints || 0);
    cumulativeForm.rewardTitle = row.rewardTitle || '';
    cumulativeForm.badgeCode = row.badgeCode || '';
    cumulativeForm.description = row.description || '';
    cumulativeForm.enabled = Boolean(row.enabled);
    cumulativeForm.version = Number(row.version || 0);
    error.value = '';
    drawerVisible.value = true;
};

const openCumulativeCreate = () => {
    drawerType.value = 'cumulative';
    drawerMode.value = 'create';
    cumulativeForm.id = null;
    cumulativeForm.milestoneDays = nextMilestoneDays();
    cumulativeForm.rewardPoints = 0;
    cumulativeForm.rewardTitle = '';
    cumulativeForm.badgeCode = '';
    cumulativeForm.description = '';
    cumulativeForm.enabled = true;
    cumulativeForm.version = 0;
    error.value = '';
    drawerVisible.value = true;
};

const nextConsecutiveMinDays = () => {
    const finiteMax = consecutiveRewards.value
        .map(item => item.maxDays == null ? Number(item.minDays || 0) : Number(item.maxDays || 0))
        .reduce((max, value) => Math.max(max, value), 0);
    return finiteMax + 1;
};

const nextMilestoneDays = () => {
    const maxDays = cumulativeRewards.value
        .map(item => Number(item.milestoneDays || 0))
        .reduce((max, value) => Math.max(max, value), 0);
    return maxDays + 1;
};

const buildConsecutivePayload = () => ({
    id: consecutiveForm.id,
    minDays: Number(consecutiveForm.minDays || 0),
    maxDays: consecutiveForm.maxDays == null || consecutiveForm.maxDays === ''
        ? null
        : Number(consecutiveForm.maxDays),
    bonusPoints: Number(consecutiveForm.bonusPoints || 0),
    rewardTier: consecutiveForm.rewardTier.trim(),
    rewardDesc: consecutiveForm.rewardDesc.trim(),
    enabled: Boolean(consecutiveForm.enabled),
    version: Number(consecutiveForm.version || 0)
});

const buildCumulativePayload = () => ({
    id: cumulativeForm.id,
    milestoneDays: Number(cumulativeForm.milestoneDays || 0),
    rewardPoints: Number(cumulativeForm.rewardPoints || 0),
    rewardTitle: cumulativeForm.rewardTitle.trim(),
    badgeCode: cumulativeForm.badgeCode.trim(),
    description: cumulativeForm.description.trim(),
    enabled: Boolean(cumulativeForm.enabled),
    version: Number(cumulativeForm.version || 0)
});

const validateConsecutivePayload = (payload) => {
    if (drawerMode.value === 'edit' && !payload.id) return '连续签到奖励配置 ID 不能为空';
    if (payload.minDays <= 0) return '最小连续天数必须大于 0';
    if (payload.maxDays !== null && payload.maxDays < payload.minDays) return '最大连续天数不能小于最小连续天数';
    if (payload.bonusPoints < 0) return '额外积分不能小于 0';
    const conflict = consecutiveRewards.value.find(item => {
        if (Number(item.id) === Number(payload.id || 0)) return false;
        return rangesOverlap(payload.minDays, payload.maxDays, Number(item.minDays || 0), normalizeMax(item.maxDays));
    });
    if (conflict) return `连续签到区间与「${rangeLabel(conflict)}」重叠`;
    return '';
};

const validateCumulativePayload = (payload) => {
    if (drawerMode.value === 'edit' && !payload.id) return '累计签到里程碑配置 ID 不能为空';
    if (payload.milestoneDays <= 0) return '里程碑天数必须大于 0';
    if (payload.rewardPoints < 0) return '奖励积分不能小于 0';
    const duplicated = cumulativeRewards.value.some(item =>
        Number(item.id) !== Number(payload.id || 0) && Number(item.milestoneDays) === payload.milestoneDays);
    if (duplicated) return `累计签到 ${payload.milestoneDays} 天的里程碑已存在`;
    return '';
};

const saveReward = async () => {
    error.value = '';
    saving.value = true;
    try {
        if (drawerType.value === 'consecutive') {
            const payload = buildConsecutivePayload();
            const validationError = validateConsecutivePayload(payload);
            if (validationError) throw new Error(validationError);
            if (drawerMode.value === 'create') {
                await createAdminConsecutiveRewardApi(payload);
                toast.success('连续签到奖励配置已新增');
            } else {
                await updateAdminConsecutiveRewardApi(payload);
                toast.success('连续签到奖励配置已更新');
            }
        } else {
            const payload = buildCumulativePayload();
            const validationError = validateCumulativePayload(payload);
            if (validationError) throw new Error(validationError);
            if (drawerMode.value === 'create') {
                await createAdminCumulativeRewardApi(payload);
                toast.success('累计签到里程碑配置已新增');
            } else {
                await updateAdminCumulativeRewardApi(payload);
                toast.success('累计签到里程碑配置已更新');
            }
        }
        drawerVisible.value = false;
        await loadRewards();
    } catch (e) {
        error.value = e.message || '签到奖励配置保存失败';
        toast.error(error.value);
    } finally {
        saving.value = false;
    }
};

const deleteConsecutiveReward = async (row) => {
    if (!confirm(`确定删除「${rangeLabel(row)}」连续签到奖励吗？该操作会停用并隐藏配置，不影响历史领取记录。`)) {
        return;
    }
    deletingKey.value = `consecutive-${row.id}`;
    error.value = '';
    try {
        await deleteAdminConsecutiveRewardApi(row.id, row.version);
        toast.success('连续签到奖励配置已删除');
        await loadRewards();
    } catch (e) {
        error.value = e.message || '连续签到奖励配置删除失败';
        toast.error(error.value);
    } finally {
        deletingKey.value = '';
    }
};

const deleteCumulativeReward = async (row) => {
    if (!confirm(`确定删除「累计签到 ${row.milestoneDays} 天」里程碑吗？该操作会停用并隐藏配置，不影响历史领取记录。`)) {
        return;
    }
    deletingKey.value = `cumulative-${row.id}`;
    error.value = '';
    try {
        await deleteAdminCumulativeRewardApi(row.id, row.version);
        toast.success('累计签到里程碑配置已删除');
        await loadRewards();
    } catch (e) {
        error.value = e.message || '累计签到里程碑配置删除失败';
        toast.error(error.value);
    } finally {
        deletingKey.value = '';
    }
};

const normalizeMax = (value) => value == null || value === '' ? null : Number(value);
const rangesOverlap = (minA, maxA, minB, maxB) => {
    const normalizedMaxA = maxA == null ? Number.MAX_SAFE_INTEGER : Number(maxA);
    const normalizedMaxB = maxB == null ? Number.MAX_SAFE_INTEGER : Number(maxB);
    return Number(minA) <= normalizedMaxB && Number(minB) <= normalizedMaxA;
};
const hasConsecutiveConflict = (items) => {
    for (let i = 0; i < items.length; i += 1) {
        for (let j = i + 1; j < items.length; j += 1) {
            if (rangesOverlap(
                Number(items[i].minDays || 0),
                normalizeMax(items[i].maxDays),
                Number(items[j].minDays || 0),
                normalizeMax(items[j].maxDays)
            )) {
                return true;
            }
        }
    }
    return false;
};
const hasDuplicateCumulative = (items) => {
    const seen = new Set();
    return items.some(item => {
        const key = Number(item.milestoneDays || 0);
        if (seen.has(key)) return true;
        seen.add(key);
        return false;
    });
};
const isSorted = (items, readValue) => {
    for (let i = 1; i < items.length; i += 1) {
        if (readValue(items[i - 1]) > readValue(items[i])) {
            return false;
        }
    }
    return true;
};

onMounted(loadRewards);
</script>

<template>
    <div class="sign-in-reward-tab">
        <section class="ag-section">
            <div class="ag-section-head">
                <h2 class="ag-section-title">连续签到奖励</h2>
                <span class="ag-section-subtitle">按连续签到区间额外发放积分</span>
            </div>
            <div class="mini-summary-grid">
                <div class="summary-chip">
                    <span>配置数</span>
                    <strong>{{ consecutiveRewards.length }}</strong>
                </div>
                <div class="summary-chip">
                    <span>启用中</span>
                    <strong>{{ consecutiveEnabledCount }}</strong>
                </div>
            </div>
            <p class="ag-hint">连续签到奖励不会单独写入奖励领取记录，而是并入当天签到积分。</p>
            <div class="tab-toolbar">
                <div class="tab-count-group">
                    <span class="tab-count">按最小连续天数升序展示</span>
                    <span class="tab-count-muted">区间不能重叠，无上限档位应在最后</span>
                </div>
                <button type="button" class="ag-btn primary" @click="openConsecutiveCreate">+ 新增档位</button>
            </div>
            <div v-if="configWarnings.length" class="warning-panel">
                <p v-for="warning in configWarnings" :key="warning">{{ warning }}</p>
            </div>
            <div v-if="error" class="ag-error">{{ error }}</div>
            <div v-if="loading" class="ag-table-empty">签到奖励加载中...</div>
            <div v-else-if="!consecutiveRewards.length" class="ag-table-empty">暂无连续签到奖励配置</div>
            <div v-else class="ag-table-wrap">
                <table class="ag-table reward-table">
                    <thead>
                        <tr>
                            <th>区间</th>
                            <th>额外积分</th>
                            <th>档位</th>
                            <th>说明</th>
                            <th>状态</th>
                            <th>更新时间</th>
                            <th>版本</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in consecutiveRewards" :key="row.id">
                            <td><strong>{{ rangeLabel(row) }}</strong></td>
                            <td class="delta plus">+{{ row.bonusPoints }}</td>
                            <td>{{ row.rewardTier || '-' }}</td>
                            <td class="desc-cell">{{ row.rewardDesc || '-' }}</td>
                            <td>
                                <span :class="['status-chip', row.enabled ? 'settled' : 'failed']">
                                    {{ row.enabled ? '启用' : '停用' }}
                                </span>
                            </td>
                            <td class="time-cell"><span>{{ formatAdminDateTime(row.updatedAt) }}</span></td>
                            <td>v{{ row.version }}</td>
                            <td class="action-cell">
                                <button type="button" class="ag-btn secondary small" @click="openConsecutiveEdit(row)">
                                    编辑
                                </button>
                                <button
                                    type="button"
                                    class="ag-btn danger small"
                                    :disabled="deletingKey === `consecutive-${row.id}`"
                                    @click="deleteConsecutiveReward(row)"
                                >
                                    {{ deletingKey === `consecutive-${row.id}` ? '删除中...' : '删除' }}
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="ag-section">
            <div class="ag-section-head">
                <h2 class="ag-section-title">累计签到里程碑</h2>
                <span class="ag-section-subtitle">达到累计签到节点后只发放一次</span>
            </div>
            <div class="mini-summary-grid">
                <div class="summary-chip">
                    <span>配置数</span>
                    <strong>{{ cumulativeRewards.length }}</strong>
                </div>
                <div class="summary-chip">
                    <span>启用中</span>
                    <strong>{{ cumulativeEnabledCount }}</strong>
                </div>
            </div>
            <div class="tab-toolbar">
                <div class="tab-count-group">
                    <span class="tab-count">按累计签到天数升序展示</span>
                    <span class="tab-count-muted">删除只隐藏配置，历史领取记录保留</span>
                </div>
                <button type="button" class="ag-btn primary" @click="openCumulativeCreate">+ 新增里程碑</button>
            </div>
            <div v-if="loading" class="ag-table-empty">签到奖励加载中...</div>
            <div v-else-if="!cumulativeRewards.length" class="ag-table-empty">暂无累计签到里程碑配置</div>
            <div v-else class="ag-table-wrap">
                <table class="ag-table reward-table">
                    <thead>
                        <tr>
                            <th>里程碑</th>
                            <th>奖励积分</th>
                            <th>奖励名称</th>
                            <th>徽章码</th>
                            <th>说明</th>
                            <th>状态</th>
                            <th>更新时间</th>
                            <th>版本</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in cumulativeRewards" :key="row.id">
                            <td><strong>{{ row.milestoneDays }} 天</strong></td>
                            <td class="delta plus">+{{ row.rewardPoints }}</td>
                            <td>{{ row.rewardTitle || '-' }}</td>
                            <td>{{ row.badgeCode || '-' }}</td>
                            <td class="desc-cell">{{ row.description || '-' }}</td>
                            <td>
                                <span :class="['status-chip', row.enabled ? 'settled' : 'failed']">
                                    {{ row.enabled ? '启用' : '停用' }}
                                </span>
                            </td>
                            <td class="time-cell"><span>{{ formatAdminDateTime(row.updatedAt) }}</span></td>
                            <td>v{{ row.version }}</td>
                            <td class="action-cell">
                                <button type="button" class="ag-btn secondary small" @click="openCumulativeEdit(row)">
                                    编辑
                                </button>
                                <button
                                    type="button"
                                    class="ag-btn danger small"
                                    :disabled="deletingKey === `cumulative-${row.id}`"
                                    @click="deleteCumulativeReward(row)"
                                >
                                    {{ deletingKey === `cumulative-${row.id}` ? '删除中...' : '删除' }}
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <ADrawer
            v-model:visible="drawerVisible"
            :title="drawerTitle"
            width="560px"
        >
            <div class="editor-stack">
                <template v-if="drawerType === 'consecutive'">
                    <section class="editor-panel">
                        <div class="editor-panel-head">
                            <span class="editor-panel-kicker">区间</span>
                            <strong>定义连续签到达到哪个区间时额外加分</strong>
                        </div>
                        <div class="metric-grid">
                            <label class="metric-field">
                                <span>最小天数</span>
                                <input v-model.number="consecutiveForm.minDays" type="number" min="1" class="ag-input" />
                            </label>
                            <label class="metric-field">
                                <span>最大天数</span>
                                <input v-model.number="consecutiveForm.maxDays" type="number" min="1" class="ag-input" placeholder="留空表示无上限" />
                            </label>
                        </div>
                    </section>
                    <section class="editor-panel">
                        <div class="metric-grid">
                            <label class="metric-field">
                                <span>额外积分</span>
                                <input v-model.number="consecutiveForm.bonusPoints" type="number" min="0" class="ag-input" />
                            </label>
                            <label class="metric-field">
                                <span>档位标识</span>
                                <input v-model="consecutiveForm.rewardTier" type="text" maxlength="32" class="ag-input" />
                            </label>
                        </div>
                        <div class="form-row">
                            <label class="form-label">奖励说明</label>
                            <textarea v-model="consecutiveForm.rewardDesc" rows="4" maxlength="128" class="ag-textarea" />
                        </div>
                        <label class="ag-toggle">
                            <input v-model="consecutiveForm.enabled" type="checkbox" />
                            <span>启用这条连续签到奖励</span>
                        </label>
                    </section>
                </template>

                <template v-else>
                    <section class="editor-panel">
                        <div class="editor-panel-head">
                            <span class="editor-panel-kicker">里程碑</span>
                            <strong>定义累计签到达到某个节点时的一次性奖励</strong>
                        </div>
                        <div class="metric-grid">
                            <label class="metric-field">
                                <span>里程碑天数</span>
                                <input v-model.number="cumulativeForm.milestoneDays" type="number" min="1" class="ag-input" />
                            </label>
                            <label class="metric-field">
                                <span>奖励积分</span>
                                <input v-model.number="cumulativeForm.rewardPoints" type="number" min="0" class="ag-input" />
                            </label>
                        </div>
                    </section>
                    <section class="editor-panel">
                        <div class="form-row">
                            <label class="form-label">奖励名称</label>
                            <input v-model="cumulativeForm.rewardTitle" type="text" maxlength="64" class="ag-input" />
                        </div>
                        <div class="form-row">
                            <label class="form-label">徽章码</label>
                            <input v-model="cumulativeForm.badgeCode" type="text" maxlength="64" class="ag-input" />
                        </div>
                        <div class="form-row">
                            <label class="form-label">说明</label>
                            <textarea v-model="cumulativeForm.description" rows="4" maxlength="255" class="ag-textarea" />
                        </div>
                        <label class="ag-toggle">
                            <input v-model="cumulativeForm.enabled" type="checkbox" />
                            <span>启用这条累计签到奖励</span>
                        </label>
                    </section>
                </template>

                <div v-if="error" class="ag-error">{{ error }}</div>
            </div>

            <template #footer>
                <button type="button" class="ag-btn secondary" @click="drawerVisible = false">取消</button>
                <button type="button" class="ag-btn primary" :disabled="saving" @click="saveReward">
                    {{ saving ? '保存中...' : (drawerMode === 'create' ? '新增配置' : '保存修改') }}
                </button>
            </template>
        </ADrawer>
    </div>
</template>

<style scoped>
.sign-in-reward-tab {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.mini-summary-grid {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    margin-bottom: 14px;
}

.summary-chip {
    display: inline-flex;
    gap: 8px;
    align-items: center;
    padding: 8px 12px;
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 999px;
    color: #4b5563;
    font-size: 12px;
}

.summary-chip strong {
    font-size: 14px;
    color: #111827;
}

.reward-table {
    min-width: 1040px;
}

.tab-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
}

.tab-count-group {
    display: flex;
    gap: 10px;
    align-items: center;
}

.tab-count {
    color: #6b7280;
    font-size: 13px;
}

.tab-count-muted {
    color: #9ca3af;
    font-size: 12px;
}

.warning-panel {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 12px;
    padding: 10px 12px;
    background: #fff7ed;
    border: 1px solid #fed7aa;
    border-radius: 8px;
    color: #c2410c;
    font-size: 13px;
}

.warning-panel p {
    margin: 0;
}

.desc-cell {
    min-width: 220px;
    color: #6b7280;
}

.editor-stack {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.editor-panel {
    padding: 14px;
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
}

.editor-panel-head {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 12px;
}

.editor-panel-head strong {
    color: #111827;
    font-size: 14px;
}

.editor-panel-kicker {
    font-size: 11px;
    font-weight: 700;
    color: #6366f1;
    text-transform: uppercase;
}

.metric-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.metric-field {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.metric-field span {
    font-size: 12px;
    font-weight: 700;
    color: #4b5563;
}

.ag-textarea {
    width: 100%;
    min-height: 96px;
    padding: 10px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    color: #111827;
    background: #fff;
    resize: vertical;
    outline: none;
    box-sizing: border-box;
}

.ag-textarea:focus {
    border-color: #6366f1;
}

@media (max-width: 760px) {
    .metric-grid {
        grid-template-columns: 1fr;
    }

    .tab-toolbar,
    .tab-count-group {
        flex-direction: column;
        align-items: stretch;
    }

    .tab-toolbar .ag-btn {
        width: 100%;
    }
}
</style>
