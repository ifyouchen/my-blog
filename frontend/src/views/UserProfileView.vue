<script setup>import {computed, inject, ref, watch} from 'vue';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import {useHead} from '@unhead/vue';
import ArticleFeed from '@/components/ArticleFeed.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import EmptyState from '@/components/EmptyState.vue';
import ReportDialog from '@/components/ReportDialog.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import UserProfileSummary from '@/components/UserProfileSummary.vue';
import {getUserArticlesApi} from '@/api/articles';
import {getUserProfileApi} from '@/api/auth';
import {getUserFollowersApi, getUserFollowingListApi, getFollowStatusApi} from '@/api/following';
import {useStableListRequest} from '@/composables/useStableListRequest';
import {useSession} from '@/stores/session';
import {buildProfileSummaryStats} from '@/utils/profileSummary';

const route = useRoute();
const router = useRouter();
const { state } = useSession();
const toast = inject('toast', { error: () => {}, success: () => {} });
const loginModal = inject('loginModal', { requireLogin: () => false });
const profile = ref(null);
const articles = ref([]);
const reportDialogVisible = ref(false);
const followDialogVisible = ref(false);
const followDialogType = ref('followers');
const followDialogList = ref([]);
const followDialogLoading = ref(false);
const followStatus = ref({ followed: false, followedBack: false, mutual: false });
const page = ref(1);

useHead({
    title: computed(() => {
        if (profile.value?.user?.username) {
            return `${profile.value.user.username} 的个人主页 - my-blog`;
        }
        return '用户主页 - my-blog';
    })
});
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
const summaryStats = computed(() => {
    const base = buildProfileSummaryStats(profile.value || {}, { includeSocial: false });
    return [
        ...base,
        { key: 'followers', label: '粉丝', value: profile.value ? (profile.value.followerCount || 0) : '--', clickable: true },
        { key: 'following', label: '关注', value: profile.value ? (profile.value.followingCount || 0) : '--', clickable: true }
    ];
});
const summarySubtitle = computed(() => (
    profile.value?.user?.username ? `@${profile.value.user.username}` : ''
));
const isMutualFollow = computed(() => !!(followStatus.value && followStatus.value.mutual));

const loadProfile = async ({ reset = false } = {}) => {
    if (reset) {
        resetStableRequest();
        profile.value = null;
        articles.value = [];
        total.value = 0;
        followStatus.value = { followed: false, followedBack: false, mutual: false };
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

    if (!isOwnProfile.value && state.user) {
        getFollowStatusApi(userId.value).then(function(s) { followStatus.value = s; }).catch(function() {});
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
    if (state.user) {
        getFollowStatusApi(userId.value).then(function(s) { followStatus.value = s; }).catch(function() {});
    }
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

const openFollowDialog = async (type) => {
    followDialogType.value = type;
    followDialogVisible.value = true;
    followDialogLoading.value = true;
    followDialogList.value = [];
    try {
        if (type === 'followers') {
            followDialogList.value = await getUserFollowersApi(userId.value);
        } else {
            followDialogList.value = await getUserFollowingListApi(userId.value);
        }
    } catch (e) {
        // ignore
    } finally {
        followDialogLoading.value = false;
    }
};
const closeFollowDialog = () => { followDialogVisible.value = false; };
const goToUserProfile = (uid) => { closeFollowDialog(); router.push('/users/' + uid); };

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
            @stat-click="(stat) => { if (stat.key === 'followers' || stat.key === 'following') openFollowDialog(stat.key); }"
        >
            <template #actions>
                <RouterLink
                    v-if="isOwnProfile"
                    class="profile-owner-action"
                    to="/settings/profile"
                >
                    编辑资料
                </RouterLink>
                <template v-else>
                    <span v-if="isMutualFollow" class="mutual-follow-badge">互关</span>
                    <AuthorFollowButton
                        :user-id="profile.user.id"
                        :followed="profile.following"
                        @change="handleFollowChange"
                    />
                </template>
                <button
                    v-if="!isOwnProfile"
                    type="button"
                    class="profile-report-action"
                    @click="openReportUser"
                >
                    举报
                </button>
            </template>
            <template
                v-if="profile.user.location || profile.user.website || profile.user.github || profile.user.twitter"
                #extra
            >
                <ul class="profile-social-links">
                    <li v-if="profile.user.location" class="profile-social-item">
                        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                            <path d="M10 2a6 6 0 0 1 6 6c0 4-6 10-6 10S4 12 4 8a6 6 0 0 1 6-6Zm0 3.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5Z" stroke="currentColor" stroke-width="1.4"/>
                        </svg>
                        <span>{{ profile.user.location }}</span>
                    </li>
                    <li v-if="profile.user.website" class="profile-social-item profile-social-link">
                        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                            <circle cx="10" cy="10" r="7.5" stroke="currentColor" stroke-width="1.4"/>
                            <path d="M10 2.5c0 0-3 2.5-3 7.5s3 7.5 3 7.5 3-2.5 3-7.5-3-7.5-3-7.5ZM2.5 10h15" stroke="currentColor" stroke-width="1.4"/>
                        </svg>
                        <a :href="profile.user.website" target="_blank" rel="noopener noreferrer">{{ profile.user.website }}</a>
                    </li>
                    <li v-if="profile.user.github" class="profile-social-item profile-social-link">
                        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                            <path d="M10 2a8 8 0 0 0-2.53 15.59c.4.07.55-.17.55-.38v-1.37c-2.23.48-2.69-1.07-2.69-1.07-.36-.93-.89-1.17-.89-1.17-.73-.5.05-.49.05-.49.8.06 1.23.83 1.23.83.71 1.22 1.87.87 2.33.66.07-.52.28-.87.5-1.07-1.77-.2-3.63-.89-3.63-3.95 0-.87.31-1.58.83-2.14-.08-.2-.36-1.01.08-2.1 0 0 .67-.22 2.2.82A7.69 7.69 0 0 1 10 6.84c.68 0 1.36.09 2 .27 1.52-1.04 2.19-.82 2.19-.82.44 1.09.16 1.9.08 2.1.52.56.83 1.27.83 2.14 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48v2.2c0 .21.14.46.55.38A8 8 0 0 0 10 2Z" stroke="currentColor" stroke-width="1.2" fill="none"/>
                        </svg>
                        <a :href="`https://github.com/${profile.user.github}`" target="_blank" rel="noopener noreferrer">{{ profile.user.github }}</a>
                    </li>
                    <li v-if="profile.user.twitter" class="profile-social-item profile-social-link">
                        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                            <path d="M17.5 4.25a7.96 7.96 0 0 1-2.35.68 4.06 4.06 0 0 0 1.78-2.26 8.06 8.06 0 0 1-2.59 1 4.04 4.04 0 0 0-6.88 3.68A11.47 11.47 0 0 1 3.14 5.1a4.03 4.03 0 0 0 1.25 5.38 4 4 0 0 1-1.83-.5v.05a4.04 4.04 0 0 0 3.24 3.95 4.04 4.04 0 0 1-1.82.07 4.05 4.05 0 0 0 3.78 2.81A8.1 8.1 0 0 1 2.5 18a11.42 11.42 0 0 0 6.18 1.81c7.42 0 11.48-6.15 11.48-11.48l-.01-.52A8.22 8.22 0 0 0 22.5 5.5a8.07 8.07 0 0 1-2.35.64 4.07 4.07 0 0 0 1.78-2.27" stroke="currentColor" stroke-width="1.2"/>
                        </svg>
                        <a :href="`https://twitter.com/${profile.user.twitter}`" target="_blank" rel="noopener noreferrer">@{{ profile.user.twitter }}</a>
                    </li>
                </ul>
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
    <Teleport to="body">
        <div v-if="followDialogVisible" class="follow-dialog-overlay" @click.self="closeFollowDialog">
            <div class="follow-dialog" role="dialog">
                <div class="follow-dialog-header">
                    <h2 class="follow-dialog-title">
                        {{ followDialogType === 'followers' ? '粉丝' : '关注' }}
                        <span class="follow-dialog-count">{{ followDialogList.length }}</span>
                    </h2>
                    <button type="button" class="follow-dialog-close" aria-label="关闭" @click="closeFollowDialog">
                        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M5 5l10 10M15 5l-10 10" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
                    </button>
                </div>
                <div class="follow-dialog-body">
                    <div v-if="followDialogLoading" class="follow-dialog-loading">
                        <span class="follow-loading-dot"></span><span class="follow-loading-dot"></span><span class="follow-loading-dot"></span>
                    </div>
                    <ul v-else-if="followDialogList.length" class="follow-user-list">
                        <li v-for="user in followDialogList" :key="user.id" class="follow-user-item" @click="goToUserProfile(user.id)">
                            <img class="follow-user-avatar" :src="user.avatar" :alt="user.nickname" />
                            <div class="follow-user-info">
                                <span class="follow-user-name">{{ user.nickname }}</span>
                                <span class="follow-user-handle">@{{ user.username }}</span>
                            </div>
                        </li>
                    </ul>
                    <p v-else class="follow-dialog-empty">{{ followDialogType === 'followers' ? '暂无粉丝' : '还没有关注任何人' }}</p>
                </div>
            </div>
        </div>
    </Teleport>

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

.profile-social-links {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 20px;
    list-style: none;
    margin: 0;
    padding: 0;
}

.profile-social-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--muted);
    font-size: 14px;
    line-height: 1;
}

.profile-social-item svg {
    width: 15px;
    height: 15px;
    flex-shrink: 0;
}

.profile-social-link a {
    color: var(--brand);
    text-decoration: none;
    transition: color 0.12s;
}

.profile-social-link a:hover {
    text-decoration: underline;
}

.mutual-follow-badge {
    display: inline-flex;
    align-items: center;
    height: 28px;
    padding: 0 10px;
    font-size: 12px;
    font-weight: 600;
    color: #059669;
    background: rgba(5, 150, 105, 0.08);
    border: 1px solid rgba(5, 150, 105, 0.24);
    border-radius: var(--radius-sm);
}

.follow-dialog-overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: rgba(0, 0, 0, 0.44);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
    animation: fade-in 0.12s ease;
}
@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }
.follow-dialog {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 12px;
    box-shadow: 0 20px 60px rgba(0,0,0,0.18);
    width: min(440px, 100%);
    max-height: 70vh;
    display: flex;
    flex-direction: column;
    animation: slide-up 0.14s ease;
}
@keyframes slide-up { from { transform: translateY(8px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.follow-dialog-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid var(--line);
    flex-shrink: 0;
}
.follow-dialog-title {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--text-strong);
    display: flex;
    align-items: center;
    gap: 8px;
}
.follow-dialog-count {
    font-size: 13px;
    font-weight: 500;
    color: var(--muted);
    background: var(--surface-soft);
    padding: 1px 8px;
    border-radius: 999px;
}
.follow-dialog-close {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    color: var(--muted);
    cursor: pointer;
    background: none;
    border: none;
    border-radius: var(--radius-sm);
    transition: color 0.12s, background 0.12s;
}
.follow-dialog-close:hover { color: var(--text); background: var(--surface-soft); }
.follow-dialog-close svg { width: 18px; height: 18px; }
.follow-dialog-body { overflow-y: auto; flex: 1; padding: 8px 0; }
.follow-user-list { list-style: none; margin: 0; padding: 0; }
.follow-user-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 20px;
    cursor: pointer;
    transition: background 0.1s;
}
.follow-user-item:hover { background: var(--surface-soft); }
.follow-user-avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
    border: 1px solid var(--line);
}
.follow-user-info { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.follow-user-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-strong);
    line-height: 1.3;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.follow-user-handle { font-size: 12px; color: var(--muted); }
.follow-dialog-empty { text-align: center; color: var(--muted); font-size: 14px; padding: 36px 20px; margin: 0; }
.follow-dialog-loading { display: flex; align-items: center; justify-content: center; gap: 6px; padding: 40px 20px; }
.follow-loading-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--brand);
    animation: dot-bounce 1.2s ease-in-out infinite;
}
.follow-loading-dot:nth-child(2) { animation-delay: 0.16s; }
.follow-loading-dot:nth-child(3) { animation-delay: 0.32s; }
@keyframes dot-bounce {
    0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
    40% { transform: scale(1); opacity: 1; }
}
</style>
