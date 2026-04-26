<script setup>
import { computed, inject } from 'vue';
import { RouterLink } from 'vue-router';
import { useRouter } from 'vue-router';
import SidebarBlock from '@/components/SidebarBlock.vue';

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
                    v-for="special in props.specials"
                    :key="special.id"
                    class="special-item"
                    :to="`/columns/${special.id}`"
                >
                    <img :src="special.coverUrl" :alt="`${special.title} 封面`" loading="lazy">
                    <span>{{ special.title }}</span>
                </RouterLink>
                <p v-if="!loading && !props.specials.length" class="sidebar-empty">专栏内容正在整理中。</p>
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
    </aside>
</template>

<style scoped>
.sidebar-empty {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
    padding: 16px 14px;
    background: linear-gradient(180deg, rgba(248, 251, 255, 0.96), #ffffff);
    border: 1px dashed rgba(208, 219, 236, 0.92);
    border-radius: 16px;
}

.special-item,
.author-rank li {
    transition: transform 0.16s ease, border-color 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease;
}

.special-item:hover,
.special-item:focus-visible,
.author-rank li:hover {
    background: color-mix(in srgb, var(--brand-soft) 34%, #ffffff);
    box-shadow: 0 14px 24px rgba(31, 78, 168, 0.08);
    transform: translateY(-1px);
}

.author-rank a {
    color: var(--text);
    text-decoration: none;
}

.home-author-avatar {
    display: inline-flex;
    border-radius: 14px;
}
</style>
