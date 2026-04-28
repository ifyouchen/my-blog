<script setup>
import { onMounted, reactive, ref } from 'vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {
    createCategoryApi,
    createTagApi,
    deleteAdminCommentApi,
    deleteCategoryApi,
    deleteTagApi,
    getAdminArticlesApi,
    getAdminCategoriesApi,
    getAdminCommentsApi,
    getAdminLogsApi,
    getAdminStatsApi,
    getAdminTagsApi,
    getAdminUsersApi,
    updateAdminArticleStatusApi,
    updateAdminUserStatusApi,
    updateCategoryApi,
    updateTagApi
} from '@/api/admin';

const DEFAULT_PAGE_SIZE = 10;

const stats = ref({
    totalUsers: 0,
    normalUsers: 0,
    totalArticles: 0,
    publishedArticles: 0,
    totalComments: 0
});
const users = ref([]);
const articles = ref([]);
const comments = ref([]);
const logs = ref([]);

const categoryForm = reactive({ name: '', description: '', sortOrder: 0 });
const tagForm = reactive({ name: '', description: '' });

const categoryState = reactive(createPagedResourceState(8));
const tagState = reactive(createPagedResourceState(8));

/**
 * 创建后台分页状态。
 *
 * @param pageSize 每页数量
 * @returns {object} 分页状态
 */
function createPagedResourceState(pageSize = DEFAULT_PAGE_SIZE) {
    return {
        items: [],
        page: 1,
        pageSize,
        total: 0,
        enabled: '',
        loading: false,
        error: '',
        jumpPage: '1'
    };
}

/**
 * 规范化启用状态筛选值。
 *
 * @param {string} value 筛选值
 * @returns {boolean|null} 启用状态
 */
function normalizeEnabledFilter(value) {
    if (value === 'true') {
        return true;
    }
    if (value === 'false') {
        return false;
    }
    return null;
}

/**
 * 获取分页总页数。
 *
 * @param {object} state 分页状态
 * @returns {number} 总页数
 */
function getTotalPages(state) {
    return Math.max(1, Math.ceil((state.total || 0) / state.pageSize));
}

/**
 * 获取当前页起始序号。
 *
 * @param {object} state 分页状态
 * @returns {number} 起始序号
 */
function getPageStart(state) {
    if (!state.total) {
        return 0;
    }
    return (state.page - 1) * state.pageSize + 1;
}

/**
 * 获取当前页结束序号。
 *
 * @param {object} state 分页状态
 * @returns {number} 结束序号
 */
function getPageEnd(state) {
    return Math.min(state.page * state.pageSize, state.total || 0);
}

/**
 * 生成分页按钮集合。
 *
 * @param {object} state 分页状态
 * @returns {Array<object>} 分页按钮
 */
function getPaginationItems(state) {
    const totalPages = getTotalPages(state);
    const currentPage = state.page;
    const items = [];

    const appendPage = (page) => {
        if (!items.some((item) => item.type === 'page' && item.value === page)) {
            items.push({ type: 'page', value: page });
        }
    };
    const appendEllipsis = (key) => {
        items.push({ type: 'ellipsis', value: key });
    };

    if (totalPages <= 7) {
        for (let page = 1; page <= totalPages; page += 1) {
            appendPage(page);
        }
        return items;
    }

    appendPage(1);
    if (currentPage > 4) {
        appendEllipsis('left');
    }

    const start = Math.max(2, currentPage - 1);
    const end = Math.min(totalPages - 1, currentPage + 1);
    for (let page = start; page <= end; page += 1) {
        appendPage(page);
    }

    if (currentPage < totalPages - 3) {
        appendEllipsis('right');
    }
    appendPage(totalPages);
    return items;
}

/**
 * 加载后台分类分页数据。
 *
 * @param {number} targetPage 目标页码
 * @returns {Promise<void>}
 */
async function fetchCategories(targetPage = categoryState.page) {
    categoryState.loading = true;
    categoryState.error = '';
    try {
        const result = await getAdminCategoriesApi(
            targetPage,
            categoryState.pageSize,
            normalizeEnabledFilter(categoryState.enabled)
        );
        const nextPage = result.page || targetPage;
        const nextTotal = result.total || 0;
        const maxPage = Math.max(1, Math.ceil(nextTotal / categoryState.pageSize));

        if (nextTotal > 0 && nextPage > maxPage) {
            categoryState.page = maxPage;
            categoryState.jumpPage = String(maxPage);
            await fetchCategories(maxPage);
            return;
        }

        if (nextTotal > 0 && nextPage > 1 && !(result.items || []).length) {
            categoryState.page = nextPage - 1;
            categoryState.jumpPage = String(categoryState.page);
            await fetchCategories(categoryState.page);
            return;
        }

        categoryState.items = result.items || [];
        categoryState.total = nextTotal;
        categoryState.page = nextPage;
        categoryState.jumpPage = String(categoryState.page);
    } catch (error) {
        categoryState.items = [];
        categoryState.total = 0;
        categoryState.error = error.message || '获取分类失败';
    } finally {
        categoryState.loading = false;
    }
}

/**
 * 加载后台标签分页数据。
 *
 * @param {number} targetPage 目标页码
 * @returns {Promise<void>}
 */
async function fetchTags(targetPage = tagState.page) {
    tagState.loading = true;
    tagState.error = '';
    try {
        const result = await getAdminTagsApi(
            targetPage,
            tagState.pageSize,
            normalizeEnabledFilter(tagState.enabled)
        );
        const nextPage = result.page || targetPage;
        const nextTotal = result.total || 0;
        const maxPage = Math.max(1, Math.ceil(nextTotal / tagState.pageSize));

        if (nextTotal > 0 && nextPage > maxPage) {
            tagState.page = maxPage;
            tagState.jumpPage = String(maxPage);
            await fetchTags(maxPage);
            return;
        }

        if (nextTotal > 0 && nextPage > 1 && !(result.items || []).length) {
            tagState.page = nextPage - 1;
            tagState.jumpPage = String(tagState.page);
            await fetchTags(tagState.page);
            return;
        }

        tagState.items = result.items || [];
        tagState.total = nextTotal;
        tagState.page = nextPage;
        tagState.jumpPage = String(tagState.page);
    } catch (error) {
        tagState.items = [];
        tagState.total = 0;
        tagState.error = error.message || '获取标签失败';
    } finally {
        tagState.loading = false;
    }
}

/**
 * 切换资源页码。
 *
 * @param {object} state 分页状态
 * @param {Function} loader 加载函数
 * @param {number} targetPage 目标页码
 * @returns {Promise<void>}
 */
async function changeResourcePage(state, loader, targetPage) {
    const totalPages = getTotalPages(state);
    if (state.loading || targetPage < 1 || targetPage > totalPages || targetPage === state.page) {
        return;
    }
    state.page = targetPage;
    state.jumpPage = String(targetPage);
    await loader(targetPage);
}

/**
 * 提交资源跳页。
 *
 * @param {object} state 分页状态
 * @param {Function} loader 加载函数
 * @returns {Promise<void>}
 */
async function submitResourceJump(state, loader) {
    const parsedPage = Number.parseInt(state.jumpPage, 10);
    if (Number.isNaN(parsedPage)) {
        state.jumpPage = String(state.page);
        return;
    }
    const targetPage = Math.min(Math.max(1, parsedPage), getTotalPages(state));
    await changeResourcePage(state, loader, targetPage);
}

/**
 * 更新资源筛选条件。
 *
 * @param {object} state 分页状态
 * @param {Function} loader 加载函数
 * @param {string} enabled 启用状态筛选
 * @returns {Promise<void>}
 */
async function changeResourceFilter(state, loader, enabled) {
    state.enabled = enabled;
    state.page = 1;
    state.jumpPage = '1';
    await loader(1);
}

/**
 * 获取统计数据。
 *
 * @returns {Promise<void>}
 */
const fetchStats = async () => {
    try {
        stats.value = await getAdminStatsApi();
    } catch (error) {
        console.error('获取统计数据失败:', error);
    }
};

/**
 * 获取用户列表。
 *
 * @returns {Promise<void>}
 */
const fetchUsers = async () => {
    try {
        const result = await getAdminUsersApi(1, 20);
        users.value = result.items || [];
    } catch (error) {
        console.error('获取用户列表失败:', error);
    }
};

/**
 * 获取文章列表。
 *
 * @returns {Promise<void>}
 */
const fetchArticles = async () => {
    try {
        const result = await getAdminArticlesApi(1, 20);
        articles.value = result.items || [];
    } catch (error) {
        console.error('获取文章列表失败:', error);
    }
};

/**
 * 获取评论列表。
 *
 * @returns {Promise<void>}
 */
const fetchComments = async () => {
    try {
        const result = await getAdminCommentsApi(1, 20);
        comments.value = result.items || [];
    } catch (error) {
        console.error('获取评论列表失败:', error);
    }
};

/**
 * 获取管理员日志。
 *
 * @returns {Promise<void>}
 */
const fetchLogs = async () => {
    try {
        const result = await getAdminLogsApi(1, 20);
        logs.value = result.items || [];
    } catch (error) {
        console.error('获取操作日志失败:', error);
    }
};

/**
 * 切换用户状态。
 *
 * @param {number} userId 用户 ID
 * @param {string} currentStatus 当前状态
 * @returns {Promise<void>}
 */
const toggleUserStatus = async (userId, currentStatus) => {
    const newStatus = currentStatus === 'NORMAL' ? 'DISABLED' : 'NORMAL';
    try {
        await updateAdminUserStatusApi(userId, newStatus);
        await fetchUsers();
        await fetchLogs();
    } catch (error) {
        alert(error.message || '操作失败');
    }
};

/**
 * 切换文章状态。
 *
 * @param {number} articleId 文章 ID
 * @param {string} currentStatus 当前状态
 * @returns {Promise<void>}
 */
const toggleArticleStatus = async (articleId, currentStatus) => {
    const newStatus = currentStatus === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED';
    try {
        await updateAdminArticleStatusApi(articleId, newStatus);
        await fetchArticles();
        await fetchLogs();
    } catch (error) {
        alert(error.message || '操作失败');
    }
};

/**
 * 删除评论。
 *
 * @param {number} commentId 评论 ID
 * @returns {Promise<void>}
 */
const handleDeleteComment = async (commentId) => {
    if (!confirm('确定删除这条评论吗？')) {
        return;
    }
    try {
        await deleteAdminCommentApi(commentId);
        await fetchComments();
        await fetchLogs();
    } catch (error) {
        alert(error.message || '删除失败');
    }
};

/**
 * 提交新增分类。
 *
 * @returns {Promise<void>}
 */
const submitCategory = async () => {
    try {
        await createCategoryApi({
            name: categoryForm.name,
            description: categoryForm.description,
            sortOrder: Number(categoryForm.sortOrder || 0)
        });
        categoryForm.name = '';
        categoryForm.description = '';
        categoryForm.sortOrder = 0;
        await fetchCategories(categoryState.page);
        await fetchLogs();
    } catch (error) {
        alert(error.message || '分类创建失败');
    }
};

/**
 * 切换分类启用状态。
 *
 * @param {object} category 分类数据
 * @returns {Promise<void>}
 */
const toggleCategory = async (category) => {
    try {
        await updateCategoryApi(category.id, {
            name: category.name,
            description: category.description,
            sortOrder: category.sortOrder || 0,
            enabled: !category.enabled
        });
        await fetchCategories(categoryState.page);
        await fetchLogs();
    } catch (error) {
        alert(error.message || '分类更新失败');
    }
};

/**
 * 删除分类。
 *
 * @param {number} categoryId 分类 ID
 * @returns {Promise<void>}
 */
const removeCategory = async (categoryId) => {
    if (!confirm('确定删除这个分类吗？')) {
        return;
    }
    try {
        await deleteCategoryApi(categoryId);
        await fetchCategories(categoryState.page);
        await fetchLogs();
    } catch (error) {
        alert(error.message || '分类删除失败');
    }
};

/**
 * 提交新增标签。
 *
 * @returns {Promise<void>}
 */
const submitTag = async () => {
    try {
        await createTagApi({
            name: tagForm.name,
            description: tagForm.description
        });
        tagForm.name = '';
        tagForm.description = '';
        await fetchTags(tagState.page);
        await fetchLogs();
    } catch (error) {
        alert(error.message || '标签创建失败');
    }
};

/**
 * 切换标签启用状态。
 *
 * @param {object} tag 标签数据
 * @returns {Promise<void>}
 */
const toggleTag = async (tag) => {
    try {
        await updateTagApi(tag.id, {
            name: tag.name,
            description: tag.description,
            enabled: !tag.enabled
        });
        await fetchTags(tagState.page);
        await fetchLogs();
    } catch (error) {
        alert(error.message || '标签更新失败');
    }
};

/**
 * 删除标签。
 *
 * @param {number} tagId 标签 ID
 * @returns {Promise<void>}
 */
const removeTag = async (tagId) => {
    if (!confirm('确定删除这个标签吗？')) {
        return;
    }
    try {
        await deleteTagApi(tagId);
        await fetchTags(tagState.page);
        await fetchLogs();
    } catch (error) {
        alert(error.message || '标签删除失败');
    }
};

onMounted(() => {
    fetchStats();
    fetchUsers();
    fetchArticles();
    fetchComments();
    fetchCategories(1);
    fetchTags(1);
    fetchLogs();
});
</script>

<template>
    <SiteHeader />
    <main class="page-shell admin-layout">
        <aside class="dashboard-nav">
            <p class="eyebrow">管理后台</p>
            <a href="#stats">数据概览</a>
            <a href="#users">用户管理</a>
            <a href="#articles">文章管理</a>
            <a href="#comments">评论管理</a>
            <a href="#categories">分类管理</a>
            <a href="#tags">标签管理</a>
            <a href="#logs">操作日志</a>
        </aside>

        <section class="dashboard-main">
            <section id="stats" class="admin-stats">
                <article>
                    <span>用户总数</span>
                    <strong>{{ stats.totalUsers }}</strong>
                </article>
                <article>
                    <span>活跃用户</span>
                    <strong>{{ stats.normalUsers }}</strong>
                </article>
                <article>
                    <span>文章总数</span>
                    <strong>{{ stats.totalArticles }}</strong>
                </article>
                <article>
                    <span>已发布</span>
                    <strong>{{ stats.publishedArticles }}</strong>
                </article>
                <article>
                    <span>评论总数</span>
                    <strong>{{ stats.totalComments }}</strong>
                </article>
            </section>

            <section id="users" class="table-panel">
                <div class="section-heading">
                    <div>
                        <p class="eyebrow">用户管理</p>
                        <h1>用户列表</h1>
                    </div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>用户名</th>
                            <th>邮箱</th>
                            <th>状态</th>
                            <th>注册时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="user in users" :key="user.id">
                            <td>{{ user.id }}</td>
                            <td>{{ user.username }}</td>
                            <td>{{ user.email }}</td>
                            <td><span class="status-pill">{{ user.status }}</span></td>
                            <td>{{ user.createdAt }}</td>
                            <td>
                                <button
                                    type="button"
                                    @click="toggleUserStatus(user.id, user.status)"
                                >
                                    {{ user.status === 'NORMAL' ? '禁用' : '启用' }}
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </section>

            <section id="articles" class="table-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">文章管理</p>
                        <h2>文章列表</h2>
                    </div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>标题</th>
                            <th>分类</th>
                            <th>状态</th>
                            <th>阅读</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="article in articles" :key="article.id">
                            <td>{{ article.title }}</td>
                            <td>{{ article.category }}</td>
                            <td><span class="status-pill">{{ article.status }}</span></td>
                            <td>{{ article.viewCount }}</td>
                            <td>
                                <button
                                    type="button"
                                    @click="toggleArticleStatus(article.id, article.status)"
                                >
                                    {{ article.status === 'PUBLISHED' ? '下架' : '发布' }}
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </section>

            <section id="comments" class="table-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">评论管理</p>
                        <h2>评论列表</h2>
                    </div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>文章ID</th>
                            <th>内容</th>
                            <th>时间</th>
                            <th>操作</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="comment in comments" :key="comment.id">
                            <td>{{ comment.id }}</td>
                            <td>{{ comment.articleId }}</td>
                            <td>{{ comment.content }}</td>
                            <td>{{ comment.createdAt }}</td>
                            <td>
                                <button
                                    type="button"
                                    @click="handleDeleteComment(comment.id)"
                                >
                                    删除
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </section>

            <section id="categories" class="dashboard-content-panel admin-resource-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">内容组织</p>
                        <h2>分类管理</h2>
                    </div>
                </div>
                <div class="admin-resource-toolbar">
                    <form class="admin-inline-form" @submit.prevent="submitCategory">
                        <input v-model.trim="categoryForm.name" type="text" placeholder="分类名称" required>
                        <input v-model.trim="categoryForm.description" type="text" placeholder="分类说明">
                        <input v-model.number="categoryForm.sortOrder" type="number" placeholder="排序值">
                        <button type="submit">新增分类</button>
                    </form>
                    <div class="admin-filter-row">
                        <label for="category-enabled-filter">状态筛选</label>
                        <select
                            id="category-enabled-filter"
                            :value="categoryState.enabled"
                            @change="changeResourceFilter(categoryState, fetchCategories, $event.target.value)"
                        >
                            <option value="">全部</option>
                            <option value="true">仅启用</option>
                            <option value="false">仅禁用</option>
                        </select>
                    </div>
                </div>

                <div class="admin-resource-body">
                    <p v-if="categoryState.loading" class="backend-state-text">分类数据加载中...</p>
                    <p v-else-if="categoryState.error" class="backend-state-text error-text">{{ categoryState.error }}</p>
                    <template v-else-if="categoryState.items.length">
                        <div class="admin-resource-table-wrap">
                            <table class="admin-resource-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>名称</th>
                                        <th>说明</th>
                                        <th>排序</th>
                                        <th>状态</th>
                                        <th>操作</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="category in categoryState.items" :key="category.id">
                                        <td>{{ category.id }}</td>
                                        <td>{{ category.name }}</td>
                                        <td>{{ category.description || '-' }}</td>
                                        <td>{{ category.sortOrder !== null && category.sortOrder !== undefined ? category.sortOrder : 0 }}</td>
                                        <td><span class="status-pill">{{ category.enabled ? 'NORMAL' : 'DISABLED' }}</span></td>
                                        <td class="table-actions">
                                            <button type="button" @click="toggleCategory(category)">
                                                {{ category.enabled ? '禁用' : '启用' }}
                                            </button>
                                            <button type="button" class="danger-link" @click="removeCategory(category.id)">删除</button>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </template>
                    <p v-else class="backend-state-text">暂无分类数据</p>
                </div>

                <nav v-if="getTotalPages(categoryState) > 1" class="backend-pagination" aria-label="分类分页">
                    <p>
                        第 {{ categoryState.page }} / {{ getTotalPages(categoryState) }} 页，
                        共 {{ categoryState.total }} 条，当前 {{ getPageStart(categoryState) }}-{{ getPageEnd(categoryState) }} 条
                    </p>
                    <div class="backend-pagination-actions">
                        <button
                            type="button"
                            :disabled="categoryState.loading || categoryState.page <= 1"
                            @click="changeResourcePage(categoryState, fetchCategories, 1)"
                        >
                            首页
                        </button>
                        <button
                            type="button"
                            :disabled="categoryState.loading || categoryState.page <= 1"
                            @click="changeResourcePage(categoryState, fetchCategories, categoryState.page - 1)"
                        >
                            上一页
                        </button>
                        <template v-for="item in getPaginationItems(categoryState)" :key="`category-${item.type}-${item.value}`">
                            <span v-if="item.type === 'ellipsis'" class="pagination-ellipsis">...</span>
                            <button
                                v-else
                                type="button"
                                :class="{ active: item.value === categoryState.page }"
                                :disabled="categoryState.loading || item.value === categoryState.page"
                                @click="changeResourcePage(categoryState, fetchCategories, item.value)"
                            >
                                {{ item.value }}
                            </button>
                        </template>
                        <button
                            type="button"
                            :disabled="categoryState.loading || categoryState.page >= getTotalPages(categoryState)"
                            @click="changeResourcePage(categoryState, fetchCategories, categoryState.page + 1)"
                        >
                            下一页
                        </button>
                        <button
                            type="button"
                            :disabled="categoryState.loading || categoryState.page >= getTotalPages(categoryState)"
                            @click="changeResourcePage(categoryState, fetchCategories, getTotalPages(categoryState))"
                        >
                            末页
                        </button>
                    </div>
                    <form class="backend-pagination-jump" @submit.prevent="submitResourceJump(categoryState, fetchCategories)">
                        <label for="category-page-jump">跳至</label>
                        <input
                            id="category-page-jump"
                            v-model="categoryState.jumpPage"
                            type="number"
                            min="1"
                            :max="getTotalPages(categoryState)"
                            :disabled="categoryState.loading"
                            inputmode="numeric"
                        >
                        <span>页</span>
                        <button type="submit" :disabled="categoryState.loading">跳转</button>
                    </form>
                </nav>
            </section>

            <section id="tags" class="dashboard-content-panel admin-resource-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">内容组织</p>
                        <h2>标签管理</h2>
                    </div>
                </div>
                <div class="admin-resource-toolbar">
                    <form class="admin-inline-form admin-inline-form-tags" @submit.prevent="submitTag">
                        <input v-model.trim="tagForm.name" type="text" placeholder="标签名称" required>
                        <input v-model.trim="tagForm.description" type="text" placeholder="标签说明">
                        <button type="submit">新增标签</button>
                    </form>
                    <div class="admin-filter-row">
                        <label for="tag-enabled-filter">状态筛选</label>
                        <select
                            id="tag-enabled-filter"
                            :value="tagState.enabled"
                            @change="changeResourceFilter(tagState, fetchTags, $event.target.value)"
                        >
                            <option value="">全部</option>
                            <option value="true">仅启用</option>
                            <option value="false">仅禁用</option>
                        </select>
                    </div>
                </div>

                <div class="admin-resource-body">
                    <p v-if="tagState.loading" class="backend-state-text">标签数据加载中...</p>
                    <p v-else-if="tagState.error" class="backend-state-text error-text">{{ tagState.error }}</p>
                    <template v-else-if="tagState.items.length">
                        <div class="admin-resource-table-wrap">
                            <table class="admin-resource-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>名称</th>
                                        <th>说明</th>
                                        <th>状态</th>
                                        <th>操作</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="tag in tagState.items" :key="tag.id">
                                        <td>{{ tag.id }}</td>
                                        <td>{{ tag.name }}</td>
                                        <td>{{ tag.description || '-' }}</td>
                                        <td><span class="status-pill">{{ tag.enabled ? 'NORMAL' : 'DISABLED' }}</span></td>
                                        <td class="table-actions">
                                            <button type="button" @click="toggleTag(tag)">
                                                {{ tag.enabled ? '禁用' : '启用' }}
                                            </button>
                                            <button type="button" class="danger-link" @click="removeTag(tag.id)">删除</button>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </template>
                    <p v-else class="backend-state-text">暂无标签数据</p>
                </div>

                <nav v-if="getTotalPages(tagState) > 1" class="backend-pagination" aria-label="标签分页">
                    <p>
                        第 {{ tagState.page }} / {{ getTotalPages(tagState) }} 页，
                        共 {{ tagState.total }} 条，当前 {{ getPageStart(tagState) }}-{{ getPageEnd(tagState) }} 条
                    </p>
                    <div class="backend-pagination-actions">
                        <button
                            type="button"
                            :disabled="tagState.loading || tagState.page <= 1"
                            @click="changeResourcePage(tagState, fetchTags, 1)"
                        >
                            首页
                        </button>
                        <button
                            type="button"
                            :disabled="tagState.loading || tagState.page <= 1"
                            @click="changeResourcePage(tagState, fetchTags, tagState.page - 1)"
                        >
                            上一页
                        </button>
                        <template v-for="item in getPaginationItems(tagState)" :key="`tag-${item.type}-${item.value}`">
                            <span v-if="item.type === 'ellipsis'" class="pagination-ellipsis">...</span>
                            <button
                                v-else
                                type="button"
                                :class="{ active: item.value === tagState.page }"
                                :disabled="tagState.loading || item.value === tagState.page"
                                @click="changeResourcePage(tagState, fetchTags, item.value)"
                            >
                                {{ item.value }}
                            </button>
                        </template>
                        <button
                            type="button"
                            :disabled="tagState.loading || tagState.page >= getTotalPages(tagState)"
                            @click="changeResourcePage(tagState, fetchTags, tagState.page + 1)"
                        >
                            下一页
                        </button>
                        <button
                            type="button"
                            :disabled="tagState.loading || tagState.page >= getTotalPages(tagState)"
                            @click="changeResourcePage(tagState, fetchTags, getTotalPages(tagState))"
                        >
                            末页
                        </button>
                    </div>
                    <form class="backend-pagination-jump" @submit.prevent="submitResourceJump(tagState, fetchTags)">
                        <label for="tag-page-jump">跳至</label>
                        <input
                            id="tag-page-jump"
                            v-model="tagState.jumpPage"
                            type="number"
                            min="1"
                            :max="getTotalPages(tagState)"
                            :disabled="tagState.loading"
                            inputmode="numeric"
                        >
                        <span>页</span>
                        <button type="submit" :disabled="tagState.loading">跳转</button>
                    </form>
                </nav>
            </section>

            <section id="logs" class="table-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">审计记录</p>
                        <h2>管理员操作日志</h2>
                    </div>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>管理员</th>
                            <th>操作</th>
                            <th>请求</th>
                            <th>结果</th>
                            <th>目标</th>
                            <th>详情</th>
                            <th>时间</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="log in logs" :key="log.id">
                            <td>{{ log.id }}</td>
                            <td>{{ log.adminUsername || `#${log.adminUserId}` }}</td>
                            <td>{{ log.operation }}</td>
                            <td>{{ log.requestMethod || '-' }} {{ log.requestUri || '-' }}</td>
                            <td>{{ log.resultStatus || '-' }}</td>
                            <td>{{ log.targetType }} #{{ log.targetId !== null && log.targetId !== undefined ? log.targetId : '-' }}</td>
                            <td>{{ log.detail }}</td>
                            <td>{{ log.createdAt }}</td>
                        </tr>
                    </tbody>
                </table>
            </section>
        </section>
    </main>
</template>

<style scoped>
.admin-resource-panel {
    display: grid;
    gap: 18px;
}

.admin-resource-toolbar {
    display: grid;
    gap: 14px;
}

.admin-inline-form {
    display: grid;
    grid-template-columns: minmax(180px, 1.1fr) minmax(240px, 1.6fr) 140px 120px;
    gap: 12px;
    align-items: center;
}

.admin-inline-form-tags {
    grid-template-columns: minmax(200px, 1.2fr) minmax(240px, 1.6fr) 120px;
}

.admin-inline-form input,
.admin-filter-row select,
.backend-pagination-jump input {
    min-height: 40px;
    padding: 0 12px;
    color: var(--text);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    outline: 0;
}

.admin-inline-form input:focus,
.admin-filter-row select:focus,
.backend-pagination-jump input:focus {
    border-color: var(--brand);
}

.admin-resource-panel button,
.admin-resource-panel select {
    font: inherit;
}

.admin-resource-panel button {
    min-height: 40px;
    padding: 0 14px;
    color: var(--text);
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.admin-resource-panel button:hover:not(:disabled) {
    color: var(--brand-strong);
    border-color: var(--brand);
}

.admin-filter-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
}

.admin-filter-row label,
.backend-pagination p,
.backend-pagination-jump label,
.backend-pagination-jump span {
    color: var(--muted);
    font-size: 14px;
}

.admin-filter-row select {
    min-width: 136px;
}

.admin-resource-body {
    min-height: 240px;
}

.admin-resource-table-wrap {
    overflow-x: auto;
}

.admin-resource-table {
    width: 100%;
    min-width: 720px;
    border-spacing: 0;
}

.admin-resource-table th,
.admin-resource-table td {
    padding: 14px 12px;
    text-align: left;
    border-bottom: 1px solid var(--line);
    vertical-align: top;
}

.admin-resource-table th {
    color: var(--muted);
    font-size: 13px;
}

.backend-state-text {
    margin: 0;
    padding: 32px 0;
    color: var(--muted);
}

.table-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
}

.table-actions button {
    min-height: 34px;
    padding: 0 12px;
}

.danger-link {
    color: #d14343 !important;
}

.backend-pagination {
    display: grid;
    gap: 12px;
    padding-top: 2px;
    border-top: 1px solid var(--line);
}

.backend-pagination p {
    margin: 0;
}

.backend-pagination-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
}

.backend-pagination-actions button,
.backend-pagination-jump button {
    min-width: 40px;
    min-height: 36px;
    padding: 0 12px;
    color: var(--text);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
}

.backend-pagination-actions button:hover:not(:disabled),
.backend-pagination-jump button:hover:not(:disabled),
.backend-pagination-actions button.active {
    color: #ffffff;
    background: var(--brand);
    border-color: var(--brand);
}

.backend-pagination-jump {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
}

.pagination-ellipsis {
    color: var(--muted);
}

@media (max-width: 960px) {
    .admin-inline-form,
    .admin-inline-form-tags {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 760px) {
    .backend-pagination-jump {
        width: 100%;
    }

    .backend-pagination-jump input,
    .backend-pagination-jump button {
        width: 100%;
    }
}
</style>
