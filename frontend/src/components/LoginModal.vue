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
const close = () => {
    account.value = '';
    password.value = '';
    error.value = '';
    emit('close');
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
            @click.self="close"
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

<style scoped src="@/styles/components/LoginModal.css"></style>
