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

<style scoped>
.topics-hero {
    display: grid;
    gap: 8px;
}

.topics-hero h1,
.topics-hero p {
    margin: 0;
}

.topics-grid-panel {
    margin-top: 24px;
}

.topics-search-panel {
    display: grid;
    gap: 10px;
    margin-top: 22px;
    padding: 14px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.topics-search-form {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 12px;
    align-items: end;
}

.topics-search-field {
    display: grid;
    gap: 6px;
    min-width: 0;
    color: var(--muted);
    font-size: 13px;
}

.topics-search-field input {
    width: 100%;
    height: 42px;
    padding: 0 12px;
    color: var(--text);
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    outline: none;
}

.topics-search-field input:focus {
    border-color: var(--brand);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.topics-search-actions,
.topics-filter-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.topics-search-actions button,
.topics-filter-row button {
    height: 42px;
    padding: 0 14px;
    border-radius: var(--radius-sm);
    border: 1px solid var(--line);
    background: var(--surface-soft);
    color: var(--text);
    cursor: pointer;
}

.topics-search-actions button[type="submit"],
.topics-filter-row button.active {
    background: var(--brand);
    border-color: var(--brand);
    color: #fff;
}

.topics-search-actions button:disabled,
.topics-filter-row button:disabled {
    cursor: not-allowed;
    opacity: 0.55;
}

.topics-result-meta {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
}

.topics-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 18px;
}

.topic-card {
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: hidden;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    box-shadow: none;
    cursor: pointer;
    transition: background 0.12s, border-color 0.12s;
}

.topic-card:hover,
.topic-card:focus-visible {
    background: var(--surface-soft);
    border-color: var(--line-strong);
}

.topic-card:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: 2px;
}

.topic-card-cover {
    display: block;
    width: 100%;
}

.topic-card-cover img {
    width: 100%;
    aspect-ratio: 16 / 9;
    object-fit: cover;
    display: block;
    transition: transform 0.22s ease;
}

.topic-card:hover .topic-card-cover img,
.topic-card:focus-visible .topic-card-cover img {
    transform: scale(1.04);
}

.topic-card-body {
    display: grid;
    grid-template-rows: auto auto minmax(0, 1fr) auto;
    gap: 12px;
    flex: 1;
    padding: 18px;
}

.topic-card-meta {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    color: var(--muted);
    font-size: 12px;
}

.topic-card h2,
.topic-card p {
    margin: 0;
}

.topic-card h2 {
    color: var(--text);
    line-height: 1.45;
    display: -webkit-box;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    transition: color 0.18s ease;
}

.topic-card p {
    color: var(--muted);
    line-height: 1.7;
    display: -webkit-box;
    min-height: calc(1.7em * 2);
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
}

.topic-card-footer {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
    color: var(--muted);
    font-size: 13px;
}

.topic-label {
    color: var(--text);
    font-weight: 600;
}

.topic-open {
    color: var(--brand);
    font-weight: 700;
}

.topic-card:hover h2,
.topic-card:focus-visible h2,
.topic-card:hover .topic-label,
.topic-card:focus-visible .topic-label {
    color: var(--brand-strong);
}

.topics-state {
    color: var(--muted);
}

.topics-state.refreshing {
    margin-bottom: 12px;
    padding: 8px 12px;
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
    background: var(--brand-soft);
    border: 1px solid rgba(37, 99, 235, 0.14);
    border-radius: var(--radius-sm);
}

.topics-state.error {
    color: var(--error);
}

.infinite-list-footer {
    display: grid;
    justify-items: center;
    gap: 10px;
    margin-top: 22px;
}

.infinite-load-trigger {
    width: 1px;
    height: 1px;
}

.infinite-load-status {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
}

.load-more-button {
    min-height: 38px;
    padding: 0 16px;
    color: var(--text);
    cursor: pointer;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.load-more-button:hover:not(:disabled),
.load-more-button:focus-visible {
    color: var(--brand-strong);
    border-color: var(--brand);
}

.load-more-button:disabled {
    cursor: not-allowed;
    opacity: 0.55;
}

.load-more-button.error {
    color: var(--error);
    border-color: rgba(220, 38, 38, 0.35);
}

@media (max-width: 1080px) {
    .topics-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 720px) {
    .topics-search-form {
        grid-template-columns: 1fr;
    }

    .topics-search-actions {
        display: grid;
        grid-template-columns: 1fr 1fr;
    }

    .topics-grid {
        grid-template-columns: 1fr;
    }
}
</style>
