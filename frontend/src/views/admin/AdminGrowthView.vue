<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    adminAdjustPointsApi,
    adminGetPointAccountApi,
    createAdminGrowthRuleApi,
    getAdminGrowthRulesApi,
    getAdminLevelThresholdsApi,
    getAdminRevenueSharesApi,
    retryRevenueShareSettlementApi,
    saveAdminLevelThresholdsApi,
    updateAdminGrowthRuleApi,
} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';

const toast = useToast();

const toLocalDateTimeInput = (value) => {
    if (!value) return '';
    return String(value).replace(' ', 'T').slice(0, 16);
};

const RULE_EVENT_OPTIONS = [
    {value: 'PUBLISH', label: '发布文章'},
    {value: 'COMMENT', label: '发表评论'},
    {value: 'READ', label: '阅读文章'},
    {value: 'FAVORITE', label: '收藏文章'},
    {value: 'SHARE', label: '分享文章'},
    {value: 'FOLLOW', label: '关注用户'},
    {value: 'LIKE', label: '点赞文章'},
    {value: 'LEVEL_UP', label: '等级升级'},
];

const RULE_ROLE_OPTIONS = [
    {value: 'ACTOR', label: '操作者'},
    {value: 'AUTHOR', label: '作者'},
];

const LIMIT_STRATEGY_OPTIONS = [
    {value: 'SKIP', label: '达上限跳过'},
    {value: 'PARTIAL', label: '发放剩余额度'},
];

const ruleEventLabel = (eventType) =>
    RULE_EVENT_OPTIONS.find((item) => item.value === eventType)?.label || eventType || '-';
const ruleRoleLabel = (role) =>
    RULE_ROLE_OPTIONS.find((item) => item.value === role)?.label || role || '-';
const limitStrategyLabel = (strategy) =>
    LIMIT_STRATEGY_OPTIONS.find((item) => item.value === strategy)?.label || strategy || '-';

// ── 经验规则配置 ─────────────────────────────────────────────────
const growthRules = ref([]);
const growthRulesLoading = ref(false);
const growthRulesError = ref('');
const growthRuleSaving = ref(false);
const growthRuleForm = reactive({
    id: null,
    eventType: 'PUBLISH',
    role: 'AUTHOR',
    expAmount: 20,
    dailyLimit: 0,
    dailyLimitStrategy: 'SKIP',
    enabled: true,
    effectiveAt: '',
    reason: '',
    version: null,
});

const resetGrowthRuleForm = () => {
    growthRuleForm.id = null;
    growthRuleForm.eventType = 'PUBLISH';
    growthRuleForm.role = 'AUTHOR';
    growthRuleForm.expAmount = 20;
    growthRuleForm.dailyLimit = 0;
    growthRuleForm.dailyLimitStrategy = 'SKIP';
    growthRuleForm.enabled = true;
    growthRuleForm.effectiveAt = '';
    growthRuleForm.reason = '';
    growthRuleForm.version = null;
};

const loadGrowthRules = async () => {
    growthRulesLoading.value = true;
    growthRulesError.value = '';
    try {
        growthRules.value = await getAdminGrowthRulesApi();
    } catch (e) {
        growthRulesError.value = e.message || '经验规则加载失败';
        growthRules.value = [];
    } finally {
        growthRulesLoading.value = false;
    }
};

const editGrowthRule = (row) => {
    growthRuleForm.id = row.id;
    growthRuleForm.eventType = row.eventType || 'PUBLISH';
    growthRuleForm.role = row.role || 'AUTHOR';
    growthRuleForm.expAmount = Number(row.expAmount || 0);
    growthRuleForm.dailyLimit = Number(row.dailyLimit || 0);
    growthRuleForm.dailyLimitStrategy = row.dailyLimitStrategy || 'SKIP';
    growthRuleForm.enabled = Boolean(row.enabled);
    growthRuleForm.effectiveAt = toLocalDateTimeInput(row.effectiveAt);
    growthRuleForm.reason = row.reason || '';
    growthRuleForm.version = row.version ?? 0;
};

const buildGrowthRulePayload = () => ({
    id: growthRuleForm.id,
    eventType: growthRuleForm.eventType,
    role: growthRuleForm.role,
    expAmount: Number(growthRuleForm.expAmount || 0),
    dailyLimit: Number(growthRuleForm.dailyLimit || 0),
    dailyLimitStrategy: growthRuleForm.dailyLimitStrategy,
    enabled: Boolean(growthRuleForm.enabled),
    effectiveAt: growthRuleForm.effectiveAt || null,
    reason: String(growthRuleForm.reason || '').trim(),
    version: growthRuleForm.version,
});

const validateGrowthRule = () => {
    const payload = buildGrowthRulePayload();
    if (!payload.eventType) return '请选择行为类型';
    if (!payload.role) return '请选择作用角色';
    if (!payload.expAmount || payload.expAmount <= 0) return '经验值必须大于 0';
    if (payload.dailyLimit < 0) return '每日上限不能小于 0';
    return '';
};

const saveGrowthRule = async () => {
    const error = validateGrowthRule();
    if (error) {
        growthRulesError.value = error;
        return;
    }
    growthRuleSaving.value = true;
    growthRulesError.value = '';
    try {
        const payload = buildGrowthRulePayload();
        if (payload.id) {
            await updateAdminGrowthRuleApi(payload);
            toast.success('经验规则已更新');
        } else {
            await createAdminGrowthRuleApi(payload);
            toast.success('经验规则已新增');
        }
        resetGrowthRuleForm();
        await loadGrowthRules();
    } catch (e) {
        growthRulesError.value = e.message || '经验规则保存失败';
        toast.error(growthRulesError.value);
    } finally {
        growthRuleSaving.value = false;
    }
};

// ── 等级阈值配置 ─────────────────────────────────────────────────
const levelThresholds = ref([]);
const thresholdLoading = ref(false);
const thresholdSaving = ref(false);
const thresholdError = ref('');

const normalizeThresholdRow = (row = {}, index = 0) => ({
    level: Number(row.level || index + 1),
    minExp: Number(row.minExp || 0),
    levelName: row.levelName || `LV${Number(row.level || index + 1)}`,
    description: row.description || '',
    version: row.version ?? 0,
});

const loadLevelThresholds = async () => {
    thresholdLoading.value = true;
    thresholdError.value = '';
    try {
        const result = await getAdminLevelThresholdsApi();
        levelThresholds.value = (result || []).map(normalizeThresholdRow);
    } catch (e) {
        thresholdError.value = e.message || '等级阈值加载失败';
        levelThresholds.value = [];
    } finally {
        thresholdLoading.value = false;
    }
};

const addThresholdRow = () => {
    const nextLevel = levelThresholds.value.length
        ? Math.max(...levelThresholds.value.map((item) => Number(item.level || 0))) + 1
        : 1;
    const prevMinExp = levelThresholds.value.length
        ? Number(levelThresholds.value[levelThresholds.value.length - 1].minExp || 0)
        : 0;
    levelThresholds.value.push({
        level: nextLevel,
        minExp: nextLevel === 1 ? 0 : prevMinExp + 100,
        levelName: `LV${nextLevel}`,
        description: '',
        version: 0,
    });
};

const validateThresholds = () => {
    if (!levelThresholds.value.length) return '至少保留一个等级阈值';
    const rows = [...levelThresholds.value].sort((a, b) => Number(a.level) - Number(b.level));
    let lastMinExp = -1;
    for (const row of rows) {
        if (!row.level || Number(row.level) <= 0) return '等级必须大于 0';
        if (Number(row.minExp) < 0) return '最低经验不能小于 0';
        if (!String(row.levelName || '').trim()) return '等级名称不能为空';
        if (Number(row.minExp) < lastMinExp) return '最低经验需要随等级递增';
        lastMinExp = Number(row.minExp);
    }
    return '';
};

const saveLevelThresholds = async () => {
    const error = validateThresholds();
    if (error) {
        thresholdError.value = error;
        return;
    }
    thresholdSaving.value = true;
    thresholdError.value = '';
    try {
        const payload = [...levelThresholds.value]
            .sort((a, b) => Number(a.level) - Number(b.level))
            .map((row) => ({
                level: Number(row.level),
                minExp: Number(row.minExp),
                levelName: String(row.levelName || '').trim(),
                description: String(row.description || '').trim(),
                version: Number(row.version || 0),
            }));
        await saveAdminLevelThresholdsApi(payload);
        toast.success('等级阈值已保存');
        await loadLevelThresholds();
    } catch (e) {
        thresholdError.value = e.message || '等级阈值保存失败';
        toast.error(thresholdError.value);
    } finally {
        thresholdSaving.value = false;
    }
};

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
    loadGrowthRules();
    loadLevelThresholds();
    loadRevenueShares();
});
</script>

<template>
    <div class="admin-growth">
        <!-- 经验规则配置 -->
        <section class="ag-section">
            <div class="ag-section-head">
                <h2 class="ag-section-title">经验规则配置</h2>
                <span class="ag-section-subtitle">行为事件触发后按规则发放经验</span>
            </div>

            <div class="rule-config-grid">
                <div class="ag-form rule-form">
                    <div class="form-row">
                        <label class="form-label">行为类型</label>
                        <select v-model="growthRuleForm.eventType" class="ag-input">
                            <option v-for="option in RULE_EVENT_OPTIONS" :key="option.value" :value="option.value">
                                {{ option.label }}
                            </option>
                        </select>
                    </div>
                    <div class="form-row">
                        <label class="form-label">作用角色</label>
                        <select v-model="growthRuleForm.role" class="ag-input">
                            <option v-for="option in RULE_ROLE_OPTIONS" :key="option.value" :value="option.value">
                                {{ option.label }}
                            </option>
                        </select>
                    </div>
                    <div class="form-row compact-row">
                        <label class="form-label">单次经验</label>
                        <input v-model.number="growthRuleForm.expAmount" type="number" min="1" class="ag-input" />
                    </div>
                    <div class="form-row compact-row">
                        <label class="form-label">每日上限</label>
                        <input v-model.number="growthRuleForm.dailyLimit" type="number" min="0" class="ag-input" />
                    </div>
                    <div class="form-row">
                        <label class="form-label">超限策略</label>
                        <select v-model="growthRuleForm.dailyLimitStrategy" class="ag-input">
                            <option v-for="option in LIMIT_STRATEGY_OPTIONS" :key="option.value" :value="option.value">
                                {{ option.label }}
                            </option>
                        </select>
                    </div>
                    <label class="ag-toggle">
                        <input v-model="growthRuleForm.enabled" type="checkbox" />
                        <span>启用规则</span>
                    </label>
                    <div class="form-row">
                        <label class="form-label">生效时间</label>
                        <input v-model="growthRuleForm.effectiveAt" type="datetime-local" class="ag-input" />
                    </div>
                    <div class="form-row">
                        <label class="form-label">变更原因</label>
                        <input v-model="growthRuleForm.reason" type="text" maxlength="500" class="ag-input" />
                    </div>
                    <div v-if="growthRulesError" class="ag-error">{{ growthRulesError }}</div>
                    <div class="ag-action-row">
                        <button
                            type="button"
                            class="ag-btn primary"
                            :disabled="growthRuleSaving"
                            @click="saveGrowthRule"
                        >
                            {{ growthRuleSaving ? '保存中...' : (growthRuleForm.id ? '保存规则' : '新增规则') }}
                        </button>
                        <button type="button" class="ag-btn secondary" @click="resetGrowthRuleForm">
                            清空
                        </button>
                    </div>
                </div>

                <div class="ag-table-wrap">
                    <div v-if="growthRulesLoading" class="ag-table-empty">规则加载中...</div>
                    <div v-else-if="!growthRules.length" class="ag-table-empty">暂无经验规则</div>
                    <table v-else class="ag-table rules-table">
                        <thead>
                            <tr>
                                <th>行为</th>
                                <th>角色</th>
                                <th>经验</th>
                                <th>上限</th>
                                <th>策略</th>
                                <th>生效</th>
                                <th>版本</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="rule in growthRules" :key="rule.id">
                                <td>
                                    <strong>{{ ruleEventLabel(rule.eventType) }}</strong>
                                    <small>{{ rule.eventType }}</small>
                                </td>
                                <td>{{ ruleRoleLabel(rule.role) }}</td>
                                <td class="delta plus">+{{ rule.expAmount }}</td>
                                <td>{{ rule.dailyLimit || '不限' }}</td>
                                <td>{{ limitStrategyLabel(rule.dailyLimitStrategy) }}</td>
                                <td>
                                    <span :class="['status-chip', rule.enabled ? 'settled' : 'failed']">
                                        {{ rule.enabled ? '启用' : '停用' }}
                                    </span>
                                    <small v-if="rule.effectiveAt">{{ formatAdminDateTime(rule.effectiveAt) }}</small>
                                </td>
                                <td>v{{ rule.version }}</td>
                                <td>
                                    <button type="button" class="ag-btn secondary small" @click="editGrowthRule(rule)">
                                        编辑
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>

        <!-- 等级阈值配置 -->
        <section class="ag-section">
            <div class="ag-section-head">
                <h2 class="ag-section-title">等级阈值配置</h2>
                <span class="ag-section-subtitle">最低经验值决定用户等级展示</span>
            </div>

            <div v-if="thresholdError" class="ag-error">{{ thresholdError }}</div>
            <div v-if="thresholdLoading" class="ag-table-empty">等级阈值加载中...</div>
            <div v-else class="ag-table-wrap">
                <table class="ag-table threshold-table">
                    <thead>
                        <tr>
                            <th>等级</th>
                            <th>最低经验</th>
                            <th>等级名称</th>
                            <th>描述</th>
                            <th>版本</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in levelThresholds" :key="row.level">
                            <td><input v-model.number="row.level" type="number" min="1" class="ag-input cell-input" /></td>
                            <td><input v-model.number="row.minExp" type="number" min="0" class="ag-input cell-input" /></td>
                            <td><input v-model.trim="row.levelName" type="text" class="ag-input cell-input" /></td>
                            <td><input v-model.trim="row.description" type="text" class="ag-input cell-input wide" /></td>
                            <td>v{{ row.version || 0 }}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div class="ag-action-row threshold-actions">
                <button type="button" class="ag-btn secondary" @click="addThresholdRow">新增等级</button>
                <button
                    type="button"
                    class="ag-btn primary"
                    :disabled="thresholdSaving || thresholdLoading"
                    @click="saveLevelThresholds"
                >
                    {{ thresholdSaving ? '保存中...' : '保存等级阈值' }}
                </button>
            </div>
        </section>

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

.rule-config-grid {
    display: grid;
    grid-template-columns: minmax(260px, 360px) minmax(0, 1fr);
    gap: 18px;
    align-items: start;
}

.rule-form {
    max-width: none;
    padding: 16px;
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
}

.compact-row {
    max-width: 180px;
}

.ag-toggle {
    display: inline-flex;
    gap: 8px;
    align-items: center;
    width: fit-content;
    color: #374151;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
}

.ag-toggle input {
    width: 16px;
    height: 16px;
    accent-color: #6366f1;
}

.ag-action-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
}

.rules-table td strong {
    display: block;
    color: #111827;
    font-size: 13px;
}

.rules-table td small {
    display: block;
    margin-top: 3px;
    color: #9ca3af;
    font-size: 11px;
}

.threshold-actions {
    margin-top: 14px;
}

.cell-input {
    width: 110px;
    min-width: 0;
}

.cell-input.wide {
    width: min(280px, 32vw);
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

@media (max-width: 980px) {
    .rule-config-grid {
        grid-template-columns: 1fr;
    }

    .compact-row {
        max-width: none;
    }
}
</style>

