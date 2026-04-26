<script setup>
import { inject, ref, watch } from 'vue';
import { followUserApi, unfollowUserApi } from '@/api/following';

const props = defineProps({
    userId: {
        type: Number,
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
    min-height: 34px;
    padding: 0 14px;
    color: var(--brand-strong);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    background: rgba(31, 122, 224, 0.08);
    border: 1px solid rgba(31, 122, 224, 0.18);
    border-radius: 8px;
    transition: background-color 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.author-follow-button:hover:not(:disabled) {
    background: rgba(31, 122, 224, 0.14);
    border-color: rgba(31, 122, 224, 0.28);
    transform: translateY(-1px);
}

.author-follow-button.active {
    color: var(--brand-strong);
    background: rgba(31, 122, 224, 0.1);
    border-color: rgba(31, 122, 224, 0.2);
}

.author-follow-button.compact {
    min-height: 30px;
    padding: 0 12px;
    font-size: 12px;
}

.author-follow-button.detail {
    min-width: 94px;
    min-height: 40px;
    padding: 0 16px;
    font-size: 14px;
    border-radius: 999px;
}

.author-follow-button:disabled {
    cursor: not-allowed;
    opacity: 0.72;
}
</style>
