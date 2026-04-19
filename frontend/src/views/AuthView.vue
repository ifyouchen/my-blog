<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import { useSession } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const { login, register } = useSession();
const isRegister = computed(() => route.name === 'register');

const form = reactive({
    username: '',
    account: '',
    email: '',
    password: '',
    confirmPassword: ''
});

const errors = reactive({});
const successMessage = ref('');

const resetFeedback = () => {
    Object.keys(errors).forEach((key) => {
        delete errors[key];
    });
    successMessage.value = '';
};

const validateEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

const validate = () => {
    resetFeedback();

    if (isRegister.value && form.username.trim().length < 3) {
        errors.username = '用户名至少 3 个字符';
    }

    if (isRegister.value) {
        if (!validateEmail(form.email.trim())) {
            errors.email = '请输入正确的邮箱';
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
                password: form.password
            });
            successMessage.value = '注册成功，已为你进入创作者后台';
        } else {
            await login({
                account: form.account.trim(),
                password: form.password
            });
            successMessage.value = '登录成功，已为你进入创作者后台';
        }
    } catch (error) {
        errors.submit = error.message || '请求失败';
        return;
    }

    window.setTimeout(() => {
        router.push('/dashboard/articles');
    }, 500);
};

watch(isRegister, () => {
    resetFeedback();
});
</script>

<template>
    <SiteHeader />
    <main class="auth-layout">
        <section class="auth-visual">
            <p class="eyebrow">my-blog</p>
            <h1>把工程经验沉淀成作品</h1>
            <p>登录后可以发布文章、保存草稿、评论互动、收藏文章，并进入自己的创作者中心。</p>
        </section>

        <section class="auth-panel" :aria-labelledby="isRegister ? 'register-title' : 'login-title'">
            <p class="eyebrow">{{ isRegister ? '创建账号' : '欢迎回来' }}</p>
            <h2 :id="isRegister ? 'register-title' : 'login-title'">
                {{ isRegister ? '注册 my-blog' : '登录 my-blog' }}
            </h2>

            <form class="form-stack" @submit.prevent="submit">
                <label v-if="isRegister">
                    <span>用户名</span>
                    <input v-model.trim="form.username" type="text" placeholder="请输入用户名">
                    <small v-if="errors.username">{{ errors.username }}</small>
                </label>
                <label>
                    <span>{{ isRegister ? '邮箱' : '账号' }}</span>
                    <input
                        v-if="isRegister"
                        v-model.trim="form.email"
                        type="email"
                        placeholder="请输入邮箱"
                    >
                    <input
                        v-else
                        v-model.trim="form.account"
                        type="text"
                        placeholder="用户名或邮箱"
                    >
                    <small v-if="isRegister && errors.email">{{ errors.email }}</small>
                    <small v-if="!isRegister && errors.account">{{ errors.account }}</small>
                </label>
                <label>
                    <span>密码</span>
                    <input v-model="form.password" type="password" placeholder="请输入密码">
                    <small v-if="errors.password">{{ errors.password }}</small>
                </label>
                <label v-if="isRegister">
                    <span>确认密码</span>
                    <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码">
                    <small v-if="errors.confirmPassword">{{ errors.confirmPassword }}</small>
                </label>
                <button class="primary-action form-submit" type="submit">
                    {{ isRegister ? '注册' : '登录' }}
                </button>
                <small v-if="errors.submit">{{ errors.submit }}</small>
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
</template>
