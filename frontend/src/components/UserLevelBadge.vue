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
</script>

<template>
    <span class="user-level-badge" :class="{ compact }" :aria-label="`等级 LV${displayLevel}`">
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
    background: linear-gradient(135deg, #22c55e 0%, #2563eb 100%);
    border: 1px solid rgba(255, 255, 255, 0.36);
    border-radius: 4px;
    box-shadow: 0 5px 12px rgba(37, 99, 235, 0.18);
    flex: 0 0 auto;
}

.user-level-badge.compact {
    height: 16px;
    min-width: 30px;
    padding: 0 5px;
    font-size: 10px;
}
</style>
