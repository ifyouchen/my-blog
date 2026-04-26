<script setup>
import { onMounted, reactive, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import AdminPagination from '@/components/admin/AdminPagination.vue';
import {
    deleteAdminArticleApi,
    getAdminArticlesApi,
    getCategoriesApi,
    updateAdminArticleStatusApi
} from '@/api/admin';
import {
    createPagedState,
    readPositiveInt,
    readQueryText,
    resolveAdminOverflowPage,
    syncAdminQuery,
    useAdminRefresh
} from '@/views/admin/adminShared';

const route = useRoute();
const router = useRouter();

const state = reactive({
    ...createPagedState(),
    status: '',
    keyword: '',
    category: '',
    categoryOptions: [],
    feedback: '',
    feedbackType: 'success',
    actionLoadingId: null
});

const applyRouteState = () => {
    state.page = readPositiveInt(route.query.page, 1);
    state.status = readQueryText(route, 'status');
    state.keyword = readQueryText(route, 'keyword');
    state.category = readQueryText(route, 'category');
    state.jumpPage = String(state.page);
};

const loadCategories = async () => {
    try {
        const categories = await getCategoriesApi(true);
        state.categoryOptions = (categories || []).map((item) => item.name).filter(Boolean);
    } catch (error) {
        state.categoryOptions = [];
    }
};

const loadArticles = async () => {
    state.loading = true;
    state.error = '';
    try {
        const result = await getAdminArticlesApi(
            state.page,
            state.pageSize,
            state.status || null,
            state.keyword || null,
            state.category || null
        );
        const overflowPage = resolveAdminOverflowPage(state, result);
        if (overflowPage) {
            state.page = overflowPage;
            state.jumpPage = String(overflowPage);
            await syncQuery({
                page: overflowPage > 1 ? String(overflowPage) : undefined
            });
            return;
        }
        state.items = result.items || [];
        state.total = result.total || 0;
        state.page = result.page || state.page;
        state.jumpPage = String(state.page);
    } catch (error) {
        state.items = [];
        state.total = 0;
        state.error = error.message || '文章列表加载失败';
    } finally {
        state.loading = false;
    }
};

const syncQuery = async (patch = {}) => {
    await syncAdminQuery(router, route, {
        page: patch.page ?? (state.page > 1 ? String(state.page) : undefined),
        status: patch.status ?? (state.status || undefined),
        keyword: patch.keyword ?? (state.keyword || undefined),
        category: patch.category ?? (state.category || undefined)
    });
};

const submitFilters = async () => {
    state.page = 1;
    await syncQuery({
        page: undefined,
        status: state.status || undefined,
        keyword: state.keyword || undefined,
        category: state.category || undefined
    });
};

const resetFilters = async () => {
    state.status = '';
    state.keyword = '';
    state.category = '';
    state.page = 1;
    await syncQuery({ page: undefined, status: undefined, keyword: undefined, category: undefined });
};

const changePage = async (targetPage) => {
    state.page = targetPage;
    await syncQuery({
        page: targetPage > 1 ? String(targetPage) : undefined
    });
};

const setFeedback = (message, type = 'success') => {
    state.feedback = message;
    state.feedbackType = type;
};

const toggleArticleStatus = async (article) => {
    const nextStatus = article.status === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED';
    const confirmed = window.confirm(
        nextStatus === 'OFFLINE'
            ? `确定下架文章《${article.title}》吗？`
            : `确定重新发布文章《${article.title}》吗？`
    );
    if (!confirmed) {
        return;
    }
    state.actionLoadingId = article.id;
    try {
        await updateAdminArticleStatusApi(article.id, nextStatus);
        setFeedback(nextStatus === 'OFFLINE' ? '文章已下架' : '文章已重新发布');
        await loadArticles();
    } catch (error) {
        setFeedback(error.message || '文章状态更新失败', 'error');
    } finally {
        state.actionLoadingId = null;
    }
};

const deleteArticle = async (article) => {
    if (!window.confirm(`确定删除文章《${article.title}》吗？删除后将从前台移除。`)) {
        return;
    }
    state.actionLoadingId = article.id;
    try {
        await deleteAdminArticleApi(article.id);
        setFeedback('文章已删除');
        await loadArticles();
    } catch (error) {
        setFeedback(error.message || '文章删除失败', 'error');
    } finally {
        state.actionLoadingId = null;
    }
};

useAdminRefresh(loadArticles);

onMounted(loadCategories);

watch(
    () => [route.query.page, route.query.status, route.query.keyword, route.query.category],
    () => {
        applyRouteState();
        loadArticles();
    },
    { immediate: true }
);
</script>

<template>
    <section class="dashboard-content-panel">
        <div class="admin-toolbar">
            <form class="admin-filter-toolbar" @submit.prevent="submitFilters">
                <label>
                    <span>文章状态</span>
                    <select v-model="state.status">
                        <option value="">全部</option>
                        <option value="PUBLISHED">已发布</option>
                        <option value="DRAFT">草稿</option>
                        <option value="OFFLINE">已下架</option>
                    </select>
                </label>
                <label>
                    <span>分类</span>
                    <select v-model="state.category">
                        <option value="">全部分类</option>
                        <option v-for="category in state.categoryOptions" :key="category" :value="category">
                            {{ category }}
                        </option>
                    </select>
                </label>
                <label class="admin-filter-grow">
                    <span>关键词</span>
                    <input v-model.trim="state.keyword" type="text" placeholder="搜索标题">
                </label>
                <div class="admin-filter-actions">
                    <button type="submit">查询</button>
                    <button type="button" @click="resetFilters">重置</button>
                </div>
            </form>
            <p v-if="state.feedback" :class="['backend-state-text', state.feedbackType === 'error' ? 'error-text' : 'success-text']">
                {{ state.feedback }}
            </p>
        </div>

        <div class="admin-table-shell">
            <p v-if="state.loading" class="backend-state-text">文章数据加载中...</p>
            <p v-else-if="state.error" class="backend-state-text error-text">{{ state.error }}</p>
            <template v-else>
                <div class="admin-table-wrap" data-testid="admin-articles-table">
                    <table class="admin-table">
                        <thead>
                            <tr>
                                <th>标题</th>
                                <th>作者</th>
                                <th>分类</th>
                                <th>状态</th>
                                <th>阅读 / 点赞 / 收藏 / 评论</th>
                                <th>发布时间</th>
                                <th>创建时间</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="article in state.items" :key="article.id">
                                <td>
                                    <RouterLink :to="`/articles/${article.id}`">{{ article.title }}</RouterLink>
                                </td>
                                <td>
                                    <strong>{{ article.authorNickname || article.authorUsername || '-' }}</strong>
                                    <p class="admin-subtext">@{{ article.authorUsername || '-' }}</p>
                                </td>
                                <td>{{ article.category || '-' }}</td>
                                <td>
                                    <span class="status-pill" :class="{ warning: article.status === 'OFFLINE' }">
                                        {{ article.status }}
                                    </span>
                                </td>
                                <td>
                                    {{ article.viewCount }} / {{ article.likeCount }} / {{ article.favoriteCount }} / {{ article.commentCount }}
                                </td>
                                <td>{{ article.publishedAt || '-' }}</td>
                                <td>{{ article.createdAt }}</td>
                                <td class="table-actions">
                                    <button
                                        type="button"
                                        :disabled="state.actionLoadingId === article.id"
                                        @click="toggleArticleStatus(article)"
                                    >
                                        {{ state.actionLoadingId === article.id ? '处理中...' : (article.status === 'PUBLISHED' ? '下架' : '发布') }}
                                    </button>
                                    <button
                                        type="button"
                                        class="danger-link"
                                        :disabled="state.actionLoadingId === article.id"
                                        @click="deleteArticle(article)"
                                    >
                                        删除
                                    </button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p v-if="!state.items.length" class="backend-state-text">当前筛选条件下没有文章</p>
            </template>
        </div>

        <AdminPagination :state="state" label="文章分页" @page-change="changePage" />
    </section>
</template>
