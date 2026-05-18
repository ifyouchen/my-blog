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

<style scoped>
.badge-wall {
    display: grid;
    gap: 12px;
}

.badge-group {
    border: 1px solid var(--line);
    border-radius: 8px;
    overflow: hidden;
    background: var(--surface);
}

.badge-group-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 42px;
    padding: 0 14px;
    color: var(--text-strong);
    background: var(--surface-soft);
    border: 0;
    font-size: 13px;
    font-weight: 800;
    cursor: pointer;
}

.badge-group-head strong {
    color: var(--brand);
    font-size: 12px;
}

.badge-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
    gap: 10px;
    padding: 12px;
}

.badge-card {
    display: grid;
    gap: 8px;
    min-width: 0;
    min-height: 78px;
    padding: 12px;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fff;
}

.badge-card.locked {
    opacity: 0.48;
    filter: grayscale(0.85);
}

.badge-card.equipped {
    border-color: rgba(37, 99, 235, 0.32);
    box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.08);
}

.badge-card p {
    margin: 0;
    color: var(--text-muted);
    font-size: 12px;
    line-height: 1.45;
}

@media (max-width: 640px) {
    .badge-grid {
        grid-template-columns: 1fr;
    }
}
</style>
