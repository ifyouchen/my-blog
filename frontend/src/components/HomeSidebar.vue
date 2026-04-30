<script setup>
import {computed, inject} from 'vue';
import {RouterLink, useRouter} from 'vue-router';
import SidebarBlock from '@/components/SidebarBlock.vue';
import AdBanner from '@/components/AdBanner.vue';

const router = useRouter();
const loginModal = inject('loginModal', { requireLogin: () => false });
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
    }
});
const loading = computed(() => false);

const writeArticle = () => {
    const canContinue = loginModal.requireLogin(() => router.push('/editor/new'), {
        title: '登录后开始创作',
        message: '登录后可以保存草稿、发布文章，并在个人中心管理内容。',
        actionText: '登录并写文章'
    });
    if (canContinue) {
        router.push('/editor/new');
    }
};

</script>

<template>
    <aside class="sidebar" aria-label="侧边栏" data-testid="home-sidebar">
        <SidebarBlock eyebrow="本周" title="热门专题" compact data-testid="home-specials">
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
                    >
                    <span>{{ topic.title }}</span>
                </RouterLink>
                <p v-if="!loading && !props.topics.length" class="sidebar-empty">专题内容正在整理中。</p>
            </div>
        </SidebarBlock>

        <SidebarBlock eyebrow="创作者" title="活跃作者" compact data-testid="home-authors">
            <ol class="author-rank">
                <li v-for="(author, index) in props.authors" :key="author.user.id">
                    <span class="rank-no">{{ index + 1 }}</span>
                    <RouterLink class="home-author-avatar" :to="`/users/${author.user.id}`">
                        <img :src="author.user.avatar" alt="作者头像" loading="lazy">
                    </RouterLink>
                    <div>
                        <RouterLink :to="`/users/${author.user.id}`">{{ author.user.name }}</RouterLink>
                        <span>{{ author.totalViewCount }} 阅读 · {{ author.articleCount }} 篇</span>
                    </div>
                </li>
                <li v-if="!loading && !props.authors.length" class="sidebar-empty">
                    作者榜正在生成中。
                </li>
            </ol>
        </SidebarBlock>

        <SidebarBlock eyebrow="开始创作" title="把项目经验写成下一篇文章" compact class="write-box" data-testid="home-write-box">
            <p>标题、Markdown、分类、标签、封面图，第一版先把写作路径做顺。</p>
            <button class="primary-action block" type="button" data-testid="home-write-button" @click="writeArticle">发布文章</button>
        </SidebarBlock>

        <AdBanner :slot-code="'home_sidebar'" />
    </aside>
</template>

<style scoped>
.sidebar-empty {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
    padding: 12px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

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
</style>
