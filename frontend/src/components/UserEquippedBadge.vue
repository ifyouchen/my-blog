<script setup>
import {computed} from 'vue';

const props = defineProps({
    badge: {
        type: Object,
        default: null
    },
    compact: {
        type: Boolean,
        default: false
    }
});

const visible = computed(() => Boolean(props.badge?.code));
const toneClass = computed(() => `tone-${props.badge?.tone || 'slate'}`);
const title = computed(() => props.badge?.name || '');
const iconText = computed(() => {
    const code = String(props.badge?.code || '');
    if (code.startsWith('LEVEL_')) return 'L';
    if (code.startsWith('SIGN_')) return 'S';
    if (code.includes('ANNUAL')) return 'A';
    return 'B';
});
</script>

<template>
    <span
        v-if="visible"
        class="user-equipped-badge"
        :class="[toneClass, { compact }]"
        :title="title"
        :aria-label="`佩戴徽章：${title}`"
    >
        <span class="badge-gem" aria-hidden="true">{{ iconText }}</span>
        <span class="badge-name">{{ title }}</span>
    </span>
</template>

<style scoped src="@/styles/components/UserEquippedBadge.css"></style>
