<script setup>
import { onMounted, provide } from 'vue';
import { RouterView } from 'vue-router';
import LoginModal from '@/components/LoginModal.vue';
import Toast from '@/components/Toast.vue';
import { useLoginModal } from '@/composables/useLoginModal';
import { useToast } from '@/composables/useToast';
import { useSession } from '@/stores/session';

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

onMounted(() => {
    initializeSession();
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
