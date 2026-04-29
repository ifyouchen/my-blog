<script setup>
import { inject, ref, watch } from 'vue';
import { subscribeColumnApi, unsubscribeColumnApi } from '@/api/columns';

const props = defineProps({
    columnId: {
        type: [Number, String],
        required: true
    },
    subscribed: {
        type: Boolean,
        default: false
    },
    compact: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['change']);

const loginModal = inject('loginModal', { requireLogin: () => false });
const toast = inject('toast', { error: () => {} });
const submitting = ref(false);
const localSubscribed = ref(Boolean(props.subscribed));

watch(() => props.subscribed, (value) => {
    localSubscribed.value = Boolean(value);
});

const toggleSubscribe = async () => {
    if (submitting.value) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => toggleSubscribe(), {
        title: localSubscribed.value ? '登录后管理专栏订阅' : '登录后订阅专栏',
        message: '订阅后可以持续追踪这个主题下的新文章。',
        actionText: localSubscribed.value ? '登录并管理订阅' : '登录并订阅'
    });
    if (!canContinue) {
        return;
    }
    submitting.value = true;
    const previous = localSubscribed.value;
    localSubscribed.value = !previous;
    try {
        if (previous) {
            await unsubscribeColumnApi(props.columnId);
        } else {
            await subscribeColumnApi(props.columnId);
        }
        emit('change', localSubscribed.value);
    } catch (error) {
        localSubscribed.value = previous;
        toast.error(error.message || '操作失败');
        emit('change', previous, error);
    } finally {
        submitting.value = false;
    }
};
</script>

<template>
    <button
        type="button"
        class="column-subscribe-button"
        :class="{ compact, active: localSubscribed }"
        :disabled="submitting"
        data-testid="column-subscribe-button"
        @click="toggleSubscribe"
    >
        {{ localSubscribed ? '已订阅' : '订阅专栏' }}
    </button>
</template>

<style scoped>
.column-subscribe-button {
    min-height: 36px;
    padding: 0 16px;
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid rgba(37, 99, 235, 0.24);
    border-radius: var(--radius-md);
    transition: color 0.14s ease, background 0.14s ease, border-color 0.14s ease, opacity 0.14s ease;
}

.column-subscribe-button:hover:not(:disabled) {
    color: var(--brand-strong);
    background: var(--brand-soft);
    border-color: rgba(37, 99, 235, 0.34);
}

.column-subscribe-button.active {
    color: var(--muted);
    background: var(--surface-soft);
    border-color: var(--line);
}

.column-subscribe-button.active:hover:not(:disabled) {
    color: var(--brand);
    background: var(--brand-soft);
    border-color: rgba(37, 99, 235, 0.24);
}

.column-subscribe-button.compact {
    min-height: 32px;
    padding: 0 14px;
}

.column-subscribe-button:disabled {
    cursor: not-allowed;
    opacity: 0.62;
}
</style>
