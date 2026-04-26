<script setup>
import { computed, inject, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import EmptyState from '@/components/EmptyState.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import UserProfileSummary from '@/components/UserProfileSummary.vue';
import { getUserArticlesApi } from '@/api/articles';
import { getUserProfileApi } from '@/api/auth';
import { useSession } from '@/stores/session';
import { buildProfileSummaryStats } from '@/utils/profileSummary';

const route = useRoute();
const { state } = useSession();
const toast = inject('toast', { error: () => {} });
const profile = ref(null);
const articles = ref([]);
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const loading = ref(false);
const errorMessage = ref('');
const userId = computed(() => Number(route.params.id));
const isOwnProfile = computed(() => profile.value?.user?.id === state.user?.id);
const summaryStats = computed(() => buildProfileSummaryStats(profile.value || {}, {
    includeSocial: true
}));
const summarySubtitle = computed(() => (
    profile.value?.user?.username ? `@${profile.value.user.username}` : ''
));

const loadProfile = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const [profileData, articlePage] = await Promise.all([
            getUserProfileApi(userId.value),
            getUserArticlesApi(userId.value, { page: page.value, pageSize })
        ]);
        profile.value = profileData;
        articles.value = articlePage.items || [];
        total.value = articlePage.total || 0;
    } catch (error) {
        profile.value = null;
        articles.value = [];
        total.value = 0;
        errorMessage.value = error.message || '作者主页加载失败';
    } finally {
        loading.value = false;
    }
};

const changePage = async (nextPage) => {
    page.value = nextPage;
    await loadProfile();
};

const handleFollowChange = (followed, error) => {
    if (!profile.value) {
        return;
    }
    if (error) {
        toast.error(error.message || '关注操作失败');
        return;
    }
    profile.value.following = followed;
    profile.value.followerCount = Math.max(0, (profile.value.followerCount || 0) + (followed ? 1 : -1));
};

watch(() => route.params.id, () => {
    page.value = 1;
    loadProfile();
}, { immediate: true });
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <UserProfileSummary
            v-if="profile"
            :mode="isOwnProfile ? 'owner' : 'public'"
            eyebrow="个人主页"
            :avatar-src="profile.user.avatar"
            :title="profile.user.nickname"
            :subtitle="summarySubtitle"
            :bio="profile.user.bio"
            :stats="summaryStats"
        >
            <template #actions>
                <RouterLink
                    v-if="isOwnProfile"
                    class="profile-owner-action"
                    to="/settings/profile"
                >
                    编辑资料
                </RouterLink>
                <AuthorFollowButton
                    v-else
                    :user-id="profile.user.id"
                    :followed="profile.following"
                    @change="handleFollowChange"
                />
            </template>
        </UserProfileSummary>

        <EmptyState
            v-else
            eyebrow="作者主页"
            title="暂时无法加载作者信息"
            :description="errorMessage || '请稍后重试。'"
            tone="error"
        />

        <ArticleFeed
            :articles="articles"
            :page="page"
            :page-size="pageSize"
            :total="total"
            :loading="loading"
            :error-message="errorMessage"
            :sort-items="[]"
            eyebrow="作者文章"
            title="最新发布"
            empty-text="这位作者还没有公开发布文章"
            @page-change="changePage"
        />
    </main>
</template>

<style scoped>
.profile-owner-action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 40px;
    padding: 0 16px;
    color: var(--text);
    background: #ffffff;
    border: 1px solid var(--line);
    border-radius: 12px;
    box-shadow: 0 10px 24px rgba(31, 78, 168, 0.05);
}

.profile-owner-action:hover {
    color: var(--brand-strong);
    border-color: rgba(40, 118, 255, 0.18);
    background: rgba(40, 118, 255, 0.05);
}
</style>
