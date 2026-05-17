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

<style scoped>
.user-level-badge {
    position: relative;
    overflow: hidden;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 18px;
    min-width: 34px;
    padding: 0 6px;
    color: #ffffff;
    font-size: 11px;
    font-weight: 800;
    line-height: 1;
    letter-spacing: 0;
    text-shadow: 0 1px 1px rgba(14, 22, 36, 0.24);
    background: linear-gradient(135deg, var(--level-from), var(--level-to));
    border: 1px solid var(--level-border, rgba(255, 255, 255, 0.36));
    border-radius: 4px;
    box-shadow: 0 5px 12px var(--level-shadow);
    flex: 0 0 auto;
}

.user-level-badge::after {
    content: "";
    position: absolute;
    inset: 0;
    background: linear-gradient(115deg, transparent 0%, rgba(255, 255, 255, 0.28) 48%, transparent 64%);
    opacity: 0;
    transform: translateX(-120%);
    pointer-events: none;
}

.user-level-badge.elite {
    min-width: 42px;
    padding: 0 7px 0 6px;
    box-shadow: 0 5px 14px var(--level-shadow), inset 0 0 0 1px rgba(255, 255, 255, 0.18);
}

.user-level-badge.elite::after {
    opacity: 1;
    transform: translateX(118%);
    transition: transform 1.2s ease;
}

.badge-mark {
    position: relative;
    width: 7px;
    height: 7px;
    margin-right: 3px;
    border-radius: 2px;
    background: var(--level-mark);
    box-shadow: 0 0 8px var(--level-glow);
    transform: rotate(45deg);
    flex: 0 0 auto;
}

.user-level-badge.level-tone-1 {
    --level-from: #9fc6a4;
    --level-to: #5aaf6b;
    --level-shadow: rgba(90, 175, 107, 0.2);
    --level-mark: #ffffff;
    --level-glow: rgba(255, 255, 255, 0.42);
}

.user-level-badge.level-tone-2 {
    --level-from: #6ed3c4;
    --level-to: #28a9c7;
    --level-shadow: rgba(40, 169, 199, 0.22);
    --level-mark: #ffffff;
    --level-glow: rgba(255, 255, 255, 0.42);
}

.user-level-badge.level-tone-3 {
    --level-from: #66c7f4;
    --level-to: #4379e7;
    --level-shadow: rgba(67, 121, 231, 0.22);
    --level-mark: #ffffff;
    --level-glow: rgba(255, 255, 255, 0.42);
}

.user-level-badge.level-tone-4 {
    --level-from: #b997ff;
    --level-to: #7a5bd9;
    --level-shadow: rgba(122, 91, 217, 0.24);
    --level-mark: #ffffff;
    --level-glow: rgba(255, 255, 255, 0.42);
}

.user-level-badge.level-tone-5 {
    --level-from: #ffb562;
    --level-to: #ec6a2c;
    --level-shadow: rgba(236, 106, 44, 0.24);
    --level-mark: #ffffff;
    --level-glow: rgba(255, 255, 255, 0.42);
}

.user-level-badge.level-tone-6 {
    --level-from: #ffe08a;
    --level-to: #d99b22;
    --level-border: rgba(255, 232, 154, 0.74);
    --level-mark: #fff8d7;
    --level-glow: rgba(251, 191, 36, 0.72);
    --level-shadow: rgba(217, 155, 34, 0.3);
}

.user-level-badge.level-tone-7 {
    --level-from: #f2d18b;
    --level-to: #8854d0;
    --level-border: rgba(233, 213, 255, 0.72);
    --level-mark: #fde68a;
    --level-glow: rgba(168, 85, 247, 0.58);
    --level-shadow: rgba(136, 84, 208, 0.3);
}

.user-level-badge.level-tone-8 {
    --level-from: #8bd3ff;
    --level-to: #6d5dfc;
    --level-border: rgba(191, 219, 254, 0.74);
    --level-mark: #dbeafe;
    --level-glow: rgba(96, 165, 250, 0.62);
    --level-shadow: rgba(109, 93, 252, 0.3);
}

.user-level-badge.level-tone-9 {
    --level-from: #2a2f3d;
    --level-to: #c18a2b;
    --level-border: rgba(250, 204, 21, 0.7);
    --level-mark: #facc15;
    --level-glow: rgba(250, 204, 21, 0.64);
    --level-shadow: rgba(24, 24, 27, 0.34);
}

.user-level-badge.level-tone-10 {
    --level-from: #fb7185;
    --level-to: #b45309;
    --level-border: rgba(254, 215, 170, 0.82);
    --level-mark: #fff7ed;
    --level-glow: rgba(251, 113, 133, 0.7);
    --level-shadow: rgba(180, 83, 9, 0.34);
    text-shadow: 0 1px 1px rgba(92, 22, 22, 0.34);
}

.user-level-badge.legendary {
    box-shadow:
        0 6px 16px var(--level-shadow),
        0 0 0 1px rgba(254, 215, 170, 0.34),
        inset 0 0 0 1px rgba(255, 255, 255, 0.2);
}

.user-level-badge.compact {
    height: 16px;
    min-width: 30px;
    padding: 0 5px;
    font-size: 10px;
}

.user-level-badge.compact.elite {
    min-width: 37px;
    padding: 0 6px 0 5px;
}

.user-level-badge.compact .badge-mark {
    width: 6px;
    height: 6px;
    margin-right: 2px;
}
</style>
