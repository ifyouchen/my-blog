<script setup>
import { RouterLink } from 'vue-router';
import { authors, specials } from '@/data/home';
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
                    :key="special.title"
                    class="special-item"
                    to="/columns"
                >
                    <img :src="special.image" :alt="special.alt">
                    <span>{{ special.title }}</span>
                </RouterLink>
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
                <li v-for="(author, index) in authors" :key="author.name">
                    <span class="rank-no">{{ index + 1 }}</span>
                    <img :src="author.avatar" alt="作者头像">
                    <div>
                        <strong>{{ author.name }}</strong>
                        <span>{{ author.title }}</span>
                    </div>
                </li>
            </ol>
        </section>

        <section class="side-section write-box">
            <p class="eyebrow">开始创作</p>
            <h2>把项目经验写成下一篇文章</h2>
            <p>标题、Markdown、分类、标签、封面图，第一版先把写作路径做顺。</p>
            <RouterLink class="primary-action block" to="/editor/new">发布文章</RouterLink>
        </section>
    </aside>
</template>
