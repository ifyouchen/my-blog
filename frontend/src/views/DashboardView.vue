<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import { getMyFavoritesApi } from '@/api/favorites';
import { deleteArticleApi, getMyArticlesApi } from '@/api/articles';
import { useSession } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const { isLoggedIn } = useSession();

const isFavorites = computed(() => route.name === 'favorites');
const articleStatus = ref(String(route.query.status || ''));
const currentPage = ref(Number.parseInt(route.query.page || '1', 10) || 1);
const pageSize = 10;
const articles = ref([]);
const favorites = ref([]);
const total = ref(0);
const isLoading = ref(false);
const loadError = ref('');
const feedback = ref('');

const statusOptions = [
    { label: '全部', value: '' },
    { label: '草稿', value: 'DRAFT' },
    { label: '已发布', value: 'PUBLISHED' },
    { label: '已下架', value: 'OFFLINE' },
    { label: '已删除', value: 'DELETED' }
];

const syncRoute = (overrides = {}) => {
    const nextPage = String(overrides.page ?? currentPage.value);
    const nextStatus = overrides.status ?? articleStatus.value;
    router.replace({
        path: isFavorites.value ? '/dashboard/favorites' : '/dashboard/articles',
        query: {
            page: nextPage === '1' ? undefined : nextPage,
            status: isFavorites.value ? undefined : (nextStatus || undefined)
        }
    });
};

const fetchArticles = async () => {
    isLoading.value = true;
    loadError.value = '';
    try {
        const pageResult = await getMyArticlesApi({
            page: currentPage.value,
            pageSize,
            status: articleStatus.value
        });
        articles.value = pageResult.items || [];
        total.value = pageResult.total || 0;
    } catch (error) {
        articles.value = [];
        total.value = 0;
        loadError.value = error.message || '加载文章失败';
    } finally {
        isLoading.value = false;
    }
};

const fetchFavorites = async () => {
    isLoading.value = true;
    loadError.value = '';
    try {
        const pageResult = await getMyFavoritesApi(currentPage.value, pageSize);
        favorites.value = pageResult.items || [];
        total.value = pageResult.total || 0;
    } catch (error) {
        favorites.value = [];
        total.value = 0;
        loadError.value = error.message || '加载收藏失败';
    } finally {
        isLoading.value = false;
    }
};

const fetchCurrentTab = async () => {
    if (!isLoggedIn.value) {
        loadError.value = '请先登录后查看个人中心内容';
        articles.value = [];
        favorites.value = [];
        total.value = 0;
        return;
    }
    if (isFavorites.value) {
        await fetchFavorites();
        return;
    }
    await fetchArticles();
};

const changePage = (page) => {
    currentPage.value = page;
    syncRoute({ page });
};

const changeStatus = (status) => {
    articleStatus.value = status;
    currentPage.value = 1;
    syncRoute({ page: 1, status });
};

const editArticle = (articleId) => {
    router.push(`/editor/${articleId}`);
};

const removeArticle = async (articleId) => {
    if (!confirm('确定删除这篇文章吗？')) {
        return;
    }
    try {
        await deleteArticleApi(articleId);
        feedback.value = '文章已删除';
        await fetchCurrentTab();
    } catch (error) {
        feedback.value = error.message || '删除失败';
    }
};

watch(
    () => [route.name, route.query.page, route.query.status],
    () => {
        currentPage.value = Number.parseInt(route.query.page || '1', 10) || 1;
        articleStatus.value = String(route.query.status || '');
        fetchCurrentTab();
    },
    { immediate: true }
);

watch(isLoggedIn, () => {
    fetchCurrentTab();
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell dashboard-layout">
        <aside class="dashboard-nav">
            <p class="eyebrow">创作者后台</p>
            <RouterLink to="/dashboard/articles">我的文章</RouterLink>
            <RouterLink to="/dashboard/favorites">我的收藏</RouterLink>
            <RouterLink to="/settings/profile">个人资料</RouterLink>
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

            <div v-if="!isFavorites" class="dashboard-toolbar">
                <div class="status-tabs">
                    <button
                        v-for="option in statusOptions"
                        :key="option.value || 'ALL'"
                        type="button"
                        :class="{ active: articleStatus === option.value }"
                        @click="changeStatus(option.value)"
                    >
                        {{ option.label }}
                    </button>
                </div>
                <p v-if="feedback" class="form-message success">{{ feedback }}</p>
            </div>

            <div v-if="isFavorites" class="favorite-grid">
                <p v-if="isLoading" class="loading-text">加载中...</p>
                <p v-else-if="loadError" class="error-text">{{ loadError }}</p>
                <article v-else-if="favorites.length" v-for="article in favorites" :key="article.id" class="favorite-card">
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
                <p v-if="isLoading" class="loading-text">正在加载文章...</p>
                <p v-else-if="loadError" class="error-text">{{ loadError }}</p>
                <table v-else>
                    <thead>
                        <tr>
                            <th>标题</th>
                            <th>状态</th>
                            <th>阅读</th>
                            <th>点赞</th>
                            <th>收藏</th>
                            <th>评论</th>
                            <th>更新时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="article in articles" :key="article.id">
                            <td>{{ article.title }}</td>
                            <td><span class="status-pill">{{ article.status }}</span></td>
                            <td>{{ article.viewCount }}</td>
                            <td>{{ article.likeCount }}</td>
                            <td>{{ article.favoriteCount }}</td>
                            <td>{{ article.commentCount }}</td>
                            <td>{{ article.publishedText }}</td>
                            <td class="table-actions">
                                <RouterLink :to="`/articles/${article.id}`">查看</RouterLink>
                                <button type="button" @click="editArticle(article.id)">编辑</button>
                                <button type="button" class="danger-link" @click="removeArticle(article.id)">删除</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <p v-if="!isLoading && !loadError && !articles.length" class="empty-text">还没有文章，先去写下第一篇吧</p>
            </div>

            <div v-if="total > pageSize" class="dashboard-pagination">
                <button type="button" :disabled="currentPage <= 1 || isLoading" @click="changePage(currentPage - 1)">上一页</button>
                <span>第 {{ currentPage }} 页</span>
                <button type="button" :disabled="currentPage * pageSize >= total || isLoading" @click="changePage(currentPage + 1)">下一页</button>
            </div>
        </section>
    </main>
</template>

<style scoped>
.dashboard-toolbar {
    display: grid;
    gap: 14px;
    margin-bottom: 20px;
}

.status-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.status-tabs button {
    padding: 8px 14px;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fff;
    color: var(--muted);
}

.status-tabs button.active {
    color: var(--brand);
    border-color: var(--brand);
    background: rgba(15, 143, 117, 0.08);
}

.table-actions {
    display: flex;
    gap: 10px;
    align-items: center;
}

.table-actions button {
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--brand);
}

.danger-link {
    color: #d14343 !important;
}

.dashboard-pagination {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 18px;
}
</style>
