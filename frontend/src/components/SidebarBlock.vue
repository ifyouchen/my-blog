<script setup>
const props = defineProps({
    eyebrow: {
        type: String,
        default: ''
    },
    title: {
        type: String,
        required: true
    },
    compact: {
        type: Boolean,
        default: false
    }
});
</script>

<template>
    <section class="sidebar-block" :class="{ compact }">
        <header class="sidebar-block-head">
            <div class="sidebar-block-copy">
                <p v-if="props.eyebrow" class="eyebrow">{{ props.eyebrow }}</p>
                <h2>{{ props.title }}</h2>
            </div>
            <div v-if="$slots.action" class="sidebar-block-action">
                <slot name="action"></slot>
            </div>
        </header>
        <div class="sidebar-block-body">
            <slot></slot>
        </div>
    </section>
</template>

<style scoped src="@/styles/components/SidebarBlock.css"></style>
