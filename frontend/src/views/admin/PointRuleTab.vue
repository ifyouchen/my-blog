<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import {useToast} from '@/composables/useToast';
import {useConfirmDialog} from '@/composables/useConfirmDialog';
import {
    createAdminPointRuleApi,
    deleteAdminPointRuleApi,
    getAdminPointRulesApi,
    updateAdminPointRuleApi,
} from '@/api/growth';
import ADrawer from '@/components/ADrawer.vue';
import AdminSelect from '@/components/admin/AdminSelect.vue';

const toast = useToast();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const SOURCE_TYPE_OPTIONS = [
    {value: 'SIGN_IN', label: '签到基础积分'},
    {value: 'INVITE', label: '邀请奖励积分'},
    {value: 'RECHARGE', label: '每 1 元兑换积分数'},
    {value: 'PUBLISH', label: '发布文章奖励积分'},
    {value: 'UNLOCK_SHARE_RATIO', label: '平台分成比例 %'},
];

const sourceTypeLabel = (st) => SOURCE_TYPE_OPTIONS.find(o => o.value === st)?.label || st || '-';

// ── Rule Templates ──────────────────────────────────────────────────
const POINT_TEMPLATES = [
    {name: '签到 +5', fill: {sourceType: 'SIGN_IN', pointAmount: 5, dailyLimit: 1, enabled: true}},
    {name: '签到 +10', fill: {sourceType: 'SIGN_IN', pointAmount: 10, dailyLimit: 1, enabled: true}},
    {name: '邀请 +100', fill: {sourceType: 'INVITE', pointAmount: 100, dailyLimit: 0, enabled: true}},
    {name: '邀请 +200', fill: {sourceType: 'INVITE', pointAmount: 200, dailyLimit: 0, enabled: true}},
    {name: '发布 +20', fill: {sourceType: 'PUBLISH', pointAmount: 20, dailyLimit: 5, enabled: true}},
    {name: '积分/元 10:1', fill: {sourceType: 'RECHARGE', pointAmount: 10, dailyLimit: 0, enabled: true}},
    {name: '分成 30%', fill: {sourceType: 'UNLOCK_SHARE_RATIO', pointAmount: 30, dailyLimit: 0, enabled: true}},
    {name: '分成 50%', fill: {sourceType: 'UNLOCK_SHARE_RATIO', pointAmount: 50, dailyLimit: 0, enabled: true}},
];

// ── State ──
const pointRules = ref([]);
const pointRulesLoading = ref(false);
const pointRulesError = ref('');
const pointRuleSaving = ref(false);

const drawerVisible = ref(false);
const drawerMode = ref('create'); // 'create' | 'edit'

const pointRuleForm = reactive({
    id: null, sourceType: 'SIGN_IN', pointAmount: 10, dailyLimit: 0, enabled: true, reason: '', version: null,
});

const resetPointRuleForm = () => {
    pointRuleForm.id = null;
    pointRuleForm.sourceType = 'SIGN_IN';
    pointRuleForm.pointAmount = 10;
    pointRuleForm.dailyLimit = 0;
    pointRuleForm.enabled = true;
    pointRuleForm.reason = '';
    pointRuleForm.version = null;
};

const drawerTitle = computed(() => drawerMode.value === 'edit' ? '编辑积分规则' : '新增积分规则');

const pointAmountLabel = computed(() => {
    switch (pointRuleForm.sourceType) {
        case 'RECHARGE': return '每 1 元兑换积分';
        case 'UNLOCK_SHARE_RATIO': return '平台分成比例 %';
        default: return '单次奖励积分';
    }
});

// ── Impact Preview ──
const impactPreviewText = computed(() => {
    const label = SOURCE_TYPE_OPTIONS.find(o => o.value === pointRuleForm.sourceType)?.label || pointRuleForm.sourceType;
    const daily = pointRuleForm.dailyLimit > 0 ? `每日最多 ${pointRuleForm.dailyLimit} 次` : '不限次数';
    if (pointRuleForm.sourceType === 'UNLOCK_SHARE_RATIO') {
        return `${label}：${pointRuleForm.pointAmount}% 平台抽成 · ${daily}`;
    }
    if (pointRuleForm.sourceType === 'RECHARGE') {
        return `${label}：每 1 元 = ${pointRuleForm.pointAmount} 积分 · ${daily}`;
    }
    return `${label}：+${pointRuleForm.pointAmount} 积分 · ${daily}`;
});

// ── API ──
const loadPointRules = async () => {
    pointRulesLoading.value = true;
    pointRulesError.value = '';
    try {
        pointRules.value = await getAdminPointRulesApi();
    } catch (e) {
        pointRulesError.value = e.message || '积分规则加载失败';
        pointRules.value = [];
    } finally {
        pointRulesLoading.value = false;
    }
};

const buildPointRulePayload = () => ({
    id: pointRuleForm.id, sourceType: pointRuleForm.sourceType,
    pointAmount: Number(pointRuleForm.pointAmount || 0),
    dailyLimit: Number(pointRuleForm.dailyLimit || 0),
    enabled: Boolean(pointRuleForm.enabled),
    reason: String(pointRuleForm.reason || '').trim(),
    version: pointRuleForm.version,
});

const validatePointRule = () => {
    const p = buildPointRulePayload();
    if (!p.sourceType) return '请选择来源类型';
    if (p.sourceType === 'UNLOCK_SHARE_RATIO') {
        if (p.pointAmount < 0 || p.pointAmount > 100) return '分成比例必须在 0~100 之间';
    } else {
        if (p.pointAmount <= 0) return '积分奖励必须大于 0';
    }
    if (p.dailyLimit < 0) return '每日上限不能小于 0';
    return '';
};

const savePointRule = async () => {
    const error = validatePointRule();
    if (error) { pointRulesError.value = error; return; }
    pointRuleSaving.value = true;
    pointRulesError.value = '';
    try {
        const p = buildPointRulePayload();
        if (p.id) {
            await updateAdminPointRuleApi(p);
            toast.success('积分规则已更新');
        } else {
            await createAdminPointRuleApi(p);
            toast.success('积分规则已新增');
        }
        drawerVisible.value = false;
        resetPointRuleForm();
        await loadPointRules();
    } catch (e) {
        const msg = e.message || '积分规则保存失败';
        if (msg.includes('OPTIMISTIC_LOCK') || msg.includes('version') || msg.includes('刷新')) {
            pointRulesError.value = '保存失败：数据已被其他人修改，请刷新后重试';
        } else {
            pointRulesError.value = msg;
        }
        toast.error(pointRulesError.value);
    } finally {
        pointRuleSaving.value = false;
    }
};

// ── Actions ──
const openCreate = () => {
    resetPointRuleForm();
    drawerMode.value = 'create';
    pointRulesError.value = '';
    drawerVisible.value = true;
};

const openEdit = (row) => {
    pointRuleForm.id = row.id;
    pointRuleForm.sourceType = row.sourceType || 'SIGN_IN';
    pointRuleForm.pointAmount = Number(row.pointAmount || 0);
    pointRuleForm.dailyLimit = Number(row.dailyLimit || 0);
    pointRuleForm.enabled = Boolean(row.enabled);
    pointRuleForm.reason = row.reason || '';
    pointRuleForm.version = row.version ?? 0;
    drawerMode.value = 'edit';
    pointRulesError.value = '';
    drawerVisible.value = true;
};

const applyTemplate = (tmpl) => {
    pointRuleForm.sourceType = tmpl.fill.sourceType;
    pointRuleForm.pointAmount = tmpl.fill.pointAmount;
    pointRuleForm.dailyLimit = tmpl.fill.dailyLimit;
    pointRuleForm.enabled = tmpl.fill.enabled;
};

const restorePointRule = async (row) => {
    if (!row.id || row.version === undefined) return;
    openConfirmDialog({
        title: '恢复积分规则',
        message: `确定要恢复 ${sourceTypeLabel(row.sourceType)} 规则吗？`,
        confirmText: '恢复',
        onConfirm: async () => {
            pointRulesError.value = '';
            try {
                await createAdminPointRuleApi({
                    sourceType: row.sourceType,
                    pointAmount: row.pointAmount,
                    dailyLimit: row.dailyLimit,
                    enabled: row.enabled,
                    reason: '管理员恢复已删除规则',
                });
                toast.success('积分规则已恢复');
                await loadPointRules();
            } catch (e) {
                const msg = e.message || '恢复失败';
                pointRulesError.value = msg;
                toast.error(msg);
            }
        }
    });
};

const deletePointRule = async (row) => {
    if (!row.id || row.version === undefined) return;
    openConfirmDialog({
        title: '删除积分规则',
        message: `确定要删除 ${sourceTypeLabel(row.sourceType)} 规则吗？`,
        confirmText: '删除',
        tone: 'danger',
        onConfirm: async () => {
            pointRulesError.value = '';
            try {
                await deleteAdminPointRuleApi(row.id, row.version);
                toast.success('积分规则已删除');
                await loadPointRules();
            } catch (e) {
                const msg = e.message || '删除失败';
                if (msg.includes('OPTIMISTIC_LOCK') || msg.includes('version') || msg.includes('刷新')) {
                    pointRulesError.value = '删除失败：数据已被其他人修改，请刷新后重试';
                } else {
                    pointRulesError.value = msg;
                }
                toast.error(pointRulesError.value);
            }
        }
    });
};

onMounted(() => { loadPointRules(); });

defineExpose({loadPointRules});
</script>

<template>
    <section class="ag-section">
        <div class="ag-section-head">
            <h2 class="ag-section-title">积分规则配置</h2>
            <span class="ag-section-subtitle">配置各行为来源的积分发放规则</span>
        </div>

        <!-- Templates bar -->
        <div class="tmpl-bar">
            <span class="tmpl-label">快速模板：</span>
            <button
                v-for="tmpl in POINT_TEMPLATES"
                :key="tmpl.name"
                type="button"
                class="ag-btn secondary small tmpl-btn"
                @click="openCreate(); applyTemplate(tmpl)"
            >{{ tmpl.name }}</button>
        </div>

        <!-- Error -->
        <div v-if="pointRulesError" class="ag-error">{{ pointRulesError }}</div>

        <!-- Toolbar -->
        <div class="tab-toolbar">
            <span class="tab-count">共 {{ pointRules.length }} 条规则</span>
            <button type="button" class="ag-btn primary" @click="openCreate">+ 新增规则</button>
        </div>

        <!-- Loading / Empty / Table -->
        <div v-if="pointRulesLoading" class="ag-table-empty">积分规则加载中...</div>
        <div v-else-if="!pointRules.length" class="ag-table-empty">暂无积分规则</div>
        <div v-else class="ag-table-wrap">
            <table class="ag-table rules-table">
                <thead>
                    <tr>
                        <th>来源类型</th>
                        <th>单次积分/比例</th>
                        <th>每日上限</th>
                        <th>状态</th>
                        <th>版本</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="rule in pointRules" :key="rule.id" :class="{'row-deleted': rule.deleted}">
                        <td>
                            <strong>{{ sourceTypeLabel(rule.sourceType) }}</strong>
                            <small>{{ rule.sourceType }}</small>
                        </td>
                        <td class="delta plus">+{{ rule.pointAmount }}</td>
                        <td>{{ rule.dailyLimit || '不限' }}</td>
                        <td>
                            <span :class="['status-chip', rule.deleted ? 'failed' : (rule.enabled ? 'settled' : 'pending')]">
                                {{ rule.statusLabel || (rule.enabled ? '启用' : '停用') }}
                            </span>
                        </td>
                        <td>v{{ rule.version }}</td>
                        <td class="action-cell">
                            <template v-if="rule.deleted">
                                <button type="button" class="ag-btn secondary small" @click="restorePointRule(rule)">恢复</button>
                            </template>
                            <template v-else>
                                <button type="button" class="ag-btn secondary small" @click="openEdit(rule)">编辑</button>
                                <button type="button" class="ag-btn danger small" @click="deletePointRule(rule)">删除</button>
                            </template>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- ── Drawer ── -->
        <ADrawer v-model:visible="drawerVisible" :title="drawerTitle" width="440px">
            <div class="ag-form drawer-form">
                <div class="form-row">
                    <label class="form-label">来源类型</label>
                    <AdminSelect v-model="pointRuleForm.sourceType" :options="SOURCE_TYPE_OPTIONS" />
                </div>
                <div class="form-row">
                    <label class="form-label">{{ pointAmountLabel }}</label>
                    <input v-model.number="pointRuleForm.pointAmount" type="number" min="0" class="ag-input" />
                </div>
                <div class="form-row">
                    <label class="form-label">每日上限</label>
                    <input v-model.number="pointRuleForm.dailyLimit" type="number" min="0" class="ag-input" />
                </div>
                <label class="ag-toggle">
                    <input v-model="pointRuleForm.enabled" type="checkbox" />
                    <span>启用规则</span>
                </label>
                <div class="form-row">
                    <label class="form-label">变更原因</label>
                    <input v-model="pointRuleForm.reason" type="text" maxlength="500" class="ag-input" />
                </div>

                <!-- Impact Preview -->
                <div class="impact-preview">
                    <span class="impact-label">规则预览</span>
                    <p class="impact-text">{{ impactPreviewText }}</p>
                    <p class="impact-hint">保存后立即生效。如果存在已删除的同类型规则，将自动恢复为新规则。</p>
                </div>

                <div v-if="pointRulesError" class="ag-error">{{ pointRulesError }}</div>
            </div>

            <template #footer>
                <button type="button" class="ag-btn secondary" @click="drawerVisible = false">取消</button>
                <button type="button" class="ag-btn primary" :disabled="pointRuleSaving" @click="savePointRule">
                    {{ pointRuleSaving ? '保存中...' : (drawerMode === 'edit' ? '保存修改' : '创建规则') }}
                </button>
            </template>
        </ADrawer>
        <ConfirmDialog
            :visible="confirmDialog.visible"
            :eyebrow="confirmDialog.eyebrow"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :confirm-text="confirmDialog.confirmText"
            :cancel-text="confirmDialog.cancelText"
            :tone="confirmDialog.tone"
            :loading="confirmDialog.loading"
            @close="closeConfirmDialog"
            @confirm="executeConfirmDialog"
        />
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
