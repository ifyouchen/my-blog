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
    disabled: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['change']);

const loginModal = inject('loginModal', { requireLogin: () => false });
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
        :class="{ compact, active: localFollowed }"
        :disabled="disabled || submitting"
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
    background: rgba(17, 153, 132, 0.08);
    border: 1px solid rgba(17, 153, 132, 0.18);
    border-radius: 8px;
    transition: background-color 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.author-follow-button:hover:not(:disabled) {
    background: rgba(17, 153, 132, 0.14);
    border-color: rgba(17, 153, 132, 0.28);
    transform: translateY(-1px);
}

.author-follow-button.active {
    color: var(--muted);
    background: #f8fbfa;
    border-color: rgba(219, 227, 223, 0.92);
}

.author-follow-button.compact {
    min-height: 30px;
    padding: 0 12px;
    font-size: 12px;
}

.author-follow-button:disabled {
    cursor: not-allowed;
    opacity: 0.72;
}
</style>
