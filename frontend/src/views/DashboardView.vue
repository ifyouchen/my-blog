<script setup>
import { ref, computed, onMounted } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import { getMyFavoritesApi } from '@/api/favorites';
import { articles, dashboardArticles } from '@/data/home';
import { useSession } from '@/stores/session';

const route = useRoute();
const { state: sessionState, isLoggedIn } = useSession();

const isFavorites = computed(() => route.name === 'favorites');
const remoteFavorites = ref([]);
const isLoading = ref(false);
const loadError = ref('');

const favoriteArticles = computed(() => {
    if (isLoggedIn.value && remoteFavorites.value.length > 0) {
        return remoteFavorites.value;
    }
    const storedFavorites = articles.filter((article) => {
        return localStorage.getItem(`my-blog-favorited-${article.id}`) === 'true';
    });

    return storedFavorites.length ? storedFavorites : articles.slice(0, 2);
});

const fetchMyFavorites = async () => {
    if (!isLoggedIn.value) {
        return;
    }
    isLoading.value = true;
    loadError.value = '';
    try {
        const data = await getMyFavoritesApi(1, 20);
        remoteFavorites.value = data.list || [];
    } catch (error) {
        loadError.value = error.message || '加载失败';
    } finally {
        isLoading.value = false;
    }
};

onMounted(() => {
    if (isFavorites.value) {
        fetchMyFavorites();
    }
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell dashboard-layout">
        <aside class="dashboard-nav">
            <p class="eyebrow">创作者后台</p>
            <RouterLink to="/dashboard/articles">我的文章</RouterLink>
            <RouterLink to="/dashboard/favorites">我的收藏</RouterLink>
            <RouterLink to="/editor/new">写文章</RouterLink>
        </aside>

        <section class="dashboard-main">
            <div class="section-heading">
                <div>
                    <p class="eyebrow">{{ isFavorites ? '收藏夹' : '内容管理' }}</p>
                    <h1>{{ isFavorites ? '我的收藏' : '我的文章' }}</h1>
                </div>
                <RouterLink v-if="!isFavorites" class="primary-action" to="/editor/new">新建文章</RouterLink>
            </div>

            <div v-if="isFavorites" class="favorite-grid">
                <p v-if="isLoading" class="loading-text">加载中...</p>
                <p v-else-if="loadError" class="error-text">{{ loadError }}</p>
                <article v-else-if="favoriteArticles.length" v-for="article in favoriteArticles" :key="article.id" class="favorite-card">
                    <img :src="article.cover" :alt="article.coverAlt">
                    <div>
                        <span>{{ article.category }}</span>
                        <h2>{{ article.title }}</h2>
                        <p>{{ article.summary }}</p>
                        <RouterLink :to="`/articles/${article.id}`">继续阅读</RouterLink>
                    </div>
                </article>
                <p v-else class="empty-text">暂无收藏，去发现感兴趣的文章吧</p>
            </div>

            <div v-else class="table-panel">
                <table>
                    <thead>
                        <tr>
                            <th>标题</th>
                            <th>状态</th>
                            <th>阅读</th>
                            <th>点赞</th>
                            <th>评论</th>
                            <th>更新时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="article in dashboardArticles" :key="article.id">
                            <td>{{ article.title }}</td>
                            <td><span class="status-pill">{{ article.status }}</span></td>
                            <td>{{ article.views }}</td>
                            <td>{{ article.likes }}</td>
                            <td>{{ article.comments }}</td>
                            <td>{{ article.updatedAt }}</td>
                            <td>
                                <RouterLink :to="`/articles/${article.id}`">查看</RouterLink>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</template>