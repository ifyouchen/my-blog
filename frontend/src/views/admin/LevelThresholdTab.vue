<script setup>
import {onMounted, ref} from 'vue';
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

const RECOMMENDED_THRESHOLDS = [
    {level: 1, minExp: 0, levelName: '新手', description: '刚入门的博客读者'},
    {level: 2, minExp: 100, levelName: '学徒', description: '已积累一定阅读经验'},
    {level: 3, minExp: 300, levelName: '进阶', description: '活跃参与社区互动'},
    {level: 4, minExp: 800, levelName: '达人', description: '持续输出高质量内容'},
    {level: 5, minExp: 1800, levelName: '专家', description: '社区认可的内容创作者'},
    {level: 6, minExp: 3500, levelName: '大师作者', description: '领域深度贡献者'},
    {level: 7, minExp: 7000, levelName: '资深专家', description: '平台资深内容贡献者'},
    {level: 8, minExp: 13000, levelName: '领域导师', description: '持续产出优质内容'},
    {level: 9, minExp: 22000, levelName: '技术领袖', description: '具备社区影响力'},
    {level: 10, minExp: 35000, levelName: '荣耀创作者', description: '长期杰出贡献者'},
];

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

const applyRecommendedThresholds = () => {
    if (!confirm('确定应用推荐等级曲线吗？当前页面中的等级阈值会被替换，保存后生效。')) {
        return;
    }
    const versionByLevel = new Map(levelThresholds.value.map((row) => [Number(row.level), Number(row.version || 0)]));
    levelThresholds.value = RECOMMENDED_THRESHOLDS.map((row) => ({
        ...row,
        version: versionByLevel.get(row.level) || 0,
    }));
    thresholdError.value = '';
};

const validateThresholds = () => {
    if (!levelThresholds.value.length) return '至少保留一个等级阈值';
    const rows = [...levelThresholds.value].sort((a, b) => Number(a.level) - Number(b.level));
    let lastMinExp = -1;
    const seenLevels = new Set();
    for (const row of rows) {
        const level = Number(row.level);
        if (!level || level <= 0) return '等级必须大于 0';
        if (seenLevels.has(level)) return `Lv.${level} 重复，请保持等级唯一`;
        seenLevels.add(level);
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
            <span class="ag-section-subtitle">最低经验值决定用户等级展示，推荐曲线强调长期活跃和内容认可</span>
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
            <button type="button" class="ag-btn secondary" @click="applyRecommendedThresholds">应用推荐曲线</button>
            <button type="button" class="ag-btn primary" :disabled="thresholdSaving || thresholdLoading" @click="saveLevelThresholds">
                {{ thresholdSaving ? '保存中...' : '保存等级阈值' }}
            </button>
        </div>
    </section>
</template>
