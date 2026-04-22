<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import CreatorSidebar from '@/components/CreatorSidebar.vue';
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
const jumpPage = ref(String(currentPage.value));

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
    const totalPages = Math.max(1, Math.ceil(total.value / pageSize));
    if (page < 1 || page > totalPages || page === currentPage.value || isLoading.value) {
        return;
    }
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

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));
const pageStart = computed(() => {
    if (!total.value) {
        return 0;
    }
    return (currentPage.value - 1) * pageSize + 1;
});
const pageEnd = computed(() => Math.min(currentPage.value * pageSize, total.value));

const paginationItems = computed(() => {
    const pages = [];
    const appendPage = (page) => {
        if (!pages.some((item) => item.type === 'page' && item.value === page)) {
            pages.push({ type: 'page', value: page });
        }
    };
    const appendEllipsis = (key) => {
        pages.push({ type: 'ellipsis', value: key });
    };

    if (totalPages.value <= 7) {
        for (let page = 1; page <= totalPages.value; page++) {
            appendPage(page);
        }
        return pages;
    }

    appendPage(1);
    if (currentPage.value > 4) {
        appendEllipsis('left');
    }

    const start = Math.max(2, currentPage.value - 1);
    const end = Math.min(totalPages.value - 1, currentPage.value + 1);
    for (let page = start; page <= end; page++) {
        appendPage(page);
    }

    if (currentPage.value < totalPages.value - 3) {
        appendEllipsis('right');
    }
    appendPage(totalPages.value);
    return pages;
});

const submitJump = () => {
    const page = Number.parseInt(jumpPage.value, 10);
    if (Number.isNaN(page)) {
        jumpPage.value = String(currentPage.value);
        return;
    }
    const targetPage = Math.min(Math.max(1, page), totalPages.value);
    jumpPage.value = String(targetPage);
    changePage(targetPage);
};

watch(
    () => [route.name, route.query.page, route.query.status],
    () => {
        currentPage.value = Number.parseInt(route.query.page || '1', 10) || 1;
        articleStatus.value = String(route.query.status || '');
        jumpPage.value = String(currentPage.value);
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
        <CreatorSidebar />

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

            <section v-if="isFavorites" class="dashboard-content-panel">
                <p v-if="isLoading" class="loading-text">加载中...</p>
                <p v-else-if="loadError" class="error-text">{{ loadError }}</p>
                <div v-else-if="favorites.length" class="favorite-grid">
                    <article v-for="article in favorites" :key="article.id" class="favorite-card">
                        <img :src="article.cover" :alt="article.coverAlt">
                        <div>
                            <span>{{ article.category }}</span>
                            <h2>{{ article.title }}</h2>
                            <p>{{ article.summary }}</p>
                            <RouterLink :to="`/articles/${article.id}`">继续阅读</RouterLink>
                        </div>
                    </article>
                </div>
                <p v-else class="empty-text">暂无收藏，去发现感兴趣的文章吧</p>
            </section>

            <section v-else class="dashboard-content-panel table-panel">
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
            </section>

            <nav v-if="totalPages > 1" class="dashboard-pagination" aria-label="后台分页">
                <p>
                    第 {{ currentPage }} / {{ totalPages }} 页，
                    共 {{ total }} 条，当前 {{ pageStart }}-{{ pageEnd }} 条
                </p>
                <div class="dashboard-pagination-actions">
                    <button type="button" :disabled="isLoading || currentPage <= 1" @click="changePage(1)">首页</button>
                    <button type="button" :disabled="isLoading || currentPage <= 1" @click="changePage(currentPage - 1)">上一页</button>
                    <template v-for="item in paginationItems" :key="`${item.type}-${item.value}`">
                        <span v-if="item.type === 'ellipsis'" class="pagination-ellipsis">...</span>
                        <button
                            v-else
                            type="button"
                            :class="{ active: item.value === currentPage }"
                            :disabled="isLoading || item.value === currentPage"
                            @click="changePage(item.value)"
                        >
                            {{ item.value }}
                        </button>
                    </template>
                    <button type="button" :disabled="isLoading || currentPage >= totalPages" @click="changePage(currentPage + 1)">下一页</button>
                    <button type="button" :disabled="isLoading || currentPage >= totalPages" @click="changePage(totalPages)">末页</button>
                </div>
                <form class="dashboard-pagination-jump" @submit.prevent="submitJump">
                    <label for="dashboard-page-jump">跳至</label>
                    <input
                        id="dashboard-page-jump"
                        v-model="jumpPage"
                        type="number"
                        min="1"
                        :max="totalPages"
                        :disabled="isLoading"
                        inputmode="numeric"
                    >
                    <span>页</span>
                    <button type="submit" :disabled="isLoading">跳转</button>
                </form>
            </nav>
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
    display: grid;
    gap: 12px;
    padding: 16px 18px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
}

.dashboard-pagination p {
    margin: 0;
    color: var(--muted);
    font-size: 14px;
}

.dashboard-pagination-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
}

.dashboard-pagination-actions button,
.dashboard-pagination-jump button {
    min-width: 40px;
    min-height: 36px;
    padding: 0 12px;
    color: var(--text);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 8px;
}

.dashboard-pagination-actions button:hover:not(:disabled),
.dashboard-pagination-jump button:hover:not(:disabled),
.dashboard-pagination-actions button.active {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.dashboard-pagination-jump {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.dashboard-pagination-jump label,
.dashboard-pagination-jump span {
    color: var(--muted);
    font-size: 14px;
}

.dashboard-pagination-jump input {
    width: 76px;
    min-height: 36px;
    padding: 0 10px;
    color: var(--text);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 8px;
    outline: 0;
}

.dashboard-pagination-jump input:focus {
    border-color: var(--brand);
}

@media (max-width: 760px) {
    .dashboard-pagination-jump {
        width: 100%;
    }

    .dashboard-pagination-jump input,
    .dashboard-pagination-jump button {
        width: 100%;
    }
}
</style>
