<script setup>
import { computed, ref, watch } from 'vue';
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
            <div v-if="props.sortItems.length" class="sort-tabs" aria-label="排序">
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

        <article
            v-for="article in articles"
            :key="article.id"
            class="post-item"
            :class="{ 'featured-post': article.featured }"
        >
            <RouterLink class="post-cover" :to="`/articles/${article.id}`" :aria-label="`查看文章：${article.title}`">
                <img :src="article.cover" :alt="article.coverAlt">
            </RouterLink>
            <div class="post-content">
                <div class="post-meta">
                    <span>{{ article.category }}</span>
                    <span>{{ article.readingTime }}</span>
                    <span>{{ article.publishedText }}</span>
                </div>
                <h3>
                    <RouterLink :to="`/articles/${article.id}`">
                        {{ article.title }}
                    </RouterLink>
                </h3>
                <p>{{ article.summary }}</p>
                <div class="post-footer">
                    <RouterLink class="author" :to="`/users/${article.author.id}`">
                        <img :src="article.author.avatar" alt="作者头像">
                        <span>{{ article.author.name }}</span>
                    </RouterLink>
                    <span>{{ article.stats.views }}</span>
                    <span>{{ article.stats.likes }}</span>
                    <span>{{ article.stats.comments }}</span>
                </div>
            </div>
        </article>

        <div v-if="articles.length === 0" class="empty-state">
            {{ emptyText }}
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
