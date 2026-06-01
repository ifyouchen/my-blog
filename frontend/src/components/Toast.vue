<script setup>
import { useToast } from '@/composables/useToast';

const { toasts, removeToast } = useToast();

const icons = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ'
};
</script>

<template>
    <Teleport to="body">
        <TransitionGroup name="toast" tag="div" class="toast-container">
            <div
                v-for="toast in toasts"
                :key="toast.id"
                class="toast"
                :class="toast.type"
            >
                <span class="toast-icon">{{ icons[toast.type] || icons.info }}</span>
                <span class="toast-message">{{ toast.message }}</span>
                <button
                    type="button"
                    class="toast-close"
                    aria-label="关闭"
                    @click="removeToast(toast.id)"
                >
                    ×
                </button>
            </div>
        </TransitionGroup>
    </Teleport>
</template>

<style scoped src="@/styles/components/Toast.css"></style>
