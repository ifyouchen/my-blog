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

<style scoped src="@/styles/components/BadgePicker.css"></style>
