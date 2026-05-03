<script setup>
import {RouterLink} from 'vue-router';
import SidebarBlock from '@/components/SidebarBlock.vue';
import AdBanner from '@/components/AdBanner.vue';

const props = defineProps({
    specials: {
        type: Array,
        default: () => []
    },
    authors: {
        type: Array,
        default: () => []
    },
    topics: {
        type: Array,
        default: () => []
    },
    featured: {
        type: Array,
        default: () => []
    }
});
</script>

<template>
    <aside class="sidebar" aria-label="侧边栏" data-testid="home-sidebar">
        <SidebarBlock v-if="props.featured.length" eyebrow="本周" title="精选推荐" compact data-testid="home-featured">
            <ol class="featured-sidebar-list">
                <li v-for="article in props.featured.slice(0, 3)" :key="article.id">
                    <RouterLink :to="`/articles/${article.id}`" class="featured-sidebar-item">
                        <span class="featured-sidebar-badge">⭐</span>
                        <span class="featured-sidebar-title">{{ article.title }}</span>
                    </RouterLink>
                </li>
            </ol>
        </SidebarBlock>

        <SidebarBlock v-if="props.specials.length" eyebrow="专栏" title="推荐专栏" compact data-testid="home-specials">
            <div class="special-list">
                <RouterLink
                    v-for="column in props.specials"
                    :key="column.id"
                    class="special-item"
                    :to="`/columns/${column.id}`"
                >
                    <img
                        v-if="column.coverUrl"
                        :src="column.coverUrl"
                        :alt="`${column.title} 封面`"
                        loading="lazy"
                        decoding="async"
                    >
                    <span>{{ column.title }}</span>
                </RouterLink>
            </div>
        </SidebarBlock>

        <SidebarBlock v-if="props.topics.length" eyebrow="本周" title="热门专题" compact data-testid="home-topics">
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

        <SidebarBlock v-if="props.authors.length" eyebrow="创作者" title="活跃作者" compact data-testid="home-authors">
            <ol class="author-rank">
                <li v-for="(author, index) in props.authors" :key="author.user.id">
                    <span class="rank-no">{{ index + 1 }}</span>
                    <RouterLink class="home-author-avatar" :to="`/users/${author.user.id}`">
                        <img :src="author.user.avatar" :alt="`${author.user.name} 的头像`" loading="lazy" decoding="async" />
                    </RouterLink>
                    <div>
                        <RouterLink :to="`/users/${author.user.id}`">{{ author.user.name }}</RouterLink>
                        <span>{{ author.totalViewCount }} 阅读 · {{ author.articleCount }} 篇</span>
                    </div>
                </li>
            </ol>
        </SidebarBlock>

        <AdBanner :slot-code="'home_sidebar'" />
    </aside>
</template>

<style scoped>
.special-item:hover,
.special-item:focus-visible {
    background: var(--surface-soft);
}

.author-rank li:hover {
    background: var(--surface-soft);
}

.author-rank a {
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
    text-decoration: none;
    transition: color 0.12s;
}

.author-rank a:hover {
    color: var(--brand);
}

.home-author-avatar {
    display: inline-flex;
}

.featured-sidebar-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 0;
    margin: 0;
    list-style: none;
}

.featured-sidebar-item {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    padding: 6px 8px;
    border-radius: var(--radius-sm);
    text-decoration: none;
    color: var(--text);
    transition: background 0.12s;
    font-size: 13px;
}

.featured-sidebar-item:hover {
    background: var(--surface-soft);
    color: var(--brand);
}

.featured-sidebar-badge {
    flex-shrink: 0;
    font-size: 11px;
    margin-top: 2px;
}

.featured-sidebar-title {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.5;
}
</style>
