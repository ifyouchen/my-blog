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

const STATUS_LABELS = {
    PUBLISHED: '已发布',
    DRAFT: '草稿',
    SCHEDULED: '定时发布',
    REVIEW_PENDING: '待审核',
    OFFLINE: '已下架',
    DELETED: '已删除',
    NORMAL: '正常',
    DISABLED: '禁用',
    PENDING: '待处理',
    SUCCESS: '成功',
    FAILED: '失败',
    REJECTED: '已驳回'
};

const OPERATION_LABELS = {
    UPDATE_USER_STATUS: '更新用户状态',
    UPDATE_USER_ROLE: '更新用户角色',
    DISABLE_USER: '禁用用户',
    RECOMMEND_USER: '推荐用户',
    UNRECOMMEND_USER: '取消推荐用户',
    UPDATE_ARTICLE_STATUS: '更新文章状态',
    BATCH_UPDATE_ARTICLE_STATUS: '批量更新文章状态',
    DELETE_ARTICLE: '删除文章',
    BATCH_DELETE_ARTICLE: '批量删除文章',
    FEATURE_ARTICLE: '设置精选文章',
    UNFEATURE_ARTICLE: '取消精选文章',
    DELETE_COMMENT: '删除评论',
    APPROVE_COMMENT: '通过评论',
    REJECT_COMMENT: '驳回评论',
    CREATE_CATEGORY: '创建分类',
    UPDATE_CATEGORY: '更新分类',
    DELETE_CATEGORY: '删除分类',
    CREATE_CATEGORY_GROUP: '创建分类组',
    UPDATE_CATEGORY_GROUP: '更新分类组',
    DELETE_CATEGORY_GROUP: '删除分类组',
    CREATE_TAG: '创建标签',
    UPDATE_TAG: '更新标签',
    DELETE_TAG: '删除标签',
    CREATE_COLUMN: '创建专栏',
    UPDATE_COLUMN: '更新专栏',
    DELETE_COLUMN: '删除专栏',
    ADD_COLUMN_ARTICLE: '添加专栏文章',
    REMOVE_COLUMN_ARTICLE: '移除专栏文章',
    CREATE_TOPIC: '创建专题',
    UPDATE_TOPIC: '更新专题',
    DELETE_TOPIC: '删除专题',
    ADD_TOPIC_ARTICLE: '添加专题文章',
    REMOVE_TOPIC_ARTICLE: '移除专题文章',
    BIND_TOPIC_ARTICLE: '添加专题文章',
    UNBIND_TOPIC_ARTICLE: '移除专题文章',
    CREATE_AD: '创建广告',
    UPDATE_AD: '更新广告',
    DELETE_AD: '删除广告',
    CREATE_ANNOUNCEMENT: '创建公告',
    UPDATE_ANNOUNCEMENT: '更新公告',
    DELETE_ANNOUNCEMENT: '删除公告',
    PUBLISH_ANNOUNCEMENT: '发布公告',
    UNPUBLISH_ANNOUNCEMENT: '撤回公告',
    CREATE_SENSITIVE_WORD: '创建敏感词',
    UPDATE_SENSITIVE_WORD: '更新敏感词',
    DELETE_SENSITIVE_WORD: '删除敏感词'
};

export const formatAdminStatusLabel = (status, fallback = '-') => {
    if (!status) {
        return fallback;
    }
    return STATUS_LABELS[status] || status;
};

export const formatAdminOperationLabel = (operation, fallback = '-') => {
    if (!operation) {
        return fallback;
    }
    return OPERATION_LABELS[operation] || operation;
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
