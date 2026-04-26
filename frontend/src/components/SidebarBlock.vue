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

<style scoped>
.sidebar-block {
    display: grid;
    gap: 14px;
    padding: 20px;
    background: linear-gradient(180deg, rgba(248, 251, 255, 0.98), #ffffff);
    border: 1px solid rgba(208, 219, 236, 0.92);
    border-radius: 22px;
    box-shadow: 0 18px 36px rgba(31, 78, 168, 0.06);
}

.sidebar-block.compact {
    padding: 18px;
    gap: 12px;
}

.sidebar-block-head {
    display: flex;
    align-items: end;
    justify-content: space-between;
    gap: 12px;
}

.sidebar-block-copy {
    display: grid;
    gap: 4px;
    min-width: 0;
}

.sidebar-block-copy .eyebrow,
.sidebar-block-copy h2 {
    margin: 0;
}

.sidebar-block-copy h2 {
    color: var(--text-strong);
    font-size: 28px;
    line-height: 1.1;
    letter-spacing: -0.02em;
}

.compact .sidebar-block-copy h2 {
    font-size: 24px;
}

.sidebar-block-body {
    display: grid;
    gap: 12px;
}

@media (max-width: 760px) {
    .sidebar-block {
        padding: 18px;
        border-radius: 20px;
    }

    .sidebar-block-head {
        align-items: start;
        flex-direction: column;
    }

    .sidebar-block-copy h2 {
        font-size: 24px;
    }
}
</style>
