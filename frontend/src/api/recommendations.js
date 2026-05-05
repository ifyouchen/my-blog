import {request} from './http';
import {normalizeArticle, normalizeColumn, normalizeUser} from './transformers';

export const getRecommendedAuthorsApi = async (limit = 6) => {
    const data = await request(`/recommendations/authors?limit=${limit}`, {
        suppressAuthPrompt: true
    });
    return (data || []).map(normalizeUser);
};

export const getRecommendedColumnsFromHubApi = async (limit = 6) => {
    const data = await request(`/recommendations/columns?limit=${limit}`, {
        suppressAuthPrompt: true
    });
    return (data || []).map(normalizeColumn);
};

export const getFeaturedRecommendationArticlesApi = async ({ page = 1, pageSize = 6 } = {}) => {
    const data = await request(`/recommendations/articles?page=${page}&pageSize=${pageSize}`, {
        suppressAuthPrompt: true
    });
    return (data || []).map(normalizeArticle);
};
