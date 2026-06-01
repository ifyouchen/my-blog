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

<style scoped src="@/styles/components/ColumnSubscribeButton.css"></style>
