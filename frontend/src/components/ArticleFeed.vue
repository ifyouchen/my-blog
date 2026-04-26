<script setup>
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { articles as defaultArticles } from '@/data/home';
import { ARTICLE_SORT_LATEST } from '@/constants/articleSort';

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
    errorMessage: {
        type: String,
        default: ''
    },
    sort: {
        type: String,
        default: ARTICLE_SORT_LATEST
    },
    sortItems: {
        type: Array,
        default: () => []
    }
});

const emit = defineEmits(['page-change', 'sort-change']);
const router = useRouter();
const jumpPage = ref(String(props.page));

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));
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
    if (props.loading || page < 1 || page > totalPages.value || page === props.page) {
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

const openArticle = (articleId) => {
    router.push(`/articles/${articleId}`);
};

watch(
    () => props.page,
    (page) => {
        jumpPage.value = String(page);
    }
);
</script>

<template>
    <section class="feed" :class="{ loading }" aria-labelledby="feed-title" data-feed-root>
        <div class="section-heading">
            <div>
                <p class="eyebrow">{{ eyebrow }}</p>
                <h2 id="feed-title">{{ title }}</h2>
            </div>
            <div class="sort-tabs" aria-label="排序" :class="{ invisible: !props.sortItems.length }">
                <button
                    v-for="item in props.sortItems"
                    :key="item.value"
                    type="button"
                    :class="{ active: props.sort === item.value }"
                    :disabled="loading || props.sort === item.value"
                    @click="emit('sort-change', item.value)"
                >
                    {{ item.label }}
                </button>
            </div>
        </div>

        <template v-if="!loading && articles.length === 0" class="empty-state">
            <div class="empty-state">{{ emptyText }}</div>
        </template>

        <template v-else-if="!loading">
            <article
                v-for="article in articles"
                :key="article.id"
                class="post-item"
                :class="{ 'featured-post': article.featured, 'interactive-post': true }"
                role="link"
                tabindex="0"
                @click="openArticle(article.id)"
                @keydown.enter="openArticle(article.id)"
                @keydown.space.prevent="openArticle(article.id)"
            >
                <div class="post-cover" :aria-label="`查看文章：${article.title}`">
                    <img :src="article.cover" :alt="article.coverAlt" loading="lazy">
                </div>
                <div class="post-content">
                    <div class="post-meta">
                        <span>{{ article.category }}</span>
                        <span>{{ article.readingTime }}</span>
                        <span>{{ article.publishedText }}</span>
                    </div>
                    <h3 class="post-title">{{ article.title }}</h3>
                    <p>{{ article.summary }}</p>
                    <div class="post-footer">
                        <RouterLink
                            class="author author-hotspot"
                            :to="`/users/${article.author.id}`"
                            @click.stop
                            @keydown.enter.stop
                            @keydown.space.stop
                        >
                            <img :src="article.author.avatar" alt="作者头像">
                            <span>{{ article.author.name }}</span>
                        </RouterLink>
                        <span>{{ article.stats.views }}</span>
                        <span>{{ article.stats.likes }}</span>
                        <span>{{ article.stats.comments }}</span>
                    </div>
                </div>
            </article>
        </template>

        <div v-if="loading" class="loading-placeholder">
            <div v-for="i in 3" :key="i" class="skeleton-post">
                <div class="skeleton-cover"></div>
                <div class="skeleton-content">
                    <div class="skeleton-title"></div>
                    <div class="skeleton-text"></div>
                    <div class="skeleton-text short"></div>
                </div>
            </div>
        </div>

        <p v-if="errorMessage" class="feed-message">{{ errorMessage }}</p>

        <nav v-if="totalPages > 1" class="pagination-bar" aria-label="文章分页">
            <p>
                第 {{ page }} / {{ totalPages }} 页，
                共 {{ total }} 篇，当前 {{ pageStart }}-{{ pageEnd }} 篇
            </p>
            <div class="pagination-actions">
                <button type="button" :disabled="loading || page <= 1" @click="goToPage(1)">首页</button>
                <button type="button" :disabled="loading || page <= 1" @click="goToPage(page - 1)">上一页</button>
                <template v-for="item in paginationItems" :key="`${item.type}-${item.value}`">
                    <span v-if="item.type === 'ellipsis'" class="pagination-ellipsis">...</span>
                    <button
                        v-else
                        type="button"
                        :class="{ active: item.value === page }"
                        :disabled="loading || item.value === page"
                        @click="goToPage(item.value)"
                    >
                        {{ item.value }}
                    </button>
                </template>
                <button type="button" :disabled="loading || page >= totalPages" @click="goToPage(page + 1)">下一页</button>
                <button type="button" :disabled="loading || page >= totalPages" @click="goToPage(totalPages)">末页</button>
            </div>
            <form class="pagination-jump" @submit.prevent="submitJump">
                <label for="page-jump-input">跳至</label>
                <input
                    id="page-jump-input"
                    v-model="jumpPage"
                    type="number"
                    min="1"
                    :max="totalPages"
                    :disabled="loading"
                    inputmode="numeric"
                    aria-label="输入页码"
                >
                <span>页</span>
                <button type="submit" :disabled="loading">跳转</button>
            </form>
        </nav>
    </section>
</template>

<style scoped>
.invisible {
    visibility: hidden;
}

.interactive-post {
    cursor: pointer;
    transition: transform 0.18s ease, border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease;
}

.interactive-post:hover,
.interactive-post:focus-visible {
    background: color-mix(in srgb, var(--surface-soft) 36%, white);
    border-color: rgba(31, 122, 224, 0.16);
    box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
    transform: translateY(-2px);
}

.interactive-post:focus-visible {
    outline: 2px solid rgba(31, 122, 224, 0.2);
    outline-offset: 4px;
}

.post-title {
    transition: color 0.18s ease;
}

.interactive-post:hover .post-title,
.interactive-post:focus-visible .post-title,
.interactive-post:hover .author-hotspot,
.interactive-post:focus-visible .author-hotspot {
    color: var(--brand-strong);
}

.interactive-post:hover .post-cover img,
.interactive-post:focus-visible .post-cover img {
    transform: scale(1.04);
}

.post-cover img {
    transition: transform 0.22s ease;
}

.author-hotspot {
    position: relative;
    z-index: 1;
    border-radius: 999px;
    transition: color 0.18s ease;
}

.author-hotspot:hover,
.author-hotspot:focus-visible {
    color: var(--brand-strong);
}

/* Loading skeleton */
.loading-placeholder {
    display: flex;
    flex-direction: column;
    gap: 18px;
}

.skeleton-post {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 236px;
    gap: 22px;
    align-items: center;
    padding: 20px;
    background: linear-gradient(180deg, rgba(248, 251, 255, 0.98), #ffffff);
    border: 1px solid rgba(208, 219, 236, 0.9);
    border-radius: 22px;
    box-shadow: 0 18px 36px rgba(31, 78, 168, 0.05);
}

.skeleton-cover {
    grid-column: 2;
    width: 100%;
    height: 148px;
    border-radius: 18px;
    background: linear-gradient(90deg, rgba(226, 232, 240, 0.86) 25%, rgba(241, 245, 249, 0.96) 50%, rgba(226, 232, 240, 0.86) 75%);
    background-size: 200% 100%;
    animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

.skeleton-content {
    grid-column: 1;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.skeleton-title {
    height: 26px;
    width: 72%;
    border-radius: 999px;
    background: linear-gradient(90deg, rgba(226, 232, 240, 0.86) 25%, rgba(241, 245, 249, 0.96) 50%, rgba(226, 232, 240, 0.86) 75%);
    background-size: 200% 100%;
    animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

.skeleton-text {
    height: 14px;
    width: 100%;
    border-radius: 999px;
    background: linear-gradient(90deg, rgba(226, 232, 240, 0.86) 25%, rgba(241, 245, 249, 0.96) 50%, rgba(226, 232, 240, 0.86) 75%);
    background-size: 200% 100%;
    animation: skeleton-shimmer 1.4s ease-in-out infinite;
}

.skeleton-text.short {
    width: 40%;
}

:deep(.empty-state) {
    padding: 34px 28px;
    color: var(--muted);
    text-align: center;
    background: linear-gradient(180deg, rgba(248, 251, 255, 0.96), #ffffff);
    border: 1px solid rgba(208, 219, 236, 0.9);
    border-radius: 22px;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

@keyframes skeleton-shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}

@media (max-width: 760px) {
    .skeleton-post {
        grid-template-columns: 1fr;
        gap: 16px;
    }

    .skeleton-cover,
    .skeleton-content {
        grid-column: auto;
    }

    .skeleton-cover {
        height: 180px;
    }
}
</style>
