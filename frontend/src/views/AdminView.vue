<script setup>
import { ref, onMounted } from 'vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {
    getAdminStatsApi,
    getAdminUsersApi,
    updateAdminUserStatusApi,
    getAdminArticlesApi,
    updateAdminArticleStatusApi,
    getAdminCommentsApi,
    deleteAdminCommentApi
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
const isLoading = ref(false);

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

const toggleUserStatus = async (userId, currentStatus) => {
    const newStatus = currentStatus === 'NORMAL' ? 'DISABLED' : 'NORMAL';
    try {
        await updateAdminUserStatusApi(userId, newStatus);
        await fetchUsers();
    } catch (error) {
        alert(error.message || '操作失败');
    }
};

const toggleArticleStatus = async (articleId, currentStatus) => {
    const newStatus = currentStatus === 'PUBLISHED' ? 'OFFLINE' : 'PUBLISHED';
    try {
        await updateAdminArticleStatusApi(articleId, newStatus);
        await fetchArticles();
    } catch (error) {
        alert(error.message || '操作失败');
    }
};

const handleDeleteComment = async (commentId) => {
    if (!confirm('确定删除这条评论吗？')) return;
    try {
        await deleteAdminCommentApi(commentId);
        await fetchComments();
    } catch (error) {
        alert(error.message || '删除失败');
    }
};

onMounted(() => {
    fetchStats();
    fetchUsers();
    fetchArticles();
    fetchComments();
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
        </section>
    </main>
</template>