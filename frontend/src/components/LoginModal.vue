<script setup>
import {ref} from 'vue';
import {useRouter} from 'vue-router';
import {useSession} from '@/stores/session';

const props = defineProps({
    visible: {
        type: Boolean,
        default: false
    },
    prompt: {
        type: Object,
        default: () => ({
            title: '继续前请先登录',
            message: '登录后可以同步你的操作记录，并继续刚才的动作。',
            actionText: '登录并继续'
        })
    }
});

const emit = defineEmits(['close', 'success']);

const router = useRouter();
const { login } = useSession();

const account = ref('');
const password = ref('');
const error = ref('');
const loading = ref(false);
let overlayPointerDownOnSelf = false;

const close = () => {
    account.value = '';
    password.value = '';
    error.value = '';
    emit('close');
};

const onOverlayPointerDown = (event) => {
    overlayPointerDownOnSelf = event.target === event.currentTarget;
};

const resetOverlayPointer = () => {
    overlayPointerDownOnSelf = false;
};

const onOverlayPointerUp = (event) => {
    const shouldClose = overlayPointerDownOnSelf && event.target === event.currentTarget;
    resetOverlayPointer();
    if (shouldClose) {
        close();
    }
};

const handleLogin = async () => {
    if (!account.value || !password.value) {
        error.value = '请输入账号和密码';
        return;
    }
    loading.value = true;
    error.value = '';
    try {
        await login({ account: account.value, password: password.value });
        emit('success');
        close();
    } catch (e) {
        error.value = e.message || '登录失败';
    } finally {
        loading.value = false;
    }
};

const goToRegister = () => {
    close();
    router.push('/register');
};
</script>

<template>
    <Teleport to="body">
        <div
            v-if="visible"
            class="modal-overlay"
            data-testid="login-modal"
            @pointerdown="onOverlayPointerDown"
            @pointerup="onOverlayPointerUp"
            @pointercancel="resetOverlayPointer"
            @keydown.esc="close"
        >
            <div class="modal-content">
                <button class="modal-close" type="button" aria-label="关闭登录弹窗" @click="close">×</button>
                <p class="modal-eyebrow">需要登录</p>
                <h2>{{ prompt.title }}</h2>
                <p class="modal-message">{{ prompt.message }}</p>
                <form class="modal-form" @submit.prevent="handleLogin">
                    <div class="form-group">
                        <input
                            v-model="account"
                            type="text"
                            placeholder="用户名或邮箱"
                            autocomplete="username"
                            data-testid="login-modal-account-input"
                        >
                    </div>
                    <div class="form-group">
                        <input
                            v-model="password"
                            type="password"
                            placeholder="密码"
                            autocomplete="current-password"
                            data-testid="login-modal-password-input"
                        >
                    </div>
                    <p v-if="error" class="form-error">{{ error }}</p>
                    <button type="submit" class="submit-btn" data-testid="login-modal-submit" :disabled="loading">
                        {{ loading ? '登录中...' : prompt.actionText }}
                    </button>
                </form>
                <p class="form-footer">
                    还没有账号？<button type="button" @click="goToRegister">立即注册</button>
                </p>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.modal-overlay {
    position: fixed;
    display: flex;
    inset: 0;
    align-items: center;
    justify-content: center;
    padding: 18px;
    background: rgba(0, 0, 0, 0.48);
    z-index: 1000;
}

.modal-content {
    position: relative;
    width: 360px;
    max-width: 100%;
    padding: 32px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: var(--shadow-md);
}

.modal-close {
    position: absolute;
    top: 12px;
    right: 12px;
    width: 28px;
    height: 28px;
    font-size: 20px;
    line-height: 1;
    color: var(--muted);
    cursor: pointer;
    background: none;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.modal-close:hover {
    color: var(--text);
    background: var(--surface-soft);
}

.modal-content h2 {
    margin: 0 0 10px;
    font-size: 22px;
    text-align: center;
}

.modal-eyebrow {
    margin: 0 0 8px;
    color: var(--brand-strong);
    font-size: 13px;
    font-weight: 800;
    text-align: center;
}

.modal-message {
    margin: 0 0 22px;
    color: var(--muted);
    font-size: 14px;
    line-height: 1.7;
    text-align: center;
}

.modal-form {
    display: grid;
    gap: 14px;
}

.form-group {
    margin: 0;
}

.form-group input {
    width: 100%;
    padding: 10px 12px;
    font-size: 14px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    outline: none;
    background: var(--surface);
    color: var(--text);
    box-sizing: border-box;
}

.form-group input:focus {
    border-color: var(--brand);
}

.form-error {
    margin: 0 0 12px;
    color: #dc3545;
    font-size: 13px;
    text-align: center;
}

.submit-btn {
    width: 100%;
    padding: 10px;
    font-size: 14px;
    font-weight: 600;
    color: #ffffff;
    cursor: pointer;
    background: var(--brand);
    border: none;
    border-radius: var(--radius-sm);
}

.submit-btn:hover:not(:disabled) {
    background: var(--brand-strong);
}

.submit-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.form-footer {
    margin: 16px 0 0;
    text-align: center;
    color: var(--muted);
    font-size: 14px;
}

.form-footer button {
    color: var(--brand);
    cursor: pointer;
    background: none;
    border: none;
    font-size: 14px;
}

.form-footer button:hover {
    text-decoration: underline;
}
</style>
