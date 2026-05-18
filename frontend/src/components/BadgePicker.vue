<script setup>
import {computed} from 'vue';
import UserEquippedBadge from '@/components/UserEquippedBadge.vue';

const props = defineProps({
    badges: {
        type: Array,
        default: () => []
    },
    saving: {
        type: Boolean,
        default: false
    }
});

const emit = defineEmits(['equip']);

const ownedBadges = computed(() => props.badges.filter((badge) => badge.owned));
const equippedCode = computed(() => props.badges.find((badge) => badge.equipped)?.code || '');
</script>

<template>
    <div class="badge-picker">
        <button
            type="button"
            class="badge-clear-btn"
            :disabled="saving || !equippedCode"
            @click="emit('equip', null)"
        >
            取消佩戴
        </button>
        <div class="badge-picker-list">
            <button
                v-for="badge in ownedBadges"
                :key="badge.code"
                type="button"
                class="badge-pick-btn"
                :class="{ active: badge.code === equippedCode }"
                :disabled="saving"
                @click="emit('equip', badge.code)"
            >
                <UserEquippedBadge :badge="badge" />
            </button>
        </div>
    </div>
</template>

<style scoped>
.badge-picker {
    display: grid;
    gap: 10px;
}

.badge-picker-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.badge-pick-btn,
.badge-clear-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 34px;
    padding: 5px 8px;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fff;
    cursor: pointer;
}

.badge-pick-btn.active {
    border-color: var(--brand);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.badge-clear-btn {
    width: fit-content;
    color: var(--text-muted);
    font-size: 12px;
    font-weight: 700;
}

.badge-pick-btn:disabled,
.badge-clear-btn:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}
</style>
