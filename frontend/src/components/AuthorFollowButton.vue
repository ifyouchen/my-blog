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

<style scoped src="@/styles/components/AuthorFollowButton.css"></style>
