<script setup>
import {computed, inject, onMounted, onUnmounted, ref, watch} from 'vue';
import {RouterLink, useRoute} from 'vue-router';
import {useHead} from '@unhead/vue';
import {getArticleApi, getArticleNeighborsApi, getArticleRecommendationsApi} from '@/api/articles';
import {likeArticleApi, unlikeArticleApi} from '@/api/likes';
import {favoriteArticleApi, unfavoriteArticleApi} from '@/api/favorites';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import CommentList from '@/components/CommentList.vue';
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import ReportDialog from '@/components/ReportDialog.vue';
import AdBanner from '@/components/AdBanner.vue';
import {useSession} from '@/stores/session';

const route = useRoute();
const loginModal = inject('loginModal', { requireLogin: () => false });
const toast = inject('toast', { error: () => {}, success: () => {} });
const { state } = useSession();

const remoteArticle = ref(null);
const isLoading = ref(false);
const loadError = ref('');
const recommendationSections = ref([]);
const relatedLoading = ref(false);
const neighborPrev = ref(null);
const neighborNext = ref(null);
const articleId = computed(() => String(route.params.id || '').replace(/-.+$/, ''));
const articleUrl = computed(() => `${window.location.origin}/articles/${articleId.value}`);
const article = computed(() => remoteArticle.value);
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
const pageTitle = computed(() => {
    if (article.value?.title) {
        return `${article.value.title} - my-blog`;
    }
    if (isLoading.value) {
        return '加载中 - my-blog';
    }
    return '文章详情 - my-blog';
});

useHead({
    title: pageTitle,
    meta: computed(() => {
        if (!article.value?.seoDescription && !article.value?.summary) {
            return [];
        }
        return [{
            name: 'description',
            content: article.value.seoDescription || article.value.summary
        }];
    })
});

const liked = ref(false);
const favorited = ref(false);
const likeCount = ref(0);
const favoriteCount = ref(0);
const commentCount = ref(0);
const showBackToTop = ref(false);
const readingProgress = ref(0);
const likeSubmitting = ref(false);
const favoriteSubmitting = ref(false);
const reportDialogVisible = ref(false);
const immersiveMode = ref(false);
const showShareMenu = ref(false);
const shareCopied = ref(false);
const tocDrawerOpen = ref(false);
const mobileTocCloseButtonRef = ref(null);
let shareHoverTimer = null;
const SHARE_HOVER_DELAY = 280;
const RECENT_READING_KEY = 'my-blog:recent-reading';
const RECENT_READING_LIMIT = 20;

const saveRecentArticle = (source) => {
    if (!source?.id) {
        return;
    }
    try {
        const current = JSON.parse(localStorage.getItem(RECENT_READING_KEY) || '[]');
        const safeCurrent = Array.isArray(current) ? current : [];
        const item = {
            id: source.id,
            title: source.title,
            summary: source.summary,
            cover: source.cover,
            category: source.category,
            publishedText: source.publishedText,
            authorName: source.author?.name || '',
            readAt: Date.now()
        };
        const next = [
            item,
            ...safeCurrent.filter((entry) => String(entry.id) !== String(source.id))
        ].slice(0, RECENT_READING_LIMIT);
        localStorage.setItem(RECENT_READING_KEY, JSON.stringify(next));
    } catch {
        localStorage.removeItem(RECENT_READING_KEY);
    }
};

const openShareMenu = () => {
    if (shareHoverTimer) clearTimeout(shareHoverTimer);
    shareHoverTimer = window.setTimeout(() => {
        showShareMenu.value = true;
    }, SHARE_HOVER_DELAY);
};

const closeShareMenu = () => {
    if (shareHoverTimer) clearTimeout(shareHoverTimer);
    shareHoverTimer = window.setTimeout(() => {
        showShareMenu.value = false;
    }, SHARE_HOVER_DELAY);
};
const currentUserId = computed(() => state.user?.id || null);
const showAuthorFollow = computed(() => {
    if (!article.value?.author?.id) {
        return false;
    }
    return currentUserId.value !== article.value.author.id;
});
const showArticleReport = computed(() => {
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
    const delta = nextFollowed ? 1 : -1;
    remoteArticle.value = {
        ...remoteArticle.value,
        author: {
            ...remoteArticle.value.author,
            followed: nextFollowed,
            followerCount: Math.max(0, (remoteArticle.value.author.followerCount || 0) + delta)
        }
    };
};

const fetchArticle = async () => {
    const routeId = String(route.params.id);
    const numericId = routeId.replace(/-.+$/, '');
    remoteArticle.value = null;
    loadError.value = '';
    isLoading.value = true;
    recommendationSections.value = [];
    try {
        const detail = await getArticleApi(numericId);
        if (String(route.params.id) === routeId) {
            remoteArticle.value = detail;
        }
    } catch (error) {
        if (String(route.params.id) === routeId) {
            loadError.value = error.message || '文章不存在';
        }
    } finally {
        if (String(route.params.id) === routeId) {
            isLoading.value = false;
        }
    }
};

const fetchRecommendations = async (id) => {
    relatedLoading.value = true;
    try {
        const result = await getArticleRecommendationsApi(id, 12);
        recommendationSections.value = result.sections || [];
    } catch {
        recommendationSections.value = [];
    } finally {
        relatedLoading.value = false;
    }
};

const fetchNeighbors = async (id) => {
    try {
        const nb = await getArticleNeighborsApi(id);
        neighborPrev.value = nb.prev || null;
        neighborNext.value = nb.next || null;
    } catch {
        neighborPrev.value = null;
        neighborNext.value = null;
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

const openReportArticle = () => {
    if (!article.value) {
        return;
    }
    const canContinue = loginModal.requireLogin(() => openReportArticle(), {
        title: '登录后举报',
        message: '登录后可以提交内容举报，管理员会在后台处理。',
        actionText: '登录并举报'
    });
    if (!canContinue) {
        return;
    }
    reportDialogVisible.value = true;
};

const handleReportSuccess = () => {};

const toggleImmersive = () => {
    immersiveMode.value = !immersiveMode.value;
    if (immersiveMode.value) {
        document.body.classList.add('immersive-mode');
    } else {
        document.body.classList.remove('immersive-mode');
    }
};

const handleImmersiveKeydown = (e) => {
    if (e.key === 'Escape' && immersiveMode.value) {
        toggleImmersive();
        return;
    }
    if (e.key === 'Escape' && tocDrawerOpen.value) {
        tocDrawerOpen.value = false;
    }
};

const copyArticleLink = async () => {
    const url = articleUrl.value;
    try {
        if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(url);
        } else {
            const textarea = document.createElement('textarea');
            textarea.value = url;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
        }
        shareCopied.value = true;
        showShareMenu.value = false;
        window.setTimeout(() => { shareCopied.value = false; }, 2000);
    } catch (_) {
        toast.error('复制失败，请手动复制链接');
    }
};

const shareToWeibo = () => {
    if (!article.value) return;
    const url = encodeURIComponent(articleUrl.value);
    const title = encodeURIComponent(article.value.title || '');
    window.open(`https://service.weibo.com/share/share.php?url=${url}&title=${title}`, '_blank');
    showShareMenu.value = false;
};

const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
};

const handleScroll = () => {
    const scrollY = window.scrollY;
    showBackToTop.value = scrollY > 500;
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    readingProgress.value = docHeight > 0 ? Math.min(100, Math.round((scrollY / docHeight) * 100)) : 0;
};

watch(article, syncArticleState, { immediate: true });
watch(() => route.params.id, fetchArticle, { immediate: true });
watch(remoteArticle, (val) => {
    if (val?.id) {
        saveRecentArticle(val);
        fetchRecommendations(val.id);
        fetchNeighbors(val.id);
    }
});

onMounted(() => {
    window.addEventListener('scroll', handleScroll);
    window.addEventListener('keydown', handleImmersiveKeydown);
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
    window.removeEventListener('keydown', handleImmersiveKeydown);
    document.body.classList.remove('immersive-mode');
    if (shareHoverTimer) clearTimeout(shareHoverTimer);
    document.body.classList.remove('mobile-toc-open');
});

watch(() => route.fullPath, () => {
    tocDrawerOpen.value = false;
});

watch(tocDrawerOpen, (open) => {
    if (open) {
        document.body.classList.add('mobile-toc-open');
        window.setTimeout(() => {
            mobileTocCloseButtonRef.value?.focus();
        }, 0);
        return;
    }
    document.body.classList.remove('mobile-toc-open');
});

</script>

<template>
    <SiteHeader />
    <div
        v-if="article && readingProgress > 0"
        class="reading-progress-bar"
        role="progressbar"
        :aria-valuenow="readingProgress"
        aria-valuemin="0"
        aria-valuemax="100"
        :style="{ width: `${readingProgress}%` }"
    ></div>
    <main v-if="article" :class="['page-shell', 'detail-layout', { 'detail-immersive': immersiveMode }]" data-testid="article-detail-page">
        <article class="article-main" data-testid="article-detail-main">
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
                            <RouterLink class="article-byline" :to="`/users/${article.author.id}`">
                                <img
                                    v-if="article.author.avatar"
                                    class="byline-avatar"
                                    :src="article.author.avatar"
                                    :alt="article.author.name"
                                    loading="lazy"
                                    decoding="async"
                                >
                                <span class="byline-avatar-fallback" v-else>{{ article.author.name[0] }}</span>
                                <div class="byline-info">
                                    <strong class="byline-name">{{ article.author.name }}</strong>
                                    <span class="byline-meta">
                                        {{ article.publishedText }}
                                        <template v-if="article.category"> · {{ article.category }}</template>
                                        <template v-if="article.readingTime"> · {{ article.readingTime }}</template>
                                    </span>
                                </div>
                            </RouterLink>
                            <div class="article-stats-row">
                                <span class="article-stat"><strong>{{ article.viewCount }}</strong> 阅读</span>
                                <span class="article-stat"><strong>{{ likeCount }}</strong> 点赞</span>
                                <span class="article-stat"><strong>{{ commentCount }}</strong> 评论</span>
                                <span class="article-stat"><strong>{{ favoriteCount }}</strong> 收藏</span>
                                <span class="article-stat"><strong>{{ article.author.followerCount }}</strong> 粉丝</span>
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
                            <button
                                v-if="showArticleReport"
                                type="button"
                                class="article-quick-button subtle"
                                @click="openReportArticle"
                            >
                                <span class="article-quick-label">举报</span>
                            </button>
                            <!-- 分享 -->
                            <div
                                class="article-share-wrap"
                                @mouseenter="openShareMenu"
                                @mouseleave="closeShareMenu"
                            >
                                <button
                                    type="button"
                                    :class="['article-quick-button', { active: shareCopied }]"
                                    :title="shareCopied ? '已复制链接' : '分享文章'"
                                >
                                    <span class="article-quick-label">{{ shareCopied ? '已复制' : '分享' }}</span>
                                </button>
                                <div v-if="showShareMenu" class="article-share-card">
                                    <div class="share-card-arrow"></div>
                                    <div class="share-card-body">
                                        <div class="share-option" @click="copyArticleLink">
                                            <svg class="share-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                                            <span>复制链接</span>
                                        </div>
                                        <div class="share-option" @click="shareToWeibo">
                                            <svg class="share-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M10.98 13.9c-.77-.3-1.58-.28-2.3-.06.48.28.98.53 1.5.74.84.34 1.7.62 2.58.84.2-.5.26-1.03-.06-1.48-.26-.33-.68-.37-1.08-.48l-.64-.56zm1.06-2.3c.4.22.88.26 1.3.1.42-.17.72-.56.8-1 .04-.23-.02-.46-.17-.64-.35-.42-1-.58-1.5-.52-.44.06-.84.36-1.06.74-.22.38-.24.84-.04 1.24.02.03.04.06.06.08z"/><path d="M21.86 4.84c-.54-1.62-2.3-2.9-4.48-3.23-2.9-.44-6.1.7-8.58 2.97-.98.9-1.8 1.92-2.4 3.01L3.2 8.82c-.65.28-1.2.7-1.64 1.22-.8.93-1 2.14-.42 3.08.27.44.65.77 1.1.98l-.78.66c-.44.38-.72.9-.78 1.48-.12 1.06.52 2.08 1.56 2.6.25.12.52.2.8.26 1.18.28 2.44.54 3.6.86.1.64.54 1.2 1.18 1.64 1.66 1.14 4.08 1.4 6.4.54 1.42-.52 2.7-1.4 3.78-2.55 1.86-2 3.2-4.6 3.56-7.04.18-1.18.08-2.3-.22-3.3-.1-.32-.2-.64-.32-.94zM7.3 16.68c-1.26-.38-3.68-.82-4.66-1.7-.36-.32-.58-.74-.58-1.18 0-.74.56-1.38 1.32-1.5.52-.08 1.04.08 1.48.34.82.48 1.5 1.14 2.2 1.76.92.82 1.93 1.56 3.08 2.06-1.16-.02-2.26-.4-2.84-.78zm4.7 2.2c-2.1.16-4.06-.82-5.06-2.22-1-1.4-.74-3.08.9-3.88.86-.42 1.86-.4 2.74-.02 1.26.54 2.14 1.64 2.62 2.88.3.78.36 1.62.1 2.48-.08.28-.24.54-.44.76h-.86zm6.22-5.32c-1.1 1.48-2.94 2.36-4.44 2.6-.36.06-.72.08-1.08.06.32-.3.6-.64.8-1.02.4-.7.56-1.5.4-2.28-.26-1.28-1.14-2.42-2.3-3.2-1.12-.76-2.48-1.06-3.86-.82-.7.12-1.38.4-1.96.8-.28.2-.54.42-.78.66.46-1.22 1.18-2.3 2.1-3.16 2.06-1.88 4.76-2.8 7.18-2.46 1.72.24 3.16 1.14 3.64 2.32.02.04.04.08.06.12.5 1.38.5 3.12-.18 4.86-.14.36-.3.72-.48 1.06z"/></svg>
                                            <span>微博</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <!-- 沉浸阅读 -->
                            <button
                                type="button"
                                :class="['article-quick-button', { active: immersiveMode }]"
                                :title="immersiveMode ? '退出沉浸模式 (Esc)' : '沉浸阅读'"
                                @click="toggleImmersive"
                            >
                                <span class="article-quick-label">{{ immersiveMode ? '退出' : '沉浸' }}</span>
                            </button>
                        </div>
                    </div>
                </section>

                <figure v-if="article.cover" class="article-cover">
                    <img
                        class="article-hero"
                        :src="article.cover"
                        :alt="article.coverAlt || `${article.title} 封面图`"
                        loading="eager"
                        fetchpriority="high"
                        decoding="async"
                    >
                </figure>

                <MarkdownPreview v-if="articleMarkdown" :content="articleMarkdown" />
                <section v-else class="article-content-empty">
                    <p>正文暂时为空，稍后再来看一眼。</p>
                </section>

                <!-- 上下篇导航 -->
                <section v-if="neighborPrev || neighborNext" class="article-neighbors">
                    <RouterLink
                        v-if="neighborPrev"
                        :to="`/articles/${neighborPrev.id}`"
                        class="article-neighbor article-neighbor--prev"
                    >
                        <span class="article-neighbor-label">上一篇</span>
                        <span class="article-neighbor-title">{{ neighborPrev.title }}</span>
                    </RouterLink>
                    <span v-else class="article-neighbor article-neighbor--prev article-neighbor--empty" />
                    <RouterLink
                        v-if="neighborNext"
                        :to="`/articles/${neighborNext.id}`"
                        class="article-neighbor article-neighbor--next"
                    >
                        <span class="article-neighbor-label">下一篇</span>
                        <span class="article-neighbor-title">{{ neighborNext.title }}</span>
                    </RouterLink>
                    <span v-else class="article-neighbor article-neighbor--next article-neighbor--empty" />
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
            <AdBanner :slot-code="'article_sidebar'" />
            <section
                v-if="recommendationSections.length > 0 || relatedLoading"
                class="side-related"
                data-testid="article-related-section"
            >
                <h2 class="side-related-title">相关推荐</h2>
                <div v-if="relatedLoading" class="side-related-loading">加载中...</div>
                <div v-else class="side-related-sections">
                    <section
                        v-for="section in recommendationSections"
                        :key="section.key"
                        class="side-related-group"
                    >
                        <h3>{{ section.title }}</h3>
                        <div class="side-related-list">
                            <RouterLink
                                v-for="rel in section.items.slice(0, 5)"
                                :key="rel.id"
                                class="side-related-item"
                                :to="`/articles/${rel.id}`"
                            >
                                <img
                                    v-if="rel.cover"
                                    class="side-related-thumb"
                                    :src="rel.cover"
                                    :alt="rel.title"
                                    loading="lazy"
                                    decoding="async"
                                >
                                <div class="side-related-info">
                                    <p class="side-related-item-title">{{ rel.title }}</p>
                                    <div class="side-related-stats">
                                        <span v-if="rel.category">{{ rel.category }}</span>
                                        <span>{{ rel.viewCount }} 阅读</span>
                                    </div>
                                </div>
                            </RouterLink>
                        </div>
                    </section>
                </div>
            </section>
        </aside>
    </main>
    <!-- 加载骨架屏 -->
    <main v-else-if="isLoading" class="page-shell detail-layout" aria-busy="true" aria-label="文章加载中">
        <div class="article-main">
            <div class="article-skeleton">
                <div class="article-body">
                    <div class="skeleton-panel">
                        <div class="skeleton-meta-row">
                            <div class="skeleton-chip"></div>
                            <div class="skeleton-chip"></div>
                            <div class="skeleton-chip"></div>
                        </div>
                        <div class="skeleton-heading"></div>
                        <div class="skeleton-heading skeleton-heading-short"></div>
                        <div class="skeleton-summary"></div>
                        <div class="skeleton-summary skeleton-summary-short"></div>
                        <div class="skeleton-author-row">
                            <div class="skeleton-avatar-sm"></div>
                            <div class="skeleton-author-meta">
                                <div class="skeleton-name"></div>
                                <div class="skeleton-date"></div>
                            </div>
                        </div>
                    </div>
                    <div class="skeleton-hero"></div>
                    <div class="skeleton-content-block">
                        <div v-for="i in 6" :key="i" class="skeleton-paragraph" :style="{ width: `${70 + (i % 3) * 10}%` }"></div>
                        <div class="skeleton-paragraph skeleton-short"></div>
                        <div v-for="i in 4" :key="`b${i}`" class="skeleton-paragraph" :style="{ width: `${65 + (i % 4) * 8}%` }"></div>
                    </div>
                </div>
            </div>
        </div>
        <aside class="detail-side">
            <div class="skeleton-toc">
                <div class="skeleton-toc-title"></div>
                <div v-for="i in 5" :key="i" class="skeleton-toc-item" :style="{ width: `${50 + (i % 3) * 15}%` }"></div>
            </div>
        </aside>
    </main>

    <!-- 错误状态 -->
    <main v-else class="page-shell detail-layout">
        <section class="article-main empty-state">
            <p class="eyebrow">未找到文章</p>
            <h1>这篇文章暂时不可访问</h1>
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
    <button
        v-if="remoteArticle"
        class="mobile-toc-trigger"
        type="button"
        @click="tocDrawerOpen = true"
    >
        目录
    </button>
    <div v-if="tocDrawerOpen" class="mobile-toc-overlay" @click.self="tocDrawerOpen = false">
        <section class="mobile-toc-drawer" aria-label="文章目录抽屉">
            <div class="mobile-toc-header">
                <h3>文章目录</h3>
                <button ref="mobileTocCloseButtonRef" type="button" @click="tocDrawerOpen = false">关闭</button>
            </div>
            <ArticleToc
                :content="articleMarkdown"
                mobile-visible
                @navigate="tocDrawerOpen = false"
            />
        </section>
    </div>
    <ReportDialog
        v-if="article"
        :visible="reportDialogVisible"
        target-type="ARTICLE"
        :target-id="article.id"
        :target-title="article.title"
        @close="reportDialogVisible = false"
        @success="handleReportSuccess"
    />
</template>

<style scoped>
/* 阅读进度条 */
.reading-progress-bar {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 9999;
    height: 3px;
    background: linear-gradient(90deg, var(--brand), #60a5fa);
    border-radius: 0 2px 2px 0;
    transition: width 0.1s linear;
    pointer-events: none;
}

.mobile-toc-trigger {
    position: fixed;
    right: 16px;
    bottom: 72px;
    z-index: 70;
    display: none;
    padding: 10px 14px;
    border: 0;
    border-radius: 999px;
    background: var(--brand);
    color: #fff;
    box-shadow: var(--shadow-md);
}

.mobile-toc-overlay {
    position: fixed;
    inset: 0;
    z-index: 120;
    display: grid;
    align-items: end;
    background: rgba(15, 23, 42, 0.38);
}

:global(body.mobile-toc-open) {
    overflow: hidden;
}

.mobile-toc-drawer {
    max-height: 72vh;
    overflow: auto;
    padding: 14px;
    background: var(--surface);
    border-radius: 14px 14px 0 0;
}

.mobile-toc-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
}

.mobile-toc-header h3 {
    margin: 0;
    font-size: 16px;
}

.mobile-toc-header button {
    border: 0;
    background: transparent;
    color: var(--brand);
}

@media (max-width: 980px) {
    .mobile-toc-trigger {
        display: inline-flex;
    }
}

/* 骨架屏动画 */
@keyframes skeleton-shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}

.skeleton-hero,
.skeleton-chip,
.skeleton-heading,
.skeleton-summary,
.skeleton-avatar-sm,
.skeleton-name,
.skeleton-date,
.skeleton-paragraph,
.skeleton-toc-title,
.skeleton-toc-item {
    background: linear-gradient(90deg, var(--surface-muted, #f0f3f6) 25%, #e4eaf0 50%, var(--surface-muted, #f0f3f6) 75%);
    background-size: 200% 100%;
    border-radius: 6px;
    animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

.skeleton-hero {
    width: 100%;
    max-width: var(--layout-reading-width);
    aspect-ratio: 16 / 9;
    max-height: 360px;
    margin: 0 0 16px;
    border-radius: var(--radius-sm);
}

.skeleton-panel {
    display: grid;
    gap: 14px;
    padding: 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    margin-bottom: 16px;
}

.skeleton-meta-row {
    display: flex;
    gap: 8px;
}

.skeleton-chip {
    height: 20px;
    width: 64px;
    border-radius: 999px;
}

.skeleton-heading {
    height: 36px;
    width: 90%;
}

.skeleton-heading-short {
    width: 60%;
    height: 28px;
}

.skeleton-summary {
    height: 16px;
    width: 100%;
}

.skeleton-summary-short {
    width: 72%;
}

.skeleton-author-row {
    display: flex;
    gap: 12px;
    align-items: center;
    margin-top: 4px;
}

.skeleton-avatar-sm {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    flex-shrink: 0;
}

.skeleton-author-meta {
    display: grid;
    gap: 8px;
    flex: 1;
}

.skeleton-name {
    height: 14px;
    width: 120px;
}

.skeleton-date {
    height: 12px;
    width: 80px;
}

.skeleton-content-block {
    display: grid;
    gap: 12px;
    padding: 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.skeleton-paragraph {
    height: 14px;
}

.skeleton-short {
    width: 40% !important;
    height: 14px;
    margin-bottom: 10px;
}

.skeleton-toc {
    display: grid;
    gap: 10px;
    padding: 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.skeleton-toc-title {
    height: 18px;
    width: 60%;
    margin-bottom: 4px;
}

.skeleton-toc-item {
    height: 12px;
}

.article-heading-panel {
    display: grid;
    gap: 16px;
    margin-bottom: 24px;
    padding: 20px 20px 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
}

.article-heading-top,
.article-heading-bottom {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
}

.article-meta-row {
    margin: 0;
}

.article-heading-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 24px;
    flex: 1 1 460px;
    min-width: 0;
    align-items: center;
}

.article-byline {
    display: flex;
    align-items: center;
    gap: 10px;
    text-decoration: none;
    color: inherit;
    flex-shrink: 0;
}

.byline-avatar,
.byline-avatar-fallback {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
}

.byline-avatar-fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--surface-soft);
    color: var(--muted);
    font-size: 13px;
    font-weight: 700;
}

.byline-info {
    display: grid;
    gap: 1px;
}

.byline-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--text);
    line-height: 1.4;
}

.byline-meta {
    font-size: 12px;
    color: var(--muted);
    line-height: 1.4;
    white-space: nowrap;
}

.article-stats-row {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: center;
}

.article-stat {
    font-size: 12px;
    color: var(--muted);
    line-height: 1;
    white-space: nowrap;
}

.article-stat strong {
    color: var(--text-strong);
    font-size: 13px;
    font-weight: 600;
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
    min-width: 84px;
    min-height: 34px;
    padding: 0 14px;
    color: var(--text);
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    transition: color 0.12s, border-color 0.12s, background 0.12s;
}

.article-quick-button:hover:not(:disabled) {
    color: var(--brand);
    background: var(--brand-soft);
    border-color: var(--brand);
}

.article-quick-button.active {
    color: var(--brand);
    background: var(--brand-soft);
    border-color: var(--brand);
}

.article-quick-button.subtle {
    color: var(--muted);
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
    padding: 24px 20px;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px dashed var(--line);
    border-radius: var(--radius-sm);
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
    min-height: 28px;
    padding: 0 10px;
    line-height: 1;
    color: var(--text);
    font-size: 13px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.comment-placeholder {
    padding: 32px;
    text-align: center;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.side-related {
    display: grid;
    gap: 12px;
    padding: 16px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.side-related-title {
    margin: 0;
    font-size: 15px;
    font-weight: 700;
    color: var(--text-strong);
}

.side-related-loading {
    color: var(--muted);
    font-size: 13px;
    padding: 8px 0;
}

.side-related-sections {
    display: grid;
    gap: 18px;
}

.side-related-group {
    display: grid;
    gap: 8px;
}

.side-related-group h3 {
    margin: 0;
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
}

.side-related-list {
    display: flex;
    flex-direction: column;
    gap: 0;
}

.side-related-item {
    display: grid;
    grid-template-columns: 56px minmax(0, 1fr);
    gap: 10px;
    align-items: center;
    text-decoration: none;
    padding: 8px 6px;
    margin: 0 -6px;
    border-radius: var(--radius-sm);
    transition: background 0.12s;
}

.side-related-item:hover {
    background: var(--surface-soft);
}

.side-related-thumb {
    width: 56px;
    height: 40px;
    object-fit: cover;
    border-radius: var(--radius-sm);
    flex-shrink: 0;
}

.side-related-info {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.side-related-item-title {
    margin: 0;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-strong);
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.side-related-stats {
    display: flex;
    gap: 8px;
    font-size: 11px;
    color: var(--muted);
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

    .article-related-list {
        gap: 0;
    }
}

/* 沉浸阅读模式 */
.detail-immersive {
    max-width: 100%;
    padding-left: 0;
    padding-right: 0;
}

.detail-immersive .detail-side {
    display: none;
}

.detail-immersive .article-main {
    max-width: 800px;
    margin: 0 auto;
    border: none;
    border-radius: 0;
    box-shadow: none;
    background: transparent;
}

.detail-immersive .article-heading-panel {
    border: none;
    border-radius: 0;
    padding: 0;
    background: transparent;
}

.detail-immersive .article-body {
    padding: 28px 24px;
}

/* 分享卡片（B 站风格） */
.article-share-wrap {
    position: relative;
}

.article-share-card {
    position: absolute;
    right: 0;
    bottom: calc(100% + 10px);
    z-index: 100;
    min-width: 150px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 10px;
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
    animation: share-card-in 0.2s ease;
}

@keyframes share-card-in {
    from {
        opacity: 0;
        transform: translateY(6px) scale(0.96);
    }
    to {
        opacity: 1;
        transform: translateY(0) scale(1);
    }
}

.share-card-arrow {
    position: absolute;
    bottom: -6px;
    right: 36px;
    width: 12px;
    height: 12px;
    background: var(--surface);
    border-right: 1px solid var(--line);
    border-bottom: 1px solid var(--line);
    transform: rotate(45deg);
}

.share-card-body {
    padding: 8px;
}

.share-option {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 9px 12px;
    font-size: 14px;
    color: var(--text-strong);
    cursor: pointer;
    border-radius: 8px;
    transition: background 0.15s;
    white-space: nowrap;
}

.share-option:hover {
    background: var(--surface-soft);
}

.share-icon {
    flex: none;
    color: var(--muted);
}

@media (max-width: 768px) {
    .article-quick-actions .article-quick-button:last-child {
        display: none;
    }
}

/* 上下篇导航：上一篇在左，下一篇在右 */
.article-neighbors {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-top: 40px;
    padding-top: 24px;
    border-top: 1px solid var(--line);
}

.article-neighbor {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 12px 16px;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    text-decoration: none;
    transition: border-color 0.12s, background 0.12s;
}

.article-neighbor:hover {
    border-color: var(--brand);
    background: var(--brand-soft);
}

.article-neighbor--empty {
    visibility: hidden;
}

.article-neighbor--next {
    text-align: right;
}

.article-neighbor-label {
    font-size: 12px;
    color: var(--muted);
}

.article-neighbor-title {
    font-size: 14px;
    color: var(--text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

<style>
/* 沉浸阅读 — 全局规则（body/site-header 不受 scoped 限制） */
body.immersive-mode .site-header {
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.25s;
}

body.immersive-mode {
    background: #fff;
}
</style>
