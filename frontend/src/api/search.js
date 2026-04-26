import { request } from './http';

export const getSearchBootstrapApi = async () => {
    return await request('/search/bootstrap');
};

export const searchUsersApi = async ({ keyword, page = 1, pageSize = 10 }) => {
    return await request(`/search/users?keyword=${encodeURIComponent(keyword || '')}&page=${page}&pageSize=${pageSize}`);
};

export const searchColumnsApi = async ({ keyword, page = 1, pageSize = 10 }) => {
    return await request(`/search/columns?keyword=${encodeURIComponent(keyword || '')}&page=${page}&pageSize=${pageSize}`);
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
