import { buildParams, request } from './http';
import {normalizeArticle, normalizeColumn, normalizeTopic} from './transformers';

export const getSearchBootstrapApi = async () => {
    const data = await request('/search/bootstrap');
    return {
        ...(data || {}),
        recommendedTopics: (data?.recommendedTopics || []).map(normalizeTopic),
        recommendedColumns: (data?.recommendedColumns || []).map(normalizeColumn)
    };
};

export const searchUsersApi = async ({ keyword, page = 1, pageSize = 10 }) => {
    return await request(`/search/users?${buildParams({ keyword: keyword || '', page, pageSize })}`);
};

export const searchColumnsApi = async ({ keyword, page = 1, pageSize = 10 }) => {
    return await request(`/search/columns?${buildParams({ keyword: keyword || '', page, pageSize })}`);
};

export const unifiedSearchApi = async ({
    keyword = '',
    type = 'all',
    category = '',
    tag = '',
    difficulty = '',
    sort = 'recommend',
    page = 1,
    pageSize = 10
} = {}) => {
    const params = new URLSearchParams({
        keyword,
        type,
        category,
        tag,
        difficulty,
        sort,
        page: String(page),
        pageSize: String(pageSize)
    });
    const data = await request(`/search/unified?${params.toString()}`, {
        suppressAuthPrompt: true
    });
    return {
        keyword: data?.keyword || keyword,
        articles: {
            ...(data?.articles || {}),
            items: (data?.articles?.items || []).map(normalizeArticle)
        },
        topics: {
            ...(data?.topics || {}),
            items: (data?.topics?.items || []).map(normalizeTopic)
        },
        columns: {
            ...(data?.columns || {}),
            items: (data?.columns?.items || []).map(normalizeColumn)
        },
        tags: data?.tags || []
    };
};

export const getHotKeywordsApi = async (limit = 10) => {
    return await request(`/search/hot-keywords?limit=${limit}`);
};

export const getRecentKeywordsApi = async (limit = 10) => {
    return await request(`/search/recent-keywords?limit=${limit}`);
};

export const saveRecentKeywordApi = async (keyword) => {
    return await request('/search/recent-keywords', {
        method: 'POST',
        body: JSON.stringify({ keyword })
    });
};

export const clearRecentKeywordsApi = async () => {
    return await request('/search/recent-keywords', {
        method: 'DELETE'
    });
};
