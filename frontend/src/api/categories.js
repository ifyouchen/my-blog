import {request} from './http';
import {normalizeArticle} from './transformers';

export const getCategoriesApi = async (enabled = null) => {
    const params = enabled !== null ? `?enabled=${enabled}` : '';
    const data = await request(`/categories${params}`);
    return Array.isArray(data) ? data : (data.items || []);
};

export const getCategoryApi = async (id) => {
    return await request(`/categories/${id}`);
};

export const getCategoryArticlesApi = async (id, { page = 1, pageSize = 10, sort = 'latest' } = {}) => {
    const data = await request(`/categories/${id}/articles?page=${page}&pageSize=${pageSize}&sort=${sort}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

