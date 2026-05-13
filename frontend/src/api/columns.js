import {request} from './http';
import {normalizeArticle, normalizeColumn} from './transformers';

// ========== 创作者专栏管理 API ==========

export const getMyColumnsApi = async () => {
    const data = await request('/dashboard/columns');
    return (data || []).map(normalizeColumn);
};

export const createMyColumnApi = async ({ title, summary = '', coverUrl = '' }) => {
    const data = await request('/dashboard/columns', {
        method: 'POST',
        body: JSON.stringify({ title, summary, coverUrl })
    });
    return normalizeColumn(data);
};

export const updateMyColumnApi = async (columnId, { title, summary = '', coverUrl = '' }) => {
    const data = await request(`/dashboard/columns/${columnId}`, {
        method: 'PUT',
        body: JSON.stringify({ title, summary, coverUrl })
    });
    return normalizeColumn(data);
};

export const deleteMyColumnApi = async (columnId) => {
    return request(`/dashboard/columns/${columnId}`, { method: 'DELETE' });
};

export const addMyColumnArticleApi = async (columnId, articleId) => {
    return request(`/dashboard/columns/${columnId}/articles`, {
        method: 'POST',
        body: JSON.stringify({ articleId })
    });
};

export const removeMyColumnArticleApi = async (columnId, articleId) => {
    return request(`/dashboard/columns/${columnId}/articles/${articleId}`, { method: 'DELETE' });
};

// ========== 公共专栏 API ==========

export const getColumnsApi = async ({ page = 1, pageSize = 9, keyword = '' } = {}) => {
    const params = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize)
    });
    if (keyword && keyword.trim()) {
        params.set('keyword', keyword.trim());
    }
    const data = await request(`/columns?${params.toString()}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeColumn)
    };
};

export const getRecommendedColumnsApi = async (limit = 3) => {
    const data = await request(`/columns/recommended?limit=${limit}`);
    return (data || []).map(normalizeColumn);
};

export const getUserColumnsApi = async (userId, { page = 1, pageSize = 12 } = {}) => {
    const params = new URLSearchParams({
        page: String(page),
        pageSize: String(pageSize)
    });
    const data = await request(`/columns/users/${userId}?${params.toString()}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeColumn)
    };
};

export const getColumnDetailApi = async (columnId) => normalizeColumn(await request(`/columns/${columnId}`));

export const getColumnArticlesApi = async (columnId, { page = 1, pageSize = 10 } = {}) => {
    const data = await request(`/columns/${columnId}/articles?page=${page}&pageSize=${pageSize}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

export const getColumnArticleNeighborsApi = async (columnId, articleId) => {
    const data = await request(`/columns/${columnId}/articles/${articleId}/neighbors`, {
        suppressAuthPrompt: true
    });
    return {
        prev: data?.prev ? normalizeArticle(data.prev) : null,
        next: data?.next ? normalizeArticle(data.next) : null
    };
};

export const subscribeColumnApi = async (columnId) => request(`/columns/${columnId}/subscribe`, {
    method: 'POST'
});

export const unsubscribeColumnApi = async (columnId) => request(`/columns/${columnId}/subscribe`, {
    method: 'DELETE'
});
