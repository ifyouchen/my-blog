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
    gap: 22px;
    padding: 28px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
    box-shadow: var(--shadow);
}

.profile-summary-main {
    display: grid;
    grid-template-columns: 116px minmax(0, 1fr);
    gap: 20px;
    align-items: center;
}

.profile-summary-avatar {
    width: 116px;
    height: 116px;
    object-fit: cover;
    border-radius: 50%;
    border: 4px solid rgba(15, 143, 117, 0.12);
}

.profile-summary-copy {
    display: grid;
    gap: 8px;
    min-width: 0;
}

.profile-summary-title-row {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
}

.profile-summary-title-row h1 {
    margin: 0;
    font-size: 34px;
    line-height: 1.2;
}

.profile-summary-badge {
    display: inline-flex;
    align-items: center;
    min-height: 28px;
    padding: 0 12px;
    font-size: 13px;
    font-weight: 700;
    color: var(--brand-strong);
    background: rgba(15, 143, 117, 0.08);
    border: 1px solid rgba(15, 143, 117, 0.14);
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
    line-height: 1.8;
}

.profile-summary-footer {
    display: flex;
    gap: 18px;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    padding-top: 2px;
    border-top: 1px solid rgba(15, 143, 117, 0.08);
}

.profile-summary-stats {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    flex: 1 1 520px;
}

.profile-summary-stats span {
    display: inline-grid;
    gap: 6px;
    min-width: 112px;
    padding: 14px 18px;
    text-align: center;
    background: rgba(15, 143, 117, 0.05);
    border: 1px solid rgba(15, 143, 117, 0.08);
    border-radius: 8px;
    color: var(--muted);
}

.profile-summary-stats strong {
    color: var(--text);
    font-size: 22px;
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
        padding: 18px;
    }

    .profile-summary-main {
        grid-template-columns: 1fr;
    }

    .profile-summary-avatar {
        width: 92px;
        height: 92px;
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
