import { request } from './http';
import { normalizeComment, normalizeCommentPage } from './transformers';

export const pageCommentsApi = async (articleId, { page = 1, pageSize = 20, sort = 'hot' } = {}) => {
    const data = await request(`/articles/${articleId}/comments?page=${page}&pageSize=${pageSize}&sort=${sort}`);
    return normalizeCommentPage(data);
};

export const pageRepliesApi = async (rootCommentId, { page = 1, pageSize = 10 } = {}) => {
    const data = await request(`/comments/${rootCommentId}/replies?page=${page}&pageSize=${pageSize}`);
    return normalizeCommentPage(data);
};

export const createCommentApi = async (articleId, payload) => {
    return normalizeComment(await request(`/articles/${articleId}/comments`, {
        method: 'POST',
        body: payload
    }));
};

export const deleteCommentApi = async (commentId) => {
    return await request(`/comments/${commentId}`, {
        method: 'DELETE'
    });
};

export const likeCommentApi = async (commentId) => {
    return await request(`/comments/${commentId}/like`, {
        method: 'POST'
    });
};

export const unlikeCommentApi = async (commentId) => {
    return await request(`/comments/${commentId}/like`, {
        method: 'DELETE'
    });
};

export const pinCommentApi = async (commentId) => {
    return await request(`/comments/${commentId}/pin`, {
        method: 'POST'
    });
};

export const unpinCommentApi = async (commentId) => {
    return await request(`/comments/${commentId}/pin`, {
        method: 'DELETE'
    });
};
