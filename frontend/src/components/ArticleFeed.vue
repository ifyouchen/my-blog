<script setup>
import {computed, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {articles as defaultArticles} from '@/data/home';
import {ARTICLE_SORT_LATEST} from '@/constants/articleSort';

const props = defineProps({
    articles: {
        type: Array,
        default: () => defaultArticles
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
    }
});

const emit = defineEmits(['page-change', 'sort-change']);
const router = useRouter();
const jumpPage = ref(String(props.page));

const isInitialLoading = computed(() =>
    props.initialLoading || (props.loading && !props.hasLoadedOnce && props.articles.length === 0)
);
const isRefreshing = computed(() =>
    props.refreshing || (props.loading && (props.hasLoadedOnce || props.articles.length > 0))
);
const isBusy = computed(() => isInitialLoading.value || isRefreshing.value);
const showEmpty = computed(() =>
    !isInitialLoading.value
    && !isRefreshing.value
    && !props.errorMessage
    && props.articles.length === 0
);
const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));

const highlightHtml = (text) => {
    const kw = props.highlightKeyword ? props.highlightKeyword.trim() : "";
    if (!kw || !text) return text;
    const escaped = kw.replace(/[.*+?^()|\[\]\\]/g, '\\$&');
    return text.replace(new RegExp(escaped, "gi"), m => `<mark class="search-highlight">${m}</mark>`);
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
    const url = article.slug ? `/articles/${article.id}-${article.slug}` : `/articles/${article.id}`;
    router.push(url);
};

watch(
    () => props.page,
    (page) => {
        jumpPage.value = String(page);
    }
);
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
                :class="{ 'featured-post': article.featured, 'interactive-post': true }"
                role="link"
                tabindex="0"
                @click="openArticle(article)"
                @keydown.enter="openArticle(article)"
                @keydown.space.prevent="openArticle(article)"
            >
                <div class="post-cover" :aria-label="`查看文章：${article.title}`">
                    <img :src="article.cover" :alt="article.coverAlt" loading="lazy" decoding="async">
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

        <nav v-if="totalPages > 1" class="pagination-bar" aria-label="文章分页">
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

@media (max-width: 760px) {
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
}

:deep(.search-highlight) {
    background: #fef08a;
    color: #78350f;
    border-radius: 2px;
    padding: 0 1px;
    font-style: normal;
}
</style>
