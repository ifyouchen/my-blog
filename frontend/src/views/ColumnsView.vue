<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import ColumnSubscribeButton from '@/components/ColumnSubscribeButton.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getColumnsApi} from '@/api/columns';
import {useStableListRequest} from '@/composables/useStableListRequest';

const router = useRouter();
const columns = ref([]);
const currentPage = ref(1);
const total = ref(0);
const searchInput = ref('');
const activeKeyword = ref('');
const pageSize = 8;
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
        return '暂无专栏';
    }
    const start = (currentPage.value - 1) * pageSize + 1;
    const end = Math.min(currentPage.value * pageSize, total.value);
    return `第 ${start}-${end} 个，共 ${total.value} 个专栏`;
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

const fetchColumns = async (page = currentPage.value) => {
    const response = await runStableRequest(
        () => getColumnsApi({ page, pageSize, keyword: activeKeyword.value }),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '专栏加载失败',
            refreshErrorMessage: '专栏刷新失败，请稍后重试'
        }
    );
    if (response?.ignored || response?.error) {
        return;
    }
    const pageResult = response.result || {};
    columns.value = pageResult.items || [];
    total.value = pageResult.total || 0;
    currentPage.value = pageResult.page || page;
};

const resultText = computed(() => {
    if (activeKeyword.value) {
        return `匹配到 ${total.value} 个专栏`;
    }
    return `共 ${total.value} 个专栏`;
});

const submitSearch = async () => {
    activeKeyword.value = searchInput.value.trim();
    currentPage.value = 1;
    await fetchColumns(1);
};

const clearSearch = async () => {
    if (!searchInput.value && !activeKeyword.value) {
        return;
    }
    searchInput.value = '';
    activeKeyword.value = '';
    currentPage.value = 1;
    await fetchColumns(1);
};

const goToPage = async (page) => {
    const targetPage = Math.min(Math.max(page, 1), totalPages.value);
    if (targetPage === currentPage.value || loading.value) {
        return;
    }
    currentPage.value = targetPage;
    await fetchColumns(targetPage);
};

const updateSubscribedState = (column, subscribed) => {
    column.subscribed = subscribed;
    column.subscriberCount = Math.max(0, (column.subscriberCount || 0) + (subscribed ? 1 : -1));
};

onMounted(fetchColumns);
</script>

<template>
    <SiteHeader />
    <main class="page-shell" data-testid="columns-page">
        <section class="columns-hero">
            <p class="eyebrow">专栏</p>
            <h1>按主题连续阅读，而不是零散跳转</h1>
            <p>把一组值得系统看下去的文章串成完整阅读路径，读起来会更顺。</p>
        </section>

        <section class="columns-search-panel" aria-label="专栏检索">
            <form class="columns-search-form" @submit.prevent="submitSearch">
                <label class="columns-search-field">
                    <span>搜索专栏</span>
                    <input
                        v-model="searchInput"
                        type="search"
                        placeholder="标题、导读、来源"
                    >
                </label>
                <div class="columns-search-actions">
                    <button type="submit" :disabled="loading">搜索</button>
                    <button type="button" :disabled="loading || (!searchInput && !activeKeyword)" @click="clearSearch">
                        清除
                    </button>
                </div>
            </form>
            <p class="columns-result-meta">
                {{ resultText }}
                <span v-if="activeKeyword">「{{ activeKeyword }}」</span>
            </p>
        </section>

        <section class="columns-grid-panel">
            <div v-if="refreshing && columns.length" class="columns-state refreshing">正在更新专栏...</div>
            <div v-if="inlineError" class="columns-state error">{{ inlineError }}</div>
            <div v-if="initialLoading && !columns.length" class="columns-state">专栏加载中...</div>
            <div v-else-if="errorMessage && !columns.length" class="columns-state error">{{ errorMessage }}</div>
            <div v-else-if="!refreshing && hasLoadedOnce && !columns.length" class="columns-state">
                {{ activeKeyword ? '没有匹配的专栏。' : '暂时还没有可浏览的专栏。' }}
            </div>
            <div v-else class="columns-grid" data-testid="columns-grid">
                <article
                    v-for="column in columns"
                    :key="column.id"
                    class="column-card"
                    data-testid="column-card"
                    role="link"
                    tabindex="0"
                    @click="router.push(`/columns/${column.id}`)"
                    @keydown.enter="router.push(`/columns/${column.id}`)"
                    @keydown.space.prevent="router.push(`/columns/${column.id}`)"
                >
                    <div class="column-card-cover" :aria-label="`查看专栏：${column.title}`">
                        <img :src="column.coverUrl" :alt="`${column.title} 封面`" loading="lazy" decoding="async">
                    </div>
                    <div class="column-card-body">
                        <div class="column-card-meta">
                            <span>{{ column.articleCount }} 篇文章</span>
                            <span>{{ column.subscriberCount }} 人订阅</span>
                        </div>
                        <h2>
                            {{ column.title }}
                        </h2>
                        <p>{{ column.summary }}</p>
                        <div class="column-card-footer">
                            <RouterLink
                                class="column-author"
                                :to="`/users/${column.author.id}`"
                                @click.stop
                                @keydown.enter.stop
                                @keydown.space.stop
                            >
                                <img :src="column.author.avatar" alt="作者头像" loading="lazy" decoding="async">
                                <span>{{ column.author.name }}</span>
                            </RouterLink>
                            <ColumnSubscribeButton
                                :column-id="column.id"
                                :subscribed="column.subscribed"
                                compact
                                @click.stop
                                @keydown.enter.stop
                                @keydown.space.stop
                                @change="(subscribed) => updateSubscribedState(column, subscribed)"
                            />
                        </div>
                    </div>
                </article>
            </div>
        </section>

        <nav v-if="columns.length" class="columns-pagination" aria-label="专栏分页">
            <p class="columns-page-summary">{{ paginationText }}</p>
            <div class="columns-page-actions">
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
                <span v-if="pageButtons[0] > 2" class="columns-page-ellipsis">...</span>
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
                    class="columns-page-ellipsis"
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
        <div v-if="inlineError && columns.length" class="columns-pagination-retry">
            <button
                type="button"
                :disabled="loading"
                @click="fetchColumns(currentPage)"
            >
                重试当前页
            </button>
        </div>
    </main>
</template>

<style scoped src="@/styles/views/ColumnsView.css"></style>
