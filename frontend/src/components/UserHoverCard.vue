<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, onUnmounted, ref} from 'vue';
import {getUserProfileApi} from '@/api/auth';
import UserLevelBadge from '@/components/UserLevelBadge.vue';
import UserPrivilegeBadge from '@/components/UserPrivilegeBadge.vue';

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
    },
    triggerClass: {
        type: String,
        default: ''
    },
    namePrefix: {
        type: String,
        default: ''
    },
    trigger: {
        type: String,
        default: 'hover' // 'hover' | 'click'
    }
});

const isClickTrigger = computed(() => props.trigger === 'click');

const showCard = ref(false);
const loading = ref(false);
const profile = ref(null);
const error = ref('');
const panelRef = ref(null);
const placement = ref('down');
let hoverTimer = null;

const userId = computed(() => props.user?.id);
const profilePath = computed(() => (userId.value ? `/users/${userId.value}` : '/'));
const displayUser = computed(() => profile.value?.user || props.user || {});
const displayName = computed(() => displayUser.value.name || displayUser.value.nickname || displayUser.value.username || '用户');
const displayAvatar = computed(() => displayUser.value.avatar || displayUser.value.avatarUrl || props.user?.avatar || props.user?.avatarUrl);
const displayBio = computed(() => displayUser.value.bio || '这个人还没有填写简介。');
const displayLevel = computed(() => displayUser.value.currentLevel || props.user?.currentLevel || 1);

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
        await nextTick();
        positionPanel();
    } catch (requestError) {
        error.value = requestError.message || '用户信息加载失败';
    } finally {
        if (userId.value) {
            pendingProfileRequests.delete(String(userId.value));
        }
        loading.value = false;
    }
}

function positionPanel() {
    if (!showCard.value || !panelRef.value) {
        return;
    }
    const triggerRect = panelRef.value.parentElement?.getBoundingClientRect();
    const panelRect = panelRef.value.getBoundingClientRect();
    if (!triggerRect || !panelRect.height) {
        return;
    }
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight;
    const belowSpace = viewportHeight - triggerRect.bottom;
    const aboveSpace = triggerRect.top;
    placement.value = belowSpace < panelRect.height + 16 && aboveSpace > belowSpace ? 'up' : 'down';
}

function openWithDelay() {
    window.clearTimeout(hoverTimer);
    hoverTimer = window.setTimeout(async () => {
        showCard.value = true;
        await nextTick();
        positionPanel();
        await loadProfile();
    }, HOVER_DELAY);
}

function closeCard() {
    window.clearTimeout(hoverTimer);
    showCard.value = false;
    placement.value = 'down';
}

function openOnFocus() {
    window.clearTimeout(hoverTimer);
    showCard.value = true;
    nextTick(positionPanel);
    loadProfile();
}

function toggleOnClick() {
    showCard.value = !showCard.value;
    if (showCard.value) {
        nextTick(positionPanel);
        loadProfile();
    } else {
        placement.value = 'down';
    }
}

function handleDocumentClick(e) {
    if (showCard.value && !e.target.closest('.user-hover-card')) {
        showCard.value = false;
    }
}

function handleViewportChange() {
    positionPanel();
}

onMounted(() => {
    if (isClickTrigger.value) {
        document.addEventListener('click', handleDocumentClick);
    }
    window.addEventListener('resize', handleViewportChange);
    window.addEventListener('scroll', handleViewportChange, true);
});

onUnmounted(() => {
    document.removeEventListener('click', handleDocumentClick);
    window.removeEventListener('resize', handleViewportChange);
    window.removeEventListener('scroll', handleViewportChange, true);
});

onBeforeUnmount(() => {
    window.clearTimeout(hoverTimer);
});
</script>

<template>
    <span
        class="user-hover-card"
        @mouseenter="!isClickTrigger ? openWithDelay() : null"
        @mouseleave="!isClickTrigger ? closeCard() : null"
        @focusin="!isClickTrigger ? openOnFocus() : null"
        @focusout="!isClickTrigger ? closeCard() : null"
        @click.stop="isClickTrigger ? toggleOnClick() : null"
    >
        <RouterLink
            v-if="variant === 'avatar' && !isClickTrigger"
            class="user-hover-card-trigger avatar-trigger"
            :to="profilePath"
            :aria-label="`进入 ${displayName} 的个人主页`"
        >
            <img :class="avatarClass" :src="displayAvatar" :alt="displayName" decoding="async">
        </RouterLink>
        <span
            v-else-if="variant === 'avatar' && isClickTrigger"
            class="user-hover-card-trigger avatar-trigger"
        >
            <img :class="avatarClass" :src="displayAvatar" :alt="displayName" decoding="async">
        </span>
        <RouterLink
            v-else-if="!isClickTrigger"
            class="user-hover-card-trigger name-trigger"
            :class="[triggerClass, nameClass]"
            :to="profilePath"
        >
            <span v-if="namePrefix" class="user-hover-card-prefix">{{ namePrefix }}</span>
            <span class="user-hover-card-name-text">{{ displayName }}</span>
            <UserLevelBadge :level="displayLevel" compact />
            <UserPrivilegeBadge :privilege-codes="displayUser.privilegeCodes || props.user?.privilegeCodes || []" compact />
        </RouterLink>
        <span
            v-else
            class="user-hover-card-trigger name-trigger"
            :class="[triggerClass, nameClass]"
        >
            <span v-if="namePrefix" class="user-hover-card-prefix">{{ namePrefix }}</span>
            <span class="user-hover-card-name-text">{{ displayName }}</span>
            <UserLevelBadge :level="displayLevel" compact />
            <UserPrivilegeBadge :privilege-codes="displayUser.privilegeCodes || props.user?.privilegeCodes || []" compact />
        </span>

        <span
            v-if="showCard"
            ref="panelRef"
            class="user-hover-panel"
            :class="`user-hover-panel--${placement}`"
            role="tooltip"
        >
            <span class="user-hover-panel-head">
                <img class="user-hover-panel-avatar" :src="displayAvatar" :alt="displayName" decoding="async">
                <span class="user-hover-panel-main">
                    <span class="user-hover-panel-name-row">
                        <RouterLink class="user-hover-panel-name" :to="profilePath">{{ displayName }}</RouterLink>
                        <UserLevelBadge :level="displayLevel" compact />
                        <UserPrivilegeBadge :privilege-codes="displayUser.privilegeCodes || props.user?.privilegeCodes || []" compact />
                    </span>
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
            <RouterLink class="user-hover-panel-profile-btn" :to="profilePath">查看主页</RouterLink>
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
    gap: 5px;
    color: inherit;
    text-decoration: none;
    cursor: pointer;
}

.name-trigger {
    font-weight: 700;
}

.user-hover-card-prefix {
    margin-right: 2px;
}

.user-hover-card-name-text {
    min-width: 0;
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

.user-hover-panel--up {
    top: auto;
    bottom: calc(100% + 10px);
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

.user-hover-panel--up::before {
    top: auto;
    bottom: -6px;
    border-top: 0;
    border-left: 0;
    border-right: 1px solid var(--line);
    border-bottom: 1px solid var(--line);
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

.user-hover-panel-name-row {
    display: inline-flex;
    gap: 6px;
    align-items: center;
    min-width: 0;
}

.user-hover-panel-main strong,
.user-hover-panel-name {
    overflow: hidden;
    font-size: 15px;
    font-weight: 700;
    text-decoration: none;
    color: inherit;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.user-hover-panel-name:hover {
    color: var(--brand-strong);
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

.user-hover-panel-profile-btn {
    display: block;
    text-align: center;
    padding: 8px 0;
    font-size: 14px;
    font-weight: 500;
    color: var(--brand);
    text-decoration: none;
    border-radius: var(--radius-sm);
    transition: background 0.1s;
}

.user-hover-panel-profile-btn:hover {
    background: var(--brand-soft);
}

@media (max-width: 640px) {
    .user-hover-panel {
        width: min(268px, calc(100vw - 48px));
    }
}
</style>
