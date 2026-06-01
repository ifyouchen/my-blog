<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    createAdminLevelRewardApi,
    deleteAdminLevelRewardApi,
    getAdminLevelRewardsApi,
    updateAdminLevelRewardApi
} from '@/api/growth';
import {formatAdminDateTime} from '@/views/admin/adminShared';
import ADrawer from '@/components/ADrawer.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import {useConfirmDialog} from '@/composables/useConfirmDialog';

const toast = useToast();
const {
    confirmDialog,
    openConfirmDialog,
    closeConfirmDialog,
    executeConfirmDialog
} = useConfirmDialog();

const rewards = ref([]);
const loading = ref(false);
const error = ref('');
const saving = ref(false);
const drawerVisible = ref(false);
const drawerMode = ref('create');
const deletingId = ref(null);

const form = reactive({
    id: null,
    level: 1,
    rewardPoints: 0,
    rewardTitle: '',
    description: '',
    enabled: true,
    version: 0
});

const enabledCount = computed(() => rewards.value.filter(item => item.enabled).length);
const totalRewardPoints = computed(() =>
    rewards.value.filter(item => item.enabled).reduce((sum, item) => sum + Number(item.rewardPoints || 0), 0));
const drawerTitle = computed(() => drawerMode.value === 'create' ? '新增等级奖励' : '编辑等级奖励');

const loadRewards = async () => {
    loading.value = true;
    error.value = '';
    try {
        rewards.value = await getAdminLevelRewardsApi();
    } catch (e) {
        rewards.value = [];
        error.value = e.message || '等级奖励配置加载失败';
    } finally {
        loading.value = false;
    }
};

const openEdit = (row) => {
    drawerMode.value = 'edit';
    form.id = row.id;
    form.level = Number(row.level || 1);
    form.rewardPoints = Number(row.rewardPoints || 0);
    form.rewardTitle = row.rewardTitle || '';
    form.description = row.description || '';
    form.enabled = Boolean(row.enabled);
    form.version = Number(row.version || 0);
    error.value = '';
    drawerVisible.value = true;
};

const openCreate = () => {
    drawerMode.value = 'create';
    form.id = null;
    form.level = nextLevel();
    form.rewardPoints = 0;
    form.rewardTitle = '';
    form.description = '';
    form.enabled = true;
    form.version = 0;
    error.value = '';
    drawerVisible.value = true;
};

const nextLevel = () => {
    const maxLevel = rewards.value.reduce((max, item) => Math.max(max, Number(item.level || 0)), 0);
    return maxLevel + 1;
};

const validateForm = () => {
    if (drawerMode.value === 'edit' && !form.id) return '奖励配置 ID 不能为空';
    if (Number(form.level || 0) <= 0) return '等级必须大于 0';
    if (Number(form.rewardPoints) < 0) return '奖励积分不能小于 0';
    const duplicated = rewards.value.some(item =>
        Number(item.level) === Number(form.level) && Number(item.id) !== Number(form.id || 0));
    if (duplicated) return `Lv.${form.level} 的奖励配置已存在`;
    return '';
};

const buildPayload = () => ({
    id: form.id,
    level: Number(form.level),
    rewardPoints: Number(form.rewardPoints || 0),
    rewardTitle: form.rewardTitle.trim(),
    description: form.description.trim(),
    enabled: Boolean(form.enabled),
    version: Number(form.version || 0)
});

const saveReward = async () => {
    const validationError = validateForm();
    if (validationError) {
        error.value = validationError;
        return;
    }
    saving.value = true;
    error.value = '';
    try {
        const payload = buildPayload();
        if (drawerMode.value === 'create') {
            await createAdminLevelRewardApi(payload);
            toast.success(`Lv.${form.level} 奖励配置已新增`);
        } else {
            await updateAdminLevelRewardApi(payload);
            toast.success(`Lv.${form.level} 奖励配置已更新`);
        }
        drawerVisible.value = false;
        await loadRewards();
    } catch (e) {
        error.value = e.message || '等级奖励配置保存失败';
        toast.error(error.value);
    } finally {
        saving.value = false;
    }
};

const doDeleteReward = async (row) => {
    deletingId.value = row.id;
    error.value = '';
    try {
        await deleteAdminLevelRewardApi(row.id, row.version);
        toast.success(`Lv.${row.level} 奖励配置已删除`);
        await loadRewards();
    } catch (e) {
        error.value = e.message || '等级奖励配置删除失败';
        toast.error(error.value);
    } finally {
        deletingId.value = null;
    }
};

const deleteReward = (row) => {
    openConfirmDialog({
        eyebrow: '删除确认',
        title: `删除 Lv.${row.level} 等级奖励`,
        message: '该操作会停用并隐藏配置，不影响历史领取记录。',
        confirmText: '删除',
        tone: 'danger',
        onConfirm: () => doDeleteReward(row)
    });
};

onMounted(loadRewards);
</script>

<template>
    <section class="ag-section">
        <div class="ag-section-head">
            <h2 class="ag-section-title">等级奖励配置</h2>
            <span class="ag-section-subtitle">管理升级到各等级时的奖励积分与文案</span>
        </div>

        <div class="summary-grid">
            <div class="summary-card">
                <span class="summary-label">配置总数</span>
                <strong class="summary-value">{{ rewards.length }}</strong>
            </div>
            <div class="summary-card">
                <span class="summary-label">当前启用</span>
                <strong class="summary-value">{{ enabledCount }}</strong>
            </div>
            <div class="summary-card">
                <span class="summary-label">启用奖励总积分</span>
                <strong class="summary-value accent">{{ totalRewardPoints }}</strong>
            </div>
        </div>

        <p class="ag-hint">
            等级奖励会在经验入账后触发等级升级检查时发放。这里只管理奖励配置，不会补发历史未领取记录。
        </p>
        <div class="tab-toolbar">
            <div class="tab-count-group">
                <span class="tab-count">按等级升序展示</span>
                <span class="tab-count-muted">删除只隐藏配置，历史领取记录保留</span>
            </div>
            <button type="button" class="ag-btn primary" @click="openCreate">+ 新增等级奖励</button>
        </div>
        <div v-if="error" class="ag-error">{{ error }}</div>
        <div v-if="loading" class="ag-table-empty">等级奖励加载中...</div>
        <div v-else-if="!rewards.length" class="ag-table-empty">暂无等级奖励配置</div>
        <div v-else class="ag-table-wrap">
            <table class="ag-table reward-table">
                <thead>
                    <tr>
                        <th>等级</th>
                        <th>奖励积分</th>
                        <th>奖励名称</th>
                        <th>说明</th>
                        <th>状态</th>
                        <th>更新时间</th>
                        <th>版本</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="row in rewards" :key="row.id">
                        <td><strong>Lv.{{ row.level }}</strong></td>
                        <td :class="['delta', Number(row.rewardPoints || 0) > 0 ? 'plus' : '']">
                            {{ Number(row.rewardPoints || 0) > 0 ? '+' : '' }}{{ row.rewardPoints }}
                        </td>
                        <td>{{ row.rewardTitle || '-' }}</td>
                        <td class="desc-cell">{{ row.description || '-' }}</td>
                        <td>
                            <span :class="['status-chip', row.enabled ? 'settled' : 'failed']">
                                {{ row.enabled ? '启用' : '停用' }}
                            </span>
                        </td>
                        <td class="time-cell"><span>{{ formatAdminDateTime(row.updatedAt) }}</span></td>
                        <td>v{{ row.version }}</td>
                        <td class="action-cell">
                            <button type="button" class="ag-btn secondary small" @click="openEdit(row)">编辑</button>
                            <button
                                type="button"
                                class="ag-btn danger small"
                                :disabled="deletingId === row.id"
                                @click="deleteReward(row)"
                            >
                                {{ deletingId === row.id ? '删除中...' : '删除' }}
                            </button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <ADrawer v-model:visible="drawerVisible" :title="drawerTitle" width="520px">
            <div class="editor-stack">
                <section class="editor-panel">
                    <div class="editor-panel-head">
                        <span class="editor-panel-kicker">等级</span>
                        <strong>升级到这个等级时发什么奖励</strong>
                    </div>
                    <div class="metric-grid">
                        <label class="metric-field">
                            <span>等级</span>
                            <input v-model.number="form.level" type="number" min="1" class="ag-input" />
                        </label>
                        <label class="metric-field">
                            <span>奖励积分</span>
                            <input v-model.number="form.rewardPoints" type="number" min="0" class="ag-input" />
                        </label>
                    </div>
                </section>

                <section class="editor-panel">
                    <div class="form-row">
                        <label class="form-label">奖励名称</label>
                        <input v-model="form.rewardTitle" type="text" maxlength="64" class="ag-input" />
                    </div>
                    <div class="form-row">
                        <label class="form-label">奖励说明</label>
                        <textarea v-model="form.description" rows="4" maxlength="255" class="ag-textarea" />
                    </div>
                    <label class="ag-toggle">
                        <input v-model="form.enabled" type="checkbox" />
                        <span>启用这条等级奖励</span>
                    </label>
                </section>

                <div class="preview-card">
                    <span class="preview-kicker">发放预览</span>
                    <p class="preview-text">用户升级到 Lv.{{ form.level }} 时，将获得 {{ form.rewardPoints }} 积分。</p>
                </div>

                <div v-if="error" class="ag-error">{{ error }}</div>
            </div>

            <template #footer>
                <button type="button" class="ag-btn secondary" @click="drawerVisible = false">取消</button>
                <button type="button" class="ag-btn primary" :disabled="saving" @click="saveReward">
                    {{ saving ? '保存中...' : (drawerMode === 'create' ? '新增奖励' : '保存修改') }}
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

<style scoped src="@/styles/views/admin/LevelRewardTab.css"></style>
