<script setup>
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { getArticleApi } from '@/api/articles';
import SiteHeader from '@/components/SiteHeader.vue';
import { articles, comments } from '@/data/home';
import { useSession } from '@/stores/session';

const route = useRoute();
const { isLoggedIn, state } = useSession();
const remoteArticle = ref(null);
const isLoading = ref(false);
const loadError = ref('');
const useLocalFallback = ref(false);
const articleId = computed(() => Number(route.params.id));
const localArticle = computed(() => articles.find((item) => item.id === articleId.value) || null);
const article = computed(() => {
    if (remoteArticle.value) {
        return remoteArticle.value;
    }
    return useLocalFallback.value ? localArticle.value : null;
});

const getStorageKey = (type) => `my-blog-${type}-${article.value.id}`;
const readBoolean = (key) => localStorage.getItem(key) === 'true';
const parseCount = (text) => Number(String(text).replace(/[^\d]/g, '')) || 0;

const liked = ref(false);
const favorited = ref(false);
const likeCount = ref(0);
const localComments = ref([]);
const commentText = ref('');
const feedback = ref('');

const syncArticleState = () => {
    if (!article.value) {
        liked.value = false;
        favorited.value = false;
        likeCount.value = 0;
        localComments.value = [];
        commentText.value = '';
        feedback.value = '';
        return;
    }
    liked.value = readBoolean(getStorageKey('liked'));
    favorited.value = readBoolean(getStorageKey('favorited'));
    likeCount.value = parseCount(article.value.stats.likes) + (liked.value ? 1 : 0);
    localComments.value = [...comments];
    commentText.value = '';
    feedback.value = '';
};

const fetchArticle = async () => {
    const currentId = String(route.params.id);
    remoteArticle.value = null;
    useLocalFallback.value = false;
    loadError.value = '';
    isLoading.value = true;
    try {
        const detail = await getArticleApi(currentId);
        if (String(route.params.id) === currentId) {
            remoteArticle.value = detail;
        }
    } catch (error) {
        if (String(route.params.id) === currentId) {
            if (localArticle.value) {
                useLocalFallback.value = true;
                return;
            }
            loadError.value = error.message || '文章不存在';
        }
    } finally {
        if (String(route.params.id) === currentId) {
            isLoading.value = false;
        }
    }
};

const requireLogin = () => {
    if (isLoggedIn.value) {
        return true;
    }
    feedback.value = '请先登录后再互动';
    return false;
};

const toggleLike = () => {
    if (!requireLogin()) {
        return;
    }
    liked.value = !liked.value;
    localStorage.setItem(getStorageKey('liked'), String(liked.value));
    likeCount.value += liked.value ? 1 : -1;
    feedback.value = liked.value ? '已点赞' : '已取消点赞';
};

const toggleFavorite = () => {
    if (!requireLogin()) {
        return;
    }
    favorited.value = !favorited.value;
    localStorage.setItem(getStorageKey('favorited'), String(favorited.value));
    feedback.value = favorited.value ? '已加入收藏' : '已取消收藏';
};

const submitComment = () => {
    if (!requireLogin()) {
        return;
    }
    const content = commentText.value.trim();
    if (!content) {
        feedback.value = '评论内容不能为空';
        return;
    }
    localComments.value.unshift({
        id: Date.now(),
        author: state.user.nickname,
        avatar: state.user.avatar,
        content,
        time: '刚刚'
    });
    commentText.value = '';
    feedback.value = '评论已发布';
};

watch(article, syncArticleState, { immediate: true });
watch(() => route.params.id, fetchArticle, { immediate: true });
</script>

<template>
    <SiteHeader />
    <main v-if="article" class="page-shell detail-layout">
        <article class="article-main">
            <img class="article-hero" :src="article.cover" :alt="article.coverAlt">
            <div class="article-body">
                <div class="post-meta">
                    <span>{{ article.category }}</span>
                    <span>{{ article.readingTime }}</span>
                    <span>{{ article.publishedText }}</span>
                </div>
                <h1>{{ article.title }}</h1>
                <p class="article-summary">{{ article.summary }}</p>

                <RouterLink class="article-author" :to="`/users/${article.author.id}`">
                    <img :src="article.author.avatar" alt="作者头像">
                    <div>
                        <strong>{{ article.author.name || article.author.nickname || article.author.username }}</strong>
                        <span>{{ article.stats.views }} · {{ article.stats.likes }} · {{ article.stats.comments }}</span>
                    </div>
                </RouterLink>

                <div class="tag-row">
                    <RouterLink v-for="tag in article.tags" :key="tag" to="/search">
                        {{ tag }}
                    </RouterLink>
                </div>

                <section class="markdown-preview">
                    <p v-for="paragraph in article.content" :key="paragraph">{{ paragraph }}</p>
                    <pre><code>Authorization: Bearer &lt;token&gt;</code></pre>
                    <p>后端启动后，这个页面会优先从 `GET /api/articles/{id}` 获取正文、作者和标签；接口不可用时保留本地内容。</p>
                </section>
            </div>
        </article>

        <aside class="detail-side">
            <section class="side-section action-panel">
                <button type="button" :class="{ active: liked }" @click="toggleLike">
                    {{ liked ? '已点赞' : '点赞' }} {{ likeCount }}
                </button>
                <button type="button" :class="{ active: favorited }" @click="toggleFavorite">
                    {{ favorited ? '已收藏' : '收藏' }}
                </button>
                <a href="#comment-input">评论</a>
                <p v-if="feedback" class="form-message">{{ feedback }}</p>
            </section>

            <section class="side-section">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">评论</p>
                        <h2>读者讨论</h2>
                    </div>
                </div>
                <form class="comment-form" @submit.prevent="submitComment">
                    <textarea
                        id="comment-input"
                        v-model="commentText"
                        placeholder="写下你的想法"
                    ></textarea>
                    <button type="submit">发表评论</button>
                </form>
                <div class="comment-list">
                    <article v-for="comment in localComments" :key="comment.id" class="comment-item">
                        <img :src="comment.avatar" alt="评论者头像">
                        <div>
                            <strong>{{ comment.author }}</strong>
                            <span>{{ comment.time }}</span>
                            <p>{{ comment.content }}</p>
                        </div>
                    </article>
                </div>
            </section>
        </aside>
    </main>
    <main v-else class="page-shell detail-layout">
        <section class="article-main empty-state">
            <p class="eyebrow">{{ isLoading ? '加载中' : '未找到文章' }}</p>
            <h1>{{ isLoading ? '正在加载文章内容' : '这篇文章暂时不可访问' }}</h1>
            <p>{{ loadError || '请稍后刷新，或返回首页继续浏览其他内容。' }}</p>
            <RouterLink to="/">返回首页</RouterLink>
        </section>
    </main>
</template>
