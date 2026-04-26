<script setup>
import { computed, useSlots } from 'vue';

defineEmits(['avatar-load', 'avatar-error']);

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
            >
            <div class="profile-summary-copy">
                <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
                <div class="profile-summary-title-row">
                    <h1>{{ title }}</h1>
                    <span v-if="badgeText" class="profile-summary-badge">{{ badgeText }}</span>
                </div>
                <p v-if="subtitle" class="profile-summary-subtitle">{{ subtitle }}</p>
                <p class="profile-summary-bio">{{ displayBio }}</p>
                <p v-if="helperText" class="profile-summary-helper">{{ helperText }}</p>
            </div>
        </div>

        <div v-if="stats.length || hasActions" class="profile-summary-footer">
            <div v-if="stats.length" class="profile-summary-stats">
                <span v-for="stat in stats" :key="stat.key">
                    <strong>{{ stat.value }}</strong>
                    {{ stat.label }}
                </span>
            </div>
            <div v-if="hasActions" class="profile-summary-actions">
                <slot name="actions" />
            </div>
        </div>
    </section>
</template>

<style scoped>
.profile-summary {
    display: grid;
    gap: 28px;
    padding: 32px;
    background:
        radial-gradient(circle at top right, rgba(40, 118, 255, 0.14), transparent 26%),
        linear-gradient(180deg, rgba(248, 251, 255, 0.98), #ffffff);
    border: 1px solid rgba(196, 211, 232, 0.92);
    border-radius: 24px;
    box-shadow: 0 24px 60px rgba(31, 78, 168, 0.08);
}

.profile-summary-main {
    display: grid;
    grid-template-columns: 116px minmax(0, 1fr);
    gap: 24px;
    align-items: center;
}

.profile-summary-avatar {
    width: 116px;
    height: 116px;
    object-fit: cover;
    border-radius: 28px;
    border: 4px solid rgba(40, 118, 255, 0.12);
    box-shadow: 0 20px 44px rgba(40, 118, 255, 0.14);
}

.profile-summary-copy {
    display: grid;
    gap: 10px;
    min-width: 0;
}

.profile-summary-title-row {
    display: flex;
    gap: 14px;
    align-items: center;
    flex-wrap: wrap;
}

.profile-summary-title-row h1 {
    margin: 0;
    font-size: clamp(32px, 4vw, 42px);
    line-height: 1.1;
    letter-spacing: -0.02em;
}

.profile-summary-badge {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0 14px;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.04em;
    color: var(--brand-strong);
    background: rgba(40, 118, 255, 0.1);
    border: 1px solid rgba(40, 118, 255, 0.16);
    border-radius: 999px;
}

.profile-summary-subtitle {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
}

.profile-summary-bio,
.profile-summary-helper {
    margin: 0;
    color: var(--muted);
    line-height: 1.85;
    max-width: 72ch;
}

.profile-summary-footer {
    display: flex;
    gap: 20px;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    padding-top: 22px;
    border-top: 1px solid rgba(196, 211, 232, 0.86);
}

.profile-summary-stats {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    flex: 1 1 520px;
}

.profile-summary-stats span {
    display: inline-grid;
    gap: 8px;
    min-width: 128px;
    padding: 16px 18px;
    text-align: center;
    background: linear-gradient(180deg, rgba(248, 251, 255, 0.96), rgba(240, 247, 255, 0.84));
    border: 1px solid rgba(196, 211, 232, 0.9);
    border-radius: 18px;
    color: var(--muted);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.profile-summary-stats strong {
    color: var(--text);
    font-size: 24px;
    line-height: 1;
}

.profile-summary-actions {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
}

@media (max-width: 980px) {
    .profile-summary-stats {
        flex-basis: 100%;
    }
}

@media (max-width: 760px) {
    .profile-summary {
        padding: 22px 18px;
    }

    .profile-summary-main {
        grid-template-columns: 1fr;
    }

    .profile-summary-avatar {
        width: 92px;
        height: 92px;
        border-radius: 24px;
    }

    .profile-summary-title-row h1 {
        font-size: 28px;
    }

    .profile-summary-stats {
        flex-direction: column;
    }

    .profile-summary-stats span,
    .profile-summary-actions :deep(a),
    .profile-summary-actions :deep(button) {
        width: 100%;
        min-width: 0;
    }
}
</style>
