import { onMounted, onUnmounted } from 'vue';

export const DEFAULT_ADMIN_PAGE_SIZE = 10;

export const createPagedState = (pageSize = DEFAULT_ADMIN_PAGE_SIZE) => ({
    items: [],
    page: 1,
    pageSize,
    total: 0,
    loading: false,
    error: '',
    jumpPage: '1'
});

export const getTotalPages = (state) => {
    return Math.max(1, Math.ceil((state.total || 0) / state.pageSize));
};

export const getPageStart = (state) => {
    if (!state.total) {
        return 0;
    }
    return (state.page - 1) * state.pageSize + 1;
};

export const getPageEnd = (state) => {
    return Math.min(state.page * state.pageSize, state.total || 0);
};

export const getPaginationItems = (state) => {
    const totalPages = getTotalPages(state);
    const currentPage = state.page;
    const items = [];

    const appendPage = (page) => {
        if (!items.some((item) => item.type === 'page' && item.value === page)) {
            items.push({ type: 'page', value: page });
        }
    };
    const appendEllipsis = (key) => {
        items.push({ type: 'ellipsis', value: key });
    };

    if (totalPages <= 7) {
        for (let page = 1; page <= totalPages; page += 1) {
            appendPage(page);
        }
        return items;
    }

    appendPage(1);
    if (currentPage > 4) {
        appendEllipsis('left');
    }

    const start = Math.max(2, currentPage - 1);
    const end = Math.min(totalPages - 1, currentPage + 1);
    for (let page = start; page <= end; page += 1) {
        appendPage(page);
    }

    if (currentPage < totalPages - 3) {
        appendEllipsis('right');
    }
    appendPage(totalPages);
    return items;
};

export const readPositiveInt = (value, fallback = 1) => {
    const parsed = Number.parseInt(value, 10);
    if (Number.isNaN(parsed) || parsed < 1) {
        return fallback;
    }
    return parsed;
};

export const readQueryText = (route, key, fallback = '') => {
    const value = route.query[key];
    return typeof value === 'string' ? value : fallback;
};

export const readQueryBooleanText = (route, key) => {
    const value = route.query[key];
    if (value === 'true' || value === 'false') {
        return value;
    }
    return '';
};

export const formatAdminDateTime = (value, fallback = '-') => {
    if (!value) {
        return fallback;
    }
    const text = String(value).replace('T', ' ');
    return text.length > 19 ? text.slice(0, 19) : text;
};

export const syncAdminQuery = async (router, route, patch = {}) => {
    const nextQuery = {
        ...route.query,
        ...patch
    };

    Object.keys(nextQuery).forEach((key) => {
        const value = nextQuery[key];
        if (value === undefined || value === null || value === '') {
            delete nextQuery[key];
        }
    });

    const serializeQuery = (query) => {
        return JSON.stringify(
            Object.keys(query).sort().reduce((result, key) => {
                const value = query[key];
                result[key] = Array.isArray(value) ? value.map(String) : String(value);
                return result;
            }, {})
        );
    };
    if (serializeQuery(route.query) === serializeQuery(nextQuery)) {
        return false;
    }

    await router.replace({
        query: nextQuery
    });
    return true;
};

export const resolveAdminOverflowPage = (state, result) => {
    const total = Number(result?.total || 0);
    const items = Array.isArray(result?.items) ? result.items : [];
    const currentPage = Number(state?.page || 1);
    const pageSize = Number(state?.pageSize || DEFAULT_ADMIN_PAGE_SIZE);
    if (currentPage <= 1 || total <= 0 || items.length > 0) {
        return null;
    }
    const lastPage = Math.max(1, Math.ceil(total / pageSize));
    if (currentPage <= lastPage) {
        return null;
    }
    return lastPage;
};

export const useAdminRefresh = (handler) => {
    const listener = () => {
        handler();
    };

    onMounted(() => {
        window.addEventListener('admin-refresh-request', listener);
    });

    onUnmounted(() => {
        window.removeEventListener('admin-refresh-request', listener);
    });
};
