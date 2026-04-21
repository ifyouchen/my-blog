import { request } from './http';

export const likeArticleApi = async (articleId) => {
    return await request(`/articles/${articleId}/like`, {
        method: 'POST'
    });
};

export const unlikeArticleApi = async (articleId) => {
    return await request(`/articles/${articleId}/like`, {
        method: 'DELETE'
    });
};

export const getLikeStatusApi = async (articleId) => {
    return await request(`/articles/${articleId}/like/status`);
};