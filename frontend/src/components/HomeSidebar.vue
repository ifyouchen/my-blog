<script setup>
import { inject, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { useRouter } from 'vue-router';
import { getRecommendedColumnsApi } from '@/api/columns';
import { getAuthorRankingsApi } from '@/api/rankings';

const router = useRouter();
const loginModal = inject('loginModal', { requireLogin: () => false });
const specials = ref([]);
const authors = ref([]);
const loading = ref(false);

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

const fetchSidebarData = async () => {
    loading.value = true;
    try {
        const [columnItems, authorItems] = await Promise.all([
            getRecommendedColumnsApi(3),
            getAuthorRankingsApi(3)
        ]);
        specials.value = columnItems || [];
        authors.value = authorItems || [];
    } catch (error) {
        specials.value = [];
        authors.value = [];
    } finally {
        loading.value = false;
    }
};

onMounted(fetchSidebarData);
</script>

<template>
    <aside class="sidebar" aria-label="侧边栏">
        <section class="side-section">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">本周</p>
                    <h2>热门专题</h2>
                </div>
            </div>
            <div class="special-list">
                <RouterLink
                    v-for="special in specials"
                    :key="special.id"
                    class="special-item"
                    :to="`/columns/${special.id}`"
                >
                    <img :src="special.coverUrl" :alt="`${special.title} 封面`">
                    <span>{{ special.title }}</span>
                </RouterLink>
                <p v-if="!loading && !specials.length" class="sidebar-empty">专栏内容正在整理中。</p>
            </div>
        </section>

        <section class="side-section">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">创作者</p>
                    <h2>活跃作者</h2>
                </div>
            </div>
            <ol class="author-rank">
                <li v-for="(author, index) in authors" :key="author.user.id">
                    <span class="rank-no">{{ index + 1 }}</span>
                    <img :src="author.user.avatar" alt="作者头像">
                    <div>
                        <RouterLink :to="`/users/${author.user.id}`">{{ author.user.name }}</RouterLink>
                        <span>{{ author.totalViewCount }} 阅读 · {{ author.articleCount }} 篇</span>
                    </div>
                </li>
                <li v-if="!loading && !authors.length" class="sidebar-empty">
                    作者榜正在生成中。
                </li>
            </ol>
        </section>

        <section class="side-section write-box">
            <p class="eyebrow">开始创作</p>
            <h2>把项目经验写成下一篇文章</h2>
            <p>标题、Markdown、分类、标签、封面图，第一版先把写作路径做顺。</p>
            <button class="primary-action block" type="button" @click="writeArticle">发布文章</button>
        </section>
    </aside>
</template>

<style scoped>
.sidebar-empty {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.6;
}

.author-rank a {
    color: var(--text);
    text-decoration: none;
}
</style>
