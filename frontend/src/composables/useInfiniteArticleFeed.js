import { computed, ref } from 'vue';

const CACHE_PREFIX = 'my-blog:infinite-article-feed:';
const CACHE_TTL = 30 * 60 * 1000;

export function useInfiniteArticleFeed(options = {}) {
    const pageSize = Number(options.pageSize || 10);
    const getItemKey = options.getItemKey || ((item) => item?.id);

    const articles = ref([]);
    const currentPage = ref(1);
    const total = ref(0);
    const loadingMore = ref(false);
    const loadMoreError = ref('');
    let loadMoreSeq = 0;

    const hasMore = computed(() => articles.value.length < total.value);

    const mergeUnique = (currentItems, nextItems) => {
        const seen = new Set();
        const merged = [];
        [...currentItems, ...nextItems].forEach((item) => {
            const key = getItemKey(item);
            if (key === undefined || key === null) {
                merged.push(item);
                return;
            }
            if (seen.has(key)) {
                return;
            }
            seen.add(key);
            merged.push(item);
        });
        return merged;
    };

    const applyPageResult = (pageResult = {}, { append = false } = {}) => {
        const nextItems = Array.isArray(pageResult.items) ? pageResult.items : [];
        const nextPage = Number(pageResult.page || (append ? currentPage.value + 1 : 1));
        const nextTotal = Number(pageResult.total || 0);

        articles.value = append ? mergeUnique(articles.value, nextItems) : nextItems;
        currentPage.value = Number.isNaN(nextPage) || nextPage < 1 ? 1 : nextPage;
        total.value = Number.isNaN(nextTotal) ? articles.value.length : nextTotal;
        loadMoreError.value = '';
    };

    const setFeedState = (state = {}) => {
        articles.value = Array.isArray(state.items) ? state.items : [];
        currentPage.value = Number(state.page || 1);
        total.value = Number(state.total || articles.value.length);
        loadingMore.value = false;
        loadMoreError.value = '';
    };

    const getFeedState = () => ({
        items: [...articles.value],
        page: currentPage.value,
        total: total.value
    });

    const resetFeed = () => {
        loadMoreSeq += 1;
        articles.value = [];
        currentPage.value = 1;
        total.value = 0;
        loadingMore.value = false;
        loadMoreError.value = '';
    };

    const saveFeedCache = (cacheKey) => {
        if (!cacheKey || typeof sessionStorage === 'undefined') {
            return;
        }
        if (!articles.value.length && total.value === 0) {
            return;
        }
        try {
            sessionStorage.setItem(`${CACHE_PREFIX}${cacheKey}`, JSON.stringify({
                items: articles.value,
                page: currentPage.value,
                total: total.value,
                savedAt: Date.now()
            }));
        } catch {
            // Ignore storage quota and privacy-mode failures.
        }
    };

    const restoreFeedCache = (cacheKey) => {
        if (!cacheKey || typeof sessionStorage === 'undefined') {
            return false;
        }
        try {
            const raw = sessionStorage.getItem(`${CACHE_PREFIX}${cacheKey}`);
            if (!raw) {
                return false;
            }
            const cached = JSON.parse(raw);
            if (!cached?.savedAt || Date.now() - cached.savedAt > CACHE_TTL) {
                sessionStorage.removeItem(`${CACHE_PREFIX}${cacheKey}`);
                return false;
            }
            setFeedState({
                items: cached.items,
                page: cached.page,
                total: cached.total
            });
            return true;
        } catch {
            sessionStorage.removeItem(`${CACHE_PREFIX}${cacheKey}`);
            return false;
        }
    };

    const loadMore = async (requestPage, options = {}) => {
        if (loadingMore.value || !hasMore.value) {
            return { skipped: true };
        }

        const nextPage = currentPage.value + 1;
        const seq = loadMoreSeq + 1;
        loadMoreSeq = seq;
        loadingMore.value = true;
        loadMoreError.value = '';

        try {
            const pageResult = await requestPage(nextPage, pageSize);
            if (seq !== loadMoreSeq) {
                return { ignored: true };
            }
            applyPageResult(pageResult, { append: true });
            return { result: pageResult };
        } catch (error) {
            if (seq !== loadMoreSeq) {
                return { ignored: true };
            }
            loadMoreError.value = error?.message || options.errorMessage || '加载更多失败，请稍后重试';
            return { error };
        } finally {
            if (seq === loadMoreSeq) {
                loadingMore.value = false;
            }
        }
    };

    return {
        articles,
        currentPage,
        total,
        hasMore,
        loadingMore,
        loadMoreError,
        applyPageResult,
        getFeedState,
        setFeedState,
        resetFeed,
        saveFeedCache,
        restoreFeedCache,
        loadMore
    };
}
