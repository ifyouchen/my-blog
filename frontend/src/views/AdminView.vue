<script setup>
import { reactive, ref, onMounted } from 'vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {
    getAdminStatsApi,
    getAdminUsersApi,
    updateAdminUserStatusApi,
    getAdminArticlesApi,
    updateAdminArticleStatusApi,
    getAdminCommentsApi,
    getAdminLogsApi,
    deleteAdminCommentApi,
    getCategoriesApi,
    getTagsApi,
    createCategoryApi,
    updateCategoryApi,
    deleteCategoryApi,
    createTagApi,
    updateTagApi,
    deleteTagApi
} from '@/api/admin';

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
const categories = ref([]);
const tags = ref([]);
const categoryForm = reactive({ name: '', description: '', sortOrder: 0 });
const tagForm = reactive({ name: '', description: '' });

const fetchStats = async () => {
    try {
        stats.value = await getAdminStatsApi();
    } catch (error) {
        console.error('获取统计数据失败:', error);
    }
};

const fetchUsers = async () => {
    try {
        const result = await getAdminUsersApi(1, 20);
        users.value = result.items || [];
    } catch (error) {
        console.error('获取用户列表失败:', error);
    }
};

const fetchArticles = async () => {
    try {
        const result = await getAdminArticlesApi(1, 20);
        articles.value = result.items || [];
    } catch (error) {
        console.error('获取文章列表失败:', error);
    }
};

const fetchComments = async () => {
    try {
        const result = await getAdminCommentsApi(1, 20);
        comments.value = result.items || [];
    } catch (error) {
        console.error('获取评论列表失败:', error);
    }
};

const fetchLogs = async () => {
    try {
        const result = await getAdminLogsApi(1, 20);
        logs.value = result.items || [];
    } catch (error) {
        console.error('获取操作日志失败:', error);
    }
};

const fetchCategories = async () => {
    try {
        categories.value = await getCategoriesApi(null);
    } catch (error) {
        console.error('获取分类失败:', error);
    }
};

const fetchTags = async () => {
    try {
        tags.value = await getTagsApi(null);
    } catch (error) {
        console.error('获取标签失败:', error);
    }
};

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

const handleDeleteComment = async (commentId) => {
    if (!confirm('确定删除这条评论吗？')) return;
    try {
        await deleteAdminCommentApi(commentId);
        await fetchComments();
        await fetchLogs();
    } catch (error) {
        alert(error.message || '删除失败');
    }
};

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
        await fetchCategories();
        await fetchLogs();
    } catch (error) {
        alert(error.message || '分类创建失败');
    }
};

const toggleCategory = async (category) => {
    try {
        await updateCategoryApi(category.id, {
            name: category.name,
            description: category.description,
            sortOrder: category.sortOrder || 0,
            enabled: !category.enabled
        });
        await fetchCategories();
        await fetchLogs();
    } catch (error) {
        alert(error.message || '分类更新失败');
    }
};

const removeCategory = async (categoryId) => {
    if (!confirm('确定删除这个分类吗？')) return;
    try {
        await deleteCategoryApi(categoryId);
        await fetchCategories();
        await fetchLogs();
    } catch (error) {
        alert(error.message || '分类删除失败');
    }
};

const submitTag = async () => {
    try {
        await createTagApi({
            name: tagForm.name,
            description: tagForm.description
        });
        tagForm.name = '';
        tagForm.description = '';
        await fetchTags();
        await fetchLogs();
    } catch (error) {
        alert(error.message || '标签创建失败');
    }
};

const toggleTag = async (tag) => {
    try {
        await updateTagApi(tag.id, {
            name: tag.name,
            description: tag.description,
            enabled: !tag.enabled
        });
        await fetchTags();
        await fetchLogs();
    } catch (error) {
        alert(error.message || '标签更新失败');
    }
};

const removeTag = async (tagId) => {
    if (!confirm('确定删除这个标签吗？')) return;
    try {
        await deleteTagApi(tagId);
        await fetchTags();
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
    fetchCategories();
    fetchTags();
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

            <section id="categories" class="table-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">内容组织</p>
                        <h2>分类管理</h2>
                    </div>
                </div>
                <form class="admin-inline-form" @submit.prevent="submitCategory">
                    <input v-model.trim="categoryForm.name" type="text" placeholder="分类名称" required>
                    <input v-model.trim="categoryForm.description" type="text" placeholder="分类说明">
                    <input v-model.number="categoryForm.sortOrder" type="number" placeholder="排序值">
                    <button type="submit">新增分类</button>
                </form>
                <table>
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
                        <tr v-for="category in categories" :key="category.id">
                            <td>{{ category.id }}</td>
                            <td>{{ category.name }}</td>
                            <td>{{ category.description || '-' }}</td>
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
            </section>

            <section id="tags" class="table-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">内容组织</p>
                        <h2>标签管理</h2>
                    </div>
                </div>
                <form class="admin-inline-form" @submit.prevent="submitTag">
                    <input v-model.trim="tagForm.name" type="text" placeholder="标签名称" required>
                    <input v-model.trim="tagForm.description" type="text" placeholder="标签说明">
                    <button type="submit">新增标签</button>
                </form>
                <table>
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
                        <tr v-for="tag in tags" :key="tag.id">
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
                            <td>{{ log.targetType }} #{{ log.targetId ?? '-' }}</td>
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
.admin-inline-form {
    display: grid;
    grid-template-columns: 1.1fr 1.6fr 140px 120px;
    gap: 12px;
    margin-bottom: 16px;
}

.admin-inline-form input {
    padding: 10px 12px;
    border: 1px solid var(--line);
    border-radius: 8px;
}

.table-actions {
    display: flex;
    gap: 10px;
}

.danger-link {
    color: #d14343;
}

@media (max-width: 960px) {
    .admin-inline-form {
        grid-template-columns: 1fr;
    }
}
</style>
