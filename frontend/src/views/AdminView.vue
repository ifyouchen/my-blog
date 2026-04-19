<script setup>
import SiteHeader from '@/components/SiteHeader.vue';
import { adminStats, adminUsers, articles, comments } from '@/data/home';
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
                <article v-for="item in adminStats" :key="item.label">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
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
                        <tr v-for="user in adminUsers" :key="user.id">
                            <td>{{ user.id }}</td>
                            <td>{{ user.username }}</td>
                            <td>{{ user.email }}</td>
                            <td><span class="status-pill">{{ user.status }}</span></td>
                            <td>{{ user.createdAt }}</td>
                            <td><button type="button">切换状态</button></td>
                        </tr>
                    </tbody>
                </table>
            </section>

            <section id="articles" class="table-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">文章管理</p>
                        <h2>最新文章</h2>
                    </div>
                </div>
                <table>
                    <tbody>
                        <tr v-for="article in articles" :key="article.id">
                            <td>{{ article.title }}</td>
                            <td>{{ article.category }}</td>
                            <td>{{ article.stats.views }}</td>
                            <td><button type="button">下架</button></td>
                        </tr>
                    </tbody>
                </table>
            </section>

            <section id="comments" class="table-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">评论管理</p>
                        <h2>最新评论</h2>
                    </div>
                </div>
                <table>
                    <tbody>
                        <tr v-for="comment in comments" :key="comment.id">
                            <td>{{ comment.author }}</td>
                            <td>{{ comment.content }}</td>
                            <td>{{ comment.time }}</td>
                            <td><button type="button">删除</button></td>
                        </tr>
                    </tbody>
                </table>
            </section>
        </section>
    </main>
</template>
