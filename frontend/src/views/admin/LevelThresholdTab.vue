<script setup>
import {onMounted, reactive, ref} from 'vue';
import {useToast} from '@/composables/useToast';
import {
    getAdminLevelThresholdsApi,
    saveAdminLevelThresholdsApi,
} from '@/api/growth';

const toast = useToast();

const levelThresholds = ref([]);
const thresholdLoading = ref(false);
const thresholdSaving = ref(false);
const thresholdError = ref('');

const normalizeThresholdRow = (row = {}, index = 0) => ({
    level: Number(row.level || index + 1), minExp: Number(row.minExp || 0),
    levelName: row.levelName || `LV${Number(row.level || index + 1)}`, description: row.description || '',
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
        ? Math.max(...levelThresholds.value.map((item) => Number(item.level || 0))) + 1 : 1;
    const prevMinExp = levelThresholds.value.length
        ? Number(levelThresholds.value[levelThresholds.value.length - 1].minExp || 0) : 0;
    levelThresholds.value.push({
        level: nextLevel, minExp: nextLevel === 1 ? 0 : prevMinExp + 100,
        levelName: `LV${nextLevel}`, description: '', version: 0,
    });
};

const removeThresholdRow = (index) => {
    if (levelThresholds.value.length <= 1) return;
    levelThresholds.value.splice(index, 1);
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
    if (error) { thresholdError.value = error; return; }
    thresholdSaving.value = true;
    thresholdError.value = '';
    try {
        const payload = [...levelThresholds.value].sort((a, b) => Number(a.level) - Number(b.level))
            .map((row) => ({
                level: Number(row.level), minExp: Number(row.minExp),
                levelName: String(row.levelName || '').trim(), description: String(row.description || '').trim(),
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

onMounted(() => { loadLevelThresholds(); });

defineExpose({loadLevelThresholds});
</script>

<template>
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
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="(row, index) in levelThresholds" :key="row.level">
                        <td><input v-model.number="row.level" type="number" min="1" class="ag-input cell-input" /></td>
                        <td><input v-model.number="row.minExp" type="number" min="0" class="ag-input cell-input" /></td>
                        <td><input v-model.trim="row.levelName" type="text" class="ag-input cell-input" /></td>
                        <td><input v-model.trim="row.description" type="text" class="ag-input cell-input wide" /></td>
                        <td>v{{ row.version || 0 }}</td>
                        <td>
                            <button type="button" class="ag-btn danger small" @click="removeThresholdRow(index)" :disabled="levelThresholds.length <= 1">删除</button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="ag-action-row threshold-actions">
            <button type="button" class="ag-btn secondary" @click="addThresholdRow">新增等级</button>
            <button type="button" class="ag-btn primary" :disabled="thresholdSaving || thresholdLoading" @click="saveLevelThresholds">
                {{ thresholdSaving ? '保存中...' : '保存等级阈值' }}
            </button>
        </div>
    </section>
</template>
