<script setup>
import {inject, ref, watch} from 'vue';
import {followUserApi, unfollowUserApi} from '@/api/following';

const props = defineProps({
    userId: {
        type: [Number, String],
        required: true
    },
    followed: {
        type: Boolean,
        default: false
    },
    compact: {
        type: Boolean,
        default: false
    },
    detail: {
        type: Boolean,
        default: false
    },
    disabled: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['change']);

const loginModal = inject('loginModal', { requireLogin: () => false });
const toast = inject('toast', { error: () => {} });
const submitting = ref(false);
const localFollowed = ref(Boolean(props.followed));

watch(() => props.followed, (value) => {
    localFollowed.value = Boolean(value);
});

const toggleFollow = async () => {
    if (props.disabled || submitting.value) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => toggleFollow(), {
        title: localFollowed.value ? '登录后管理关注' : '登录后关注作者',
        message: '登录后可以关注喜欢的作者，首页和关注页会持续看到他们的新内容。',
        actionText: localFollowed.value ? '登录并管理关注' : '登录并关注'
    });
    if (!canContinue) {
        return;
    }
    submitting.value = true;
    const previous = localFollowed.value;
    localFollowed.value = !previous;
    try {
        if (previous) {
            await unfollowUserApi(props.userId);
        } else {
            await followUserApi(props.userId);
        }
        emit('change', localFollowed.value);
    } catch (error) {
        localFollowed.value = previous;
        toast.error(error.message || (previous ? '取消关注失败，请稍后再试' : '关注失败，请稍后再试'));
        emit('change', previous, error);
    } finally {
        submitting.value = false;
    }
};
</script>

<template>
    <button
        type="button"
        class="author-follow-button"
        :class="{ compact, detail, active: localFollowed }"
        :disabled="disabled || submitting"
        data-testid="author-follow-button"
        @click="toggleFollow"
    >
        {{ localFollowed ? '已关注' : '关注' }}
    </button>
</template>

<style scoped>
.author-follow-button {
    min-height: 32px;
    padding: 0 12px;
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--brand);
    border-radius: var(--radius-sm);
    transition: color 0.12s, background 0.12s;
}

.author-follow-button:hover:not(:disabled) {
    color: #ffffff;
    background: var(--brand);
}

.author-follow-button.active {
    color: var(--muted);
    background: var(--surface-soft);
    border-color: var(--line);
}

.author-follow-button.active:hover:not(:disabled) {
    color: var(--accent);
    border-color: var(--accent);
    background: var(--surface);
}

.author-follow-button.compact {
    min-height: 26px;
    padding: 0 10px;
    font-size: 12px;
}

.author-follow-button.detail {
    min-width: 84px;
    min-height: 34px;
    padding: 0 14px;
    font-size: 14px;
    border-radius: var(--radius-sm);
}

.author-follow-button:disabled {
    cursor: not-allowed;
    opacity: 0.6;
}
</style>
