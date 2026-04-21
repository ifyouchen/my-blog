import { request } from './http';

export const getCommentsApi = async (articleId) => {
    return await request(`/articles/${articleId}/comments`);
};

export const createCommentApi = async (articleId, payload) => {
    return await request(`/articles/${articleId}/comments`, {
        method: 'POST',
        body: JSON.stringify(payload)
    });
};

export const deleteCommentApi = async (commentId) => {
    return await request(`/comments/${commentId}`, {
        method: 'DELETE'
    });
};