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

// Build the regex once per keyword change instead of on every article render
const highlightRegex = computed(() => {
    const kw = props.highlightKeyword ? props.highlightKeyword.trim() : '';
    if (!kw) return null;
    const escaped = escapeHtml(kw).replace(/[.*+?^()|\[\]\\]/g, '\\$&');
    return new RegExp(escaped, 'gi');
});

const highlightHtml = (text) => {
    const safeText = escapeHtml(text);
    if (!highlightRegex.value || !safeText) return safeText;
    return safeText.replace(highlightRegex.value, m => `<mark class="search-highlight">${m}</mark>`);
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
        rootMargin: '200px 0px',
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
                v-memo="[article.id, article.liked, article.favorited, article.stats?.views, article.stats?.likes, article.stats?.comments, highlightRegex]"
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

<style scoped src="@/styles/components/ArticleFeed.part-1.css"></style>
<style scoped src="@/styles/components/ArticleFeed.part-2.css"></style>
