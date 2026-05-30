<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {ARTICLE_SORT_LATEST} from '@/constants/articleSort';
import UserEquippedBadge from '@/components/UserEquippedBadge.vue';
import {DEFAULT_ARTICLE_COVER_URL} from '@/utils/media';

const props = defineProps({
    articles: {
        type: Array,
        default: () => []
    },
    eyebrow: {
        type: String,
        default: '推荐阅读'
    },
    title: {
        type: String,
        default: '最新技术文章'
    },
    emptyText: {
        type: String,
        default: '暂时没有找到文章'
    },
    page: {
        type: Number,
        default: 1
    },
    pageSize: {
        type: Number,
        default: 10
    },
    total: {
        type: Number,
        default: 0
    },
    loading: {
        type: Boolean,
        default: false
    },
    initialLoading: {
        type: Boolean,
        default: false
    },
    refreshing: {
        type: Boolean,
        default: false
    },
    hasLoadedOnce: {
        type: Boolean,
        default: false
    },
    errorMessage: {
        type: String,
        default: ''
    },
    inlineErrorMessage: {
        type: String,
        default: ''
    },
    hideSort: {
        type: Boolean,
        default: false
    },
    sort: {
        type: String,
        default: ARTICLE_SORT_LATEST
    },
    sortItems: {
        type: Array,
        default: () => []
    },
    highlightKeyword: {
        type: String,
        default: ""
    },
    paginationMode: {
        type: String,
        default: 'paged',
        validator: (value) => ['paged', 'infinite'].includes(value)
    },
    hasMore: {
        type: Boolean,
        default: false
    },
    loadingMore: {
        type: Boolean,
        default: false
    },
    loadMoreError: {
        type: String,
        default: ''
    },
    mobileFeaturedLarge: {
        type: Boolean,
        default: false
    },
    articleLinkQuery: {
        type: Object,
        default: () => ({})
    }
});

const emit = defineEmits(['page-change', 'sort-change', 'load-more']);
const router = useRouter();
const jumpPage = ref(String(props.page));
const loadMoreTrigger = ref(null);
const loadMoreTriggerVisible = ref(false);
let loadMoreObserver = null;

const isInitialLoading = computed(() =>
    props.initialLoading || (props.loading && !props.hasLoadedOnce && props.articles.length === 0)
);
const isRefreshing = computed(() =>
    props.refreshing || (props.loading && (props.hasLoadedOnce || props.articles.length > 0))
);
const isBusy = computed(() => isInitialLoading.value || isRefreshing.value);
const isInfiniteMode = computed(() => props.paginationMode === 'infinite');
const canRequestLoadMore = computed(() =>
    isInfiniteMode.value
    && props.hasMore
    && !isBusy.value
    && !props.loadingMore
);
const showEmpty = computed(() =>
    !isInitialLoading.value
    && !isRefreshing.value
    && !props.errorMessage
    && props.articles.length === 0
);
const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));

const escapeHtml = (value) => String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const highlightHtml = (text) => {
    const kw = props.highlightKeyword ? props.highlightKeyword.trim() : "";
    const safeText = escapeHtml(text);
    if (!kw || !safeText) return safeText;
    const escapedKeyword = escapeHtml(kw).replace(/[.*+?^()|\[\]\\]/g, '\\$&');
    return safeText.replace(new RegExp(escapedKeyword, "gi"), m => `<mark class="search-highlight">${m}</mark>`);
};
const hasUsableCover = (article) => Boolean(article?.cover);
const setCoverFallback = (event) => {
    if (event.target.src.includes(DEFAULT_ARTICLE_COVER_URL)) {
        return;
    }
    event.target.src = DEFAULT_ARTICLE_COVER_URL;
};
const pageStart = computed(() => {
    if (!props.total) {
        return 0;
    }
    return (props.page - 1) * props.pageSize + 1;
});
const pageEnd = computed(() => Math.min(props.page * props.pageSize, props.total));

const paginationItems = computed(() => {
    const pages = [];
    const addPage = (page) => {
        if (!pages.some((item) => item.type === 'page' && item.value === page)) {
            pages.push({ type: 'page', value: page });
        }
    };
    const addEllipsis = (key) => {
        pages.push({ type: 'ellipsis', value: key });
    };

    if (totalPages.value <= 7) {
        for (let page = 1; page <= totalPages.value; page++) {
            addPage(page);
        }
        return pages;
    }

    addPage(1);
    if (props.page > 4) {
        addEllipsis('left');
    }

    const start = Math.max(2, props.page - 1);
    const end = Math.min(totalPages.value - 1, props.page + 1);
    for (let page = start; page <= end; page++) {
        addPage(page);
    }

    if (props.page < totalPages.value - 3) {
        addEllipsis('right');
    }
    addPage(totalPages.value);
    return pages;
});

const goToPage = (page) => {
    if (isBusy.value || page < 1 || page > totalPages.value || page === props.page) {
        return;
    }
    emit('page-change', page);
};

const requestLoadMore = () => {
    if (!canRequestLoadMore.value) {
        return;
    }
    emit('load-more');
};

const maybeAutoLoadMore = () => {
    if (!loadMoreTriggerVisible.value || props.loadMoreError) {
        return;
    }
    requestLoadMore();
};

const teardownLoadMoreObserver = () => {
    if (loadMoreObserver) {
        loadMoreObserver.disconnect();
        loadMoreObserver = null;
    }
};

const setupLoadMoreObserver = () => {
    teardownLoadMoreObserver();
    loadMoreTriggerVisible.value = false;
    if (!isInfiniteMode.value || typeof IntersectionObserver === 'undefined' || !loadMoreTrigger.value) {
        return;
    }

    loadMoreObserver = new IntersectionObserver((entries) => {
        loadMoreTriggerVisible.value = entries.some((entry) => entry.isIntersecting);
        maybeAutoLoadMore();
    }, {
        rootMargin: '360px 0px',
        threshold: 0
    });
    loadMoreObserver.observe(loadMoreTrigger.value);
};

const submitJump = () => {
    const page = Number.parseInt(jumpPage.value, 10);
    if (Number.isNaN(page)) {
        jumpPage.value = String(props.page);
        return;
    }
    const targetPage = Math.min(Math.max(1, page), totalPages.value);
    jumpPage.value = String(targetPage);
    goToPage(targetPage);
};

const openArticle = (article) => {
    const path = article.slug ? `/articles/${article.id}-${article.slug}` : `/articles/${article.id}`;
    const query = props.articleLinkQuery && Object.keys(props.articleLinkQuery).length > 0
        ? props.articleLinkQuery
        : undefined;
    router.push(query ? { path, query } : path);
};

watch(
    () => props.page,
    (page) => {
        jumpPage.value = String(page);
    }
);

watch(
    () => [props.paginationMode, loadMoreTrigger.value],
    setupLoadMoreObserver,
    { flush: 'post' }
);

watch(
    () => [props.articles.length, props.hasMore, props.loadingMore, props.loadMoreError, isBusy.value],
    maybeAutoLoadMore,
    { flush: 'post' }
);

onMounted(setupLoadMoreObserver);
onBeforeUnmount(teardownLoadMoreObserver);
</script>

<template>
    <section
        class="feed"
        :class="{ loading: isInitialLoading, refreshing: isRefreshing }"
        aria-labelledby="feed-title"
        data-feed-root
    >
        <div class="section-heading">
            <div>
                <p class="eyebrow">{{ eyebrow }}</p>
                <h2 id="feed-title">{{ title }}</h2>
            </div>
            <div v-if="!hideSort" class="sort-tabs" aria-label="排序" :class="{ invisible: !props.sortItems.length }">
                <button
                    v-for="item in props.sortItems"
                    :key="item.value"
                    type="button"
                    :class="{ active: props.sort === item.value }"
                    :disabled="isBusy || props.sort === item.value"
                    @click="emit('sort-change', item.value)"
                >
                    {{ item.label }}
                </button>
            </div>
        </div>

        <div v-if="isRefreshing" class="feed-refresh-note">
            正在更新内容...
        </div>

        <p v-if="inlineErrorMessage" class="feed-message feed-message-inline">{{ inlineErrorMessage }}</p>

        <template v-if="showEmpty" class="empty-state">
            <div class="empty-state">{{ emptyText }}</div>
        </template>

        <template v-else-if="articles.length > 0">
            <article
                v-for="article in articles"
                :key="article.id"
                class="post-item"
                :class="{
                    'featured-post': article.featured,
                    'mobile-featured-large': article.featured && mobileFeaturedLarge,
                    'interactive-post': true,
                    'post-item--no-cover': !hasUsableCover(article)
                }"
                role="link"
                tabindex="0"
                @click="openArticle(article)"
                @keydown.enter="openArticle(article)"
                @keydown.space.prevent="openArticle(article)"
            >
                <div v-if="hasUsableCover(article)" class="post-cover" :aria-label="`查看文章：${article.title}`">
                    <img
                        :src="article.cover"
                        :alt="article.coverAlt"
                        loading="lazy"
                        decoding="async"
                        @error="setCoverFallback"
                    >
                    <span v-if="article.featured" class="post-featured-badge" title="精选文章">⭐</span>
                </div>
                <div class="post-content">
                    <div class="post-meta">
                        <span>{{ article.category }}</span>
                        <span>{{ article.readingTime }}</span>
                        <span>{{ article.publishedText }}</span>
                    </div>
                    <h3 class="post-title" v-html="highlightHtml(article.title)"></h3>
                    <p v-html="highlightHtml(article.summary)"></p>
                    <div class="post-footer">
                        <RouterLink
                            class="author author-hotspot"
                            :to="`/users/${article.author.id}`"
                            @click.stop
                            @keydown.enter.stop
                            @keydown.space.stop
                        >
                            <img :src="article.author.avatar" alt="作者头像" loading="lazy" decoding="async">
                            <span>{{ article.author.name }}</span>
                            <UserEquippedBadge :badge="article.author.equippedBadge" compact />
                        </RouterLink>
                        <span>{{ article.stats.views }}</span>
                        <span>{{ article.stats.likes }}</span>
                        <span>{{ article.stats.comments }}</span>
                    </div>
                </div>
            </article>
        </template>

        <div v-if="isInitialLoading" class="loading-placeholder">
            <div v-for="i in 3" :key="i" class="skeleton-post">
                <div class="skeleton-cover"></div>
                <div class="skeleton-content">
                    <div class="skeleton-title"></div>
                    <div class="skeleton-text"></div>
                    <div class="skeleton-text short"></div>
                </div>
            </div>
        </div>

        <p v-if="errorMessage && articles.length === 0" class="feed-message">{{ errorMessage }}</p>

        <div
            v-if="isInfiniteMode && (articles.length > 0 || loadingMore || loadMoreError)"
            class="infinite-load-panel"
            aria-live="polite"
        >
            <span ref="loadMoreTrigger" class="infinite-load-trigger" aria-hidden="true"></span>
            <div v-if="loadingMore" class="infinite-load-status">
                <span class="infinite-load-spinner" aria-hidden="true"></span>
                <span>正在加载更多...</span>
            </div>
            <button
                v-else-if="loadMoreError"
                type="button"
                class="infinite-load-button error"
                :disabled="isBusy"
                @click="requestLoadMore"
            >
                {{ loadMoreError }}，点击重试
            </button>
            <button
                v-else-if="hasMore"
                type="button"
                class="infinite-load-button"
                :disabled="isBusy"
                @click="requestLoadMore"
            >
                加载更多
            </button>
            <p v-else class="infinite-load-finished">已加载全部文章</p>
        </div>

        <nav v-if="!isInfiniteMode && totalPages > 1" class="pagination-bar" aria-label="文章分页">
            <p>
                第 {{ page }} / {{ totalPages }} 页，
                共 {{ total }} 篇，当前 {{ pageStart }}-{{ pageEnd }} 篇
            </p>
            <div class="pagination-actions">
                <button type="button" :disabled="isBusy || page <= 1" @click="goToPage(1)">首页</button>
                <button type="button" :disabled="isBusy || page <= 1" @click="goToPage(page - 1)">上一页</button>
                <template v-for="item in paginationItems" :key="`${item.type}-${item.value}`">
                    <span v-if="item.type === 'ellipsis'" class="pagination-ellipsis">...</span>
                    <button
                        v-else
                        type="button"
                        :class="{ active: item.value === page }"
                        :disabled="isBusy || item.value === page"
                        @click="goToPage(item.value)"
                    >
                        {{ item.value }}
                    </button>
                </template>
                <button type="button" :disabled="isBusy || page >= totalPages" @click="goToPage(page + 1)">下一页</button>
                <button type="button" :disabled="isBusy || page >= totalPages" @click="goToPage(totalPages)">末页</button>
            </div>
            <form class="pagination-jump" @submit.prevent="submitJump">
                <label for="page-jump-input">跳至</label>
                <input
                    id="page-jump-input"
                    v-model="jumpPage"
                    type="number"
                    min="1"
                    :max="totalPages"
                    :disabled="isBusy"
                    inputmode="numeric"
                    aria-label="输入页码"
                >
                <span>页</span>
                <button type="submit" :disabled="isBusy">跳转</button>
            </form>
        </nav>
    </section>
</template>

<style scoped>
.invisible {
    visibility: hidden;
}

.feed-refresh-note {
    margin: 4px 0 10px;
    padding: 8px 12px;
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
    background: var(--brand-soft);
    border: 1px solid rgba(37, 99, 235, 0.14);
    border-radius: var(--radius-sm);
}

.feed-message-inline {
    margin: 0 0 10px;
}

/* 文章列表卡片 — 蓝白渐变风格，柔和交互 */
.interactive-post {
    cursor: pointer;
    transition: background 0.12s;
}

.interactive-post:hover,
.interactive-post:focus-visible {
    background: var(--surface-soft);
}

.interactive-post:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: 2px;
}

.post-title {
    transition: color 0.12s;
}

.interactive-post:hover .post-title,
.interactive-post:focus-visible .post-title {
    color: var(--brand);
}

.interactive-post:hover .post-cover img,
.interactive-post:focus-visible .post-cover img {
    transform: scale(1.02);
}

.post-cover img {
    transition: transform 0.2s ease;
}

.author-hotspot {
    position: relative;
    z-index: 1;
    transition: color 0.12s;
}

.author-hotspot:hover,
.author-hotspot:focus-visible {
    color: var(--brand);
}

.post-item--no-cover {
    grid-template-columns: minmax(0, 1fr);
}

.post-item--no-cover .post-content {
    grid-column: 1 / -1;
}

/* Loading skeleton — 去大圆角和渐变 */
.loading-placeholder {
    display: flex;
    flex-direction: column;
    gap: 0;
}

.skeleton-post {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 200px;
    gap: 16px;
    align-items: center;
    padding: 16px 0;
    background: transparent;
    border: 0;
    border-bottom: 1px solid var(--line);
    border-radius: 0;
    box-shadow: none;
}

.skeleton-cover {
    grid-column: 2;
    width: 100%;
    aspect-ratio: 16 / 9;
    border-radius: var(--radius-sm);
    background: linear-gradient(90deg, var(--surface-muted) 25%, var(--surface-soft) 50%, var(--surface-muted) 75%);
    background-size: 200% 100%;
    animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

.skeleton-content {
    grid-column: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.skeleton-title {
    height: 20px;
    width: 70%;
    border-radius: var(--radius-sm);
    background: linear-gradient(90deg, var(--surface-muted) 25%, var(--surface-soft) 50%, var(--surface-muted) 75%);
    background-size: 200% 100%;
    animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

.skeleton-text {
    height: 13px;
    width: 100%;
    border-radius: var(--radius-sm);
    background: linear-gradient(90deg, var(--surface-muted) 25%, var(--surface-soft) 50%, var(--surface-muted) 75%);
    background-size: 200% 100%;
    animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

.skeleton-text.short {
    width: 40%;
}

.infinite-load-panel {
    position: relative;
    display: grid;
    min-height: 74px;
    place-items: center;
    padding: 16px 0 4px;
}

.infinite-load-trigger {
    position: absolute;
    bottom: 120px;
    width: 1px;
    height: 1px;
    pointer-events: none;
}

.infinite-load-status,
.infinite-load-button,
.infinite-load-finished {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 40px;
    margin: 0;
    color: var(--muted);
    font-size: 13px;
}

.infinite-load-status {
    gap: 8px;
}

.infinite-load-spinner {
    width: 16px;
    height: 16px;
    border: 2px solid var(--line);
    border-top-color: var(--brand);
    border-radius: 50%;
    animation: infinite-spinner 0.8s linear infinite;
}

.infinite-load-button {
    min-width: 180px;
    padding: 0 18px;
    color: var(--brand);
    font-weight: 700;
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.infinite-load-button:hover:not(:disabled),
.infinite-load-button:focus-visible {
    color: var(--brand-strong);
    border-color: var(--brand);
}

.infinite-load-button.error {
    color: #b91c1c;
}

.infinite-load-button:disabled {
    cursor: not-allowed;
    opacity: 0.65;
}

.infinite-load-finished {
    color: var(--muted);
}

:deep(.empty-state) {
    padding: 32px 20px;
    color: var(--muted);
    text-align: center;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
}

@keyframes skeleton-shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}

@keyframes infinite-spinner {
    to { transform: rotate(360deg); }
}

@media (max-width: 760px) {
    .post-item:not(.mobile-featured-large) {
        grid-template-columns: minmax(0, 1fr) 104px;
        gap: 10px;
        align-items: start;
        padding: 12px 8px;
    }

    .post-item:not(.mobile-featured-large) .post-content {
        min-width: 0;
        order: 1;
    }

    .post-item:not(.mobile-featured-large) .post-cover {
        width: 104px;
        min-width: 104px;
        aspect-ratio: 4 / 3;
        order: 2;
        border-radius: var(--radius-sm);
    }

    .post-item:not(.mobile-featured-large) .post-meta {
        gap: 6px;
    }

    .post-item:not(.mobile-featured-large) .post-meta span:nth-child(2),
    .post-item:not(.mobile-featured-large) .post-footer span:nth-of-type(n + 2) {
        display: none;
    }

    .post-item:not(.mobile-featured-large) .post-title {
        margin: 4px 0;
        font-size: 15px;
        line-height: 1.35;
        white-space: normal;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
    }

    .post-item:not(.mobile-featured-large) .post-content p {
        margin-bottom: 8px;
        font-size: 12px;
        line-height: 1.5;
        -webkit-line-clamp: 2;
    }

    .post-item:not(.mobile-featured-large) .post-footer {
        gap: 6px;
        margin-top: 0;
    }

    .post-item:not(.mobile-featured-large) .author {
        font-size: 12px;
    }

    .post-item:not(.mobile-featured-large) .author img {
        width: 20px;
        height: 20px;
    }

    .mobile-featured-large {
        grid-template-columns: 1fr;
    }

    .mobile-featured-large .post-cover {
        aspect-ratio: 16 / 9;
        order: -1;
    }

    .post-item--no-cover,
    .post-item--no-cover:not(.mobile-featured-large) {
        grid-template-columns: minmax(0, 1fr);
    }

    .skeleton-post {
        grid-template-columns: 1fr;
        gap: 12px;
    }

    .skeleton-cover,
    .skeleton-content {
        grid-column: auto;
    }

    .skeleton-cover {
        aspect-ratio: 16 / 9;
        height: auto;
    }

    .infinite-load-panel {
        min-height: 82px;
        padding-top: 18px;
    }

    .infinite-load-button {
        width: 100%;
    }
}

:deep(.search-highlight) {
    background: #fef08a;
    color: #78350f;
    border-radius: 2px;
    padding: 0 1px;
    font-style: normal;
}
</style>
