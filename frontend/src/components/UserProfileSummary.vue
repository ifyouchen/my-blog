<script setup>
import {computed, useSlots} from 'vue';

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
</template>

<style scoped>
/* ============================================================
   个人主页头部 — 清爽横排布局，去胶囊
   ============================================================ */
.profile-summary {
    display: grid;
    gap: 24px;
    padding: 28px 32px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow);
    margin-bottom: 20px;
}

.profile-summary-main {
    display: grid;
    grid-template-columns: 100px minmax(0, 1fr);
    gap: 20px;
    align-items: start;
}

.profile-summary-avatar {
    width: 100px;
    height: 100px;
    object-fit: cover;
    border-radius: var(--radius-md);
    border: 2px solid var(--line);
}

.profile-summary-copy {
    display: grid;
    gap: 8px;
    min-width: 0;
}

.profile-summary-title-row {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
}

.profile-summary-title-row h1 {
    margin: 0;
    font-size: clamp(24px, 3vw, 32px);
    line-height: 1.2;
    font-weight: 700;
    letter-spacing: -0.01em;
    color: var(--text-strong);
}

.profile-summary-badge {
    display: inline-flex;
    align-items: center;
    height: 24px;
    padding: 0 10px;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.04em;
    color: var(--brand);
    background: var(--brand-soft);
    border: 1px solid rgba(37, 99, 235, 0.2);
    border-radius: var(--radius-sm);
}

.profile-summary-subtitle {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
}

.profile-summary-bio,
.profile-summary-helper {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
    line-height: 1.75;
    max-width: 72ch;
}

/* 底部统计行 — 横排分隔线风格，不用小卡片 */
.profile-summary-footer {
    display: flex;
    gap: 0;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    padding-top: 18px;
    border-top: 1px solid var(--line);
}

.profile-summary-stats {
    display: flex;
    flex-wrap: wrap;
    gap: 0;
    flex: 1;
}

.profile-summary-stats span {
    display: flex;
    align-items: baseline;
    gap: 6px;
    padding: 6px 20px 6px 0;
    margin-right: 20px;
    color: var(--muted);
    font-size: 13px;
    border-right: 1px solid var(--line);
}

.profile-summary-stats span:first-child {
    padding-left: 0;
}

.profile-summary-stats span:last-child {
    border-right: 0;
    margin-right: 0;
}

.profile-summary-stats span.stat-clickable {
    cursor: pointer;
    transition: color 0.12s;
}

.profile-summary-stats span.stat-clickable:hover strong {
    color: var(--brand);
}

.profile-summary-stats strong {
    color: var(--text-strong);
    font-size: 18px;
    font-weight: 700;
    line-height: 1;
}

.profile-summary-actions {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
    padding-left: 16px;
}

@media (max-width: 760px) {
    .profile-summary {
        padding: 20px 18px;
    }

    .profile-summary-main {
        grid-template-columns: 80px minmax(0, 1fr);
        gap: 16px;
        align-items: center;
    }

    .profile-summary-avatar {
        width: 80px;
        height: 80px;
    }

    .profile-summary-title-row h1 {
        font-size: 22px;
    }

    .profile-summary-stats {
        flex-direction: row;
        flex-wrap: wrap;
    }

    .profile-summary-stats span {
        padding: 4px 14px 4px 0;
        margin-right: 14px;
        font-size: 12px;
    }

    .profile-summary-stats strong {
        font-size: 16px;
    }

    .profile-summary-footer {
        flex-direction: column;
        align-items: flex-start;
        gap: 12px;
    }

    .profile-summary-actions {
        padding-left: 0;
        width: 100%;
        justify-content: flex-start;
    }

    .profile-summary-actions :deep(a),
    .profile-summary-actions :deep(button) {
        min-width: 0;
    }
}
</style>
