import {request} from './http';
import {normalizeArticle} from './transformers';

export const getTagsApi = async ({ page = 1, pageSize = 50 } = {}) => {
    const data = await request(`/tags?page=${page}&pageSize=${pageSize}`);
    return Array.isArray(data) ? data : (data.items || []);
};

export const getTagApi = async (id) => {
    return await request(`/tags/${id}`);
};

export const getHotTagsApi = async (limit = 30) => {
    return await request(`/tags/hot?limit=${limit}`);
};

export const getTagArticlesApi = async (id, { page = 1, pageSize = 10, sort = 'latest' } = {}) => {
    const data = await request(`/tags/${id}/articles?page=${page}&pageSize=${pageSize}&sort=${sort}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

