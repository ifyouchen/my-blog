<script setup>
import {computed, onBeforeUnmount, reactive, ref, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import {sendRegisterEmailCodeApi} from '@/api/auth';
import {useSession} from '@/stores/session';

const route = useRoute();
const router = useRouter();
const { login, register } = useSession();
const isRegister = computed(() => route.name === 'register');
const inviteCode = computed(() => route.query.invite || '');

const form = reactive({
    username: '',
    account: '',
    email: '',
    emailCode: '',
    password: '',
    confirmPassword: ''
});

const errors = reactive({});
const successMessage = ref('');
const showWelcome = ref(false);
const welcomeUsername = ref('');
const emailCodeSending = ref(false);
const emailCodeCooldown = ref(0);
let emailCodeTimer = null;

const resetFeedback = () => {
    Object.keys(errors).forEach((key) => {
        delete errors[key];
    });
    successMessage.value = '';
};

const validateEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
const emailCodeButtonText = computed(() => {
    if (emailCodeSending.value) {
        return '发送中...';
    }
    return emailCodeCooldown.value > 0 ? `${emailCodeCooldown.value}s 后重试` : '获取验证码';
});

const stopEmailCodeCooldown = () => {
    if (emailCodeTimer) {
        window.clearInterval(emailCodeTimer);
        emailCodeTimer = null;
    }
};

const startEmailCodeCooldown = () => {
    stopEmailCodeCooldown();
    emailCodeCooldown.value = 30;
    emailCodeTimer = window.setInterval(() => {
        emailCodeCooldown.value -= 1;
        if (emailCodeCooldown.value <= 0) {
            emailCodeCooldown.value = 0;
            stopEmailCodeCooldown();
        }
    }, 1000);
};

const sendEmailCode = async () => {
    if (!isRegister.value || emailCodeSending.value || emailCodeCooldown.value > 0) {
        return;
    }
    errors.email = '';
    errors.emailCode = '';
    errors.submit = '';
    successMessage.value = '';
    const email = form.email.trim();
    if (!validateEmail(email)) {
        errors.email = '请输入正确的邮箱';
        return;
    }

    emailCodeSending.value = true;
    startEmailCodeCooldown();
    try {
        await sendRegisterEmailCodeApi(email);
        successMessage.value = '验证码邮件请求已提交，请稍后查收';
    } catch (error) {
        errors.emailCode = error.message || '验证码发送失败，请稍后重试';
    } finally {
        emailCodeSending.value = false;
    }
};

const validate = () => {
    resetFeedback();

    if (isRegister.value && form.username.trim().length < 3) {
        errors.username = '用户名至少 3 个字符';
    }

    if (isRegister.value) {
        if (!validateEmail(form.email.trim())) {
            errors.email = '请输入正确的邮箱';
        }
        if (!/^\d{6}$/.test(form.emailCode.trim())) {
            errors.emailCode = '请输入 6 位邮箱验证码';
        }
    } else if (!form.account.trim()) {
        errors.account = '请输入用户名或邮箱';
    }

    if (form.password.length < 6) {
        errors.password = '密码至少 6 位';
    }

    if (isRegister.value && form.confirmPassword !== form.password) {
        errors.confirmPassword = '两次输入的密码不一致';
    }

    return Object.keys(errors).length === 0;
};

const submit = async () => {
    if (!validate()) {
        return;
    }

    try {
        if (isRegister.value) {
            await register({
                username: form.username.trim(),
                email: form.email.trim(),
                emailCode: form.emailCode.trim(),
                password: form.password,
                inviteCode: inviteCode.value || undefined
            });
            welcomeUsername.value = form.username.trim();
            showWelcome.value = true;
        } else {
            await login({
                account: form.account.trim(),
                password: form.password
            });
            successMessage.value = '登录成功，已为你进入创作者后台';
            window.setTimeout(() => {
                router.push('/dashboard/articles');
            }, 500);
        }
    } catch (error) {
        errors.submit = error.message || '请求失败';
    }
};

const closeWelcome = () => {
    showWelcome.value = false;
    router.push('/dashboard/articles');
};

watch(isRegister, () => {
    resetFeedback();
});

onBeforeUnmount(() => {
    stopEmailCodeCooldown();
});
</script>

<template>
    <SiteHeader />
    <main class="auth-layout" :data-testid="isRegister ? 'register-page' : 'login-page'">
        <section class="auth-visual">
            <p class="eyebrow">my-blog</p>
            <h1>把工程经验沉淀成作品</h1>
            <p>登录后可以发布文章、保存草稿、评论互动、收藏文章，并进入自己的创作者中心。</p>
            <div class="auth-highlights">
                <span class="auth-highlight">文章发布与草稿管理</span>
                <span class="auth-highlight">评论、点赞、收藏互动</span>
                <span class="auth-highlight">创作者中心与数据看板</span>
            </div>
        </section>

        <section class="auth-panel" :aria-labelledby="isRegister ? 'register-title' : 'login-title'">
            <p class="eyebrow">{{ isRegister ? '创建账号' : '欢迎回来' }}</p>
            <h2 :id="isRegister ? 'register-title' : 'login-title'">
                {{ isRegister ? '注册 my-blog' : '登录 my-blog' }}
            </h2>
            <p class="auth-panel-copy">
                {{ isRegister ? '创建账号后即可进入创作中心，搭建自己的内容主页。' : '登录后继续写作、管理内容，并查看站内互动。' }}
            </p>

            <form class="form-stack" :data-testid="isRegister ? 'register-form' : 'login-form'" @submit.prevent="submit">
                <label v-if="isRegister">
                    <span>用户名</span>
                    <input v-model.trim="form.username" type="text" placeholder="请输入用户名" data-testid="register-username-input">
                    <small v-if="errors.username">{{ errors.username }}</small>
                </label>
                <label>
                    <span>{{ isRegister ? '邮箱' : '账号' }}</span>
                    <input
                        v-if="isRegister"
                        v-model.trim="form.email"
                        type="email"
                        placeholder="请输入邮箱"
                        data-testid="register-email-input"
                    >
                    <input
                        v-else
                        v-model.trim="form.account"
                        type="text"
                        placeholder="用户名或邮箱"
                        data-testid="login-account-input"
                    >
                    <small v-if="isRegister && errors.email">{{ errors.email }}</small>
                    <small v-if="!isRegister && errors.account">{{ errors.account }}</small>
                </label>
                <label v-if="isRegister">
                    <span>邮箱验证码</span>
                    <div class="email-code-row">
                        <input
                            v-model.trim="form.emailCode"
                            type="text"
                            inputmode="numeric"
                            maxlength="6"
                            placeholder="请输入验证码"
                            data-testid="register-email-code-input"
                        >
                        <button
                            class="email-code-button"
                            type="button"
                            :disabled="emailCodeSending || emailCodeCooldown > 0"
                            data-testid="register-email-code-submit"
                            @click="sendEmailCode"
                        >
                            {{ emailCodeButtonText }}
                        </button>
                    </div>
                    <small v-if="errors.emailCode">{{ errors.emailCode }}</small>
                </label>
                <label>
                    <span>密码</span>
                    <input v-model="form.password" type="password" placeholder="请输入密码" :data-testid="isRegister ? 'register-password-input' : 'login-password-input'">
                    <small v-if="errors.password">{{ errors.password }}</small>
                </label>
                <label v-if="isRegister">
                    <span>确认密码</span>
                    <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" data-testid="register-confirm-password-input">
                    <small v-if="errors.confirmPassword">{{ errors.confirmPassword }}</small>
                </label>
                <button class="primary-action form-submit" type="submit" :data-testid="isRegister ? 'register-submit' : 'login-submit'">
                    {{ isRegister ? '注册' : '登录' }}
                </button>
                <small v-if="errors.submit">{{ errors.submit }}</small>
                <div v-if="!isRegister" class="auth-forgot">
                    <RouterLink to="/auth/forgot-password">忘记密码？</RouterLink>
                </div>
            </form>

            <p v-if="successMessage" class="form-message success">{{ successMessage }}</p>

            <p class="auth-switch">
                <span>{{ isRegister ? '已经有账号？' : '还没有账号？' }}</span>
                <RouterLink :to="isRegister ? '/login' : '/register'">
                    {{ isRegister ? '去登录' : '去注册' }}
                </RouterLink>
            </p>
        </section>
    </main>

    <!-- 注册成功欢迎弹窗 -->
    <Teleport to="body">
        <Transition name="welcome-fade">
            <div v-if="showWelcome" class="welcome-overlay" role="dialog" aria-modal="true" aria-labelledby="welcome-title" @click.self="closeWelcome">
                <div class="welcome-dialog">
                    <div class="welcome-icon" aria-hidden="true">
                        <svg viewBox="0 0 64 64" fill="none">
                            <circle cx="32" cy="32" r="30" stroke="currentColor" stroke-width="2.5"/>
                            <path d="M20 32l8 8 16-16" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                    </div>
                    <h2 id="welcome-title">欢迎加入 my-blog 👋</h2>
                    <p class="welcome-greeting">嗨，<strong>{{ welcomeUsername }}</strong>！你已成功创建账号。</p>
                    <ul class="welcome-checklist">
                        <li>
                            <span class="welcome-check" aria-hidden="true">✓</span>
                            前往「设置 → 个人资料」完善头像、简介和社交主页
                        </li>
                        <li>
                            <span class="welcome-check" aria-hidden="true">✓</span>
                            点击「写文章」开始发布你的第一篇技术文章
                        </li>
                        <li>
                            <span class="welcome-check" aria-hidden="true">✓</span>
                            在首页发现优质内容，关注感兴趣的作者
                        </li>
                    </ul>
                    <div class="welcome-actions">
                        <button class="primary-action" type="button" @click="closeWelcome">
                            进入创作者中心
                        </button>
                        <RouterLink class="welcome-action-secondary" to="/settings/profile" @click="showWelcome = false">
                            先完善个人资料
                        </RouterLink>
                    </div>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<style scoped>
.auth-forgot {
    text-align: right;
    margin-top: -4px;
}

.auth-forgot a {
    font-size: 13px;
    color: var(--brand);
    text-decoration: none;
}

.auth-forgot a:hover {
    text-decoration: underline;
}

.email-code-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 10px;
    align-items: center;
}

.email-code-button {
    min-height: 42px;
    padding: 0 14px;
    color: var(--brand);
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-md);
    cursor: pointer;
}

.email-code-button:disabled {
    cursor: not-allowed;
    color: var(--muted);
    background: var(--surface-muted);
    border-color: var(--line);
}

/* ── 欢迎弹窗 ──────────────────────────────────────────────────────── */
.welcome-overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: rgba(0, 0, 0, 0.55);
    backdrop-filter: blur(4px);
}

.welcome-dialog {
    display: grid;
    gap: 20px;
    width: 100%;
    max-width: 480px;
    padding: 36px 32px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    box-shadow: 0 24px 48px rgba(0, 0, 0, 0.18);
    text-align: center;
    animation: welcome-pop 0.32s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.welcome-icon {
    display: flex;
    justify-content: center;
    color: var(--brand);
}

.welcome-icon svg {
    width: 64px;
    height: 64px;
}

.welcome-dialog h2 {
    margin: 0;
    font-size: 26px;
    font-weight: 700;
    color: var(--text-strong);
}

.welcome-greeting {
    margin: 0;
    color: var(--muted);
    font-size: 15px;
    line-height: 1.6;
}

.welcome-checklist {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    gap: 12px;
    text-align: left;
}

.welcome-checklist li {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    font-size: 14px;
    color: var(--text);
    line-height: 1.6;
}

.welcome-check {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    min-width: 20px;
    font-size: 11px;
    font-weight: 700;
    color: #16a34a;
    background: rgba(22, 163, 74, 0.1);
    border: 1px solid rgba(22, 163, 74, 0.25);
    border-radius: 50%;
    margin-top: 1px;
}

.welcome-actions {
    display: flex;
    flex-direction: column;
    gap: 10px;
    align-items: center;
}

.welcome-action-secondary {
    font-size: 14px;
    color: var(--brand);
    text-decoration: none;
}

.welcome-action-secondary:hover {
    text-decoration: underline;
}

/* 动画 */
.welcome-fade-enter-active,
.welcome-fade-leave-active {
    transition: opacity 0.22s ease;
}

.welcome-fade-enter-from,
.welcome-fade-leave-to {
    opacity: 0;
}

@keyframes welcome-pop {
    from {
        opacity: 0;
        transform: scale(0.88) translateY(16px);
    }
    to {
        opacity: 1;
        transform: scale(1) translateY(0);
    }
}

@media (max-width: 480px) {
    .welcome-dialog {
        padding: 28px 20px;
    }

    .email-code-row {
        grid-template-columns: 1fr;
    }
}
</style>
