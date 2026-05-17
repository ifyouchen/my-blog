<script setup>
import {computed} from 'vue';

const props = defineProps({
    privilegeCodes: {
        type: Array,
        default: () => []
    },
    compact: {
        type: Boolean,
        default: false
    }
});

const hasExclusiveBadge = computed(() =>
    Array.isArray(props.privilegeCodes) && props.privilegeCodes.includes('EXCLUSIVE_BADGE'));
</script>

<template>
    <span
        v-if="hasExclusiveBadge"
        :class="['user-privilege-badge', { compact }]"
        title="专属徽章"
        aria-label="专属徽章"
    >
        徽章
    </span>
</template>

<style scoped>
.user-privilege-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 18px;
    padding: 0 6px;
    color: #7c2d12;
    font-size: 11px;
    font-weight: 700;
    line-height: 1;
    background: linear-gradient(135deg, #fde68a, #f59e0b);
    border: 1px solid rgba(245, 158, 11, 0.38);
    border-radius: 999px;
    box-shadow: 0 4px 10px rgba(245, 158, 11, 0.18);
    flex: 0 0 auto;
}

.user-privilege-badge.compact {
    height: 16px;
    padding: 0 5px;
    font-size: 10px;
}
</style>
