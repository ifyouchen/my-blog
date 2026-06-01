<script setup>
import {computed, ref} from 'vue';
import UserEquippedBadge from '@/components/UserEquippedBadge.vue';

const props = defineProps({
    badges: {
        type: Array,
        default: () => []
    }
});

const GROUPS = [
    {key: 'LEVEL', label: '等级徽章'},
    {key: 'SIGN', label: '签到徽章'},
    {key: 'ANNUAL', label: '年度徽章'}
];

const expanded = ref({LEVEL: true});

const groups = computed(() => GROUPS.map((group) => ({
    ...group,
    items: props.badges.filter((badge) => badge.type === group.key)
})).filter((group) => group.items.length));

const toggle = (key) => {
    expanded.value = {
        ...expanded.value,
        [key]: !expanded.value[key]
    };
};
</script>

<template>
    <div class="badge-wall">
        <section v-for="group in groups" :key="group.key" class="badge-group">
            <button type="button" class="badge-group-head" @click="toggle(group.key)">
                <span>{{ group.label }}</span>
                <strong>{{ group.items.filter((item) => item.owned).length }}/{{ group.items.length }}</strong>
            </button>
            <div v-if="expanded[group.key]" class="badge-grid">
                <article
                    v-for="badge in group.items"
                    :key="badge.code"
                    class="badge-card"
                    :class="{ locked: !badge.owned, equipped: badge.equipped }"
                >
                    <UserEquippedBadge :badge="badge" />
                    <p>{{ badge.description }}</p>
                </article>
            </div>
        </section>
    </div>
</template>

<style scoped src="@/styles/components/BadgeWall.css"></style>
