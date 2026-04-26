<script setup>
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

const props = defineProps({
    topics: {
        type: Array,
        default: () => []
    }
});

const route = useRoute();
const activeTopic = computed(() => String(route.query.category || '全部'));
</script>

<template>
    <section class="topic-strip" aria-label="技术分类">
        <RouterLink
            v-for="topic in props.topics"
            :key="topic"
            class="topic"
            :class="{ active: activeTopic === topic }"
            :to="topic === '全部'
                ? { path: '/', query: { sort: route.query.sort, page: undefined } }
                : { path: '/', query: { category: topic, sort: route.query.sort, page: undefined } }"
        >
            {{ topic }}
        </RouterLink>
    </section>
</template>
