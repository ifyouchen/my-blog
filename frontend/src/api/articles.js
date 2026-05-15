import {downloadFile, request} from './http';
import {normalizeArticle} from './transformers';

export const listArticlesApi = async (params = {}) => {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
            query.set(key, value);
        }
    });
    const suffix = query.toString() ? `?${query.toString()}` : '';
    const data = await request(`/articles${suffix}`, {
        suppressAuthPrompt: true
    });

    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

export const getArticleApi = async (id) => normalizeArticle(await request(`/articles/${id}`));

export const getRelatedArticlesApi = async (id, limit = 5) => {
    const data = await request(`/articles/${id}/related?limit=${limit}`);
    return (Array.isArray(data) ? data : []).map(normalizeArticle);
};

export const getArticleRecommendationsApi = async (id, limit = 12) => {
    const data = await request(`/articles/${id}/recommendations?limit=${limit}`, {
        suppressAuthPrompt: true
    });
    return {
        sections: (data?.sections || []).map((section) => ({
            key: section.key,
            title: section.title,
            items: (section.items || []).map(normalizeArticle)
        }))
    };
};

export const getArticleNeighborsApi = async (id) => {
    const data = await request(`/articles/${id}/neighbors`, {
        suppressAuthPrompt: true
    });
    return {
        prev: data?.prev ? normalizeArticle(data.prev) : null,
        next: data?.next ? normalizeArticle(data.next) : null
    };
};

export const getEditableArticleApi = async (id) => normalizeArticle(await request(`/articles/${id}/edit`));

export const createArticleApi = async (draft, status) => {
    return normalizeArticle(await request('/articles', {
        method: 'POST',
        body: buildArticlePayload(draft, status)
    }));
};

export const validateArticleForPublishApi = async (draft) => {
    return await request('/articles/validate', {
        method: 'POST',
        body: buildArticlePayload(draft, 'PUBLISHED')
    });
};

export const updateArticleApi = async (articleId, draft, status) => {
    return normalizeArticle(await request(`/articles/${articleId}`, {
        method: 'PUT',
        body: buildArticlePayload(draft, status)
    }));
};

export const updateArticleStatusApi = async (articleId, status) => {
    return normalizeArticle(await request(`/articles/${articleId}/status`, {
        method: 'PUT',
        body: { status }
    }));
};

export const updateArticleUnlockRuleApi = async (articleId, { needUnlock, unlockPointPrice } = {}) => {
    return normalizeArticle(await request(`/articles/${articleId}/unlock-rule`, {
        method: 'PUT',
        body: {
            needUnlock: Boolean(needUnlock),
            unlockPointPrice: Boolean(needUnlock) ? Number(unlockPointPrice || 0) : 0
        }
    }));
};

export const deleteArticleApi = async (articleId) => {
    return await request(`/articles/${articleId}`, {
        method: 'DELETE'
    });
};

export const getMyArticlesApi = async ({ page = 1, pageSize = 10, status = '', keyword = '' } = {}) => {
    const query = new URLSearchParams({ page, pageSize });
    if (status) {
        query.set('status', status);
    }
    if (keyword) {
        query.set('keyword', keyword);
    }
    const data = await request(`/users/me/articles?${query.toString()}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

export const getMyArticleOverviewApi = async () => {
    return await request('/users/me/articles/overview');
};

export const exportMyArticlesApi = (params = {}) => {
    const query = new URLSearchParams({ _t: Date.now() });
    if (params.status) query.set('status', params.status);
    return downloadFile(`/users/me/export/articles?${query.toString()}`, 'my-articles.csv');
};

export const getUserArticlesApi = async (userId, { page = 1, pageSize = 10 } = {}) => {
    const data = await request(`/users/${userId}/articles?page=${page}&pageSize=${pageSize}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

export const listArticleVersionsApi = async (articleId) => {
    return await request(`/articles/${articleId}/versions`);
};

export const getArticleVersionApi = async (articleId, versionNo) => {
    return await request(`/articles/${articleId}/versions/${versionNo}`);
};

export const restoreArticleVersionApi = async (articleId, versionNo) => {
    return normalizeArticle(await request(`/articles/${articleId}/versions/${versionNo}/restore`, {
        method: 'POST'
    }));
};

export const getArticleStatsApi = async (articleId) => {
    return await request(`/dashboard/articles/${articleId}/stats`);
};

const buildArticlePayload = (draft, status) => {
    const sourceTags = Array.isArray(draft.tags) ? draft.tags : String(draft.tags || '')
        .split(',');
    const tags = sourceTags
        .map((item) => String(item).trim())
        .filter(Boolean);

    return {
        title: draft.title,
        summary: draft.summary,
        content: draft.content,
        coverUrl: draft.coverUrl,
        category: draft.category,
        tags,
        status,
        slug: draft.slug || '',
        seoTitle: draft.seoTitle || '',
        seoDescription: draft.seoDescription || '',
        needUnlock: Boolean(draft.needUnlock),
        unlockPointPrice: Boolean(draft.needUnlock) ? Number(draft.unlockPointPrice || 0) : 0,
        scheduledPublishAt: draft.scheduledPublishAt || ''
    };
};
