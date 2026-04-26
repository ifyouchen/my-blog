<script setup>
import { computed, ref, watch, onMounted, onUnmounted, inject } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { getArticleApi } from '@/api/articles';
import { likeArticleApi, unlikeArticleApi } from '@/api/likes';
import { favoriteArticleApi, unfavoriteArticleApi } from '@/api/favorites';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import CommentList from '@/components/CommentList.vue';
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import { articles } from '@/data/home';
import { useSession } from '@/stores/session';

const route = useRoute();
const loginModal = inject('loginModal', { requireLogin: () => false });
const toast = inject('toast', { error: () => {} });
const { state } = useSession();

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

const liked = ref(false);
const favorited = ref(false);
const likeCount = ref(0);
const favoriteCount = ref(0);
const commentCount = ref(0);
const showBackToTop = ref(false);
const likeSubmitting = ref(false);
const favoriteSubmitting = ref(false);
const currentUserId = computed(() => state.user?.id || null);
const showAuthorFollow = computed(() => {
    if (!article.value?.author?.id) {
        return false;
    }
    return currentUserId.value !== article.value.author.id;
});

const applyInteractionState = ({
    nextLiked = liked.value,
    nextFavorited = favorited.value,
    nextLikeCount = likeCount.value,
    nextFavoriteCount = favoriteCount.value,
    syncRemote = true
} = {}) => {
    liked.value = nextLiked;
    favorited.value = nextFavorited;
    likeCount.value = Math.max(0, nextLikeCount);
    favoriteCount.value = Math.max(0, nextFavoriteCount);

    if (!syncRemote || !remoteArticle.value) {
        return;
    }
    remoteArticle.value = {
        ...remoteArticle.value,
        liked: liked.value,
        favorited: favorited.value,
        likeCount: likeCount.value,
        favoriteCount: favoriteCount.value
    };
};

const syncArticleState = () => {
    if (!article.value) {
        applyInteractionState({
            nextLiked: false,
            nextFavorited: false,
            nextLikeCount: 0,
            nextFavoriteCount: 0,
            syncRemote: false
        });
        commentCount.value = 0;
        return;
    }
    applyInteractionState({
        nextLiked: Boolean(article.value.liked),
        nextFavorited: Boolean(article.value.favorited),
        nextLikeCount: article.value.likeCount || 0,
        nextFavoriteCount: article.value.favoriteCount || 0,
        syncRemote: false
    });
    commentCount.value = article.value.commentCount || 0;
};

const handleCommentCountChange = (delta) => {
    commentCount.value = Math.max(0, commentCount.value + delta);
    if (remoteArticle.value) {
        remoteArticle.value = {
            ...remoteArticle.value,
            commentCount: commentCount.value
        };
    }
};

const handleAuthorFollowChange = (nextFollowed) => {
    if (!remoteArticle.value?.author) {
        return;
    }
    remoteArticle.value = {
        ...remoteArticle.value,
        author: {
            ...remoteArticle.value.author,
            followed: nextFollowed
        }
    };
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

const toggleLike = async () => {
    if (likeSubmitting.value || !article.value) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => toggleLike(), {
        title: '登录后点赞',
        message: '登录后可以把喜欢的文章同步到你的账号，并继续刚才的点赞操作。',
        actionText: '登录并点赞'
    });
    if (!canContinue) {
        return;
    }
    const previousLiked = liked.value;
    const previousLikeCount = likeCount.value;
    applyInteractionState({
        nextLiked: !previousLiked,
        nextLikeCount: previousLikeCount + (previousLiked ? -1 : 1)
    });
    likeSubmitting.value = true;
    try {
        if (previousLiked) {
            await unlikeArticleApi(article.value.id);
        } else {
            await likeArticleApi(article.value.id);
        }
    } catch (error) {
        applyInteractionState({
            nextLiked: previousLiked,
            nextLikeCount: previousLikeCount
        });
        toast.error(error.message || (previousLiked ? '取消点赞失败，请稍后再试' : '点赞失败，请稍后再试'));
    } finally {
        likeSubmitting.value = false;
    }
};

const toggleFavorite = async () => {
    if (favoriteSubmitting.value || !article.value) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => toggleFavorite(), {
        title: '登录后收藏',
        message: '登录后可以把文章加入收藏夹，之后在个人中心继续阅读。',
        actionText: '登录并收藏'
    });
    if (!canContinue) {
        return;
    }
    const previousFavorited = favorited.value;
    const previousFavoriteCount = favoriteCount.value;
    applyInteractionState({
        nextFavorited: !previousFavorited,
        nextFavoriteCount: previousFavoriteCount + (previousFavorited ? -1 : 1)
    });
    favoriteSubmitting.value = true;
    try {
        if (previousFavorited) {
            await unfavoriteArticleApi(article.value.id);
        } else {
            await favoriteArticleApi(article.value.id);
        }
    } catch (error) {
        applyInteractionState({
            nextFavorited: previousFavorited,
            nextFavoriteCount: previousFavoriteCount
        });
        toast.error(error.message || (previousFavorited ? '取消收藏失败，请稍后再试' : '收藏失败，请稍后再试'));
    } finally {
        favoriteSubmitting.value = false;
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

onMounted(() => {
    window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
    <SiteHeader />
    <main v-if="article" class="page-shell detail-layout" data-testid="article-detail-page">
        <article class="article-main" data-testid="article-detail-main">
            <img class="article-hero" :src="article.cover" :alt="article.coverAlt">
            <div class="article-body">
                <section class="article-heading-panel">
                    <div class="article-heading-top">
                        <div class="post-meta article-meta-row">
                            <span>{{ article.category }}</span>
                            <span>{{ article.readingTime }}</span>
                            <span>{{ article.publishedText }}</span>
                        </div>
                        <div class="article-tag-list">
                            <span v-for="tag in article.tags" :key="tag" class="article-tag-chip">
                                {{ tag }}
                            </span>
                        </div>
                    </div>
                    <h1>{{ article.title }}</h1>
                    <p class="article-summary">{{ article.summary }}</p>

                    <div class="article-heading-bottom">
                        <div class="article-heading-meta">
                            <RouterLink class="article-author" :to="`/users/${article.author.id}`">
                                <img :src="article.author.avatar" alt="作者头像">
                                <div>
                                    <strong>{{ article.author.name || article.author.nickname || article.author.username }}</strong>
                                    <span>{{ article.publishedText }} · {{ article.category }}</span>
                                </div>
                            </RouterLink>
                            <div class="article-stats-row">
                                <span class="article-stat-pill">
                                    <strong>{{ article.viewCount }}</strong>
                                    <em>阅读</em>
                                </span>
                                <span class="article-stat-pill">
                                    <strong>{{ likeCount }}</strong>
                                    <em>点赞</em>
                                </span>
                                <span class="article-stat-pill">
                                    <strong>{{ commentCount }}</strong>
                                    <em>评论</em>
                                </span>
                                <span class="article-stat-pill">
                                    <strong>{{ favoriteCount }}</strong>
                                    <em>收藏</em>
                                </span>
                            </div>
                        </div>

                        <div class="article-quick-actions">
                            <button
                                type="button"
                                :class="['article-quick-button', { active: liked }]"
                                :disabled="likeSubmitting"
                                @click="toggleLike"
                            >
                                <span class="article-quick-label">{{ liked ? '已点赞' : '点赞' }}</span>
                            </button>
                            <button
                                type="button"
                                :class="['article-quick-button', { active: favorited }]"
                                :disabled="favoriteSubmitting"
                                @click="toggleFavorite"
                            >
                                <span class="article-quick-label">{{ favorited ? '已收藏' : '收藏' }}</span>
                            </button>
                            <AuthorFollowButton
                                v-if="showAuthorFollow"
                                class="article-follow-button"
                                :user-id="article.author.id"
                                :followed="article.author.followed"
                                detail
                                @change="handleAuthorFollowChange"
                            />
                        </div>
                    </div>
                </section>

                <MarkdownPreview v-if="articleMarkdown" :content="articleMarkdown" />
                <section v-else class="article-content-empty">
                    <p>正文暂时为空，稍后再来看一眼。</p>
                </section>

                <section class="article-comment" data-testid="article-comments-section">
                    <CommentList
                        v-if="remoteArticle"
                        :article-id="article.id"
                        :initial-count="commentCount"
                        @count-change="handleCommentCountChange"
                    />
                    <div v-else class="comment-placeholder">
                        <p>登录后可查看和发表评论</p>
                    </div>
                </section>
            </div>
        </article>

        <aside class="detail-side">
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
.article-heading-panel {
    display: grid;
    gap: 18px;
    margin-bottom: 28px;
    padding: 22px 24px 24px;
    background: linear-gradient(180deg, rgba(31, 122, 224, 0.04), rgba(31, 122, 224, 0));
    border: 1px solid rgba(31, 122, 224, 0.08);
    border-radius: 20px;
}

.article-heading-top,
.article-heading-bottom {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    justify-content: space-between;
    flex-wrap: wrap;
}

.article-meta-row {
    margin: 0;
}

.article-heading-meta {
    display: grid;
    gap: 14px;
    flex: 1 1 460px;
    min-width: 0;
}

.article-heading-meta :deep(.article-author),
.article-heading-meta .article-author {
    margin: 0;
}

.article-stats-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
}

.article-stat-pill {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: 34px;
    padding: 0 12px;
    color: var(--muted);
    background: rgba(31, 122, 224, 0.05);
    border: 1px solid rgba(31, 122, 224, 0.08);
    border-radius: 999px;
    line-height: 1;
}

.article-stat-pill strong {
    color: var(--text-strong);
    font-size: 14px;
}

.article-stat-pill em {
    font-style: normal;
    font-size: 12px;
}

.article-quick-actions {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
    flex: 0 1 auto;
}

.article-quick-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 94px;
    min-height: 40px;
    padding: 0 16px;
    color: var(--brand-strong);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    background: rgba(31, 122, 224, 0.08);
    border: 1px solid rgba(31, 122, 224, 0.18);
    border-radius: 999px;
    transition: background-color 0.18s ease, border-color 0.18s ease, transform 0.18s ease, opacity 0.18s ease;
}

.article-quick-button:hover:not(:disabled) {
    background: rgba(31, 122, 224, 0.14);
    border-color: rgba(31, 122, 224, 0.28);
    transform: translateY(-1px);
}

.article-quick-button.active {
    color: var(--brand-strong);
    background: rgba(31, 122, 224, 0.1);
    border-color: rgba(31, 122, 224, 0.2);
}

.article-quick-label {
    flex: none;
}

.article-quick-button:disabled {
    cursor: not-allowed;
    opacity: 0.72;
}

.article-follow-button {
    flex: none;
}

.article-comment {
    margin-top: 40px;
    padding-top: 24px;
    border-top: 1px solid var(--line);
}

.article-content-empty {
    padding: 28px 24px;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px dashed var(--line);
    border-radius: 8px;
}

.article-content-empty p {
    margin: 0;
}

.article-tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.article-tag-chip {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 32px;
    padding: 0 12px;
    line-height: 1;
    color: var(--brand-strong);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 999px;
}

.comment-placeholder {
    padding: 40px;
    text-align: center;
    color: var(--muted);
    background: var(--surface);
    border-radius: 8px;
}

@media (max-width: 760px) {
    .article-heading-panel {
        padding: 18px;
    }

    .article-quick-actions {
        justify-content: flex-start;
    }

    .article-heading-meta {
        flex-basis: 100%;
    }
}
</style>
