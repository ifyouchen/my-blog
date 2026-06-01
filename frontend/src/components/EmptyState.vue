<script setup>
const props = defineProps({
    eyebrow: {
        type: String,
        default: ''
    },
    title: {
        type: String,
        default: '暂无内容'
    },
    description: {
        type: String,
        default: ''
    },
    tone: {
        type: String,
        default: 'default'
    },
    compact: {
        type: Boolean,
        default: false
    }
});
</script>

<template>
    <section class="empty-state-shell" :class="[tone, { compact }]">
        <div class="empty-state-mark" aria-hidden="true"></div>
        <div class="empty-state-copy">
            <p v-if="props.eyebrow" class="eyebrow">{{ props.eyebrow }}</p>
            <h3>{{ props.title }}</h3>
            <p v-if="props.description">{{ props.description }}</p>
            <slot></slot>
        </div>
    </section>
</template>

<style scoped src="@/styles/components/EmptyState.css"></style>
