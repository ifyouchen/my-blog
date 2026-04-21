import { request } from './http';
import { normalizeArticle } from './transformers';

export const favoriteArticleApi = async (articleId) => {
    return await request(`/articles/${articleId}/favorite`, {
        method: 'POST'
    });
};

export const unfavoriteArticleApi = async (articleId) => {
    return await request(`/articles/${articleId}/favorite`, {
        method: 'DELETE'
    });
};

export const getFavoriteStatusApi = async (articleId) => {
    return await request(`/articles/${articleId}/favorite/status`);
};

export const getMyFavoritesApi = async (page = 1, pageSize = 10) => {
    const data = await request(`/users/me/favorites?page=${page}&pageSize=${pageSize}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};
