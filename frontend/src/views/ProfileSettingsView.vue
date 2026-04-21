<script setup>
import { reactive, ref, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import { updateProfileApi } from '@/api/auth';
import { useSession } from '@/stores/session';

const { state, updateCurrentUser } = useSession();

const form = reactive({
    nickname: '',
    avatarUrl: '',
    bio: ''
});
const loading = ref(false);
const feedback = ref('');
const feedbackType = ref('info');

const syncFromSession = () => {
    form.nickname = state.user?.nickname || '';
    form.avatarUrl = state.user?.avatarUrl || state.user?.avatar || '';
    form.bio = state.user?.bio || '';
};

const submit = async () => {
    if (loading.value) {
        return;
    }
    loading.value = true;
    feedback.value = '';
    try {
        const user = await updateProfileApi(form);
        updateCurrentUser(user);
        feedback.value = '个人资料已更新';
        feedbackType.value = 'success';
    } catch (error) {
        feedback.value = error.message || '保存失败';
        feedbackType.value = 'error';
    } finally {
        loading.value = false;
    }
};

onMounted(syncFromSession);
</script>

<template>
    <SiteHeader />
    <main class="page-shell dashboard-layout">
        <aside class="dashboard-nav">
            <p class="eyebrow">个人中心</p>
            <RouterLink to="/dashboard/articles">我的文章</RouterLink>
            <RouterLink to="/dashboard/favorites">我的收藏</RouterLink>
            <RouterLink to="/settings/profile">个人资料</RouterLink>
        </aside>

        <section class="dashboard-main">
            <div class="section-heading">
                <div>
                    <p class="eyebrow">账户设置</p>
                    <h1>个人资料</h1>
                </div>
            </div>

            <form class="profile-form" @submit.prevent="submit">
                <label>
                    <span>昵称</span>
                    <input v-model.trim="form.nickname" type="text" placeholder="请输入昵称">
                </label>
                <label>
                    <span>头像地址</span>
                    <input v-model.trim="form.avatarUrl" type="url" placeholder="https://example.com/avatar.png">
                </label>
                <label>
                    <span>个人简介</span>
                    <textarea v-model.trim="form.bio" rows="4" placeholder="介绍一下你正在关注的技术方向"></textarea>
                </label>
                <div class="profile-form-footer">
                    <p v-if="feedback" :class="['form-message', feedbackType]">{{ feedback }}</p>
                    <button class="primary-action" type="submit" :disabled="loading">
                        {{ loading ? '保存中...' : '保存资料' }}
                    </button>
                </div>
            </form>
        </section>
    </main>
</template>

<style scoped>
.profile-form {
    display: grid;
    gap: 16px;
    padding: 24px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
}

.profile-form label {
    display: grid;
    gap: 8px;
}

.profile-form span {
    font-size: 13px;
    color: var(--muted);
}

.profile-form input,
.profile-form textarea {
    width: 100%;
    padding: 12px 14px;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fff;
    color: var(--text);
}

.profile-form-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
}

@media (max-width: 768px) {
    .profile-form-footer {
        flex-direction: column;
        align-items: stretch;
    }
}
</style>
