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
    gap: 16px;
    padding: 42px 28px;
    text-align: center;
    background:
        radial-gradient(circle at top, rgba(40, 118, 255, 0.08), transparent 40%),
        linear-gradient(180deg, rgba(248, 251, 255, 0.96), #ffffff);
    border: 1px solid rgba(208, 219, 236, 0.92);
    border-radius: 24px;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.empty-state-shell.compact {
    padding: 30px 22px;
    gap: 12px;
    border-radius: 20px;
}

.empty-state-shell.error {
    background:
        radial-gradient(circle at top, rgba(248, 113, 113, 0.08), transparent 40%),
        linear-gradient(180deg, rgba(255, 251, 251, 0.98), #ffffff);
    border-color: rgba(248, 113, 113, 0.22);
}

.empty-state-mark {
    width: 52px;
    height: 52px;
    border-radius: 18px;
    background:
        linear-gradient(135deg, rgba(37, 99, 235, 0.16), rgba(59, 130, 246, 0.08));
    box-shadow: 0 16px 30px rgba(31, 78, 168, 0.08);
}

.empty-state-shell.error .empty-state-mark {
    background:
        linear-gradient(135deg, rgba(248, 113, 113, 0.16), rgba(252, 165, 165, 0.08));
    box-shadow: 0 16px 30px rgba(185, 28, 28, 0.08);
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
    font-size: 24px;
    line-height: 1.2;
}

.empty-state-copy p {
    color: var(--muted);
    line-height: 1.75;
}

.compact .empty-state-mark {
    width: 44px;
    height: 44px;
    border-radius: 16px;
}

.compact .empty-state-copy h3 {
    font-size: 20px;
}
</style>
