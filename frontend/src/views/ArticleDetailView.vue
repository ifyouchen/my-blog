<script setup>
import { computed, ref, watch, onMounted, onUnmounted, inject } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { getArticleApi } from '@/api/articles';
import { likeArticleApi, unlikeArticleApi, getLikeStatusApi } from '@/api/likes';
import { favoriteArticleApi, unfavoriteArticleApi, getFavoriteStatusApi } from '@/api/favorites';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import CommentList from '@/components/CommentList.vue';
import { articles } from '@/data/home';

const route = useRoute();
const loginModal = inject('loginModal', { requireLogin: () => false });

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
const articleMarkdown = computed(() => {
    if (!article.value) {
        return '';
    }
    if (article.value.rawContent) {
        return article.value.rawContent;
    }
    if (Array.isArray(article.value.content)) {
        return article.value.content.join('\n\n');
    }
    return article.value.content || '';
});

const getStorageKey = (type) => `my-blog-${type}-${article.value?.id}`;
const readBoolean = (key) => localStorage.getItem(key) === 'true';
const parseCount = (text) => Number(String(text).replace(/[^\d]/g, '')) || 0;

const liked = ref(false);
const favorited = ref(false);
const likeCount = ref(0);
const feedback = ref('');
const showBackToTop = ref(false);

const syncArticleState = () => {
    if (!article.value) {
        liked.value = false;
        favorited.value = false;
        likeCount.value = 0;
        feedback.value = '';
        return;
    }
    liked.value = readBoolean(getStorageKey('liked'));
    favorited.value = readBoolean(getStorageKey('favorited'));
    if (remoteArticle.value) {
        likeCount.value = parseCount(article.value.stats?.likes || '0');
    } else {
        likeCount.value = parseCount(article.value.stats?.likes || '0') + (liked.value ? 1 : 0);
    }
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

const fetchLikeStatus = async () => {
    if (!article.value || !remoteArticle.value) {
        return;
    }
    try {
        const status = await getLikeStatusApi(article.value.id);
        liked.value = status.liked;
    } catch (error) {
        console.error('获取点赞状态失败:', error);
    }
};

const toggleLike = async () => {
    const canContinue = loginModal.requireLogin(() => toggleLike(), {
        title: '登录后点赞',
        message: '登录后可以把喜欢的文章同步到你的账号，并继续刚才的点赞操作。',
        actionText: '登录并点赞'
    });
    if (!canContinue) {
        feedback.value = '登录后可以点赞文章';
        return;
    }
    try {
        if (liked.value) {
            await unlikeArticleApi(article.value.id);
            liked.value = false;
            likeCount.value = Math.max(0, likeCount.value - 1);
            feedback.value = '已取消点赞';
        } else {
            await likeArticleApi(article.value.id);
            liked.value = true;
            likeCount.value++;
            feedback.value = '已点赞';
        }
    } catch (error) {
        feedback.value = error.message || '操作失败';
    }
};

const fetchFavoriteStatus = async () => {
    if (!article.value || !remoteArticle.value) {
        return;
    }
    try {
        const status = await getFavoriteStatusApi(article.value.id);
        favorited.value = status.favorited;
    } catch (error) {
        console.error('获取收藏状态失败:', error);
    }
};

const toggleFavorite = async () => {
    const canContinue = loginModal.requireLogin(() => toggleFavorite(), {
        title: '登录后收藏',
        message: '登录后可以把文章加入收藏夹，之后在个人中心继续阅读。',
        actionText: '登录并收藏'
    });
    if (!canContinue) {
        feedback.value = '登录后可以收藏文章';
        return;
    }
    try {
        if (favorited.value) {
            await unfavoriteArticleApi(article.value.id);
            favorited.value = false;
            feedback.value = '已取消收藏';
        } else {
            await favoriteArticleApi(article.value.id);
            favorited.value = true;
            feedback.value = '已加入收藏';
        }
    } catch (error) {
        feedback.value = error.message || '操作失败';
    }
};

const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
};

const handleScroll = () => {
    showBackToTop.value = window.scrollY > 500;
};

watch(article, syncArticleState, { immediate: true });
watch(() => route.params.id, fetchArticle, { immediate: true });
watch(remoteArticle, (newVal) => {
    if (newVal) {
        fetchLikeStatus();
        fetchFavoriteStatus();
    }
}, { immediate: true });

onMounted(() => {
    window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
});
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

                <MarkdownPreview :content="articleMarkdown" />

                <section class="article-comment">
                    <CommentList
                        v-if="remoteArticle"
                        :article-id="article.id"
                    />
                    <div v-else class="comment-placeholder">
                        <p>登录后可查看和发表评论</p>
                    </div>
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
                <p v-if="feedback" class="form-message">{{ feedback }}</p>
            </section>
            <ArticleToc
                v-if="remoteArticle"
                :content="articleMarkdown"
                class="detail-toc"
            />
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

    <button
        v-show="showBackToTop"
        class="back-to-top"
        type="button"
        @click="scrollToTop"
    >
        ↑
    </button>
</template>

<style scoped>
.article-comment {
    margin-top: 40px;
    padding-top: 24px;
    border-top: 1px solid var(--line);
}

.comment-placeholder {
    padding: 40px;
    text-align: center;
    color: var(--muted);
    background: var(--surface);
    border-radius: 8px;
}
</style>
