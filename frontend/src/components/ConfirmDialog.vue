<script setup>
const props = defineProps({
    visible: {
        type: Boolean,
        default: false
    },
    eyebrow: {
        type: String,
        default: '操作确认'
    },
    title: {
        type: String,
        default: ''
    },
    message: {
        type: String,
        default: ''
    },
    confirmText: {
        type: String,
        default: '确认'
    },
    cancelText: {
        type: String,
        default: '取消'
    },
    tone: {
        type: String,
        default: 'primary'
    },
    loading: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['close', 'confirm']);

const close = () => {
    if (props.loading) {
        return;
    }
    emit('close');
};

const confirm = () => {
    if (props.loading) {
        return;
    }
    emit('confirm');
};
</script>

<template>
    <Teleport to="body">
        <div
            v-if="visible"
            class="confirm-dialog-overlay"
            @click.self="close"
        >
            <div class="confirm-dialog-card">
                <button
                    type="button"
                    class="confirm-dialog-close"
                    aria-label="关闭确认弹窗"
                    :disabled="loading"
                    @click="close"
                >
                    ×
                </button>
                <p class="confirm-dialog-eyebrow">{{ eyebrow }}</p>
                <h3 v-if="title">{{ title }}</h3>
                <p v-if="message" class="confirm-dialog-message">{{ message }}</p>
                <slot />
                <div class="confirm-dialog-actions">
                    <button
                        type="button"
                        class="confirm-dialog-btn secondary"
                        :disabled="loading"
                        @click="close"
                    >
                        {{ cancelText }}
                    </button>
                    <button
                        type="button"
                        class="confirm-dialog-btn"
                        :class="tone"
                        :disabled="loading"
                        @click="confirm"
                    >
                        {{ loading ? '处理中...' : confirmText }}
                    </button>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<style scoped src="@/styles/components/ConfirmDialog.css"></style>
