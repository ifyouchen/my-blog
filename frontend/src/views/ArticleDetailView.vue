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

const liked = ref(false);
const favorited = ref(false);
const likeCount = ref(0);
const favoriteCount = ref(0);
const commentCount = ref(0);
const feedback = ref('');
const showBackToTop = ref(false);

const syncArticleState = () => {
    if (!article.value) {
        liked.value = false;
        favorited.value = false;
        likeCount.value = 0;
        favoriteCount.value = 0;
        commentCount.value = 0;
        feedback.value = '';
        return;
    }
    likeCount.value = article.value.likeCount || 0;
    favoriteCount.value = article.value.favoriteCount || 0;
    commentCount.value = article.value.commentCount || 0;
    liked.value = Boolean(article.value.liked);
    favorited.value = Boolean(article.value.favorited);
    feedback.value = '';
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
            favoriteCount.value = Math.max(0, favoriteCount.value - 1);
            feedback.value = '已取消收藏';
        } else {
            await favoriteArticleApi(article.value.id);
            favorited.value = true;
            favoriteCount.value++;
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
                        <RouterLink class="article-author" :to="`/users/${article.author.id}`">
                            <img :src="article.author.avatar" alt="作者头像">
                            <div>
                                <strong>{{ article.author.name || article.author.nickname || article.author.username }}</strong>
                                <span>{{ article.viewCount }} 阅读 · {{ likeCount }} 赞 · {{ commentCount }} 评论</span>
                            </div>
                        </RouterLink>

                        <div class="article-quick-actions">
                            <button type="button" :class="['article-quick-button', { active: liked }]" @click="toggleLike">
                                {{ liked ? '已点赞' : '点赞' }} {{ likeCount }}
                            </button>
                            <button type="button" :class="['article-quick-button', { active: favorited }]" @click="toggleFavorite">
                                {{ favorited ? '已收藏' : '收藏' }} {{ favoriteCount }}
                            </button>
                        </div>
                    </div>
                    <p v-if="feedback" class="form-message article-feedback">{{ feedback }}</p>
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
            <section class="side-section article-data-panel">
                <div class="side-section-head">
                    <p class="eyebrow">文章数据</p>
                    <h2>阅读表现</h2>
                </div>
                <div class="article-data-grid">
                    <div>
                        <strong>{{ article.viewCount }}</strong>
                        <span>阅读</span>
                    </div>
                    <div>
                        <strong>{{ likeCount }}</strong>
                        <span>点赞</span>
                    </div>
                    <div>
                        <strong>{{ favoriteCount }}</strong>
                        <span>收藏</span>
                    </div>
                    <div>
                        <strong>{{ commentCount }}</strong>
                        <span>评论</span>
                    </div>
                </div>
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
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
}

.article-meta-row {
    margin: 0;
}

.article-quick-actions {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
}

.article-quick-button {
    min-height: 40px;
    padding: 0 16px;
    color: var(--text);
    font-weight: 700;
    background: #ffffff;
    border: 1px solid var(--line);
    border-radius: 999px;
    transition: border-color 0.18s ease, background-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.article-quick-button:hover {
    color: var(--brand-strong);
    border-color: rgba(31, 122, 224, 0.18);
    background: var(--brand-soft);
    box-shadow: 0 10px 20px rgba(31, 122, 224, 0.08);
}

.article-quick-button.active {
    color: #ffffff;
    background: linear-gradient(135deg, var(--brand), var(--brand-strong));
    border-color: transparent;
}

.article-feedback {
    margin-top: -2px;
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

.article-data-panel {
    display: grid;
    gap: 18px;
}

.side-section-head {
    display: grid;
    gap: 4px;
}

.side-section-head h2 {
    margin: 0;
}

.article-data-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.article-data-grid div {
    display: grid;
    gap: 4px;
    padding: 14px 16px;
    background: var(--bg-secondary);
    border: 1px solid var(--line);
    border-radius: 16px;
}

.article-data-grid strong {
    color: var(--text-strong);
    font-size: 22px;
    line-height: 1.1;
}

.article-data-grid span {
    color: var(--muted);
    font-size: 13px;
}

@media (max-width: 760px) {
    .article-heading-panel {
        padding: 18px;
    }
}
</style>
