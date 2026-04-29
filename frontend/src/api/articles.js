import { request } from './http';
import { normalizeArticle } from './transformers';

export const listArticlesApi = async (params = {}) => {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
            query.set(key, value);
        }
    });
    const suffix = query.toString() ? `?${query.toString()}` : '';
    const data = await request(`/articles${suffix}`);

    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

export const getArticleApi = async (id) => normalizeArticle(await request(`/articles/${id}`));

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

export const deleteArticleApi = async (articleId) => {
    return await request(`/articles/${articleId}`, {
        method: 'DELETE'
    });
};

export const getMyArticlesApi = async ({ page = 1, pageSize = 10, status = '' } = {}) => {
    const query = new URLSearchParams({ page, pageSize });
    if (status) {
        query.set('status', status);
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

export const getUserArticlesApi = async (userId, { page = 1, pageSize = 10 } = {}) => {
    const data = await request(`/users/${userId}/articles?page=${page}&pageSize=${pageSize}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
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
        seoDescription: draft.seoDescription || ''
    };
};
