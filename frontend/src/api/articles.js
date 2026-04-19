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

export const createArticleApi = async (draft, status) => {
    const tags = String(draft.tags || '')
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);

    return normalizeArticle(await request('/articles', {
        method: 'POST',
        body: JSON.stringify({
            title: draft.title,
            summary: draft.summary,
            content: draft.content,
            coverUrl: draft.coverUrl,
            category: draft.category,
            tags,
            status
        })
    }));
};
