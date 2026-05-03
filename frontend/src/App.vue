<script setup>
import { onMounted, onUnmounted, provide } from 'vue';
import { RouterView, useRouter } from 'vue-router';
import LoginModal from '@/components/LoginModal.vue';
import Toast from '@/components/Toast.vue';
import { useLoginModal } from '@/composables/useLoginModal';
import { useToast } from '@/composables/useToast';
import { useSession } from '@/stores/session';

const router = useRouter();
const loginModal = useLoginModal();
const toast = useToast();
const { initializeSession } = useSession();
const {
    loginModalVisible,
    loginPrompt,
    hideLoginModal,
    onLoginSuccess
} = loginModal;

provide('loginModal', loginModal);
provide('toast', toast);

const handleAuthRequired = (event) => {
    const { message } = event.detail || {};
    loginModal.showLoginModal(() => {
        router.replace(router.currentRoute.value.fullPath);
    }, {
        title: '登录后继续操作',
        message: message || '登录后可以继续操作。',
        actionText: '登录并继续'
    });
};

const handleForbidden = (event) => {
    const { message } = event.detail || {};
    toast.error(message || '无权限访问');
    if (router.currentRoute.value.path.startsWith('/admin')) {
        router.replace({ path: '/403', query: { from: router.currentRoute.value.fullPath } });
    }
};

onMounted(() => {
    initializeSession();
    window.addEventListener('my-blog-auth-required', handleAuthRequired);
    window.addEventListener('my-blog-forbidden', handleForbidden);
});

onUnmounted(() => {
    window.removeEventListener('my-blog-auth-required', handleAuthRequired);
    window.removeEventListener('my-blog-forbidden', handleForbidden);
});
</script>

<template>
    <RouterView />
    <LoginModal
        :visible="loginModalVisible"
        :prompt="loginPrompt"
        @close="hideLoginModal"
        @success="onLoginSuccess"
    />
    <Toast />
</template>
