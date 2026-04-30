<script setup>import {computed, inject, onMounted, onUnmounted, ref, watch} from 'vue';
import {RouterLink, useRoute} from 'vue-router';
import {useHead} from '@unhead/vue';
import {getArticleApi, getArticleNeighborsApi, getRelatedArticlesApi} from '@/api/articles';
import {likeArticleApi, unlikeArticleApi} from '@/api/likes';
import {favoriteArticleApi, unfavoriteArticleApi} from '@/api/favorites';
import SiteHeader from '@/components/SiteHeader.vue';
import ArticleToc from '@/components/ArticleToc.vue';
import CommentList from '@/components/CommentList.vue';
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import ReportDialog from '@/components/ReportDialog.vue';
import AdBanner from '@/components/AdBanner.vue';
import {articles} from '@/data/home';
import {useSession} from '@/stores/session';

const route = useRoute();
const loginModal = inject('loginModal', { requireLogin: () => false });
const toast = inject('toast', { error: () => {}, success: () => {} });
const { state } = useSession();

const remoteArticle = ref(null);
const isLoading = ref(false);
const loadError = ref('');
const useLocalFallback = ref(false);
const relatedArticles = ref([]);
const relatedLoading = ref(false);
const neighborPrev = ref(null);
const neighborNext = ref(null);
const articleId = computed(() => String(route.params.id || '').replace(/-.+$/, ''));
const localArticle = computed(() => articles.find((item) => String(item.id) === articleId.value) || null);
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
const readingProgress = ref(0);
const likeSubmitting = ref(false);
const favoriteSubmitting = ref(false);
const reportDialogVisible = ref(false);
const immersiveMode = ref(false);
const showShareMenu = ref(false);
const shareCopied = ref(false);
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
    relatedArticles.value = [];
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

const fetchRelated = async (id) => {
    relatedLoading.value = true;
    try {
        relatedArticles.value = await getRelatedArticlesApi(id, 5);
    } catch {
        relatedArticles.value = [];
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

const handleReportSuccess = () => {
    toast.success('举报已提交，管理员会尽快处理');
};

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
    }
};

const copyArticleLink = async () => {
    const url = window.location.href;
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

const closeShareIfOutside = (e) => {
    if (!e.target.closest('.article-share-wrap')) {
        showShareMenu.value = false;
    }
};

const shareToWeibo = () => {
    if (!article.value) return;
    const url = encodeURIComponent(window.location.href);
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
        fetchRelated(val.id);
        fetchNeighbors(val.id);
    }
});

onMounted(() => {
    window.addEventListener('scroll', handleScroll);
    window.addEventListener('keydown', handleImmersiveKeydown);
    document.addEventListener('click', closeShareIfOutside);
});

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll);
    window.removeEventListener('keydown', handleImmersiveKeydown);
    document.removeEventListener('click', closeShareIfOutside);
    document.body.classList.remove('immersive-mode');
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
            <img class="article-hero" :src="article.cover" :alt="article.coverAlt" loading="eager">
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
                                <img :src="article.author.avatar" alt="作者头像" loading="lazy">
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
                            <button
                                v-if="showArticleReport"
                                type="button"
                                class="article-quick-button subtle"
                                @click="openReportArticle"
                            >
                                <span class="article-quick-label">举报</span>
                            </button>
                            <!-- 分享 -->
                            <div class="article-share-wrap">
                                <button
                                    type="button"
                                    :class="['article-quick-button', { active: shareCopied }]"
                                    :title="shareCopied ? '已复制链接' : '分享文章'"
                                    @click="showShareMenu = !showShareMenu"
                                >
                                    <span class="article-quick-label">{{ shareCopied ? '已复制' : '分享' }}</span>
                                </button>
                                <div v-if="showShareMenu" class="article-share-menu">
                                    <button type="button" @click="copyArticleLink">复制链接</button>
                                    <button type="button" @click="shareToWeibo">分享到微博</button>
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

                <section
                    v-if="relatedArticles.length > 0 || relatedLoading"
                    class="article-related"
                    data-testid="article-related-section"
                >
                    <h2 class="article-related-title">相关推荐</h2>
                    <div v-if="relatedLoading" class="article-related-loading">加载中...</div>
                    <div v-else class="article-related-list">
                        <RouterLink
                            v-for="rel in relatedArticles"
                            :key="rel.id"
                            class="article-related-item"
                            :to="`/articles/${rel.id}`"
                        >
                            <div
                                class="article-related-cover"
                                :style="rel.cover
                                    ? { backgroundImage: `url(${rel.cover})` }
                                    : undefined"
                            />
                            <div class="article-related-meta">
                                <span v-if="rel.category" class="article-related-category">{{ rel.category }}</span>
                                <p class="article-related-item-title">{{ rel.title }}</p>
                                <div class="article-related-stats">
                                    <span>{{ rel.viewCount }} 阅读</span>
                                    <span>{{ rel.likeCount }} 赞</span>
                                </div>
                            </div>
                        </RouterLink>
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
        </aside>
    </main>
    <!-- 加载骨架屏 -->
    <main v-else-if="isLoading" class="page-shell detail-layout" aria-busy="true" aria-label="文章加载中">
        <div class="article-main">
            <div class="article-skeleton">
                <div class="skeleton-hero"></div>
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
    aspect-ratio: 21 / 8;
    border-radius: 0;
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
    gap: 6px;
    min-height: 30px;
    padding: 0 10px;
    color: var(--muted);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
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

.article-related {
    margin-top: 40px;
    padding-top: 24px;
    border-top: 1px solid var(--line);
}

.article-related-title {
    margin: 0 0 16px;
    font-size: 16px;
    font-weight: 700;
    color: var(--text-strong);
}

.article-related-loading {
    color: var(--muted);
    font-size: 13px;
    padding: 12px 0;
}

.article-related-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 12px;
}

.article-related-item {
    display: flex;
    flex-direction: column;
    text-decoration: none;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    overflow: hidden;
    transition: border-color 0.12s, box-shadow 0.12s;
}

.article-related-item:hover {
    border-color: var(--brand);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.article-related-cover {
    width: 100%;
    aspect-ratio: 16 / 9;
    background: var(--surface-soft) center / cover no-repeat;
}

.article-related-meta {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 10px 12px;
    flex: 1;
}

.article-related-category {
    font-size: 11px;
    color: var(--brand);
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.04em;
}

.article-related-item-title {
    margin: 0;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-strong);
    line-height: 1.45;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.article-related-stats {
    display: flex;
    gap: 10px;
    margin-top: auto;
    padding-top: 6px;
    font-size: 12px;
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
        grid-template-columns: 1fr 1fr;
    }
}
</style>
