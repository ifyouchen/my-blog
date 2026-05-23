<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

const SKELETON_WIDTHS = ['76px', '92px', '70px', '86px', '64px', '80px'];

const props = defineProps({
    categoryGroups: {
        type: Object,
        default: () => ({})
    },
    loading: {
        type: Boolean,
        default: false
    }
});

const route = useRoute();
const expanded = ref(false);

const groupNames = computed(() => {
    const names = Object.keys(props.categoryGroups);
    return names.length ? ['全部', ...names] : ['全部'];
});

const activeGroup = computed(() => String(route.query.group || '全部'));

const activeCategories = computed(() => {
    if (activeGroup.value === '全部') return [];
    return props.categoryGroups[activeGroup.value] || [];
});

const showSkeleton = computed(() => props.loading && !Object.keys(props.categoryGroups).length);

const hasMoreGroups = computed(() => groupNames.value.length > 8);

watch([activeGroup, groupNames], ([group, groups]) => {
    const activeIndex = groups.indexOf(group);
    if (activeIndex >= 8) {
        expanded.value = true;
    }
});

const buildGroupRoute = (group) => {
    const nextFeedTab = route.query.feedTab === 'following' ? 'following' : undefined;
    return group === '全部'
        ? { path: '/', query: { sort: route.query.sort, feedTab: nextFeedTab, page: undefined } }
        : { path: '/', query: { group, sort: route.query.sort, feedTab: nextFeedTab, page: undefined, category: undefined } };
};

const buildCategoryRoute = (categoryName) => ({
    path: '/',
    query: { group: activeGroup.value, category: categoryName, sort: route.query.sort, feedTab: route.query.feedTab === 'following' ? 'following' : undefined, page: undefined }
});
</script>

<template>
    <section class="topic-strip" aria-label="技术分类" :aria-busy="showSkeleton">
        <template v-if="showSkeleton">
            <RouterLink
                class="topic"
                :class="{ active: activeGroup === '全部' }"
                :to="buildGroupRoute('全部')"
            >
                全部
            </RouterLink>
            <span
                v-for="width in SKELETON_WIDTHS"
                :key="width"
                class="topic topic-skeleton"
                :style="{ width }"
                aria-hidden="true"
            ></span>
        </template>
        <template v-else>
            <div class="topic-list" :class="{ expanded }">
            <RouterLink
                v-for="group in groupNames"
                :key="group"
                class="topic"
                :class="{ active: activeGroup === group }"
                :to="buildGroupRoute(group)"
            >
                {{ group }}
            </RouterLink>
            <button
                v-if="hasMoreGroups"
                class="topic-more-toggle"
                type="button"
                @click="expanded = !expanded"
            >
                {{ expanded ? '收起' : '展开更多' }}
            </button>
            </div>
            <div v-if="activeCategories.length" class="sub-category-strip">
                <RouterLink
                    v-for="cat in activeCategories"
                    :key="cat.name"
                    class="sub-category"
                    :class="{ active: route.query.category === cat.name }"
                    :to="buildCategoryRoute(cat.name)"
                >
                    {{ cat.name }}
                </RouterLink>
            </div>
        </template>
    </section>
</template>

<style scoped>
.topic-strip {
    min-height: 55px;
}

.topic-list {
    display: flex;
    gap: 0;
    min-width: 0;
}

.topic-skeleton {
    cursor: default;
    pointer-events: none;
}

.topic-skeleton::before {
    display: block;
    width: 100%;
    height: 14px;
    content: "";
    background: var(--surface-muted);
    border-radius: var(--radius-sm);
}

.topic-more-toggle {
    display: none;
}

.sub-category-strip {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid var(--line);
}

.sub-category {
    padding: 2px 10px;
    color: var(--muted);
    font-size: 13px;
    text-decoration: none;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: color 0.12s, background 0.12s, border-color 0.12s;
}

.sub-category:hover {
    color: var(--brand-strong);
    border-color: var(--brand);
}

.sub-category.active {
    color: #fff;
    background: var(--brand);
    border-color: var(--brand);
}

@media (max-width: 720px) {
    .topic-strip {
        min-height: auto;
        overflow-x: visible;
    }

    .topic-list {
        flex-wrap: wrap;
        row-gap: 4px;
    }

    .topic-list:not(.expanded) .topic:nth-of-type(n + 9) {
        display: none;
    }

    .topic-more-toggle {
        display: inline-flex;
        align-items: center;
        min-height: 34px;
        padding: 0 10px;
        color: var(--brand);
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        background: transparent;
        border: 0;
    }
}
</style>
