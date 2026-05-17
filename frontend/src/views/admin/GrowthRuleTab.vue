<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    createAdminGrowthRuleApi,
    deleteAdminGrowthRuleApi,
    getAdminGrowthRulesApi,
    updateAdminGrowthRuleApi,
} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';
import ADrawer from '@/components/ADrawer.vue';

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
    {value: 'SKIP', label: '达到上限后不发放', desc: '适合评论、点赞等高频行为，超过今日次数后直接跳过。'},
    {value: 'PARTIAL', label: '只发放剩余额度', desc: '适合规则中途调小或人工补偿，只补到今日经验上限。'},
];

const ruleEventLabel = (eventType) =>
    RULE_EVENT_OPTIONS.find((item) => item.value === eventType)?.label || eventType || '-';
const ruleRoleLabel = (role) =>
    RULE_ROLE_OPTIONS.find((item) => item.value === role)?.label || role || '-';
const limitStrategyLabel = (strategy) =>
    LIMIT_STRATEGY_OPTIONS.find((item) => item.value === strategy)?.label || strategy || '-';
const dailyMaxExp = (rule) => {
    const limit = Number(rule?.dailyLimit || 0);
    const amount = Number(rule?.expAmount || 0);
    return limit > 0 ? limit * amount : null;
};
const dailyLimitText = (rule) => {
    const limit = Number(rule?.dailyLimit || 0);
    return limit > 0 ? `${limit} 次` : '不限次数';
};
const dailyMaxExpText = (rule) => {
    const max = dailyMaxExp(rule);
    return max === null ? '不限' : `${max} 经验`;
};

// ── Rule Templates ──────────────────────────────────────────────────
const RULE_TEMPLATES = [
    {name: '发布文章 +30', fill: {eventType: 'PUBLISH', role: 'ACTOR', expAmount: 30, dailyLimit: 3, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '评论 +3', fill: {eventType: 'COMMENT', role: 'ACTOR', expAmount: 3, dailyLimit: 10, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '被评论 +5', fill: {eventType: 'COMMENT', role: 'AUTHOR', expAmount: 5, dailyLimit: 20, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '阅读 +1', fill: {eventType: 'READ', role: 'ACTOR', expAmount: 1, dailyLimit: 20, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '收藏 +3', fill: {eventType: 'FAVORITE', role: 'ACTOR', expAmount: 3, dailyLimit: 10, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '被收藏 +8', fill: {eventType: 'FAVORITE', role: 'AUTHOR', expAmount: 8, dailyLimit: 20, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '分享 +3', fill: {eventType: 'SHARE', role: 'ACTOR', expAmount: 3, dailyLimit: 5, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '关注 +5', fill: {eventType: 'FOLLOW', role: 'ACTOR', expAmount: 5, dailyLimit: 5, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '被关注 +10', fill: {eventType: 'FOLLOW', role: 'AUTHOR', expAmount: 10, dailyLimit: 10, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '点赞 +1', fill: {eventType: 'LIKE', role: 'ACTOR', expAmount: 1, dailyLimit: 20, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '被点赞 +3', fill: {eventType: 'LIKE', role: 'AUTHOR', expAmount: 3, dailyLimit: 30, dailyLimitStrategy: 'SKIP', enabled: true}},
];

const RECOMMENDED_RULES = [
    {eventType: 'READ', role: 'ACTOR', expAmount: 1, dailyLimit: 20},
    {eventType: 'LIKE', role: 'ACTOR', expAmount: 1, dailyLimit: 20},
    {eventType: 'FAVORITE', role: 'ACTOR', expAmount: 3, dailyLimit: 10},
    {eventType: 'SHARE', role: 'ACTOR', expAmount: 3, dailyLimit: 5},
    {eventType: 'COMMENT', role: 'ACTOR', expAmount: 3, dailyLimit: 10},
    {eventType: 'FOLLOW', role: 'ACTOR', expAmount: 5, dailyLimit: 5},
    {eventType: 'PUBLISH', role: 'ACTOR', expAmount: 30, dailyLimit: 3},
    {eventType: 'LIKE', role: 'AUTHOR', expAmount: 3, dailyLimit: 30},
    {eventType: 'FAVORITE', role: 'AUTHOR', expAmount: 8, dailyLimit: 20},
    {eventType: 'COMMENT', role: 'AUTHOR', expAmount: 5, dailyLimit: 20},
    {eventType: 'FOLLOW', role: 'AUTHOR', expAmount: 10, dailyLimit: 10},
].map((rule) => ({
    ...rule,
    dailyLimitStrategy: 'SKIP',
    enabled: true,
    effectiveAt: null,
    reason: '应用长期活跃 + 内容认可型推荐方案',
}));

// ── State ───────────────────────────────────────────────────────────
const growthRules = ref([]);
const growthRulesLoading = ref(false);
const growthRulesError = ref('');
const growthRuleSaving = ref(false);

const drawerVisible = ref(false);
const drawerMode = ref('create'); // 'create' | 'edit'

const growthRuleForm = reactive({
    id: null, eventType: 'PUBLISH', role: 'AUTHOR', expAmount: 20,
    dailyLimit: 0, dailyLimitStrategy: 'SKIP', enabled: true, effectiveAt: '', reason: '', version: null,
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

const drawerTitle = computed(() => drawerMode.value === 'edit' ? '编辑经验规则' : '新增经验规则');

const enabledRuleCount = computed(() => growthRules.value.filter(rule => rule.enabled).length);
const disabledRuleCount = computed(() => growthRules.value.length - enabledRuleCount.value);

const isEventSelected = (value) => growthRuleForm.eventType === value;
const isRoleSelected = (value) => growthRuleForm.role === value;
const isStrategySelected = (value) => growthRuleForm.dailyLimitStrategy === value;

// ── Impact Preview ──────────────────────────────────────────────────
const impactPreviewText = computed(() => {
    const event = RULE_EVENT_OPTIONS.find(o => o.value === growthRuleForm.eventType)?.label || growthRuleForm.eventType;
    const role = RULE_ROLE_OPTIONS.find(o => o.value === growthRuleForm.role)?.label || growthRuleForm.role;
    const daily = growthRuleForm.dailyLimit > 0 ? `每日最多 ${growthRuleForm.dailyLimit} 次` : '不限次数';
    const maxExp = growthRuleForm.dailyLimit > 0
        ? ` · 今日最高 ${Number(growthRuleForm.dailyLimit || 0) * Number(growthRuleForm.expAmount || 0)} 经验`
        : '';
    return `${event} · ${role} · +${growthRuleForm.expAmount} 经验 · ${daily}${maxExp}`;
});

const dailyLimitHelpText = computed(() => {
    const limit = Number(growthRuleForm.dailyLimit || 0);
    const amount = Number(growthRuleForm.expAmount || 0);
    if (limit <= 0) return '0 表示不限制次数。';
    return `按次数限制：最多触发 ${limit} 次，今日最多发放 ${limit * amount} 经验。`;
});

// ── API ─────────────────────────────────────────────────────────────
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

const buildGrowthRulePayload = () => ({
    id: growthRuleForm.id, eventType: growthRuleForm.eventType, role: growthRuleForm.role,
    expAmount: Number(growthRuleForm.expAmount || 0), dailyLimit: Number(growthRuleForm.dailyLimit || 0),
    dailyLimitStrategy: growthRuleForm.dailyLimitStrategy, enabled: Boolean(growthRuleForm.enabled),
    effectiveAt: growthRuleForm.effectiveAt || null, reason: String(growthRuleForm.reason || '').trim(),
    version: growthRuleForm.version,
});

const validateGrowthRule = () => {
    const p = buildGrowthRulePayload();
    if (!p.eventType) return '请选择行为类型';
    if (!p.role) return '请选择作用角色';
    if (!p.expAmount || p.expAmount <= 0) return '经验值必须大于 0';
    if (p.dailyLimit < 0) return '每日上限不能小于 0';
    return '';
};

const saveGrowthRule = async () => {
    const error = validateGrowthRule();
    if (error) { growthRulesError.value = error; return; }
    growthRuleSaving.value = true;
    growthRulesError.value = '';
    try {
        const p = buildGrowthRulePayload();
        if (p.id) {
            await updateAdminGrowthRuleApi(p);
            toast.success('经验规则已更新');
        } else {
            await createAdminGrowthRuleApi(p);
            toast.success('经验规则已新增');
        }
        drawerVisible.value = false;
        resetGrowthRuleForm();
        await loadGrowthRules();
    } catch (e) {
        growthRulesError.value = e.message || '经验规则保存失败';
        toast.error(growthRulesError.value);
    } finally {
        growthRuleSaving.value = false;
    }
};

const deleteGrowthRule = async (row) => {
    if (!row.id || row.version === undefined) return;
    if (!confirm(`确定要删除「${ruleEventLabel(row.eventType)} · ${ruleRoleLabel(row.role)}」规则吗？`)) {
        return;
    }
    growthRulesError.value = '';
    try {
        await deleteAdminGrowthRuleApi(row.id, row.version);
        toast.success('经验规则已删除');
        await loadGrowthRules();
    } catch (e) {
        const msg = e.message || '删除失败';
        if (msg.includes('OPTIMISTIC_LOCK') || msg.includes('version') || msg.includes('刷新')) {
            growthRulesError.value = '删除失败：数据已被其他人修改，请刷新后重试';
        } else {
            growthRulesError.value = msg;
        }
        toast.error(growthRulesError.value);
    }
};

const applyRecommendedRules = async () => {
    if (!confirm('确定应用推荐经验方案吗？相同行为和角色的规则会被更新，缺失规则会自动新增。')) {
        return;
    }
    growthRuleSaving.value = true;
    growthRulesError.value = '';
    try {
        const latestByKey = new Map(
            growthRules.value.map((rule) => [`${rule.eventType}:${rule.role}`, rule])
        );
        for (const preset of RECOMMENDED_RULES) {
            const existing = latestByKey.get(`${preset.eventType}:${preset.role}`);
            if (existing?.id) {
                await updateAdminGrowthRuleApi({
                    ...preset,
                    id: existing.id,
                    version: existing.version ?? 0,
                });
            } else {
                await createAdminGrowthRuleApi(preset);
            }
        }
        toast.success('推荐经验方案已应用');
        await loadGrowthRules();
    } catch (e) {
        growthRulesError.value = e.message || '推荐经验方案应用失败';
        toast.error(growthRulesError.value);
    } finally {
        growthRuleSaving.value = false;
    }
};

// ── Actions ─────────────────────────────────────────────────────────
const openCreate = () => {
    resetGrowthRuleForm();
    drawerMode.value = 'create';
    growthRulesError.value = '';
    drawerVisible.value = true;
};

const openEdit = (row) => {
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
    drawerMode.value = 'edit';
    growthRulesError.value = '';
    drawerVisible.value = true;
};

const applyTemplate = (tmpl) => {
    growthRuleForm.eventType = tmpl.fill.eventType;
    growthRuleForm.role = tmpl.fill.role;
    growthRuleForm.expAmount = tmpl.fill.expAmount;
    growthRuleForm.dailyLimit = tmpl.fill.dailyLimit;
    growthRuleForm.dailyLimitStrategy = tmpl.fill.dailyLimitStrategy;
    growthRuleForm.enabled = tmpl.fill.enabled;
};

onMounted(() => { loadGrowthRules(); });

defineExpose({loadGrowthRules});
</script>

<template>
    <section class="ag-section">
        <div class="ag-section-head">
            <h2 class="ag-section-title">经验规则配置</h2>
            <span class="ag-section-subtitle">每日上限按次数计算，经验按“单次经验 × 次数上限”封顶</span>
        </div>

        <!-- Templates bar -->
        <div class="tmpl-bar">
            <span class="tmpl-label">快速模板：</span>
            <button
                v-for="tmpl in RULE_TEMPLATES"
                :key="tmpl.name"
                type="button"
                class="ag-btn secondary small tmpl-btn"
                @click="openCreate(); applyTemplate(tmpl)"
            >{{ tmpl.name }}</button>
        </div>

        <!-- Error -->
        <div v-if="growthRulesError" class="ag-error">{{ growthRulesError }}</div>

        <!-- Toolbar -->
        <div class="tab-toolbar">
            <div class="tab-count-group">
                <span class="tab-count">当前 {{ growthRules.length }} 条规则</span>
                <span class="tab-count-muted">启用 {{ enabledRuleCount }}，停用 {{ disabledRuleCount }}</span>
            </div>
            <div class="tab-toolbar-actions">
                <button type="button" class="ag-btn secondary" :disabled="growthRuleSaving" @click="applyRecommendedRules">
                    应用推荐方案
                </button>
                <button type="button" class="ag-btn primary" @click="openCreate">+ 新增规则</button>
            </div>
        </div>

        <!-- Loading / Empty / Table -->
        <div v-if="growthRulesLoading" class="ag-table-empty">规则加载中...</div>
        <div v-else-if="!growthRules.length" class="ag-table-empty">暂无经验规则</div>
        <div v-else class="ag-table-wrap">
            <table class="ag-table rules-table">
                <thead>
                    <tr>
                        <th>行为</th>
                        <th>角色</th>
                        <th>经验</th>
                        <th>每日上限（次数）</th>
                        <th>每日最高经验</th>
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
                        <td>{{ dailyLimitText(rule) }}</td>
                        <td class="daily-max-cell">{{ dailyMaxExpText(rule) }}</td>
                        <td>{{ limitStrategyLabel(rule.dailyLimitStrategy) }}</td>
                        <td>
                            <span :class="['status-chip', rule.enabled ? 'settled' : 'failed']">{{ rule.enabled ? '启用' : '停用' }}</span>
                            <small v-if="rule.effectiveAt">{{ formatAdminDateTime(rule.effectiveAt) }}</small>
                        </td>
                        <td>v{{ rule.version }}</td>
                        <td class="action-cell">
                            <button type="button" class="ag-btn secondary small" @click="openEdit(rule)">编辑</button>
                            <button type="button" class="ag-btn danger small" @click="deleteGrowthRule(rule)">删除</button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- ── Drawer ── -->
        <ADrawer v-model:visible="drawerVisible" :title="drawerTitle" width="560px">
            <div class="rule-editor">
                <section class="editor-panel">
                    <div class="editor-panel-head">
                        <span class="editor-panel-kicker">行为</span>
                        <strong>谁在什么行为后获得经验</strong>
                    </div>
                    <div class="option-grid event-grid">
                        <button
                            v-for="option in RULE_EVENT_OPTIONS"
                            :key="option.value"
                            type="button"
                            :class="['option-card', {selected: isEventSelected(option.value)}]"
                            @click="growthRuleForm.eventType = option.value"
                        >
                            <span>{{ option.label }}</span>
                            <small>{{ option.value }}</small>
                        </button>
                    </div>
                    <div class="role-segment">
                        <button
                            v-for="option in RULE_ROLE_OPTIONS"
                            :key="option.value"
                            type="button"
                            :class="['segment-btn', {selected: isRoleSelected(option.value)}]"
                            @click="growthRuleForm.role = option.value"
                        >
                            {{ option.label }}
                        </button>
                    </div>
                </section>

                <section class="editor-panel">
                    <div class="editor-panel-head">
                        <span class="editor-panel-kicker">额度</span>
                        <strong>单次经验与每日次数上限</strong>
                    </div>
                    <div class="metric-grid">
                        <label class="metric-field">
                            <span>单次经验</span>
                            <input v-model.number="growthRuleForm.expAmount" type="number" min="1" class="ag-input" />
                        </label>
                        <label class="metric-field">
                            <span>每日上限（次数）</span>
                            <input v-model.number="growthRuleForm.dailyLimit" type="number" min="0" class="ag-input" />
                        </label>
                    </div>
                    <p class="field-hint">{{ dailyLimitHelpText }}</p>
                </section>

                <section class="editor-panel">
                    <div class="editor-panel-head">
                        <span class="editor-panel-kicker">策略</span>
                        <strong>达到每日上限后的处理方式</strong>
                    </div>
                    <div class="strategy-grid">
                        <button
                            v-for="option in LIMIT_STRATEGY_OPTIONS"
                            :key="option.value"
                            type="button"
                            :class="['strategy-card', {selected: isStrategySelected(option.value)}]"
                            @click="growthRuleForm.dailyLimitStrategy = option.value"
                        >
                            <span>{{ option.label }}</span>
                            <small>{{ option.desc }}</small>
                        </button>
                    </div>
                </section>

                <section class="editor-panel compact-panel">
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
                </section>

                <div class="impact-preview">
                    <span class="impact-label">规则预览</span>
                    <p class="impact-text">{{ impactPreviewText }}</p>
                    <p class="impact-hint">
                        同一行为和角色只保留一条当前配置；编辑后按最新版本发放经验。
                    </p>
                </div>

                <div v-if="growthRulesError" class="ag-error">{{ growthRulesError }}</div>
            </div>

            <template #footer>
                <button type="button" class="ag-btn secondary" @click="drawerVisible = false">取消</button>
                <button type="button" class="ag-btn primary" :disabled="growthRuleSaving" @click="saveGrowthRule">
                    {{ growthRuleSaving ? '保存中...' : (drawerMode === 'edit' ? '保存修改' : '创建规则') }}
                </button>
            </template>
        </ADrawer>
    </section>
</template>

<style scoped>
/* ── Templates bar ── */
.tmpl-bar {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
    margin-bottom: 16px;
    padding: 10px 14px;
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
}

.tmpl-label {
    font-size: 12px;
    font-weight: 600;
    color: #6b7280;
    white-space: nowrap;
}

.tmpl-btn {
    font-size: 12px !important;
    height: 28px !important;
    padding: 0 10px !important;
}

/* ── Toolbar ── */
.tab-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
}

.tab-count {
    font-size: 13px;
    color: #6b7280;
}

.tab-count-group {
    display: flex;
    gap: 10px;
    align-items: center;
}

.tab-count-muted {
    font-size: 12px;
    color: #9ca3af;
}

.tab-toolbar-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: flex-end;
}

/* ── Rule editor ── */
.rule-editor {
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
    gap: 3px;
    margin-bottom: 12px;
}

.editor-panel-head strong {
    font-size: 14px;
    color: #111827;
}

.editor-panel-kicker {
    font-size: 11px;
    font-weight: 700;
    color: #6366f1;
}

.option-grid {
    display: grid;
    gap: 8px;
}

.event-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
}

.option-card,
.strategy-card,
.segment-btn {
    border: 1px solid #d1d5db;
    background: #fff;
    color: #374151;
    cursor: pointer;
    transition: border-color 0.15s, background 0.15s, color 0.15s;
}

.option-card {
    display: flex;
    flex-direction: column;
    gap: 3px;
    align-items: flex-start;
    min-height: 58px;
    padding: 10px 12px;
    border-radius: 8px;
}

.option-card span,
.strategy-card span {
    font-size: 13px;
    font-weight: 700;
}

.option-card small {
    font-size: 11px;
    color: #9ca3af;
}

.option-card.selected,
.strategy-card.selected,
.segment-btn.selected {
    border-color: #6366f1;
    background: #eef2ff;
    color: #4338ca;
}

.role-segment {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    margin-top: 10px;
}

.segment-btn {
    height: 36px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 700;
}

.metric-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
}

.metric-field {
    display: flex;
    flex-direction: column;
    gap: 5px;
}

.metric-field span {
    font-size: 12px;
    font-weight: 700;
    color: #4b5563;
}

.field-hint {
    margin: 8px 0 0;
    font-size: 12px;
    color: #6b7280;
}

.daily-max-cell {
    color: #475569;
    font-weight: 700;
    white-space: nowrap;
}

.strategy-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
}

.strategy-card {
    display: flex;
    flex-direction: column;
    gap: 5px;
    align-items: flex-start;
    padding: 11px 12px;
    border-radius: 8px;
    text-align: left;
}

.strategy-card small {
    color: #6b7280;
    font-size: 12px;
    line-height: 1.45;
}

.compact-panel {
    display: grid;
    grid-template-columns: 1fr;
    gap: 12px;
}

/* ── Impact Preview ── */
.impact-preview {
    padding: 12px 14px;
    background: #f0f9ff;
    border: 1px solid #bae6fd;
    border-radius: 8px;
}

.impact-label {
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: #0284c7;
}

.impact-text {
    margin: 6px 0 0;
    font-size: 14px;
    font-weight: 600;
    color: #0c4a6e;
}

.impact-hint {
    margin: 6px 0 0;
    font-size: 12px;
    color: #64748b;
    line-height: 1.4;
}
</style>
