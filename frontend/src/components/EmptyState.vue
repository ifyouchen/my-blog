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

<style scoped>
.empty-state-shell {
    display: grid;
    justify-items: center;
    gap: 14px;
    padding: 36px 24px;
    text-align: center;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
}

.empty-state-shell.compact {
    padding: 24px 20px;
    gap: 10px;
}

.empty-state-shell.error {
    background: #fff8f8;
    border-color: rgba(199, 50, 50, 0.2);
}

.empty-state-mark {
    width: 40px;
    height: 40px;
    border-radius: var(--radius-sm);
    background: var(--surface-muted);
    border: 1px solid var(--line);
    box-shadow: none;
}

.empty-state-shell.error .empty-state-mark {
    background: #ffe8e8;
    border-color: rgba(199, 50, 50, 0.2);
    box-shadow: none;
}

.empty-state-copy {
    display: grid;
    gap: 8px;
    max-width: 540px;
}

.empty-state-copy .eyebrow {
    margin: 0;
}

.empty-state-copy h3,
.empty-state-copy p {
    margin: 0;
}

.empty-state-copy h3 {
    color: var(--text-strong);
    font-size: 18px;
    font-weight: 700;
    line-height: 1.3;
}

.empty-state-copy p {
    color: var(--muted);
    line-height: 1.75;
}

.compact .empty-state-mark {
    width: 36px;
    height: 36px;
    border-radius: var(--radius-sm);
}

.compact .empty-state-copy h3 {
    font-size: 16px;
}
</style>
