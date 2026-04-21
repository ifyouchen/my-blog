<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { listArticlesApi } from '@/api/articles';
import { getCategoriesApi, getTagsApi } from '@/api/admin';
import ArticleFeed from '@/components/ArticleFeed.vue';
import {
    ARTICLE_SORT_ITEMS,
    ARTICLE_SORT_LATEST,
    isDefaultArticleSort,
    normalizeArticleSort
} from '@/constants/articleSort';
import SiteHeader from '@/components/SiteHeader.vue';

const route = useRoute();
const router = useRouter();

const keyword = ref(String(route.query.keyword || ''));
const activeCategory = ref(String(route.query.category || ''));
const activeTag = ref(String(route.query.tag || ''));
const activeSort = ref(String(route.query.sort || ARTICLE_SORT_LATEST));
const currentPage = ref(Number.parseInt(route.query.page || '1', 10) || 1);
const pageSize = 10;
const articles = ref([]);
const total = ref(0);
const loading = ref(false);
const errorMessage = ref('');
const categories = ref([]);
const tags = ref([]);

const categoryOptions = computed(() => ['全部', ...categories.value.map((item) => item.name)]);
const tagOptions = computed(() => ['全部', ...tags.value.map((item) => item.name)]);

const syncRoute = (overrides = {}) => {
    const nextSort = overrides.sort ?? activeSort.value;
    router.replace({
        path: '/search',
        query: {
            keyword: overrides.keyword ?? (keyword.value.trim() || undefined),
            category: overrides.category ?? (activeCategory.value || undefined),
            tag: overrides.tag ?? (activeTag.value || undefined),
            sort: isDefaultArticleSort(nextSort) ? undefined : nextSort,
            page: String(overrides.page ?? currentPage.value) === '1' ? undefined : String(overrides.page ?? currentPage.value)
        }
    });
};

const fetchFilters = async () => {
    try {
        const [categoryList, tagList] = await Promise.all([
            getCategoriesApi(true),
            getTagsApi(true)
        ]);
        categories.value = categoryList || [];
        tags.value = tagList || [];
    } catch (error) {
        categories.value = [];
        tags.value = [];
    }
};

const fetchArticles = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const pageResult = await listArticlesApi({
            keyword: keyword.value.trim(),
            category: activeCategory.value,
            tag: activeTag.value,
            sort: activeSort.value,
            page: currentPage.value,
            pageSize
        });
        articles.value = pageResult.items || [];
        total.value = pageResult.total || 0;
    } catch (error) {
        articles.value = [];
        total.value = 0;
        errorMessage.value = error.message || '搜索失败，请稍后重试';
    } finally {
        loading.value = false;
    }
};

const runSearch = () => {
    currentPage.value = 1;
    syncRoute({ page: 1, keyword: keyword.value.trim() || undefined });
};

const changeCategory = (value) => {
    activeCategory.value = value === '全部' ? '' : value;
    currentPage.value = 1;
    syncRoute({ page: 1, category: activeCategory.value || undefined });
};

const changeTag = (value) => {
    activeTag.value = value === '全部' ? '' : value;
    currentPage.value = 1;
    syncRoute({ page: 1, tag: activeTag.value || undefined });
};

const changePage = (page) => {
    currentPage.value = page;
    syncRoute({ page });
};

const changeSort = (sort) => {
    activeSort.value = normalizeArticleSort(sort);
    currentPage.value = 1;
    syncRoute({ page: 1, sort: activeSort.value });
};

watch(
    () => route.query,
    (query) => {
        keyword.value = String(query.keyword || '');
        activeCategory.value = String(query.category || '');
        activeTag.value = String(query.tag || '');
        activeSort.value = normalizeArticleSort(String(query.sort || ARTICLE_SORT_LATEST));
        currentPage.value = Number.parseInt(query.page || '1', 10) || 1;
        fetchArticles();
    },
    { immediate: true }
);

onMounted(fetchFilters);
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <section class="search-panel">
            <p class="eyebrow">搜索文章</p>
            <h1>找到能解决问题的文章</h1>
            <form class="search-large" @submit.prevent="runSearch">
                <input v-model="keyword" type="search" aria-label="搜索关键字">
                <button type="submit">搜索</button>
            </form>

            <div class="search-filters">
                <div class="filter-group">
                    <span>分类</span>
                    <div class="tag-row">
                        <button
                            v-for="category in categoryOptions"
                            :key="category"
                            type="button"
                            :class="{ active: (activeCategory || '全部') === category }"
                            @click="changeCategory(category)"
                        >
                            {{ category }}
                        </button>
                    </div>
                </div>
                <div class="filter-group">
                    <span>标签</span>
                    <div class="tag-row">
                        <button
                            v-for="tag in tagOptions"
                            :key="tag"
                            type="button"
                            :class="{ active: (activeTag || '全部') === tag }"
                            @click="changeTag(tag)"
                        >
                            {{ tag }}
                        </button>
                    </div>
                </div>
            </div>

            <p class="result-note">共找到 {{ total }} 篇文章</p>
        </section>
        <ArticleFeed
            :articles="articles"
            :page="currentPage"
            :page-size="pageSize"
            :total="total"
            :loading="loading"
            :error-message="errorMessage"
            :sort="activeSort"
            :sort-items="ARTICLE_SORT_ITEMS"
            eyebrow="搜索结果"
            title="匹配文章"
            empty-text="换个关键词、分类或标签试试"
            @page-change="changePage"
            @sort-change="changeSort"
        />
    </main>
</template>

<style scoped>
.search-filters {
    display: grid;
    gap: 18px;
    margin-top: 20px;
}

.filter-group {
    display: grid;
    gap: 8px;
}

.filter-group > span {
    font-size: 13px;
    color: var(--muted);
}
</style>
