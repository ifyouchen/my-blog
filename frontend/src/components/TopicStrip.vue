<script setup>
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

const SKELETON_WIDTHS = ['76px', '92px', '70px', '86px', '64px', '80px'];

const props = defineProps({
    topics: {
        type: Array,
        default: () => []
    },
    loading: {
        type: Boolean,
        default: false
    }
});

const route = useRoute();
const activeTopic = computed(() => String(route.query.category || '全部'));
const visibleTopics = computed(() => props.topics.length ? props.topics : ['全部']);
const showSkeleton = computed(() => props.loading && props.topics.length <= 1);

const buildTopicRoute = (topic) => {
    const nextFeedTab = route.query.feedTab === 'following' ? 'following' : undefined;
    return topic === '全部'
        ? { path: '/', query: { sort: route.query.sort, feedTab: nextFeedTab, page: undefined } }
        : { path: '/', query: { category: topic, sort: route.query.sort, feedTab: nextFeedTab, page: undefined } };
};
</script>

<template>
    <section class="topic-strip" aria-label="技术分类" :aria-busy="showSkeleton">
        <template v-if="showSkeleton">
            <RouterLink
                class="topic"
                :class="{ active: activeTopic === '全部' }"
                :to="buildTopicRoute('全部')"
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
            <RouterLink
                v-for="topic in visibleTopics"
                :key="topic"
                class="topic"
                :class="{ active: activeTopic === topic }"
                :to="buildTopicRoute(topic)"
            >
                {{ topic }}
            </RouterLink>
        </template>
    </section>
</template>

<style scoped>
.topic-strip {
    min-height: 55px;
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
</style>
