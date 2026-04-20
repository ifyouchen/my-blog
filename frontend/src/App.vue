<script setup>
import { onMounted, provide } from 'vue';
import { RouterView } from 'vue-router';
import LoginModal from '@/components/LoginModal.vue';
import { useLoginModal } from '@/composables/useLoginModal';
import { useSession } from '@/stores/session';

const loginModal = useLoginModal();
const { initializeSession } = useSession();
const {
    loginModalVisible,
    loginPrompt,
    hideLoginModal,
    onLoginSuccess
} = loginModal;

provide('loginModal', loginModal);

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
</template>
