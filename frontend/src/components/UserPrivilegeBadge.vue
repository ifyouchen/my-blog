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

<style scoped src="@/styles/components/UserPrivilegeBadge.css"></style>
