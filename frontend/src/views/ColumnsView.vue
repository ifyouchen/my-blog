<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
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
const pageSize = 12;
const loadingMore = ref(false);
const loadMoreError = ref('');
const loadMoreTrigger = ref(null);
const loadMoreTriggerVisible = ref(false);
let loadMoreObserver = null;
let loadMoreSeq = 0;
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    loading,
    runStableRequest
} = useStableListRequest();

const hasMore = computed(() => columns.value.length < total.value);

const mergeUniqueItems = (currentItems, nextItems) => {
    const seen = new Set(currentItems.map((item) => String(item.id)));
    return [
        ...currentItems,
        ...nextItems.filter((item) => {
            const key = String(item.id);
            if (seen.has(key)) {
                return false;
            }
            seen.add(key);
            return true;
        })
    ];
};

const fetchColumns = async () => {
    loadMoreSeq += 1;
    loadMoreError.value = '';
    const response = await runStableRequest(
        () => getColumnsApi({ page: 1, pageSize, keyword: activeKeyword.value }),
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
    currentPage.value = pageResult.page || 1;
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
    await fetchColumns();
};

const clearSearch = async () => {
    if (!searchInput.value && !activeKeyword.value) {
        return;
    }
    searchInput.value = '';
    activeKeyword.value = '';
    currentPage.value = 1;
    await fetchColumns();
};

const loadMoreColumns = async () => {
    if (loading.value || loadingMore.value || !hasMore.value) {
        return;
    }
    const nextPage = currentPage.value + 1;
    const seq = loadMoreSeq + 1;
    loadMoreSeq = seq;
    loadingMore.value = true;
    loadMoreError.value = '';

    try {
        const pageResult = await getColumnsApi({ page: nextPage, pageSize, keyword: activeKeyword.value });
        if (seq !== loadMoreSeq) {
            return;
        }
        columns.value = mergeUniqueItems(columns.value, pageResult.items || []);
        total.value = pageResult.total || total.value;
        currentPage.value = pageResult.page || nextPage;
    } catch (error) {
        if (seq !== loadMoreSeq) {
            return;
        }
        loadMoreError.value = error?.message || '专栏加载失败，请稍后重试';
    } finally {
        if (seq === loadMoreSeq) {
            loadingMore.value = false;
        }
    }
};

const updateSubscribedState = (column, subscribed) => {
    column.subscribed = subscribed;
    column.subscriberCount = Math.max(0, (column.subscriberCount || 0) + (subscribed ? 1 : -1));
};

const requestLoadMoreIfVisible = () => {
    if (!loadMoreTriggerVisible.value || loadMoreError.value) {
        return;
    }
    loadMoreColumns();
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
    if (typeof IntersectionObserver === 'undefined' || !loadMoreTrigger.value) {
        return;
    }
    loadMoreObserver = new IntersectionObserver((entries) => {
        loadMoreTriggerVisible.value = entries.some((entry) => entry.isIntersecting);
        requestLoadMoreIfVisible();
    }, {
        rootMargin: '320px 0px',
        threshold: 0
    });
    loadMoreObserver.observe(loadMoreTrigger.value);
};

watch(() => loadMoreTrigger.value, setupLoadMoreObserver, { flush: 'post' });
watch(
    () => [columns.value.length, hasMore.value, loading.value, loadingMore.value, loadMoreError.value],
    requestLoadMoreIfVisible,
    { flush: 'post' }
);

onMounted(fetchColumns);
onBeforeUnmount(teardownLoadMoreObserver);
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

        <div v-if="columns.length" class="infinite-list-footer" aria-live="polite">
            <span ref="loadMoreTrigger" class="infinite-load-trigger" aria-hidden="true"></span>
            <p v-if="loadingMore" class="infinite-load-status">继续加载专栏...</p>
            <button
                v-else-if="loadMoreError"
                type="button"
                class="load-more-button error"
                @click="loadMoreColumns"
            >
                {{ loadMoreError }}，点击重试
            </button>
            <button
                v-else-if="hasMore"
                type="button"
                class="load-more-button"
                :disabled="loading"
                @click="loadMoreColumns"
            >
                加载更多专栏
            </button>
            <p v-else class="infinite-load-status">已显示全部 {{ total }} 个专栏</p>
        </div>
    </main>
</template>

<style scoped src="@/styles/views/ColumnsView.css"></style>
