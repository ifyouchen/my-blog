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

const levelTone = computed(() => Math.min(displayLevel.value, 6));
</script>

<template>
    <span
        class="user-level-badge"
        :class="[`level-tone-${levelTone}`, { compact }]"
        :aria-label="`等级 LV${displayLevel}`"
    >
        LV{{ displayLevel }}
    </span>
</template>

<style scoped>
.user-level-badge {
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
    border: 1px solid rgba(255, 255, 255, 0.36);
    border-radius: 4px;
    box-shadow: 0 5px 12px var(--level-shadow);
    flex: 0 0 auto;
}

.user-level-badge.level-tone-1 {
    --level-from: #9fc6a4;
    --level-to: #5aaf6b;
    --level-shadow: rgba(90, 175, 107, 0.2);
}

.user-level-badge.level-tone-2 {
    --level-from: #6ed3c4;
    --level-to: #28a9c7;
    --level-shadow: rgba(40, 169, 199, 0.22);
}

.user-level-badge.level-tone-3 {
    --level-from: #66c7f4;
    --level-to: #4379e7;
    --level-shadow: rgba(67, 121, 231, 0.22);
}

.user-level-badge.level-tone-4 {
    --level-from: #b997ff;
    --level-to: #7a5bd9;
    --level-shadow: rgba(122, 91, 217, 0.24);
}

.user-level-badge.level-tone-5 {
    --level-from: #ffc35a;
    --level-to: #f0832e;
    --level-shadow: rgba(240, 131, 46, 0.24);
}

.user-level-badge.level-tone-6 {
    --level-from: #ff7aa5;
    --level-to: #f04d6d;
    --level-shadow: rgba(240, 77, 109, 0.26);
}

.user-level-badge.compact {
    height: 16px;
    min-width: 30px;
    padding: 0 5px;
    font-size: 10px;
}
</style>
