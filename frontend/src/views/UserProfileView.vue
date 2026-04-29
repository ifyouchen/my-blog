<script setup>
import {computed, inject, ref, watch} from 'vue';
import {RouterLink, useRoute} from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import EmptyState from '@/components/EmptyState.vue';
import ReportDialog from '@/components/ReportDialog.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import UserProfileSummary from '@/components/UserProfileSummary.vue';
import {getUserArticlesApi} from '@/api/articles';
import {getUserProfileApi} from '@/api/auth';
import {useStableListRequest} from '@/composables/useStableListRequest';
import {useSession} from '@/stores/session';
import {buildProfileSummaryStats} from '@/utils/profileSummary';

const route = useRoute();
const { state } = useSession();
const toast = inject('toast', { error: () => {}, success: () => {} });
const loginModal = inject('loginModal', { requireLogin: () => false });
const profile = ref(null);
const articles = ref([]);
const reportDialogVisible = ref(false);
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    loading,
    runStableRequest,
    resetStableRequest
} = useStableListRequest();
const userId = computed(() => String(route.params.id || ''));
const isOwnProfile = computed(() => profile.value?.user?.id === state.user?.id);
const summaryStats = computed(() => buildProfileSummaryStats(profile.value || {}, {
    includeSocial: true
}));
const summarySubtitle = computed(() => (
    profile.value?.user?.username ? `@${profile.value.user.username}` : ''
));

const loadProfile = async ({ reset = false } = {}) => {
    if (reset) {
        resetStableRequest();
        profile.value = null;
        articles.value = [];
        total.value = 0;
    }

    const { result } = await runStableRequest(
        () => Promise.all([
            getUserProfileApi(userId.value),
            getUserArticlesApi(userId.value, { page: page.value, pageSize })
        ]),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '作者主页加载失败',
            refreshErrorMessage: '作者文章刷新失败，请稍后重试'
        }
    );

    if (!result) {
        return;
    }

    const [profileData, articlePage] = result;
    profile.value = profileData;
    articles.value = articlePage.items || [];
    total.value = articlePage.total || 0;
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

const openReportUser = () => {
    if (!profile.value?.user?.id) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => openReportUser(), {
        title: '登录后举报用户',
        message: '登录后可以提交用户举报，管理员会在后台处理。',
        actionText: '登录并举报'
    });
    if (!canContinue) {
        return;
    }
    reportDialogVisible.value = true;
};

const handleReportSuccess = () => {
    toast.success('举报已提交，管理员会尽快处理');
};

watch(() => route.params.id, () => {
    page.value = 1;
    loadProfile({ reset: true });
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
                <button
                    v-if="!isOwnProfile"
                    type="button"
                    class="profile-report-action"
                    @click="openReportUser"
                >
                    举报
                </button>
            </template>
        </UserProfileSummary>

        <section v-else-if="initialLoading" class="profile-summary-loading">
            <div class="profile-loading-avatar"></div>
            <div class="profile-loading-content">
                <span></span>
                <strong></strong>
                <p></p>
            </div>
        </section>

        <EmptyState
            v-else-if="errorMessage"
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
            :initial-loading="initialLoading"
            :refreshing="refreshing"
            :has-loaded-once="hasLoadedOnce"
            :error-message="errorMessage"
            :inline-error-message="inlineError"
            :sort-items="[]"
            eyebrow="作者文章"
            title="最新发布"
            empty-text="这位作者还没有公开发布文章"
            @page-change="changePage"
        />
    </main>
    <ReportDialog
        v-if="profile"
        :visible="reportDialogVisible"
        target-type="USER"
        :target-id="profile.user.id"
        :target-title="profile.user.nickname || profile.user.username"
        @close="reportDialogVisible = false"
        @success="handleReportSuccess"
    />
</template>

<style scoped>
.profile-owner-action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 36px;
    padding: 0 16px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    text-decoration: none;
    transition: color 0.12s, border-color 0.12s, background 0.12s;
}

.profile-owner-action:hover {
    color: var(--brand);
    border-color: var(--brand);
    background: var(--brand-soft);
}

.profile-report-action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 36px;
    padding: 0 16px;
    color: var(--muted);
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.profile-report-action:hover {
    color: var(--accent);
    border-color: rgba(220, 38, 38, 0.24);
    background: rgba(220, 38, 38, 0.06);
}

.profile-summary-loading {
    display: flex;
    gap: 18px;
    align-items: center;
    padding: 24px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.profile-loading-avatar,
.profile-loading-content span,
.profile-loading-content strong,
.profile-loading-content p {
    display: block;
    background: linear-gradient(90deg, #f2f5f8 25%, #e8eef4 37%, #f2f5f8 63%);
    background-size: 400% 100%;
    animation: profile-skeleton 1.2s ease-in-out infinite;
}

.profile-loading-avatar {
    width: 72px;
    height: 72px;
    border-radius: 50%;
}

.profile-loading-content {
    display: grid;
    flex: 1;
    gap: 10px;
}

.profile-loading-content span {
    width: 120px;
    height: 14px;
    border-radius: 999px;
}

.profile-loading-content strong {
    width: 220px;
    height: 28px;
    border-radius: 10px;
}

.profile-loading-content p {
    width: min(420px, 100%);
    height: 16px;
    border-radius: 999px;
}

@keyframes profile-skeleton {
    0% {
        background-position: 100% 0;
    }

    100% {
        background-position: 0 0;
    }
}
</style>
