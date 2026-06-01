<script setup>
import {computed, onMounted, onUnmounted, ref, useSlots} from 'vue';

const emit = defineEmits(['avatar-load', 'avatar-error', 'stat-click']);

const props = defineProps({
    mode: {
        type: String,
        default: 'public'
    },
    eyebrow: {
        type: String,
        default: ''
    },
    avatarSrc: {
        type: String,
        required: true
    },
    avatarAlt: {
        type: String,
        default: '用户头像'
    },
    title: {
        type: String,
        required: true
    },
    subtitle: {
        type: String,
        default: ''
    },
    bio: {
        type: String,
        default: ''
    },
    bioFallback: {
        type: String,
        default: '持续分享项目实践与工程经验。'
    },
    helperText: {
        type: String,
        default: ''
    },
    badgeText: {
        type: String,
        default: ''
    },
    stats: {
        type: Array,
        default: () => []
    }
});

const slots = useSlots();

const displayBio = computed(() => props.bio || props.bioFallback);
const hasActions = computed(() => Boolean(slots.actions));
const hasExtra = computed(() => Boolean(slots.extra));
const hasBadge = computed(() => Boolean(slots.badge) || Boolean(props.badgeText));

const lightboxOpen = ref(false);

const openLightbox = () => {
    lightboxOpen.value = true;
};

const closeLightbox = () => {
    lightboxOpen.value = false;
};

const onKeyDown = (e) => {
    if (e.key === 'Escape') closeLightbox();
};

onMounted(() => {
    document.addEventListener('keydown', onKeyDown);
});

onUnmounted(() => {
    document.removeEventListener('keydown', onKeyDown);
});
</script>

<template>
    <section class="profile-summary" :class="`profile-summary--${mode}`">
        <div class="profile-summary-main">
            <img
                class="profile-summary-avatar"
                :src="avatarSrc"
                :alt="avatarAlt"
                @load="$emit('avatar-load')"
                @error="$emit('avatar-error')"
                decoding="async"
                @click="openLightbox">
            <div class="profile-summary-copy">
                <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
                <div class="profile-summary-title-row">
                    <h1>{{ title }}</h1>
                    <slot v-if="hasBadge" name="badge">
                        <span class="profile-summary-badge">{{ badgeText }}</span>
                    </slot>
                </div>
                <p v-if="subtitle" class="profile-summary-subtitle">{{ subtitle }}</p>
                <p class="profile-summary-bio">{{ displayBio }}</p>
                <p v-if="helperText" class="profile-summary-helper">{{ helperText }}</p>
            </div>
        </div>

        <div v-if="hasExtra" class="profile-summary-extra">
            <slot name="extra" />
        </div>

        <div v-if="stats.length || hasActions" class="profile-summary-footer">
            <div v-if="stats.length" class="profile-summary-stats">
                <span
                    v-for="stat in stats"
                    :key="stat.key"
                    :class="{ 'stat-clickable': stat.clickable }"
                    @click="stat.clickable && emit('stat-click', stat)"
                >
                    <strong>{{ stat.value }}</strong>
                    {{ stat.label }}
                </span>
            </div>
            <div v-if="hasActions" class="profile-summary-actions">
                <slot name="actions" />
            </div>
        </div>
    </section>

    <!-- Avatar lightbox overlay -->
    <Teleport to="body">
        <div v-if="lightboxOpen" class="avatar-lightbox-overlay" @click.self="closeLightbox">
            <img :src="avatarSrc" :alt="avatarAlt" class="avatar-lightbox-image">
            <button class="avatar-lightbox-close" @click="closeLightbox" aria-label="关闭">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M6 6l12 12M18 6l-12 12"/>
                </svg>
            </button>
        </div>
    </Teleport>
</template>

<style scoped src="@/styles/components/UserProfileSummary.css"></style>

<style src="@/styles/components/UserProfileSummary.global-2.css"></style>
