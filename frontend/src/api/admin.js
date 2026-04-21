import { request } from './http';

export const getAdminStatsApi = async () => {
    return await request(`/admin/stats`);
};

export const getAdminUsersApi = async (page = 1, pageSize = 10, status = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (status) params.append('status', status);
    return await request(`/admin/users?${params}`);
};

export const updateAdminUserStatusApi = async (userId, status) => {
    return await request(`/admin/users/${userId}/status`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `status=${encodeURIComponent(status)}`
    });
};

export const getAdminArticlesApi = async (page = 1, pageSize = 10, status = null, keyword = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (status) params.append('status', status);
    if (keyword) params.append('keyword', keyword);
    return await request(`/admin/articles?${params}`);
};

export const updateAdminArticleStatusApi = async (articleId, status) => {
    return await request(`/admin/articles/${articleId}/status`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `status=${encodeURIComponent(status)}`
    });
};

export const getAdminCommentsApi = async (page = 1, pageSize = 10, articleId = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (articleId) params.append('articleId', articleId);
    return await request(`/admin/comments?${params}`);
};

export const getAdminLogsApi = async (page = 1, pageSize = 10) => {
    const params = new URLSearchParams({ page, pageSize });
    return await request(`/admin/logs?${params}`);
};

export const deleteAdminCommentApi = async (commentId) => {
    return await request(`/admin/comments/${commentId}`, {
        method: 'DELETE'
    });
};

export const getCategoriesApi = async (enabled = null) => {
    const params = enabled !== null ? `?enabled=${enabled}` : '';
    return await request(`/categories${params}`);
};

export const createCategoryApi = async (payload) => {
    return await request('/categories', {
        method: 'POST',
        body: payload
    });
};

export const updateCategoryApi = async (categoryId, payload) => {
    return await request(`/categories/${categoryId}`, {
        method: 'PUT',
        body: payload
    });
};

export const deleteCategoryApi = async (categoryId) => {
    return await request(`/categories/${categoryId}`, {
        method: 'DELETE'
    });
};

export const getTagsApi = async (enabled = null) => {
    const params = enabled !== null ? `?enabled=${enabled}` : '';
    return await request(`/tags${params}`);
};

export const createTagApi = async (payload) => {
    return await request('/tags', {
        method: 'POST',
        body: payload
    });
};

export const updateTagApi = async (tagId, payload) => {
    return await request(`/tags/${tagId}`, {
        method: 'PUT',
        body: payload
    });
};

export const deleteTagApi = async (tagId) => {
    return await request(`/tags/${tagId}`, {
        method: 'DELETE'
    });
};
