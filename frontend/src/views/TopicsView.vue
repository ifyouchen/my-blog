<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import {getTopicsApi} from '@/api/topic';
import {useStableListRequest} from '@/composables/useStableListRequest';

const router = useRouter();
const topics = ref([]);
const currentPage = ref(1);
const total = ref(0);
const searchInput = ref('');
const activeKeyword = ref('');
const activeDifficulty = ref('');
const pageSize = 8;
const difficultyOptions = [
    { value: '', label: '全部' },
    { value: 'BEGINNER', label: '入门' },
    { value: 'INTERMEDIATE', label: '进阶' },
    { value: 'ADVANCED', label: '深入' }
];
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    loading,
    runStableRequest
} = useStableListRequest();

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));
const paginationText = computed(() => {
    if (!total.value) {
        return '暂无专题';
    }
    const start = (currentPage.value - 1) * pageSize + 1;
    const end = Math.min(currentPage.value * pageSize, total.value);
    return `第 ${start}-${end} 个，共 ${total.value} 个专题`;
});

const pageButtons = computed(() => {
    const pages = [];
    const maxPage = totalPages.value;
    const start = Math.max(1, currentPage.value - 2);
    const end = Math.min(maxPage, currentPage.value + 2);
    for (let page = start; page <= end; page += 1) {
        pages.push(page);
    }
    return pages;
});

const fetchTopics = async (page = currentPage.value) => {
    const response = await runStableRequest(
        () => getTopicsApi({
            page,
            pageSize,
            keyword: activeKeyword.value,
            difficulty: activeDifficulty.value
        }),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '专题加载失败',
            refreshErrorMessage: '专题刷新失败，请稍后重试'
        }
    );
    if (response?.ignored || response?.error) {
        return;
    }
    const pageResult = response.result || {};
    topics.value = pageResult.items || [];
    total.value = pageResult.total || 0;
    currentPage.value = pageResult.page || page;
};

const resultText = computed(() => {
    if (activeKeyword.value || activeDifficulty.value) {
        return `匹配到 ${total.value} 个专题`;
    }
    return `共 ${total.value} 个专题`;
});

const submitSearch = async () => {
    activeKeyword.value = searchInput.value.trim();
    currentPage.value = 1;
    await fetchTopics(1);
};

const setDifficulty = async (difficulty) => {
    if (difficulty === activeDifficulty.value || loading.value) {
        return;
    }
    activeDifficulty.value = difficulty;
    currentPage.value = 1;
    await fetchTopics(1);
};

const clearSearch = async () => {
    if (!searchInput.value && !activeKeyword.value && !activeDifficulty.value) {
        return;
    }
    searchInput.value = '';
    activeKeyword.value = '';
    activeDifficulty.value = '';
    currentPage.value = 1;
    await fetchTopics(1);
};

const goToPage = async (page) => {
    const targetPage = Math.min(Math.max(page, 1), totalPages.value);
    if (targetPage === currentPage.value || loading.value) {
        return;
    }
    currentPage.value = targetPage;
    await fetchTopics(targetPage);
};

onMounted(fetchTopics);
</script>

<template>
    <SiteHeader />
    <main class="page-shell" data-testid="topics-page">
        <section class="topics-hero">
            <p class="eyebrow">专题</p>
            <h1>围绕特定主题，系统化阅读</h1>
            <p>每个专题将精选出的多篇文章组织在一起，帮助你深入理解一个领域。</p>
        </section>

        <section class="topics-search-panel" aria-label="专题检索">
            <form class="topics-search-form" @submit.prevent="submitSearch">
                <label class="topics-search-field">
                    <span>搜索专题</span>
                    <input
                        v-model="searchInput"
                        type="search"
                        placeholder="标题、导读、来源"
                    >
                </label>
                <div class="topics-search-actions">
                    <button type="submit" :disabled="loading">搜索</button>
                    <button
                        type="button"
                        :disabled="loading || (!searchInput && !activeKeyword && !activeDifficulty)"
                        @click="clearSearch"
                    >
                        清除
                    </button>
                </div>
            </form>
            <div class="topics-filter-row" aria-label="难度筛选">
                <button
                    v-for="option in difficultyOptions"
                    :key="option.value || 'all'"
                    type="button"
                    :class="{ active: activeDifficulty === option.value }"
                    :disabled="loading"
                    @click="setDifficulty(option.value)"
                >
                    {{ option.label }}
                </button>
            </div>
            <p class="topics-result-meta">
                {{ resultText }}
                <span v-if="activeKeyword">「{{ activeKeyword }}」</span>
            </p>
        </section>

        <section class="topics-grid-panel">
            <div v-if="refreshing && topics.length" class="topics-state refreshing">正在更新专题...</div>
            <div v-if="inlineError" class="topics-state error">{{ inlineError }}</div>
            <div v-if="initialLoading && !topics.length" class="topics-state">专题加载中...</div>
            <div v-else-if="errorMessage && !topics.length" class="topics-state error">{{ errorMessage }}</div>
            <div v-else-if="!refreshing && hasLoadedOnce && !topics.length" class="topics-state">
                {{ activeKeyword || activeDifficulty ? '没有匹配的专题。' : '暂时还没有可浏览的专题。' }}
            </div>
            <div v-else class="topics-grid" data-testid="topics-grid">
                <article
                    v-for="topic in topics"
                    :key="topic.id"
                    class="topic-card"
                    data-testid="topic-card"
                    role="link"
                    tabindex="0"
                    @click="router.push(`/topics/${topic.id}`)"
                    @keydown.enter="router.push(`/topics/${topic.id}`)"
                    @keydown.space.prevent="router.push(`/topics/${topic.id}`)"
                >
                    <div class="topic-card-cover" :aria-label="`查看专题：${topic.title}`">
                        <img :src="topic.coverUrl" :alt="`${topic.title} 封面`" loading="lazy" decoding="async">
                    </div>
                    <div class="topic-card-body">
                        <div class="topic-card-meta">
                            <span>{{ topic.articleCount }} 篇文章</span>
                        </div>
                        <h2>{{ topic.title }}</h2>
                        <p>{{ topic.summary }}</p>
                        <div class="topic-card-footer">
                            <span class="topic-label">专题阅读</span>
                            <span class="topic-open">查看专题</span>
                        </div>
                    </div>
                </article>
            </div>
        </section>

        <nav v-if="topics.length" class="topics-pagination" aria-label="专题分页">
            <p class="topics-page-summary">{{ paginationText }}</p>
            <div class="topics-page-actions">
                <button
                    type="button"
                    :disabled="loading || currentPage <= 1"
                    @click="goToPage(currentPage - 1)"
                >
                    上一页
                </button>
                <button
                    v-if="pageButtons[0] > 1"
                    type="button"
                    :disabled="loading"
                    @click="goToPage(1)"
                >
                    1
                </button>
                <span v-if="pageButtons[0] > 2" class="topics-page-ellipsis">...</span>
                <button
                    v-for="page in pageButtons"
                    :key="page"
                    type="button"
                    :class="{ active: page === currentPage }"
                    :disabled="loading || page === currentPage"
                    :aria-current="page === currentPage ? 'page' : undefined"
                    @click="goToPage(page)"
                >
                    {{ page }}
                </button>
                <span
                    v-if="pageButtons[pageButtons.length - 1] < totalPages - 1"
                    class="topics-page-ellipsis"
                >
                    ...
                </span>
                <button
                    v-if="pageButtons[pageButtons.length - 1] < totalPages"
                    type="button"
                    :disabled="loading"
                    @click="goToPage(totalPages)"
                >
                    {{ totalPages }}
                </button>
                <button
                    type="button"
                    :disabled="loading || currentPage >= totalPages"
                    @click="goToPage(currentPage + 1)"
                >
                    下一页
                </button>
            </div>
        </nav>
        <div v-if="inlineError && topics.length" class="topics-pagination-retry">
            <button
                type="button"
                :disabled="loading"
                @click="fetchTopics(currentPage)"
            >
                重试当前页
            </button>
        </div>
    </main>
</template>

<style scoped src="@/styles/views/TopicsView.css"></style>
