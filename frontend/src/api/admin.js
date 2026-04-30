import {request} from './http';

export const getAdminStatsApi = async () => {
    return await request(`/admin/stats`);
};

export const getAdminUsersApi = async (page = 1, pageSize = 10, status = null, keyword = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (status) params.append('status', status);
    if (keyword) params.append('keyword', keyword);
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

export const getAdminArticlesApi = async (page = 1, pageSize = 10, status = null, keyword = null, category = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (status) params.append('status', status);
    if (keyword) params.append('keyword', keyword);
    if (category) params.append('category', category);
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

export const deleteAdminArticleApi = async (articleId) => {
    return await request(`/admin/articles/${articleId}`, {
        method: 'DELETE'
    });
};

export const batchUpdateAdminArticleStatusApi = async (ids, status) => {
    return await request('/admin/articles/batch/status', {
        method: 'POST',
        body: { ids, status }
    });
};

export const batchDeleteAdminArticlesApi = async (ids) => {
    return await request('/admin/articles/batch/delete', {
        method: 'POST',
        body: { ids }
    });
};

export const getAdminCommentsApi = async (page = 1, pageSize = 10, articleId = null, keyword = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (articleId) params.append('articleId', articleId);
    if (keyword) params.append('keyword', keyword);
    return await request(`/admin/comments?${params}`);
};

export const getAdminLogsApi = async (page = 1, pageSize = 10, filters = {}) => {
    const params = new URLSearchParams({ page, pageSize });
    if (filters.actionType) params.append('actionType', filters.actionType);
    if (filters.resultStatus) params.append('resultStatus', filters.resultStatus);
    if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
    if (filters.dateTo) params.append('dateTo', filters.dateTo);
    return await request(`/admin/logs?${params}`);
};

export const getAdminReportsApi = async (page = 1, pageSize = 10, filters = {}) => {
    const params = new URLSearchParams({ page, pageSize });
    if (filters.status) params.append('status', filters.status);
    if (filters.targetType) params.append('targetType', filters.targetType);
    if (filters.reasonType) params.append('reasonType', filters.reasonType);
    return await request(`/admin/reports?${params}`);
};

export const getAdminReportDetailApi = async (reportId) => {
    return await request(`/admin/reports/${reportId}`);
};

export const resolveAdminReportApi = async (reportId, payload) => {
    return await request(`/admin/reports/${reportId}/resolve`, {
        method: 'POST',
        body: payload
    });
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

export const getAdminCategoriesApi = async (page = 1, pageSize = 10, enabled = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (enabled !== null) params.append('enabled', enabled);
    return await request(`/admin/categories?${params}`);
};

export const createCategoryApi = async (payload) => {
    return await request('/admin/categories', {
        method: 'POST',
        body: payload
    });
};

export const updateCategoryApi = async (categoryId, payload) => {
    return await request(`/admin/categories/${categoryId}`, {
        method: 'PUT',
        body: payload
    });
};

export const deleteCategoryApi = async (categoryId) => {
    return await request(`/admin/categories/${categoryId}`, {
        method: 'DELETE'
    });
};

export const getTagsApi = async (enabled = null) => {
    const params = enabled !== null ? `?enabled=${enabled}` : '';
    return await request(`/tags${params}`);
};

export const getAdminTagsApi = async (page = 1, pageSize = 10, enabled = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (enabled !== null) params.append('enabled', enabled);
    return await request(`/admin/tags?${params}`);
};

export const createTagApi = async (payload) => {
    return await request('/admin/tags', {
        method: 'POST',
        body: payload
    });
};

export const updateTagApi = async (tagId, payload) => {
    return await request(`/admin/tags/${tagId}`, {
        method: 'PUT',
        body: payload
    });
};

export const deleteTagApi = async (tagId) => {
    return await request(`/admin/tags/${tagId}`, {
        method: 'DELETE'
    });
};

// ===== 专栏管理 =====

export const getAdminColumnsApi = async (page = 1, pageSize = 10, keyword = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (keyword) params.append('keyword', keyword);
    return await request(`/admin/columns?${params}`);
};

export const createAdminColumnApi = async (payload) => {
    return await request('/admin/columns', {
        method: 'POST',
        body: payload
    });
};

export const updateAdminColumnApi = async (columnId, payload) => {
    return await request(`/admin/columns/${columnId}`, {
        method: 'PUT',
        body: payload
    });
};

export const deleteAdminColumnApi = async (columnId) => {
    return await request(`/admin/columns/${columnId}`, {
        method: 'DELETE'
    });
};

export const addAdminColumnArticleApi = async (columnId, payload) => {
    return await request(`/admin/columns/${columnId}/articles`, {
        method: 'POST',
        body: payload
    });
};

export const removeAdminColumnArticleApi = async (columnId, articleId) => {
    return await request(`/admin/columns/${columnId}/articles/${articleId}`, {
        method: 'DELETE'
    });
};

// ===== 专题管理 =====

export const getAdminTopicsApi = async (page = 1, pageSize = 10, keyword = null) => {
    const params = new URLSearchParams({ page, pageSize });
    if (keyword) params.append('keyword', keyword);
    return await request(`/admin/topics?${params}`);
};

export const createAdminTopicApi = async (payload) => {
    return await request('/admin/topics', {
        method: 'POST',
        body: payload
    });
};

export const updateAdminTopicApi = async (topicId, payload) => {
    return await request(`/admin/topics/${topicId}`, {
        method: 'PUT',
        body: payload
    });
};

export const deleteAdminTopicApi = async (topicId) => {
    return await request(`/admin/topics/${topicId}`, {
        method: 'DELETE'
    });
};

export const addAdminTopicArticleApi = async (topicId, payload) => {
    return await request(`/admin/topics/${topicId}/articles`, {
        method: 'POST',
        body: payload
    });
};

export const removeAdminTopicArticleApi = async (topicId, articleId) => {
    return await request(`/admin/topics/${topicId}/articles/${articleId}`, {
        method: 'DELETE'
    });
};

// ===== 精选文章 =====

export const featureAdminArticleApi = async (articleId) => {
    return await request(`/admin/articles/${articleId}/feature`, {
        method: 'POST'
    });
};

export const unfeatureAdminArticleApi = async (articleId) => {
    return await request(`/admin/articles/${articleId}/unfeature`, {
        method: 'POST'
    });
};

// ===== 公告管理 =====

export const getAdminAnnouncementsApi = async (page = 1, pageSize = 10) => {
    return await request(`/admin/announcements?page=${page}&pageSize=${pageSize}`);
};

export const createAdminAnnouncementApi = async (payload) => {
    return await request('/admin/announcements', {
        method: 'POST',
        body: payload
    });
};

export const updateAdminAnnouncementApi = async (id, payload) => {
    return await request(`/admin/announcements/${id}`, {
        method: 'PUT',
        body: payload
    });
};

export const deleteAdminAnnouncementApi = async (id) => {
    return await request(`/admin/announcements/${id}`, {
        method: 'DELETE'
    });
};

export const publishAdminAnnouncementApi = async (id) => {
    return await request(`/admin/announcements/${id}/publish`, {
        method: 'POST'
    });
};

export const unpublishAdminAnnouncementApi = async (id) => {
    return await request(`/admin/announcements/${id}/unpublish`, {
        method: 'POST'
    });
};
