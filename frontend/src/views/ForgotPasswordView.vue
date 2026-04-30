<script setup>
import {reactive, ref} from 'vue';
import {RouterLink} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import {forgotPasswordApi} from '@/api/auth';

const form = reactive({ email: '' });
const error = ref('');
const loading = ref(false);
const sent = ref(false);

const validateEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

const submit = async () => {
    error.value = '';
    if (!validateEmail(form.email.trim())) {
        error.value = '请输入正确的邮箱地址';
        return;
    }
    loading.value = true;
    try {
        await forgotPasswordApi(form.email.trim());
        sent.value = true;
    } catch (err) {
        error.value = err.message || '发送失败，请稍后重试';
    } finally {
        loading.value = false;
    }
};
</script>

<template>
    <SiteHeader />
    <main class="auth-layout" data-testid="forgot-password-page">
        <section class="auth-visual">
            <p class="eyebrow">my-blog</p>
            <h1>找回你的账号</h1>
            <p>输入注册邮箱后，我们将发送密码重置链接，帮助你重新获得账号访问权限。</p>
        </section>

        <section class="auth-panel">
            <p class="eyebrow">账号找回</p>
            <h2>忘记密码</h2>
            <p class="auth-panel-copy">输入你的注册邮箱，我们会发送一封包含重置链接的邮件。</p>

            <template v-if="!sent">
                <form class="form-stack" data-testid="forgot-password-form" @submit.prevent="submit">
                    <label>
                        <span>注册邮箱</span>
                        <input
                            v-model.trim="form.email"
                            type="email"
                            placeholder="请输入注册时的邮箱"
                            data-testid="forgot-email-input"
                        >
                    </label>
                    <button class="primary-action form-submit" type="submit" :disabled="loading" data-testid="forgot-submit">
                        {{ loading ? '发送中...' : '发送重置链接' }}
                    </button>
                    <small v-if="error" class="form-error">{{ error }}</small>
                </form>
            </template>

            <div v-else class="forgot-success">
                <div class="forgot-success-icon" aria-hidden="true">
                    <svg viewBox="0 0 48 48" fill="none">
                        <circle cx="24" cy="24" r="22" stroke="currentColor" stroke-width="2"/>
                        <path d="M14 24l7 7 13-14" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </div>
                <h3>邮件已发送</h3>
                <p>我们已向 <strong>{{ form.email }}</strong> 发送了重置链接，请查收邮箱并点击链接完成密码重置。</p>
                <p class="forgot-tip">链接 30 分钟内有效，如未收到请检查垃圾邮件文件夹。</p>
            </div>

            <p class="auth-switch">
                <RouterLink to="/login">返回登录</RouterLink>
            </p>
        </section>
    </main>
</template>

<style scoped>
.forgot-success {
    display: grid;
    gap: 12px;
    padding: 24px;
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-sm);
    text-align: center;
}

.forgot-success-icon {
    display: flex;
    justify-content: center;
    color: var(--brand);
}

.forgot-success-icon svg {
    width: 48px;
    height: 48px;
}

.forgot-success h3 {
    margin: 0;
    font-size: 20px;
    color: var(--brand-strong);
}

.forgot-success p {
    margin: 0;
    color: var(--text);
    line-height: 1.7;
}

.forgot-tip {
    color: var(--muted) !important;
    font-size: 13px;
}

.form-error {
    color: var(--accent);
}
</style>

