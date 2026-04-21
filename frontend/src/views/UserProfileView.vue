<script setup>
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import ArticleFeed from '@/components/ArticleFeed.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { getUserArticlesApi } from '@/api/articles';
import { getUserProfileApi } from '@/api/auth';

const route = useRoute();
const profile = ref(null);
const articles = ref([]);
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const loading = ref(false);
const errorMessage = ref('');

const loadProfile = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const userId = Number(route.params.id);
        const [profileData, articlePage] = await Promise.all([
            getUserProfileApi(userId),
            getUserArticlesApi(userId, { page: page.value, pageSize })
        ]);
        profile.value = profileData;
        articles.value = articlePage.items || [];
        total.value = articlePage.total || 0;
    } catch (error) {
        profile.value = null;
        articles.value = [];
        total.value = 0;
        errorMessage.value = error.message || '作者主页加载失败';
    } finally {
        loading.value = false;
    }
};

const changePage = async (nextPage) => {
    page.value = nextPage;
    await loadProfile();
};

watch(() => route.params.id, () => {
    page.value = 1;
    loadProfile();
}, { immediate: true });
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <section v-if="profile" class="profile-head">
            <img :src="profile.user.avatar" alt="用户头像">
            <div>
                <p class="eyebrow">技术作者</p>
                <h1>{{ profile.user.nickname }}</h1>
                <p>{{ profile.user.bio || '持续分享项目实践与工程经验。' }}</p>
                <div class="profile-stats">
                    <span><strong>{{ profile.articleCount }}</strong> 文章</span>
                    <span><strong>{{ profile.totalViewCount }}</strong> 阅读</span>
                    <span><strong>{{ profile.totalLikeCount }}</strong> 获赞</span>
                </div>
            </div>
        </section>

        <section v-else class="collection-head">
            <p class="eyebrow">作者主页</p>
            <h1>暂时无法加载作者信息</h1>
            <p>{{ errorMessage || '请稍后重试。' }}</p>
        </section>

        <ArticleFeed
            :articles="articles"
            :page="page"
            :page-size="pageSize"
            :total="total"
            :loading="loading"
            :error-message="errorMessage"
            eyebrow="作者文章"
            title="最新发布"
            empty-text="这位作者还没有公开发布文章"
            @page-change="changePage"
        />
    </main>
</template>
