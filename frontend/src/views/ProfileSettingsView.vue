<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue';
import {RouterLink} from 'vue-router';
import {uploadImageApi} from '@/api/uploads';
import SiteHeader from '@/components/SiteHeader.vue';
import UserProfileSummary from '@/components/UserProfileSummary.vue';
import {changeEmailApi, changePasswordApi, getSecurityInfoApi, getUserHotArticlesApi, getUserProfileApi, updateProfileApi} from '@/api/auth';
import {useSession} from '@/stores/session';
import {buildProfileSummaryStats} from '@/utils/profileSummary';
import {resolveMediaUrl} from '@/utils/media';

const { state, updateCurrentUser } = useSession();
const DEFAULT_AVATAR = 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=240&q=80';

// ── Tab ──────────────────────────────────────────────────────────────────────
const activeTab = ref('profile'); // 'profile' | 'security'

// ── 个人资料 ──────────────────────────────────────────────────────────────────
const loading = ref(false);
const statsLoading = ref(false);
const hotArticlesLoading = ref(false);
const hotArticlesError = ref('');
const activeField = ref('');
const fieldFeedback = ref('');
const fieldFeedbackType = ref('info');
const avatarUploading = ref(false);
const avatarInputRef = ref(null);
const avatarPreviewFailed = ref(false);
const debouncedAvatarUrl = ref('');
let avatarUrlDebounceTimer = null;
const profileStats = reactive({
    articleCount: 0,
    totalViewCount: 0,
    totalLikeCount: 0,
    followerCount: 0,
    followingCount: 0
});
const draft = reactive({
    nickname: '',
    avatarUrl: '',
    bio: '',
    website: '',
    github: '',
    twitter: '',
    location: ''
});
const originalProfile = reactive({
    nickname: '',
    avatarUrl: '',
    bio: '',
    website: '',
    github: '',
    twitter: '',
    location: ''
});
const hotArticles = ref([]);

const FIELD_LABELS = {
    nickname: '昵称',
    avatarUrl: '头像',
    bio: '个人简介',
    website: '个人网站',
    github: 'GitHub',
    twitter: 'Twitter',
    location: '所在地'
};

// ── 账号安全 ──────────────────────────────────────────────────────────────────
const securityLoading = ref(false);
const securityInfo = ref(null);

const pwdForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' });
const pwdError = ref('');
const pwdSuccess = ref('');
const pwdLoading = ref(false);
const emailForm = reactive({ email: '', password: '' });
const emailError = ref('');
const emailSuccess = ref('');
const emailLoading = ref(false);

// ─────────────────────────────────────────────────────────────────────────────

const roleLabel = computed(() => (state.user?.role === 'ADMIN' ? '管理员' : '普通用户'));
const rawAvatar = computed(() => debouncedAvatarUrl.value || state.user?.avatarUrl || state.user?.avatar || '');
const displayAvatar = computed(() => (
    avatarPreviewFailed.value ? DEFAULT_AVATAR : resolveMediaUrl(rawAvatar.value, DEFAULT_AVATAR)
));
const displayNickname = computed(() => draft.nickname || state.user?.nickname || state.user?.username || '未设置昵称');
const displayUsername = computed(() => state.user?.username || 'my-blog 用户');
const displayBio = computed(() => draft.bio || '这里会展示你的个人简介与创作方向。');
const summaryStats = computed(() => buildProfileSummaryStats(profileStats, {
    includeSocial: true,
    loading: statsLoading.value
}));

watch(rawAvatar, () => {
    avatarPreviewFailed.value = false;
});

watch(() => draft.avatarUrl, (newUrl) => {
    if (avatarUrlDebounceTimer) clearTimeout(avatarUrlDebounceTimer);
    avatarUrlDebounceTimer = setTimeout(() => {
        debouncedAvatarUrl.value = newUrl;
        avatarUrlDebounceTimer = null;
    }, 2000);
});

const syncFromSession = () => {
    draft.nickname = state.user?.nickname || '';
    draft.avatarUrl = state.user?.avatarUrl || state.user?.avatar || '';
    draft.bio = state.user?.bio || '';
    draft.website = state.user?.website || '';
    draft.github = state.user?.github || '';
    draft.twitter = state.user?.twitter || '';
    draft.location = state.user?.location || '';
    debouncedAvatarUrl.value = draft.avatarUrl;
    avatarPreviewFailed.value = false;
    Object.assign(originalProfile, draft);
};

const loadProfileStats = async () => {
    if (!state.user?.id) {
        return;
    }
    statsLoading.value = true;
    try {
        const profile = await getUserProfileApi(state.user.id);
        profileStats.articleCount = profile.articleCount || 0;
        profileStats.totalViewCount = profile.totalViewCount || 0;
        profileStats.totalLikeCount = profile.totalLikeCount || 0;
        profileStats.followerCount = profile.followerCount || 0;
        profileStats.followingCount = profile.followingCount || 0;
    } catch (_) {
        // ignore
    } finally {
        statsLoading.value = false;
    }
};

const loadHotArticles = async () => {
    if (!state.user?.id) {
        hotArticles.value = [];
        return;
    }
    hotArticlesLoading.value = true;
    hotArticlesError.value = '';
    try {
        hotArticles.value = await getUserHotArticlesApi(state.user.id, 3);
    } catch (error) {
        hotArticlesError.value = error.message || '热门文章加载失败';
    } finally {
        hotArticlesLoading.value = false;
    }
};

const loadSecurityInfo = async () => {
    securityLoading.value = true;
    try {
        securityInfo.value = await getSecurityInfoApi();
    } catch (_) {
        // ignore
    } finally {
        securityLoading.value = false;
    }
};

const getFieldValue = (field) => draft[field] || '';
const getOriginalValue = (field) => originalProfile[field] || '';
const isEditingField = (field) => activeField.value === field;
const isFieldDirty = (field) => getFieldValue(field) !== getOriginalValue(field);

const startEditField = (field) => {
    if (loading.value) {
        return;
    }
    if (activeField.value && activeField.value !== field && isFieldDirty(activeField.value)) {
        fieldFeedback.value = `${FIELD_LABELS[activeField.value]}还有未保存内容，请先保存或取消`;
        fieldFeedbackType.value = 'warning';
        return;
    }
    fieldFeedback.value = '';
    activeField.value = field;
};

const cancelEditField = (field) => {
    draft[field] = getOriginalValue(field);
    if (field === 'avatarUrl') {
        debouncedAvatarUrl.value = getOriginalValue(field);
    }
    activeField.value = '';
    fieldFeedback.value = '';
};

const buildPayload = () => ({
    nickname: draft.nickname.trim(),
    avatarUrl: draft.avatarUrl.trim(),
    bio: draft.bio.trim(),
    website: draft.website.trim(),
    github: draft.github.trim(),
    twitter: draft.twitter.trim(),
    location: draft.location.trim()
});

const saveField = async (field) => {
    if (loading.value) {
        return;
    }
    loading.value = true;
    fieldFeedback.value = '';
    try {
        const user = await updateProfileApi(buildPayload());
        updateCurrentUser(user);
        syncFromSession();
        activeField.value = '';
        fieldFeedback.value = `${FIELD_LABELS[field]}已更新`;
        fieldFeedbackType.value = 'success';
        await Promise.all([loadProfileStats(), loadHotArticles()]);
    } catch (error) {
        fieldFeedback.value = error.message || `${FIELD_LABELS[field]}保存失败`;
        fieldFeedbackType.value = 'error';
    } finally {
        loading.value = false;
    }
};

const triggerAvatarPicker = () => {
    if (loading.value || avatarUploading.value) {
        return;
    }
    avatarInputRef.value?.click();
};

const handleAvatarSelected = async (event) => {
    const [file] = event.target?.files || [];
    event.target.value = '';
    if (!file) {
        return;
    }
    avatarUploading.value = true;
    fieldFeedback.value = '';
    try {
        const result = await uploadImageApi(file, 'avatar');
        draft.avatarUrl = result.url || '';
        debouncedAvatarUrl.value = result.url || '';
        avatarPreviewFailed.value = false;
        fieldFeedback.value = '图片已上传，保存资料后永久生效';
        fieldFeedbackType.value = 'success';
        if (!isEditingField('avatarUrl')) {
            activeField.value = 'avatarUrl';
        }
    } catch (error) {
        fieldFeedback.value = error.message || '头像上传失败';
        fieldFeedbackType.value = 'error';
    } finally {
        avatarUploading.value = false;
    }
};

const handleAvatarPreviewLoad = () => {
    avatarPreviewFailed.value = false;
};

const handleAvatarPreviewError = () => {
    if (!rawAvatar.value) {
        return;
    }
    avatarPreviewFailed.value = true;
    if (isEditingField('avatarUrl')) {
        fieldFeedback.value = '头像图片暂时无法访问，请检查链接或稍后重试';
        fieldFeedbackType.value = 'error';
    }
};

// ── 账号安全 - 修改密码 ──────────────────────────────────────────────────────
const changePassword = async () => {
    pwdError.value = '';
    pwdSuccess.value = '';
    if (!pwdForm.currentPassword) {
        pwdError.value = '请输入当前密码';
        return;
    }
    if (pwdForm.newPassword.length < 6) {
        pwdError.value = '新密码至少 6 位';
        return;
    }
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
        pwdError.value = '两次输入的新密码不一致';
        return;
    }
    pwdLoading.value = true;
    try {
        await changePasswordApi(pwdForm.currentPassword, pwdForm.newPassword);
        pwdSuccess.value = '密码已修改成功';
        pwdForm.currentPassword = '';
        pwdForm.newPassword = '';
        pwdForm.confirmPassword = '';
    } catch (error) {
        pwdError.value = error.message || '修改密码失败';
    } finally {
        pwdLoading.value = false;
    }
};

const changeEmail = async () => {
    emailError.value = '';
    emailSuccess.value = '';
    if (!emailForm.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailForm.email.trim())) {
        emailError.value = '请输入正确的邮箱格式';
        return;
    }
    if (!emailForm.password) {
        emailError.value = '请输入当前密码以确认身份';
        return;
    }
    emailLoading.value = true;
    try {
        const updated = await changeEmailApi(emailForm.email.trim(), emailForm.password);
        if (updated) {
            updateCurrentUser(updated);
        }
        emailSuccess.value = '邮箱已绑定/更换成功';
        emailForm.password = '';
        if (securityInfo.value) {
            securityInfo.value = { ...securityInfo.value, email: emailForm.email.trim() };
        }
    } catch (error) {
        emailError.value = error.message || '邮箱绑定失败';
    } finally {
        emailLoading.value = false;
    }
};

const switchTab = (tab) => {
    activeTab.value = tab;
    if (tab === 'security' && !securityInfo.value) {
        loadSecurityInfo();
    }
};

onMounted(async () => {
    syncFromSession();
    await Promise.all([loadProfileStats(), loadHotArticles()]);
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell profile-settings-page">
        <UserProfileSummary
            mode="settings"
            eyebrow="账户中心"
            :avatar-src="displayAvatar"
            avatar-alt="当前用户头像"
            :title="displayNickname"
            :subtitle="`@${displayUsername}`"
            :bio="displayBio"
            bio-fallback="这里会展示你的个人简介与创作方向。"
            :helper-text="'这里的资料会同步显示在文章作者信息和个人主页。'"
            :badge-text="roleLabel"
            :stats="summaryStats"
            @avatar-load="handleAvatarPreviewLoad"
            @avatar-error="handleAvatarPreviewError"
        >
            <template #actions>
                <RouterLink
                    v-if="state.user?.id"
                    class="profile-secondary-action"
                    :to="`/users/${state.user.id}`"
                >
                    查看个人主页
                </RouterLink>
            </template>
        </UserProfileSummary>

        <!-- Tab 切换 -->
        <nav class="settings-tabs" role="tablist" aria-label="设置分类">
            <button
                role="tab"
                class="settings-tab"
                :class="{ active: activeTab === 'profile' }"
                :aria-selected="activeTab === 'profile'"
                type="button"
                @click="switchTab('profile')"
            >
                <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                    <circle cx="10" cy="7" r="3.5" stroke="currentColor" stroke-width="1.4"/>
                    <path d="M3 17c0-3.3 3.1-6 7-6s7 2.7 7 6" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
                </svg>
                个人资料
            </button>
            <button
                role="tab"
                class="settings-tab"
                :class="{ active: activeTab === 'security' }"
                :aria-selected="activeTab === 'security'"
                type="button"
                @click="switchTab('security')"
            >
                <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                    <path d="M10 2.5 3.5 5.5v4.25c0 3.75 2.8 7.25 6.5 8.25 3.7-1 6.5-4.5 6.5-8.25V5.5L10 2.5Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/>
                    <path d="m7.5 10 1.75 1.75 3.5-3.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                账号安全
            </button>
        </nav>

        <!-- 个人资料 Tab -->
        <div v-if="activeTab === 'profile'" class="settings-tab-panel">
            <section class="profile-settings-grid">
                <section class="profile-settings-panel">
                    <div class="profile-settings-panel-head">
                        <div>
                            <p class="eyebrow">基础信息</p>
                            <h2>编辑公开资料</h2>
                        </div>
                        <p>每次只编辑一个字段，会更清楚地知道本次修改影响的内容。</p>
                    </div>

                    <div class="profile-field-list">
                        <!-- 昵称 -->
                        <article class="profile-field-card">
                            <div class="profile-field-head">
                                <div>
                                    <h3>昵称</h3>
                                    <p>将显示在文章作者信息与个人主页中。</p>
                                </div>
                                <button v-if="!isEditingField('nickname')" type="button" class="field-edit-button" @click="startEditField('nickname')">
                                    <span class="field-edit-button-icon" aria-hidden="true">
                                        <svg viewBox="0 0 20 20" fill="none"><path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                                    </span>
                                    编辑
                                </button>
                            </div>
                            <template v-if="isEditingField('nickname')">
                                <div class="profile-field-editor">
                                    <input v-model.trim="draft.nickname" type="text" placeholder="请输入昵称">
                                    <div class="profile-field-actions">
                                        <button class="primary-action profile-action-with-icon" type="button" :disabled="loading" @click="saveField('nickname')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
                                            {{ loading ? '保存中...' : '保存' }}
                                        </button>
                                        <button type="button" class="profile-action-with-icon secondary" :disabled="loading" @click="cancelEditField('nickname')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></span>
                                            取消
                                        </button>
                                    </div>
                                </div>
                            </template>
                            <p v-else class="profile-field-value">{{ getFieldValue('nickname') || '暂未设置昵称' }}</p>
                        </article>

                        <!-- 头像 -->
                        <article class="profile-field-card">
                            <div class="profile-field-head">
                                <div>
                                    <h3>头像</h3>
                                    <p>建议优先上传头像图片，必要时也可以补充公开图片链接。</p>
                                </div>
                                <button v-if="!isEditingField('avatarUrl')" type="button" class="field-edit-button" @click="startEditField('avatarUrl')">
                                    <span class="field-edit-button-icon" aria-hidden="true">
                                        <svg viewBox="0 0 20 20" fill="none"><path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                                    </span>
                                    编辑
                                </button>
                            </div>
                            <template v-if="isEditingField('avatarUrl')">
                                <div class="profile-field-editor">
                                    <input ref="avatarInputRef" type="file" accept="image/*" class="sr-only" @change="handleAvatarSelected">
                                    <div class="profile-upload-row">
                                        <button type="button" class="profile-action-with-icon secondary" :disabled="loading || avatarUploading" @click="triggerAvatarPicker">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="M10 4v8m-4-4h8m-7 7h6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
                                            {{ avatarUploading ? '上传中...' : '上传图片' }}
                                        </button>
                                        <span class="profile-upload-tip">支持 jpg、png、gif、webp，头像建议不超过 2MB。</span>
                                    </div>
                                    <label class="profile-secondary-field">
                                        <span>公开图片链接（可选）</span>
                                        <input v-model.trim="draft.avatarUrl" type="url" placeholder="/api/uploads/files/2026/04/avatar.png 或 https://example.com/avatar.png">
                                    </label>
                                    <div class="profile-field-actions">
                                        <button class="primary-action profile-action-with-icon" type="button" :disabled="loading || avatarUploading" @click="saveField('avatarUrl')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
                                            {{ loading ? '保存中...' : '保存' }}
                                        </button>
                                        <button type="button" class="profile-action-with-icon secondary" :disabled="loading || avatarUploading" @click="cancelEditField('avatarUrl')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></span>
                                            取消
                                        </button>
                                    </div>
                                </div>
                            </template>
                            <p v-else class="profile-field-value profile-field-value-link">{{ getFieldValue('avatarUrl') || '暂未设置头像' }}</p>
                        </article>

                        <!-- 个人简介 -->
                        <article class="profile-field-card">
                            <div class="profile-field-head">
                                <div>
                                    <h3>个人简介</h3>
                                    <p>一句话介绍你当前关注的方向、擅长内容或正在构建的项目。</p>
                                </div>
                                <button v-if="!isEditingField('bio')" type="button" class="field-edit-button" @click="startEditField('bio')">
                                    <span class="field-edit-button-icon" aria-hidden="true">
                                        <svg viewBox="0 0 20 20" fill="none"><path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                                    </span>
                                    编辑
                                </button>
                            </div>
                            <template v-if="isEditingField('bio')">
                                <div class="profile-field-editor">
                                    <textarea v-model.trim="draft.bio" rows="5" placeholder="介绍一下你正在关注的技术方向"></textarea>
                                    <div class="profile-field-actions">
                                        <button class="primary-action profile-action-with-icon" type="button" :disabled="loading" @click="saveField('bio')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
                                            {{ loading ? '保存中...' : '保存' }}
                                        </button>
                                        <button type="button" class="profile-action-with-icon secondary" :disabled="loading" @click="cancelEditField('bio')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></span>
                                            取消
                                        </button>
                                    </div>
                                </div>
                            </template>
                            <p v-else class="profile-field-value">{{ getFieldValue('bio') || '暂未设置个人简介' }}</p>
                        </article>
                    </div>

                    <div class="profile-settings-panel-head" style="margin-top: 8px;">
                        <div>
                            <p class="eyebrow">社交主页</p>
                            <h2>对外展示链接</h2>
                        </div>
                        <p>填写后会在个人主页公开展示，帮助读者了解你。</p>
                    </div>

                    <div class="profile-field-list">
                        <!-- 所在地 -->
                        <article class="profile-field-card">
                            <div class="profile-field-head">
                                <div>
                                    <h3>所在地</h3>
                                    <p>城市或地区，如「上海」「北京」。</p>
                                </div>
                                <button v-if="!isEditingField('location')" type="button" class="field-edit-button" @click="startEditField('location')">
                                    <span class="field-edit-button-icon" aria-hidden="true">
                                        <svg viewBox="0 0 20 20" fill="none"><path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                                    </span>
                                    编辑
                                </button>
                            </div>
                            <template v-if="isEditingField('location')">
                                <div class="profile-field-editor">
                                    <input v-model.trim="draft.location" type="text" placeholder="例如：上海">
                                    <div class="profile-field-actions">
                                        <button class="primary-action profile-action-with-icon" type="button" :disabled="loading" @click="saveField('location')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
                                            {{ loading ? '保存中...' : '保存' }}
                                        </button>
                                        <button type="button" class="profile-action-with-icon secondary" :disabled="loading" @click="cancelEditField('location')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></span>
                                            取消
                                        </button>
                                    </div>
                                </div>
                            </template>
                            <p v-else class="profile-field-value">{{ getFieldValue('location') || '暂未设置' }}</p>
                        </article>

                        <!-- 个人网站 -->
                        <article class="profile-field-card">
                            <div class="profile-field-head">
                                <div>
                                    <h3>个人网站</h3>
                                    <p>你的博客、作品集或公开主页链接。</p>
                                </div>
                                <button v-if="!isEditingField('website')" type="button" class="field-edit-button" @click="startEditField('website')">
                                    <span class="field-edit-button-icon" aria-hidden="true">
                                        <svg viewBox="0 0 20 20" fill="none"><path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                                    </span>
                                    编辑
                                </button>
                            </div>
                            <template v-if="isEditingField('website')">
                                <div class="profile-field-editor">
                                    <input v-model.trim="draft.website" type="url" placeholder="https://yoursite.com">
                                    <div class="profile-field-actions">
                                        <button class="primary-action profile-action-with-icon" type="button" :disabled="loading" @click="saveField('website')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
                                            {{ loading ? '保存中...' : '保存' }}
                                        </button>
                                        <button type="button" class="profile-action-with-icon secondary" :disabled="loading" @click="cancelEditField('website')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></span>
                                            取消
                                        </button>
                                    </div>
                                </div>
                            </template>
                            <p v-else class="profile-field-value">{{ getFieldValue('website') || '暂未设置' }}</p>
                        </article>

                        <!-- GitHub -->
                        <article class="profile-field-card">
                            <div class="profile-field-head">
                                <div>
                                    <h3>GitHub</h3>
                                    <p>只填用户名，如 <code>octocat</code>，链接会自动生成。</p>
                                </div>
                                <button v-if="!isEditingField('github')" type="button" class="field-edit-button" @click="startEditField('github')">
                                    <span class="field-edit-button-icon" aria-hidden="true">
                                        <svg viewBox="0 0 20 20" fill="none"><path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                                    </span>
                                    编辑
                                </button>
                            </div>
                            <template v-if="isEditingField('github')">
                                <div class="profile-field-editor">
                                    <input v-model.trim="draft.github" type="text" placeholder="GitHub 用户名">
                                    <div class="profile-field-actions">
                                        <button class="primary-action profile-action-with-icon" type="button" :disabled="loading" @click="saveField('github')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
                                            {{ loading ? '保存中...' : '保存' }}
                                        </button>
                                        <button type="button" class="profile-action-with-icon secondary" :disabled="loading" @click="cancelEditField('github')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></span>
                                            取消
                                        </button>
                                    </div>
                                </div>
                            </template>
                            <p v-else class="profile-field-value">{{ getFieldValue('github') || '暂未设置' }}</p>
                        </article>

                        <!-- Twitter -->
                        <article class="profile-field-card">
                            <div class="profile-field-head">
                                <div>
                                    <h3>Twitter / X</h3>
                                    <p>只填用户名，如 <code>jack</code>，链接会自动生成。</p>
                                </div>
                                <button v-if="!isEditingField('twitter')" type="button" class="field-edit-button" @click="startEditField('twitter')">
                                    <span class="field-edit-button-icon" aria-hidden="true">
                                        <svg viewBox="0 0 20 20" fill="none"><path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                                    </span>
                                    编辑
                                </button>
                            </div>
                            <template v-if="isEditingField('twitter')">
                                <div class="profile-field-editor">
                                    <input v-model.trim="draft.twitter" type="text" placeholder="Twitter 用户名">
                                    <div class="profile-field-actions">
                                        <button class="primary-action profile-action-with-icon" type="button" :disabled="loading" @click="saveField('twitter')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
                                            {{ loading ? '保存中...' : '保存' }}
                                        </button>
                                        <button type="button" class="profile-action-with-icon secondary" :disabled="loading" @click="cancelEditField('twitter')">
                                            <span class="profile-action-icon" aria-hidden="true"><svg viewBox="0 0 20 20" fill="none"><path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg></span>
                                            取消
                                        </button>
                                    </div>
                                </div>
                            </template>
                            <p v-else class="profile-field-value">{{ getFieldValue('twitter') || '暂未设置' }}</p>
                        </article>
                    </div>

                    <div class="profile-settings-panel-footer">
                        <p v-if="fieldFeedback" :class="['form-message', fieldFeedbackType]">{{ fieldFeedback }}</p>
                        <p v-else class="profile-settings-panel-tip">修改后会同步显示在文章作者信息、评论区和个人主页。</p>
                    </div>
                </section>

                <section class="profile-settings-panel profile-settings-hot">
                    <div class="profile-settings-panel-head">
                        <div>
                            <p class="eyebrow">我的热门文章</p>
                            <h2>最能代表你的内容</h2>
                        </div>
                        <p>按阅读优先、点赞次之的热度排序，帮助你快速回看最受欢迎的内容。</p>
                    </div>

                    <div v-if="hotArticlesLoading && hotArticles.length" class="profile-hot-state subtle">正在更新热门文章...</div>
                    <div v-if="hotArticlesError && hotArticles.length" class="profile-hot-state error-text subtle">{{ hotArticlesError }}</div>
                    <div v-if="hotArticlesLoading && !hotArticles.length" class="profile-hot-state">热门文章加载中...</div>
                    <div v-else-if="hotArticlesError && !hotArticles.length" class="profile-hot-state error-text">{{ hotArticlesError }}</div>
                    <div v-else-if="!hotArticles.length" class="profile-hot-state">还没有足够热度的文章，先去发布第一篇内容。</div>
                    <div v-else class="profile-hot-list">
                        <RouterLink v-for="article in hotArticles" :key="article.id" class="profile-hot-item" :to="`/articles/${article.id}`">
                            <div
                                class="profile-hot-cover"
                                :style="article.coverUrl ? { backgroundImage: `url(${resolveMediaUrl(article.coverUrl, article.cover)})` } : undefined"
                            >
                                <span class="profile-hot-category">{{ article.category || '未分类' }}</span>
                                <span class="profile-hot-rank">TOP {{ hotArticles.findIndex((item) => item.id === article.id) + 1 }}</span>
                            </div>
                            <div class="profile-hot-body">
                                <h3>{{ article.title }}</h3>
                                <div class="profile-hot-meta">
                                    <span>
                                        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M2.5 10s2.8-5 7.5-5 7.5 5 7.5 5-2.8 5-7.5 5-7.5-5-7.5-5Z" stroke="currentColor" stroke-width="1.5"/><circle cx="10" cy="10" r="2.25" stroke="currentColor" stroke-width="1.5"/></svg>
                                        {{ article.viewCount }}
                                    </span>
                                    <span>
                                        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="m10 15.5-4.625-4.463a2.917 2.917 0 0 1 4.125-4.119L10 7.413l.5-.495a2.917 2.917 0 1 1 4.125 4.119L10 15.5Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
                                        {{ article.likeCount }}
                                    </span>
                                </div>
                            </div>
                        </RouterLink>
                    </div>
                </section>
            </section>
        </div>

        <div v-if="activeTab === 'security'" class="settings-tab-panel">
            <section class="profile-settings-grid">
                <!-- 修改密码 -->
                <section class="profile-settings-panel">
                    <div class="profile-settings-panel-head">
                        <div>
                            <p class="eyebrow">账号安全</p>
                            <h2>修改密码</h2>
                        </div>
                        <p>定期更换密码，避免使用与其他平台相同的密码。</p>
                    </div>

                    <form class="security-form" @submit.prevent="changePassword">
                        <label class="security-field">
                            <span>当前密码</span>
                            <input v-model="pwdForm.currentPassword" type="password" placeholder="请输入当前密码" autocomplete="current-password">
                        </label>
                        <label class="security-field">
                            <span>新密码</span>
                            <input v-model="pwdForm.newPassword" type="password" placeholder="至少 6 位" autocomplete="new-password">
                        </label>
                        <label class="security-field">
                            <span>确认新密码</span>
                            <input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" autocomplete="new-password">
                        </label>
                        <div class="security-form-actions">
                            <button class="primary-action" type="submit" :disabled="pwdLoading">
                                {{ pwdLoading ? '修改中...' : '修改密码' }}
                            </button>
                        </div>
                        <p v-if="pwdError" class="form-message error">{{ pwdError }}</p>
                        <p v-if="pwdSuccess" class="form-message success">{{ pwdSuccess }}</p>
                    </form>

                    <div class="security-divider"></div>

                    <!-- 绑定邮箱 -->
                    <div class="security-section">
                        <h3 class="security-sub-title">绑定 / 更换邮箱</h3>
                        <p class="security-sub-desc">邮箱用于密码找回和重要通知，需输入当前密码以确认身份。</p>
                        <form class="security-form" @submit.prevent="changeEmail">
                            <label class="security-field">
                                <span>新邮箱</span>
                                <input v-model.trim="emailForm.email" type="email" placeholder="请输入新邮箱" autocomplete="email">
                            </label>
                            <label class="security-field">
                                <span>当前密码</span>
                                <input v-model="emailForm.password" type="password" placeholder="请输入当前密码确认身份" autocomplete="current-password">
                            </label>
                            <div class="security-form-actions">
                                <button class="primary-action" type="submit" :disabled="emailLoading">
                                    {{ emailLoading ? '绑定中...' : '确认绑定' }}
                                </button>
                            </div>
                            <p v-if="emailError" class="form-message error">{{ emailError }}</p>
                            <p v-if="emailSuccess" class="form-message success">{{ emailSuccess }}</p>
                        </form>
                    </div>

                    <div class="security-divider"></div>

                    <div class="security-tip-box">
                        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                            <circle cx="10" cy="10" r="7.5" stroke="currentColor" stroke-width="1.4"/>
                            <path d="M10 9v5M10 7v.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
                        </svg>
                        <p>忘记当前密码？可以通过邮箱重置。<RouterLink to="/auth/forgot-password" class="security-tip-link">前往找回密码</RouterLink></p>
                    </div>
                </section>

                <!-- 登录记录 -->
                <section class="profile-settings-panel profile-settings-hot">
                    <div class="profile-settings-panel-head">
                        <div>
                            <p class="eyebrow">安全记录</p>
                            <h2>最近登录</h2>
                        </div>
                        <p>若发现异常登录，请立即修改密码。</p>
                    </div>

                    <div v-if="securityLoading" class="profile-hot-state">加载中...</div>
                    <template v-else-if="securityInfo">
                        <div class="security-info-list">
                            <div class="security-info-item">
                                <span class="security-info-label">上次登录时间</span>
                                <span>{{ securityInfo.lastLoginAt || '暂无记录' }}</span>
                            </div>
                            <div class="security-info-item">
                                <span class="security-info-label">上次登录 IP</span>
                                <span>{{ securityInfo.lastLoginIp || '暂无记录' }}</span>
                            </div>
                            <div class="security-info-item">
                                <span class="security-info-label">注册邮箱</span>
                                <span class="security-info-email">{{ securityInfo.email || '暂无' }}</span>
                            </div>
                            <div class="security-info-item">
                                <span class="security-info-label">账号状态</span>
                                <span>
                                    <span class="security-badge" :class="securityInfo.status === 'NORMAL' ? 'active' : 'inactive'">
                                        {{ securityInfo.status === 'NORMAL' ? '正常' : '受限' }}
                                    </span>
                                </span>
                            </div>
                        </div>
                    </template>
                    <div v-else class="profile-hot-state">暂无安全信息</div>
                </section>
            </section>
        </div>
    </main>
</template>

<style scoped>
.profile-settings-page {
    display: grid;
    gap: 24px;
}

/* ── Tab 导航 ─────────────────────────────────────────────────────── */
.settings-tabs {
    display: flex;
    gap: 4px;
    padding: 6px;
    background: var(--surface-muted, #f6f8fa);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    width: fit-content;
}

.settings-tab {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: 36px;
    padding: 0 16px;
    font-size: 14px;
    font-weight: 500;
    color: var(--muted);
    background: transparent;
    border: 1px solid transparent;
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: color 0.15s, background 0.15s, border-color 0.15s;
}

.settings-tab svg {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
}

.settings-tab:hover {
    color: var(--text);
    background: var(--surface);
}

.settings-tab.active {
    color: var(--brand);
    background: var(--surface);
    border-color: var(--line);
    font-weight: 600;
}

/* ── Panel ────────────────────────────────────────────────────────── */
.profile-settings-panel {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
}

.profile-secondary-action {
    display: inline-flex;
    flex: 0 0 auto;
    align-items: center;
    justify-content: center;
    min-height: 40px;
    padding: 0 16px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.profile-secondary-action:hover {
    color: var(--brand);
    border-color: var(--brand);
    background: var(--brand-soft);
}

.profile-settings-grid {
    display: grid;
    grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
    gap: 24px;
    align-items: start;
}

.profile-settings-panel {
    display: grid;
    gap: 24px;
    padding: 28px;
}

.profile-settings-panel-head {
    display: grid;
    gap: 8px;
}

.profile-settings-panel-head h2 {
    margin: 0;
    font-size: 24px;
}

.profile-settings-panel-head p {
    margin: 0;
    color: var(--muted);
    line-height: 1.7;
}

.profile-field-list {
    display: grid;
    gap: 16px;
}

.profile-field-card {
    display: grid;
    gap: 16px;
    padding: 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: border-color 0.22s ease;
}

.profile-field-card:hover {
    border-color: var(--brand);
}

.profile-field-head {
    display: flex;
    gap: 12px;
    align-items: start;
    justify-content: space-between;
}

.profile-field-head h3 {
    margin: 0 0 6px;
    font-size: 18px;
}

.profile-field-head p {
    margin: 0;
    color: var(--muted);
    line-height: 1.7;
}

.field-edit-button,
.profile-field-actions button:not(.primary-action) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 34px;
    padding: 0 12px;
    color: var(--brand);
    font-size: 13px;
    font-weight: 700;
    cursor: pointer;
    background: var(--brand-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: border-color 0.18s ease, background 0.18s ease;
}

.field-edit-button-icon,
.profile-action-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 14px;
    height: 14px;
}

.field-edit-button-icon svg,
.profile-action-icon svg,
.profile-hot-meta svg {
    width: 100%;
    height: 100%;
}

.field-edit-button:hover,
.profile-field-actions button:not(.primary-action):hover {
    border-color: var(--brand);
    background: var(--brand-hover);
}

.profile-field-value {
    margin: 0;
    color: var(--text);
    line-height: 1.8;
    white-space: pre-wrap;
    word-break: break-word;
}

.profile-field-value-link {
    color: var(--muted);
}

.profile-field-card input,
.profile-field-card textarea {
    width: 100%;
    padding: 12px 14px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.profile-field-card input:focus,
.profile-field-card textarea:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
    outline: none;
}

.profile-field-editor {
    display: grid;
    gap: 14px;
    animation: fade-slide-up 0.24s ease;
}

.profile-secondary-field {
    display: grid;
    gap: 8px;
}

.profile-secondary-field span {
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
}

.profile-field-actions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.profile-action-with-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
}

.profile-action-with-icon.secondary {
    color: var(--text);
    background: var(--surface);
    border-color: var(--line);
    border-radius: var(--radius-md);
}

.profile-settings-panel-footer {
    display: grid;
    gap: 10px;
}

.profile-settings-panel-tip {
    margin: 0;
    color: var(--muted);
    line-height: 1.7;
}

/* ── 热门文章 ──────────────────────────────────────────────────────── */
.profile-hot-state {
    color: var(--muted);
    line-height: 1.7;
}

.profile-hot-state.subtle {
    width: fit-content;
    padding: 6px 10px;
    color: var(--brand-strong);
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-sm);
}

.profile-hot-list {
    display: grid;
    gap: 14px;
}

.profile-hot-item {
    display: grid;
    overflow: hidden;
    color: inherit;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: border-color 0.18s ease;
}

.profile-hot-item:hover {
    border-color: var(--brand);
}

.profile-hot-cover {
    position: relative;
    display: flex;
    align-items: start;
    justify-content: space-between;
    min-height: 118px;
    padding: 16px;
    background: var(--surface-muted);
    background-size: cover;
    background-position: center;
}

.profile-hot-category,
.profile-hot-rank {
    display: inline-flex;
    align-items: center;
    min-height: 26px;
    padding: 0 10px;
    color: #ffffff;
    font-size: 12px;
    font-weight: 700;
    background: rgba(0, 0, 0, 0.45);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: var(--radius-sm);
}

.profile-hot-body {
    display: grid;
    gap: 14px;
    padding: 18px;
}

.profile-hot-body h3 {
    margin: 0;
    font-size: 18px;
    line-height: 1.55;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.profile-hot-meta {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    color: var(--muted);
    font-size: 13px;
}

.profile-hot-meta span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
}

.profile-hot-meta svg {
    width: 14px;
    height: 14px;
}

.profile-upload-row {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
}

.profile-upload-tip {
    color: var(--muted);
    font-size: 13px;
}

/* ── 账号安全 ──────────────────────────────────────────────────────── */
.security-form {
    display: grid;
    gap: 16px;
}

.security-field {
    display: grid;
    gap: 8px;
}

.security-field span {
    font-size: 13px;
    font-weight: 600;
    color: var(--text);
}

.security-field input {
    width: 100%;
    padding: 10px 14px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    font-size: 14px;
}

.security-field input:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
    outline: none;
}

.security-form-actions {
    display: flex;
    gap: 12px;
}

.security-divider {
    height: 1px;
    background: var(--line);
    margin: 4px 0;
}

.security-tip-box {
    display: flex;
    gap: 10px;
    align-items: flex-start;
    padding: 14px 16px;
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-sm);
    color: var(--text);
    font-size: 14px;
    line-height: 1.7;
}

.security-tip-box svg {
    width: 18px;
    height: 18px;
    color: var(--brand);
    flex-shrink: 0;
    margin-top: 2px;
}

.security-tip-box p {
    margin: 0;
}

.security-tip-link {
    color: var(--brand);
    text-decoration: none;
    font-weight: 600;
}

.security-tip-link:hover {
    text-decoration: underline;
}

.security-info-list {
    display: grid;
    gap: 1px;
    background: var(--line);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    overflow: hidden;
    margin: 0;
}

.security-info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    background: var(--surface);
    flex-wrap: wrap;
}

.security-info-item dt,
.security-info-item .security-info-label {
    color: var(--muted);
    font-size: 13px;
    font-weight: 500;
    min-width: 100px;
}

.security-info-item dd,
.security-info-item > span:last-child {
    color: var(--text);
    font-size: 14px;
    font-weight: 500;
    text-align: right;
    margin: 0;
    word-break: break-all;
}

.security-info-email {
    font-family: monospace;
}

.security-badge {
    display: inline-flex;
    align-items: center;
    min-height: 24px;
    padding: 0 10px;
    font-size: 12px;
    font-weight: 700;
    border-radius: var(--radius-sm);
}

.security-badge.active {
    color: #16a34a;
    background: rgba(22, 163, 74, 0.08);
    border: 1px solid rgba(22, 163, 74, 0.2);
}

.security-badge.inactive {
    color: var(--accent);
    background: rgba(220, 38, 38, 0.06);
    border: 1px solid rgba(220, 38, 38, 0.2);
}

/* ── 动画 ─────────────────────────────────────────────────────────── */
@keyframes fade-slide-up {
    from {
        opacity: 0;
        transform: translateY(6px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@media (max-width: 980px) {
    .profile-settings-grid {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 760px) {
    .profile-settings-panel {
        padding: 22px 18px;
    }

    .profile-field-head {
        flex-direction: column;
    }

    .settings-tabs {
        width: 100%;
    }

    .settings-tab {
        flex: 1;
        justify-content: center;
    }
}

</style>
