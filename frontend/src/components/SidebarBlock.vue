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
/* 蓝白渐变侧边栏区块 — 白底、细边框、适度圆角 */
.sidebar-block {
    display: grid;
    gap: 12px;
    padding: 16px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
}

.sidebar-block.compact {
    padding: 14px;
    gap: 10px;
}

.sidebar-block-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.sidebar-block-copy {
    display: grid;
    gap: 2px;
    min-width: 0;
}

.sidebar-block-copy .eyebrow,
.sidebar-block-copy h2 {
    margin: 0;
}

.sidebar-block-copy h2 {
    color: var(--text-strong);
    font-size: 15px;
    font-weight: 700;
    line-height: 1.3;
    letter-spacing: 0;
}

.compact .sidebar-block-copy h2 {
    font-size: 14px;
}

.sidebar-block-body {
    display: grid;
    gap: 10px;
}

@media (max-width: 760px) {
    .sidebar-block {
        padding: 14px;
    }

    .sidebar-block-head {
        align-items: start;
        flex-direction: column;
    }
}
</style>
