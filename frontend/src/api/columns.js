import { request } from './http';
import { normalizeArticle, normalizeColumn } from './transformers';

export const getColumnsApi = async ({ page = 1, pageSize = 9 } = {}) => {
    const data = await request(`/columns?page=${page}&pageSize=${pageSize}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeColumn)
    };
};

export const getRecommendedColumnsApi = async (limit = 3) => {
    const data = await request(`/columns/recommended?limit=${limit}`);
    return (data || []).map(normalizeColumn);
};

export const getColumnDetailApi = async (columnId) => normalizeColumn(await request(`/columns/${columnId}`));

export const getColumnArticlesApi = async (columnId, { page = 1, pageSize = 10 } = {}) => {
    const data = await request(`/columns/${columnId}/articles?page=${page}&pageSize=${pageSize}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

export const subscribeColumnApi = async (columnId) => request(`/columns/${columnId}/subscribe`, {
    method: 'POST'
});

export const unsubscribeColumnApi = async (columnId) => request(`/columns/${columnId}/subscribe`, {
    method: 'DELETE'
});
