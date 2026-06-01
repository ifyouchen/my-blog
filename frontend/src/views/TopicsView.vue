<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
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
const pageSize = 12;
const loadingMore = ref(false);
const loadMoreError = ref('');
const loadMoreTrigger = ref(null);
const loadMoreTriggerVisible = ref(false);
let loadMoreObserver = null;
let loadMoreSeq = 0;
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

const hasMore = computed(() => topics.value.length < total.value);

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

const fetchTopics = async () => {
    loadMoreSeq += 1;
    loadMoreError.value = '';
    const response = await runStableRequest(
        () => getTopicsApi({
            page: 1,
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
    currentPage.value = pageResult.page || 1;
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
    await fetchTopics();
};

const setDifficulty = async (difficulty) => {
    if (difficulty === activeDifficulty.value || loading.value) {
        return;
    }
    activeDifficulty.value = difficulty;
    currentPage.value = 1;
    await fetchTopics();
};

const clearSearch = async () => {
    if (!searchInput.value && !activeKeyword.value && !activeDifficulty.value) {
        return;
    }
    searchInput.value = '';
    activeKeyword.value = '';
    activeDifficulty.value = '';
    currentPage.value = 1;
    await fetchTopics();
};

const loadMoreTopics = async () => {
    if (loading.value || loadingMore.value || !hasMore.value) {
        return;
    }
    const nextPage = currentPage.value + 1;
    const seq = loadMoreSeq + 1;
    loadMoreSeq = seq;
    loadingMore.value = true;
    loadMoreError.value = '';

    try {
        const pageResult = await getTopicsApi({
            page: nextPage,
            pageSize,
            keyword: activeKeyword.value,
            difficulty: activeDifficulty.value
        });
        if (seq !== loadMoreSeq) {
            return;
        }
        topics.value = mergeUniqueItems(topics.value, pageResult.items || []);
        total.value = pageResult.total || total.value;
        currentPage.value = pageResult.page || nextPage;
    } catch (error) {
        if (seq !== loadMoreSeq) {
            return;
        }
        loadMoreError.value = error?.message || '专题加载失败，请稍后重试';
    } finally {
        if (seq === loadMoreSeq) {
            loadingMore.value = false;
        }
    }
};

const requestLoadMoreIfVisible = () => {
    if (!loadMoreTriggerVisible.value || loadMoreError.value) {
        return;
    }
    loadMoreTopics();
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
    () => [topics.value.length, hasMore.value, loading.value, loadingMore.value, loadMoreError.value],
    requestLoadMoreIfVisible,
    { flush: 'post' }
);

onMounted(fetchTopics);
onBeforeUnmount(teardownLoadMoreObserver);
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

        <div v-if="topics.length" class="infinite-list-footer" aria-live="polite">
            <span ref="loadMoreTrigger" class="infinite-load-trigger" aria-hidden="true"></span>
            <p v-if="loadingMore" class="infinite-load-status">继续加载专题...</p>
            <button
                v-else-if="loadMoreError"
                type="button"
                class="load-more-button error"
                @click="loadMoreTopics"
            >
                {{ loadMoreError }}，点击重试
            </button>
            <button
                v-else-if="hasMore"
                type="button"
                class="load-more-button"
                :disabled="loading"
                @click="loadMoreTopics"
            >
                加载更多专题
            </button>
            <p v-else class="infinite-load-status">已显示全部 {{ total }} 个专题</p>
        </div>
    </main>
</template>

<style scoped src="@/styles/views/TopicsView.css"></style>
