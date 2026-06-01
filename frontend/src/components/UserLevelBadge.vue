<script setup>
import {computed} from 'vue';

const props = defineProps({
    level: {
        type: [Number, String],
        default: 1
    },
    compact: {
        type: Boolean,
        default: false
    }
});

const displayLevel = computed(() => {
    const value = Number(props.level);
    return Number.isFinite(value) && value > 0 ? Math.floor(value) : 1;
});

const levelTone = computed(() => Math.min(displayLevel.value, 10));
const isElite = computed(() => displayLevel.value >= 6);
const isLegendary = computed(() => displayLevel.value >= 10);
const badgeTitle = computed(() => {
    if (displayLevel.value >= 10) return '荣耀创作者';
    if (displayLevel.value >= 9) return '技术领袖';
    if (displayLevel.value >= 8) return '领域导师';
    if (displayLevel.value >= 7) return '资深专家';
    if (displayLevel.value >= 6) return '大师作者';
    return `LV${displayLevel.value}`;
});
</script>

<template>
    <span
        class="user-level-badge"
        :class="[`level-tone-${levelTone}`, { compact, elite: isElite, legendary: isLegendary }]"
        :aria-label="`等级 LV${displayLevel} ${badgeTitle}`"
        :title="`LV${displayLevel} ${badgeTitle}`"
    >
        <span v-if="isElite" class="badge-mark" aria-hidden="true"></span>
        LV{{ displayLevel }}
    </span>
</template>

<style scoped src="@/styles/components/UserLevelBadge.css"></style>
