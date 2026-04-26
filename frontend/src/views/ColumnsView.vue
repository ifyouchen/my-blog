<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import ColumnSubscribeButton from '@/components/ColumnSubscribeButton.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { getColumnsApi } from '@/api/columns';

const router = useRouter();
const columns = ref([]);
const loading = ref(false);
const errorMessage = ref('');
const currentPage = ref(1);
const total = ref(0);
const pageSize = 9;

const fetchColumns = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const pageResult = await getColumnsApi({ page: currentPage.value, pageSize });
        columns.value = pageResult.items || [];
        total.value = pageResult.total || 0;
        currentPage.value = pageResult.page || currentPage.value;
    } catch (error) {
        columns.value = [];
        total.value = 0;
        errorMessage.value = error.message || '专栏加载失败';
    } finally {
        loading.value = false;
    }
};

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize));

const changePage = async (page) => {
    if (page < 1 || page > totalPages() || page === currentPage.value || loading.value) {
        return;
    }
    currentPage.value = page;
    await fetchColumns();
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

        <section class="columns-grid-panel">
            <div v-if="loading" class="columns-state">专栏加载中...</div>
            <div v-else-if="errorMessage" class="columns-state error">{{ errorMessage }}</div>
            <div v-else-if="!columns.length" class="columns-state">暂时还没有可浏览的专栏。</div>
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
                        <img :src="column.coverUrl" :alt="`${column.title} 封面`" loading="lazy">
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
                                <img :src="column.author.avatar" alt="作者头像" loading="lazy">
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

        <nav v-if="totalPages() > 1" class="pagination-bar" aria-label="专栏分页">
            <p>第 {{ currentPage }} / {{ totalPages() }} 页，共 {{ total }} 个专栏</p>
            <div class="pagination-actions">
                <button type="button" :disabled="currentPage <= 1 || loading" @click="changePage(currentPage - 1)">上一页</button>
                <button type="button" :disabled="currentPage >= totalPages() || loading" @click="changePage(currentPage + 1)">下一页</button>
            </div>
        </nav>
    </main>
</template>

<style scoped>
.columns-hero {
    display: grid;
    gap: 8px;
}

.columns-hero h1,
.columns-hero p {
    margin: 0;
}

.columns-grid-panel {
    margin-top: 24px;
}

.columns-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 18px;
}

.column-card {
    overflow: hidden;
    background: #ffffff;
    border: 1px solid rgba(208, 219, 236, 0.92);
    border-radius: 22px;
    box-shadow: 0 18px 38px rgba(31, 78, 168, 0.07);
    cursor: pointer;
    transition: transform 0.18s ease, border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease;
}

.column-card:hover,
.column-card:focus-visible {
    background: color-mix(in srgb, var(--surface-soft) 34%, white);
    border-color: rgba(40, 118, 255, 0.18);
    box-shadow: 0 22px 40px rgba(31, 78, 168, 0.11);
    transform: translateY(-2px);
}

.column-card:focus-visible {
    outline: 2px solid rgba(40, 118, 255, 0.24);
    outline-offset: 4px;
}

.column-card-cover {
    display: block;
    width: 100%;
}

.column-card-cover img {
    width: 100%;
    aspect-ratio: 16 / 9;
    object-fit: cover;
    display: block;
    transition: transform 0.22s ease;
}

.column-card:hover .column-card-cover img,
.column-card:focus-visible .column-card-cover img {
    transform: scale(1.04);
}

.column-card-body {
    display: grid;
    gap: 12px;
    padding: 18px;
}

.column-card-meta {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    color: var(--muted);
    font-size: 12px;
}

.column-card h2,
.column-card p {
    margin: 0;
}

.column-card h2 {
    color: var(--text);
    transition: color 0.18s ease;
}

.column-card p {
    color: var(--muted);
    line-height: 1.7;
}

.column-card-footer {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
}

.column-author {
    display: inline-flex;
    gap: 10px;
    align-items: center;
    color: var(--text);
    text-decoration: none;
    position: relative;
    z-index: 1;
    transition: color 0.18s ease;
}

.column-author img {
    width: 32px;
    height: 32px;
    border-radius: 12px;
    object-fit: cover;
}

.column-card:hover h2,
.column-card:focus-visible h2,
.column-card:hover .column-author,
.column-card:focus-visible .column-author,
.column-author:hover,
.column-author:focus-visible {
    color: var(--brand-strong);
}

.columns-state {
    color: var(--muted);
}

@media (max-width: 1080px) {
    .columns-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 720px) {
    .columns-grid {
        grid-template-columns: 1fr;
    }
}
</style>
