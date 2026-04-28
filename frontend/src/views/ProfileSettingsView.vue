<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue';
import {RouterLink} from 'vue-router';
import {uploadImageApi} from '@/api/uploads';
import SiteHeader from '@/components/SiteHeader.vue';
import UserProfileSummary from '@/components/UserProfileSummary.vue';
import {getUserHotArticlesApi, getUserProfileApi, updateProfileApi} from '@/api/auth';
import {useSession} from '@/stores/session';
import {buildProfileSummaryStats} from '@/utils/profileSummary';
import {resolveMediaUrl} from '@/utils/media';

const { state, updateCurrentUser } = useSession();
const DEFAULT_AVATAR = 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=240&q=80';

const loading = ref(false);
const statsLoading = ref(false);
const hotArticlesLoading = ref(false);
const hotArticlesError = ref('');
const feedback = ref('');
const feedbackType = ref('info');
const activeField = ref('');
const fieldFeedback = ref('');
const fieldFeedbackType = ref('info');
const avatarUploading = ref(false);
const avatarInputRef = ref(null);
const avatarPreviewFailed = ref(false);
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
    bio: ''
});
const originalProfile = reactive({
    nickname: '',
    avatarUrl: '',
    bio: ''
});
const hotArticles = ref([]);

const FIELD_LABELS = {
    nickname: '昵称',
    avatarUrl: '头像',
    bio: '个人简介'
};

const roleLabel = computed(() => (state.user?.role === 'ADMIN' ? '管理员' : '普通用户'));
const rawAvatar = computed(() => draft.avatarUrl || state.user?.avatarUrl || state.user?.avatar || '');
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

/**
 * 同步当前会话用户资料到页面草稿。
 */
const syncFromSession = () => {
    draft.nickname = state.user?.nickname || '';
    draft.avatarUrl = state.user?.avatarUrl || state.user?.avatar || '';
    draft.bio = state.user?.bio || '';
    avatarPreviewFailed.value = false;
    originalProfile.nickname = draft.nickname;
    originalProfile.avatarUrl = draft.avatarUrl;
    originalProfile.bio = draft.bio;
};

/**
 * 加载当前用户统计信息。
 */
const loadProfileStats = async () => {
    if (!state.user?.id) {
        profileStats.articleCount = 0;
        profileStats.totalViewCount = 0;
        profileStats.totalLikeCount = 0;
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
    } catch (error) {
        profileStats.articleCount = 0;
        profileStats.totalViewCount = 0;
        profileStats.totalLikeCount = 0;
        profileStats.followerCount = 0;
        profileStats.followingCount = 0;
    } finally {
        statsLoading.value = false;
    }
};

/**
 * 加载当前用户热门文章。
 */
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

/**
 * 获取字段当前值。
 *
 * @param {string} field 字段名
 * @returns {string} 字段值
 */
const getFieldValue = (field) => draft[field] || '';

/**
 * 获取字段原始值。
 *
 * @param {string} field 字段名
 * @returns {string} 原始值
 */
const getOriginalValue = (field) => originalProfile[field] || '';

/**
 * 判断字段是否处于编辑态。
 *
 * @param {string} field 字段名
 * @returns {boolean} 是否编辑中
 */
const isEditingField = (field) => activeField.value === field;

/**
 * 判断字段是否有未保存改动。
 *
 * @param {string} field 字段名
 * @returns {boolean} 是否已修改
 */
const isFieldDirty = (field) => getFieldValue(field) !== getOriginalValue(field);

/**
 * 开始编辑指定字段。
 *
 * @param {string} field 字段名
 */
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

/**
 * 取消编辑指定字段。
 *
 * @param {string} field 字段名
 */
const cancelEditField = (field) => {
    draft[field] = getOriginalValue(field);
    activeField.value = '';
    fieldFeedback.value = '';
};

/**
 * 构建提交资料。
 *
 * @returns {{nickname: string, avatarUrl: string, bio: string}} 提交参数
 */
const buildPayload = () => ({
    nickname: draft.nickname.trim(),
    avatarUrl: draft.avatarUrl.trim(),
    bio: draft.bio.trim()
});

/**
 * 保存单个字段。
 *
 * @param {string} field 字段名
 */
const saveField = async (field) => {
    if (loading.value) {
        return;
    }
    loading.value = true;
    feedback.value = '';
    fieldFeedback.value = '';
    try {
        const user = await updateProfileApi(buildPayload());
        updateCurrentUser(user);
        syncFromSession();
        activeField.value = '';
        feedback.value = `${FIELD_LABELS[field]}已更新`;
        feedbackType.value = 'success';
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
                    <article class="profile-field-card">
                        <div class="profile-field-head">
                            <div>
                                <h3>昵称</h3>
                                <p>将显示在文章作者信息与个人主页中。</p>
                            </div>
                            <button
                                v-if="!isEditingField('nickname')"
                                type="button"
                                class="field-edit-button"
                                @click="startEditField('nickname')"
                            >
                                <span class="field-edit-button-icon" aria-hidden="true">
                                    <svg viewBox="0 0 20 20" fill="none">
                                        <path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                                    </svg>
                                </span>
                                编辑
                            </button>
                        </div>
                        <template v-if="isEditingField('nickname')">
                            <div class="profile-field-editor">
                                <input v-model.trim="draft.nickname" type="text" placeholder="请输入昵称">
                                <div class="profile-field-actions">
                                    <button class="primary-action profile-action-with-icon" type="button" :disabled="loading" @click="saveField('nickname')">
                                        <span class="profile-action-icon" aria-hidden="true">
                                            <svg viewBox="0 0 20 20" fill="none">
                                                <path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
                                            </svg>
                                        </span>
                                        {{ loading ? '保存中...' : '保存' }}
                                    </button>
                                    <button type="button" class="profile-action-with-icon secondary" :disabled="loading" @click="cancelEditField('nickname')">
                                        <span class="profile-action-icon" aria-hidden="true">
                                            <svg viewBox="0 0 20 20" fill="none">
                                                <path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
                                            </svg>
                                        </span>
                                        取消
                                    </button>
                                </div>
                            </div>
                        </template>
                        <p v-else class="profile-field-value">{{ getFieldValue('nickname') || '暂未设置昵称' }}</p>
                    </article>

                    <article class="profile-field-card">
                        <div class="profile-field-head">
                            <div>
                                <h3>头像</h3>
                                <p>可以上传头像图片，也可以继续保留图片链接形式。</p>
                            </div>
                            <button
                                v-if="!isEditingField('avatarUrl')"
                                type="button"
                                class="field-edit-button"
                                @click="startEditField('avatarUrl')"
                            >
                                <span class="field-edit-button-icon" aria-hidden="true">
                                    <svg viewBox="0 0 20 20" fill="none">
                                        <path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                                    </svg>
                                </span>
                                编辑
                            </button>
                        </div>
                        <template v-if="isEditingField('avatarUrl')">
                            <div class="profile-field-editor">
                                <input
                                    ref="avatarInputRef"
                                    type="file"
                                    accept="image/*"
                                    class="sr-only"
                                    @change="handleAvatarSelected"
                                >
                                <div class="profile-upload-row">
                                    <button
                                        type="button"
                                        class="profile-action-with-icon secondary"
                                        :disabled="loading || avatarUploading"
                                        @click="triggerAvatarPicker"
                                    >
                                        <span class="profile-action-icon" aria-hidden="true">
                                            <svg viewBox="0 0 20 20" fill="none">
                                                <path d="M10 4v8m-4-4h8m-7 7h6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
                                            </svg>
                                        </span>
                                        {{ avatarUploading ? '上传中...' : '上传图片' }}
                                    </button>
                                    <span class="profile-upload-tip">支持 jpg、png、gif、webp，头像建议不超过 2MB。</span>
                                </div>
                                <input
                                    v-model.trim="draft.avatarUrl"
                                    type="url"
                                    placeholder="/api/uploads/files/2026/04/avatar.png 或 https://example.com/avatar.png"
                                >
                                <div class="profile-field-actions">
                                    <button class="primary-action profile-action-with-icon" type="button" :disabled="loading || avatarUploading" @click="saveField('avatarUrl')">
                                        <span class="profile-action-icon" aria-hidden="true">
                                            <svg viewBox="0 0 20 20" fill="none">
                                                <path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
                                            </svg>
                                        </span>
                                        {{ loading ? '保存中...' : '保存' }}
                                    </button>
                                    <button type="button" class="profile-action-with-icon secondary" :disabled="loading || avatarUploading" @click="cancelEditField('avatarUrl')">
                                        <span class="profile-action-icon" aria-hidden="true">
                                            <svg viewBox="0 0 20 20" fill="none">
                                                <path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
                                            </svg>
                                        </span>
                                        取消
                                    </button>
                                </div>
                            </div>
                        </template>
                        <p v-else class="profile-field-value profile-field-value-link">
                            {{ getFieldValue('avatarUrl') || '暂未设置头像' }}
                        </p>
                    </article>

                    <article class="profile-field-card">
                        <div class="profile-field-head">
                            <div>
                                <h3>个人简介</h3>
                                <p>一句话介绍你当前关注的方向、擅长内容或正在构建的项目。</p>
                            </div>
                            <button
                                v-if="!isEditingField('bio')"
                                type="button"
                                class="field-edit-button"
                                @click="startEditField('bio')"
                            >
                                <span class="field-edit-button-icon" aria-hidden="true">
                                    <svg viewBox="0 0 20 20" fill="none">
                                        <path d="M13.75 3.75a1.768 1.768 0 1 1 2.5 2.5L7.5 15H5v-2.5l8.75-8.75Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                                    </svg>
                                </span>
                                编辑
                            </button>
                        </div>
                        <template v-if="isEditingField('bio')">
                            <div class="profile-field-editor">
                                <textarea
                                    v-model.trim="draft.bio"
                                    rows="5"
                                    placeholder="介绍一下你正在关注的技术方向"
                                ></textarea>
                                <div class="profile-field-actions">
                                    <button class="primary-action profile-action-with-icon" type="button" :disabled="loading" @click="saveField('bio')">
                                        <span class="profile-action-icon" aria-hidden="true">
                                            <svg viewBox="0 0 20 20" fill="none">
                                                <path d="m4.5 10 3.5 3.5 7.5-7.5" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
                                            </svg>
                                        </span>
                                        {{ loading ? '保存中...' : '保存' }}
                                    </button>
                                    <button type="button" class="profile-action-with-icon secondary" :disabled="loading" @click="cancelEditField('bio')">
                                        <span class="profile-action-icon" aria-hidden="true">
                                            <svg viewBox="0 0 20 20" fill="none">
                                                <path d="m6 6 8 8m0-8-8 8" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
                                            </svg>
                                        </span>
                                        取消
                                    </button>
                                </div>
                            </div>
                        </template>
                        <p v-else class="profile-field-value">{{ getFieldValue('bio') || '暂未设置个人简介' }}</p>
                    </article>
                </div>

                <div class="profile-settings-panel-footer">
                    <p v-if="fieldFeedback" :class="['form-message', fieldFeedbackType]">{{ fieldFeedback }}</p>
                    <p v-else class="profile-settings-panel-tip">修改后会同步显示在文章作者信息、评论区和个人主页。</p>
                    <p v-if="feedback" :class="['form-message', feedbackType]">{{ feedback }}</p>
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

                <div v-if="hotArticlesLoading && hotArticles.length" class="profile-hot-state subtle">
                    正在更新热门文章...
                </div>
                <div v-if="hotArticlesError && hotArticles.length" class="profile-hot-state error-text subtle">
                    {{ hotArticlesError }}
                </div>
                <div v-if="hotArticlesLoading && !hotArticles.length" class="profile-hot-state">
                    热门文章加载中...
                </div>
                <div v-else-if="hotArticlesError && !hotArticles.length" class="profile-hot-state error-text">
                    {{ hotArticlesError }}
                </div>
                <div v-else-if="!hotArticles.length" class="profile-hot-state">
                    还没有足够热度的文章，先去发布第一篇内容。
                </div>
                <div v-else class="profile-hot-list">
                    <RouterLink
                        v-for="article in hotArticles"
                        :key="article.id"
                        class="profile-hot-item"
                        :to="`/articles/${article.id}`"
                    >
                        <div
                            class="profile-hot-cover"
                            :style="article.coverUrl
                                ? { backgroundImage: `url(${resolveMediaUrl(article.coverUrl, article.cover)})` }
                                : undefined"
                        >
                            <span class="profile-hot-category">{{ article.category || '未分类' }}</span>
                            <span class="profile-hot-rank">TOP {{ hotArticles.findIndex((item) => item.id === article.id) + 1 }}</span>
                        </div>
                        <div class="profile-hot-body">
                            <h3>{{ article.title }}</h3>
                            <div class="profile-hot-meta">
                                <span>
                                    <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                                        <path d="M2.5 10s2.8-5 7.5-5 7.5 5 7.5 5-2.8 5-7.5 5-7.5-5-7.5-5Z" stroke="currentColor" stroke-width="1.5"/>
                                        <circle cx="10" cy="10" r="2.25" stroke="currentColor" stroke-width="1.5"/>
                                    </svg>
                                    {{ article.viewCount }}
                                </span>
                                <span>
                                    <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                                        <path d="m10 15.5-4.625-4.463a2.917 2.917 0 0 1 4.125-4.119L10 7.413l.5-.495a2.917 2.917 0 1 1 4.125 4.119L10 15.5Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                                    </svg>
                                    {{ article.likeCount }}
                                </span>
                                <span>
                                    <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
                                        <path d="M5.5 4.5h9a1 1 0 0 1 1 1v11l-5.5-3-5.5 3v-11a1 1 0 0 1 1-1Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                                    </svg>
                                    {{ article.favoriteCount }}
                                </span>
                            </div>
                        </div>
                    </RouterLink>
                </div>
            </section>
        </section>
    </main>
</template>

<style scoped>
.profile-settings-page {
    display: grid;
    gap: 24px;
}

.profile-settings-panel {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
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
    transition: border-color 0.22s ease, box-shadow 0.22s ease;
}

.profile-field-card:hover {
    border-color: var(--brand);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
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

.field-edit-button {
    box-shadow: none;
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
.profile-field-actions button:not(.primary-action):hover,
.profile-hot-item:hover {
    border-color: var(--brand);
    box-shadow: none;
}

.field-edit-button:hover {
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

.profile-hot-state.error-text.subtle {
    color: var(--accent);
    background: rgba(209, 67, 67, 0.06);
    border-color: rgba(209, 67, 67, 0.12);
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
    transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.profile-hot-item:hover {
    box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
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
}
</style>
