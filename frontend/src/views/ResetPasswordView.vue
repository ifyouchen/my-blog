<script setup>
import {onMounted, reactive, ref} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import {resetPasswordApi} from '@/api/auth';

const route = useRoute();
const router = useRouter();

const token = ref('');
const form = reactive({ newPassword: '', confirmPassword: '' });
const error = ref('');
const loading = ref(false);
const success = ref(false);

onMounted(() => {
    token.value = route.query.token || '';
    if (!token.value) {
        error.value = '重置链接无效或已过期，请重新申请。';
    }
});

const validate = () => {
    if (form.newPassword.length < 6) {
        error.value = '新密码至少 6 位';
        return false;
    }
    if (form.newPassword !== form.confirmPassword) {
        error.value = '两次输入的密码不一致';
        return false;
    }
    return true;
};

const submit = async () => {
    error.value = '';
    if (!token.value) {
        error.value = '重置链接无效或已过期，请重新申请。';
        return;
    }
    if (!validate()) {
        return;
    }
    loading.value = true;
    try {
        await resetPasswordApi(token.value, form.newPassword);
        success.value = true;
        setTimeout(() => router.push('/login'), 2000);
    } catch (err) {
        error.value = err.message || '重置失败，请确认链接是否有效';
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <SiteHeader />
    <main class="auth-layout" data-testid="reset-password-page">
        <section class="auth-visual">
            <p class="eyebrow">DevNotes</p>
            <h1>设置新密码</h1>
            <p>通过安全链接重置密码，重置后原密码将立即失效。</p>
        </section>

        <section class="auth-panel">
            <p class="eyebrow">密码重置</p>
            <h2>设置新密码</h2>
            <p class="auth-panel-copy">请输入并确认你的新密码，密码长度至少 6 位。</p>

            <template v-if="!success && !(!token && error)">
                <form class="form-stack" data-testid="reset-password-form" @submit.prevent="submit">
                    <label>
                        <span>新密码</span>
                        <input
                            v-model="form.newPassword"
                            type="password"
                            placeholder="请输入新密码（至少 6 位）"
                            data-testid="reset-new-password-input"
                        >
                    </label>
                    <label>
                        <span>确认新密码</span>
                        <input
                            v-model="form.confirmPassword"
                            type="password"
                            placeholder="请再次输入新密码"
                            data-testid="reset-confirm-password-input"
                        >
                    </label>
                    <button class="primary-action form-submit" type="submit" :disabled="loading || !token" data-testid="reset-submit">
                        {{ loading ? '重置中...' : '确认重置密码' }}
                    </button>
                    <small v-if="error" class="form-error">{{ error }}</small>
                </form>
            </template>

            <div v-else-if="success" class="reset-success">
                <div class="reset-success-icon" aria-hidden="true">
                    <svg viewBox="0 0 48 48" fill="none">
                        <circle cx="24" cy="24" r="22" stroke="currentColor" stroke-width="2"/>
                        <path d="M14 24l7 7 13-14" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </div>
                <h3>密码重置成功</h3>
                <p>你的新密码已生效，正在为你跳转到登录页...</p>
            </div>

            <div v-else class="reset-invalid">
                <p>{{ error }}</p>
                <RouterLink to="/auth/forgot-password">重新申请重置链接</RouterLink>
            </div>

            <p class="auth-switch">
                <RouterLink to="/login">返回登录</RouterLink>
            </p>
        </section>
    </main>
</template>

<style scoped src="@/styles/views/ResetPasswordView.css"></style>
