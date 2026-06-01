<script setup>
import {RouterLink} from 'vue-router';
import SidebarBlock from '@/components/SidebarBlock.vue';
import AdBanner from '@/components/AdBanner.vue';

const props = defineProps({
    topics: {
        type: Array,
        default: () => []
    },
    columns: {
        type: Array,
        default: () => []
    },
    recentArticles: {
        type: Array,
        default: () => []
    }
});

const articlePath = (article) => {
    if (!article) return '/';
    return article.slug ? `/articles/${article.id}-${article.slug}` : `/articles/${article.id}`;
};
</script>

<template>
    <aside class="sidebar home-sidebar" aria-label="侧边栏" data-testid="home-sidebar">

        <AdBanner :slot-code="'home_sidebar'" />

        <!-- 最近阅读 -->
        <SidebarBlock
            v-if="props.recentArticles.length"
            eyebrow="历史"
            title="最近阅读"
            compact
            data-testid="home-recent"
        >
            <div class="recent-list">
                <RouterLink
                    v-for="article in props.recentArticles"
                    :key="article.id"
                    class="recent-item"
                    :to="articlePath(article)"
                >
                    <span
                        v-if="article.cover"
                        class="recent-item-cover"
                    >
                        <img
                            :src="article.cover"
                            :alt="`${article.title} 封面`"
                            loading="lazy"
                            decoding="async"
                        >
                    </span>
                    <span v-else class="recent-item-cover recent-item-fallback">
                        {{ (article.category || article.title || '?')[0] }}
                    </span>
                    <span class="recent-item-body">
                        <span class="recent-item-title">{{ article.title }}</span>
                        <span v-if="article.category" class="recent-item-cat">{{ article.category }}</span>
                    </span>
                </RouterLink>
            </div>
        </SidebarBlock>

        <!-- 本周热议专题 -->
        <SidebarBlock v-if="props.topics.length" eyebrow="本周" title="热议专题" compact data-testid="home-topics">
            <div class="special-list">
                <RouterLink
                    v-for="topic in props.topics"
                    :key="topic.id"
                    class="special-item"
                    :to="`/topics/${topic.id}`"
                >
                    <img
                        v-if="topic.coverUrl"
                        :src="topic.coverUrl"
                        :alt="`${topic.title} 封面`"
                        loading="lazy"
                        decoding="async"
                    >
                    <span>{{ topic.title }}</span>
                </RouterLink>
            </div>
        </SidebarBlock>

        <!-- 推荐专栏 -->
        <SidebarBlock v-if="props.columns.length" eyebrow="专栏" title="推荐专栏" compact data-testid="home-columns">
            <div class="column-list">
                <RouterLink
                    v-for="column in props.columns"
                    :key="column.id"
                    class="column-item"
                    :to="`/columns/${column.id}`"
                >
                    <img
                        :src="column.coverUrl"
                        :alt="`${column.title} 封面`"
                        loading="lazy"
                        decoding="async"
                    >
                    <span class="column-item-body">
                        <span class="column-item-title">{{ column.title }}</span>
                        <span class="column-item-meta">{{ column.articleCount || 0 }} 篇文章</span>
                    </span>
                </RouterLink>
            </div>
        </SidebarBlock>
    </aside>
</template>

<style scoped src="@/styles/components/HomeSidebar.css"></style>
