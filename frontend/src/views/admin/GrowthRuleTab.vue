<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    createAdminGrowthRuleApi,
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
    {value: 'SKIP', label: '达上限跳过'},
    {value: 'PARTIAL', label: '发放剩余额度'},
];

const ruleEventLabel = (eventType) =>
    RULE_EVENT_OPTIONS.find((item) => item.value === eventType)?.label || eventType || '-';
const ruleRoleLabel = (role) =>
    RULE_ROLE_OPTIONS.find((item) => item.value === role)?.label || role || '-';
const limitStrategyLabel = (strategy) =>
    LIMIT_STRATEGY_OPTIONS.find((item) => item.value === strategy)?.label || strategy || '-';

// ── Rule Templates ──────────────────────────────────────────────────
const RULE_TEMPLATES = [
    {name: '发布文章 +10', fill: {eventType: 'PUBLISH', role: 'ACTOR', expAmount: 10, dailyLimit: 10, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '发布文章 +5(作者)', fill: {eventType: 'PUBLISH', role: 'AUTHOR', expAmount: 5, dailyLimit: 20, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '评论 +2', fill: {eventType: 'COMMENT', role: 'ACTOR', expAmount: 2, dailyLimit: 20, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '阅读 +1', fill: {eventType: 'READ', role: 'ACTOR', expAmount: 1, dailyLimit: 50, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '收藏 +3', fill: {eventType: 'FAVORITE', role: 'ACTOR', expAmount: 3, dailyLimit: 10, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '分享 +2', fill: {eventType: 'SHARE', role: 'ACTOR', expAmount: 2, dailyLimit: 10, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '关注 +1', fill: {eventType: 'FOLLOW', role: 'ACTOR', expAmount: 1, dailyLimit: 15, dailyLimitStrategy: 'SKIP', enabled: true}},
    {name: '点赞 +1', fill: {eventType: 'LIKE', role: 'ACTOR', expAmount: 1, dailyLimit: 20, dailyLimitStrategy: 'SKIP', enabled: true}},
];

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

// ── Impact Preview ──────────────────────────────────────────────────
const impactPreviewText = computed(() => {
    const event = RULE_EVENT_OPTIONS.find(o => o.value === growthRuleForm.eventType)?.label || growthRuleForm.eventType;
    const role = RULE_ROLE_OPTIONS.find(o => o.value === growthRuleForm.role)?.label || growthRuleForm.role;
    const daily = growthRuleForm.dailyLimit > 0 ? `每日最多 ${growthRuleForm.dailyLimit} 次` : '不限次数';
    return `${event} · ${role} · +${growthRuleForm.expAmount} 经验 · ${daily}`;
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
            <span class="ag-section-subtitle">行为事件触发后按规则发放经验</span>
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
            <span class="tab-count">共 {{ growthRules.length }} 条规则</span>
            <button type="button" class="ag-btn primary" @click="openCreate">+ 新增规则</button>
        </div>

        <!-- Loading / Empty / Table -->
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
                        <span :class="['status-chip', rule.enabled ? 'settled' : 'failed']">{{ rule.enabled ? '启用' : '停用' }}</span>
                        <small v-if="rule.effectiveAt">{{ formatAdminDateTime(rule.effectiveAt) }}</small>
                    </td>
                    <td>v{{ rule.version }}</td>
                    <td><button type="button" class="ag-btn secondary small" @click="openEdit(rule)">编辑</button></td>
                </tr>
            </tbody>
        </table>

        <!-- ── Drawer ── -->
        <ADrawer v-model:visible="drawerVisible" :title="drawerTitle" width="440px">
            <div class="ag-form drawer-form">
                <div class="form-row">
                    <label class="form-label">行为类型</label>
                    <select v-model="growthRuleForm.eventType" class="ag-input">
                        <option v-for="option in RULE_EVENT_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
                    </select>
                </div>
                <div class="form-row">
                    <label class="form-label">作用角色</label>
                    <select v-model="growthRuleForm.role" class="ag-input">
                        <option v-for="option in RULE_ROLE_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
                    </select>
                </div>
                <div class="form-row">
                    <label class="form-label">单次经验</label>
                    <input v-model.number="growthRuleForm.expAmount" type="number" min="1" class="ag-input" />
                </div>
                <div class="form-row">
                    <label class="form-label">每日上限</label>
                    <input v-model.number="growthRuleForm.dailyLimit" type="number" min="0" class="ag-input" />
                </div>
                <div class="form-row">
                    <label class="form-label">超限策略</label>
                    <select v-model="growthRuleForm.dailyLimitStrategy" class="ag-input">
                        <option v-for="option in LIMIT_STRATEGY_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
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

                <!-- Impact Preview -->
                <div class="impact-preview">
                    <span class="impact-label">规则预览</span>
                    <p class="impact-text">{{ impactPreviewText }}</p>
                    <p class="impact-hint">保存后立即生效。已启用规则将开始对用户行为发放经验。</p>
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

/* ── Drawer form ── */
.drawer-form {
    max-width: none;
    gap: 16px;
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
