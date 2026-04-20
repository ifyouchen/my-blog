<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { listArticlesApi } from '@/api/articles';
import ArticleFeed from '@/components/ArticleFeed.vue';
import HomeIntro from '@/components/HomeIntro.vue';
import HomeSidebar from '@/components/HomeSidebar.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import TopicStrip from '@/components/TopicStrip.vue';
import { articles as fallbackArticles } from '@/data/home';

const articles = ref(fallbackArticles);
const currentPage = ref(1);
const pageSize = 10;
const total = ref(fallbackArticles.length);
const loading = ref(false);
const errorMessage = ref('');
const route = useRoute();
const router = useRouter();
let requestSeq = 0;
let firstLoad = true;

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const normalizePage = (value) => {
    const page = Number.parseInt(value, 10);
    return Number.isNaN(page) || page < 1 ? 1 : page;
};

const scrollToFeed = async () => {
    await nextTick();
    document.querySelector('[data-feed-root]')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
    });
};

const loadArticles = async (page, shouldScroll = false) => {
    const seq = requestSeq + 1;
    requestSeq = seq;
    loading.value = true;
    errorMessage.value = '';

    try {
        const pageResult = await listArticlesApi({ page, pageSize });
        if (seq !== requestSeq) {
            return;
        }

        const nextTotal = Number(pageResult.total || 0);
        const lastPage = Math.max(1, Math.ceil(nextTotal / pageSize));
        if (nextTotal > 0 && page > lastPage) {
            await router.replace({
                query: {
                    ...route.query,
                    page: lastPage === 1 ? undefined : String(lastPage)
                }
            });
            return;
        }

        currentPage.value = page;
        total.value = nextTotal;
        articles.value = pageResult.items;
    } catch (error) {
        errorMessage.value = '文章列表加载失败，已显示本地示例内容';
        articles.value = fallbackArticles;
        total.value = fallbackArticles.length;
        currentPage.value = 1;
    } finally {
        if (seq === requestSeq) {
            loading.value = false;
            if (shouldScroll) {
                await scrollToFeed();
            }
        }
    }
};

const changePage = async (page) => {
    const targetPage = Math.min(Math.max(1, page), totalPages.value);
    if (loading.value || targetPage === currentPage.value) {
        return;
    }
    await router.push({
        query: {
            ...route.query,
            page: targetPage === 1 ? undefined : String(targetPage)
        }
    });
};

watch(
    () => route.query.page,
    (page) => {
        const shouldScroll = !firstLoad;
        firstLoad = false;
        loadArticles(normalizePage(page), shouldScroll);
    },
    { immediate: true }
);
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <HomeIntro />
        <TopicStrip />
        <div class="content-grid">
            <ArticleFeed
                :articles="articles"
                :page="currentPage"
                :page-size="pageSize"
                :total="total"
                :loading="loading"
                :error-message="errorMessage"
                @page-change="changePage"
            />
            <HomeSidebar />
        </div>
    </main>
</template>
