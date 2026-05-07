<script setup>
import {RouterLink} from 'vue-router';
import SidebarBlock from '@/components/SidebarBlock.vue';
import AdBanner from '@/components/AdBanner.vue';

const props = defineProps({
    topics: {
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

        <AdBanner :slot-code="'home_sidebar'" />
    </aside>
</template>

<style scoped>
.home-sidebar {
    max-height: calc(100vh - 88px);
    overflow-y: auto;
    padding-right: 4px;
    scrollbar-gutter: stable;
    scrollbar-width: thin;
}

.home-sidebar::-webkit-scrollbar {
    width: 6px;
}

.home-sidebar::-webkit-scrollbar-thumb {
    background: var(--line-strong);
    border-radius: 999px;
}

.home-sidebar::-webkit-scrollbar-track {
    background: transparent;
}

/* 最近阅读 */
.recent-list {
    display: grid;
    gap: 2px;
}

.recent-item {
    display: grid;
    grid-template-columns: 40px minmax(0, 1fr);
    gap: 10px;
    align-items: center;
    min-height: 44px;
    padding: 6px 8px;
    border-radius: var(--radius-sm);
    text-decoration: none;
    transition: background 0.12s;
}

.recent-item:hover,
.recent-item:focus-visible {
    background: var(--surface-soft);
}

.recent-item-cover {
    display: block;
    width: 40px;
    height: 30px;
    overflow: hidden;
    border-radius: 4px;
    background: var(--surface-muted);
    flex-shrink: 0;
}

.recent-item-cover img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.recent-item-fallback {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: var(--brand);
    font-size: 13px;
    font-weight: 700;
    background: var(--brand-soft);
}

.recent-item-body {
    display: grid;
    gap: 2px;
    min-width: 0;
}

.recent-item-title {
    display: -webkit-box;
    overflow: hidden;
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
    line-height: 1.4;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    transition: color 0.12s;
}

.recent-item:hover .recent-item-title,
.recent-item:focus-visible .recent-item-title {
    color: var(--brand);
}

.recent-item-cat {
    color: var(--muted);
    font-size: 11px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 热议专题 */
.special-item:hover,
.special-item:focus-visible {
    background: var(--surface-soft);
}

@media (max-width: 980px) {
    .home-sidebar {
        max-height: none;
        overflow-y: visible;
        padding-right: 0;
    }
}
</style>
