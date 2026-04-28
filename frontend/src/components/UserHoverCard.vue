<script setup>
import {computed, onBeforeUnmount, ref} from 'vue';
import {getUserProfileApi} from '@/api/auth';

const profileCache = new Map();
const pendingProfileRequests = new Map();
const HOVER_DELAY = 500;

const props = defineProps({
    user: {
        type: Object,
        required: true
    },
    variant: {
        type: String,
        default: 'name'
    },
    avatarClass: {
        type: String,
        default: ''
    },
    nameClass: {
        type: String,
        default: ''
    }
});

const showCard = ref(false);
const loading = ref(false);
const profile = ref(null);
const error = ref('');
let hoverTimer = null;

const userId = computed(() => props.user?.id);
const profilePath = computed(() => (userId.value ? `/users/${userId.value}` : '/'));
const displayUser = computed(() => profile.value?.user || props.user || {});
const displayName = computed(() => displayUser.value.name || displayUser.value.nickname || displayUser.value.username || '用户');
const displayAvatar = computed(() => displayUser.value.avatar || displayUser.value.avatarUrl || props.user?.avatar || props.user?.avatarUrl);
const displayBio = computed(() => displayUser.value.bio || '这个人还没有填写简介。');

const profileStats = computed(() => {
    const source = profile.value || {};
    return [
        {label: '文章', value: source.articleCount || 0},
        {label: '粉丝', value: source.followerCount || 0},
        {label: '关注', value: source.followingCount || 0}
    ];
});

async function loadProfile() {
    if (!userId.value || profile.value || loading.value) {
        return;
    }
    if (profileCache.has(String(userId.value))) {
        profile.value = profileCache.get(String(userId.value));
        return;
    }

    loading.value = true;
    error.value = '';
    try {
        const cacheKey = String(userId.value);
        let request = pendingProfileRequests.get(cacheKey);
        if (!request) {
            request = getUserProfileApi(userId.value);
            pendingProfileRequests.set(cacheKey, request);
        }
        const result = await request;
        profileCache.set(cacheKey, result);
        profile.value = result;
    } catch (requestError) {
        error.value = requestError.message || '用户信息加载失败';
    } finally {
        if (userId.value) {
            pendingProfileRequests.delete(String(userId.value));
        }
        loading.value = false;
    }
}

function openWithDelay() {
    window.clearTimeout(hoverTimer);
    hoverTimer = window.setTimeout(async () => {
        showCard.value = true;
        await loadProfile();
    }, HOVER_DELAY);
}

function closeCard() {
    window.clearTimeout(hoverTimer);
    showCard.value = false;
}

function openOnFocus() {
    window.clearTimeout(hoverTimer);
    showCard.value = true;
    loadProfile();
}

onBeforeUnmount(() => {
    window.clearTimeout(hoverTimer);
});
</script>

<template>
    <span
        class="user-hover-card"
        @mouseenter="openWithDelay"
        @mouseleave="closeCard"
        @focusin="openOnFocus"
        @focusout="closeCard"
    >
        <RouterLink
            v-if="variant === 'avatar'"
            class="user-hover-card-trigger avatar-trigger"
            :to="profilePath"
            :aria-label="`进入 ${displayName} 的个人主页`"
        >
            <img :class="avatarClass" :src="displayAvatar" :alt="displayName">
        </RouterLink>
        <RouterLink
            v-else
            class="user-hover-card-trigger name-trigger"
            :class="nameClass"
            :to="profilePath"
        >
            {{ displayName }}
        </RouterLink>

        <span v-if="showCard" class="user-hover-panel" role="tooltip">
            <span class="user-hover-panel-head">
                <img class="user-hover-panel-avatar" :src="displayAvatar" :alt="displayName">
                <span class="user-hover-panel-main">
                    <strong>{{ displayName }}</strong>
                    <small v-if="displayUser.username">@{{ displayUser.username }}</small>
                    <small v-else>内容创作者</small>
                </span>
            </span>
            <span v-if="loading" class="user-hover-panel-bio muted">正在加载个人简介...</span>
            <span v-else-if="error" class="user-hover-panel-bio error">{{ error }}</span>
            <span v-else class="user-hover-panel-bio">{{ displayBio }}</span>
            <span class="user-hover-panel-stats">
                <span v-for="stat in profileStats" :key="stat.label">
                    <strong>{{ stat.value }}</strong>
                    <small>{{ stat.label }}</small>
                </span>
            </span>
            <RouterLink class="user-hover-panel-link" :to="profilePath">查看个人主页</RouterLink>
        </span>
    </span>
</template>

<style scoped>
.user-hover-card {
    position: relative;
    display: inline-flex;
    align-items: center;
    width: fit-content;
}

.user-hover-card-trigger {
    display: inline-flex;
    align-items: center;
    color: inherit;
    text-decoration: none;
}

.name-trigger {
    font-weight: 700;
}

.user-hover-card-trigger:hover,
.user-hover-card-trigger:focus-visible {
    color: var(--brand-strong);
}

.avatar-trigger:hover img,
.avatar-trigger:focus-visible img {
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14);
}

.user-hover-panel {
    position: absolute;
    top: calc(100% + 10px);
    left: 0;
    z-index: 30;
    display: grid;
    gap: 10px;
    width: 268px;
    padding: 14px;
    color: var(--text);
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-lg);
    box-shadow: 0 18px 44px rgba(15, 23, 42, 0.16);
}

.user-hover-panel::before {
    position: absolute;
    top: -6px;
    left: 18px;
    width: 10px;
    height: 10px;
    content: "";
    background: var(--surface);
    border-top: 1px solid var(--line);
    border-left: 1px solid var(--line);
    transform: rotate(45deg);
}

.user-hover-panel-head {
    display: flex;
    gap: 10px;
    align-items: center;
}

.user-hover-panel-avatar {
    width: 44px;
    height: 44px;
    object-fit: cover;
    border-radius: 50%;
    background: var(--surface-soft);
}

.user-hover-panel-main {
    display: grid;
    min-width: 0;
}

.user-hover-panel-main strong {
    overflow: hidden;
    font-size: 15px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.user-hover-panel-main small,
.user-hover-panel-bio,
.user-hover-panel-stats small {
    color: var(--muted);
}

.user-hover-panel-bio {
    display: -webkit-box;
    overflow: hidden;
    font-size: 13px;
    line-height: 1.6;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
}

.user-hover-panel-bio.error {
    color: #b42318;
}

.user-hover-panel-stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    padding: 9px 0;
    border-top: 1px solid var(--line);
    border-bottom: 1px solid var(--line);
}

.user-hover-panel-stats span {
    display: grid;
    gap: 2px;
}

.user-hover-panel-stats strong {
    font-size: 15px;
}

.user-hover-panel-stats small {
    font-size: 12px;
}

.user-hover-panel-link {
    color: var(--brand-strong);
    font-size: 13px;
    font-weight: 700;
    text-decoration: none;
}

.user-hover-panel-link:hover {
    text-decoration: underline;
}

@media (max-width: 640px) {
    .user-hover-panel {
        width: min(268px, calc(100vw - 48px));
    }
}
</style>
