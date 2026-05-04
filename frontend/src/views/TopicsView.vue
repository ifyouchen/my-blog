<script setup>
import {onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import {getTopicsApi} from '@/api/topic';
import {useStableListRequest} from '@/composables/useStableListRequest';

const router = useRouter();
const topics = ref([]);
const currentPage = ref(1);
const total = ref(0);
const pageSize = 9;
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    loading,
    runStableRequest
} = useStableListRequest();

const fetchTopics = async () => {
    const response = await runStableRequest(
        () => getTopicsApi({ page: currentPage.value, pageSize }),
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
    currentPage.value = pageResult.page || currentPage.value;
};

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize));

const changePage = async (page) => {
    if (page < 1 || page > totalPages() || page === currentPage.value || loading.value) {
        return;
    }
    currentPage.value = page;
    await fetchTopics();
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

        <section class="topics-grid-panel">
            <div v-if="refreshing && topics.length" class="topics-state refreshing">正在更新专题...</div>
            <div v-if="inlineError" class="topics-state error">{{ inlineError }}</div>
            <div v-if="initialLoading && !topics.length" class="topics-state">专题加载中...</div>
            <div v-else-if="errorMessage && !topics.length" class="topics-state error">{{ errorMessage }}</div>
            <div v-else-if="!refreshing && hasLoadedOnce && !topics.length" class="topics-state">暂时还没有可浏览的专题。</div>
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

        <nav v-if="totalPages() > 1" class="pagination-bar" aria-label="专题分页">
            <p>第 {{ currentPage }} / {{ totalPages() }} 页，共 {{ total }} 个专题</p>
            <div class="pagination-actions">
                <button
                    type="button"
                    :disabled="currentPage <= 1 || loading"
                    @click="changePage(currentPage - 1)"
                >
                    上一页
                </button>
                <button
                    type="button"
                    :disabled="currentPage >= totalPages() || loading"
                    @click="changePage(currentPage + 1)"
                >
                    下一页
                </button>
            </div>
        </nav>
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

@media (max-width: 1080px) {
    .topics-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 720px) {
    .topics-grid {
        grid-template-columns: 1fr;
    }
}
</style>
