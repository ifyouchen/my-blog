import {request} from './http';
import {normalizeArticle, normalizeColumn, normalizeUser} from './transformers';

export const getRecommendedAuthorsApi = async (limit = 6) => {
    const data = await request(`/recommendations/authors?limit=${limit}`);
    return (data || []).map(normalizeUser);
};

export const getRecommendedColumnsFromHubApi = async (limit = 6) => {
    const data = await request(`/recommendations/columns?limit=${limit}`);
    return (data || []).map(normalizeColumn);
};

export const getFeaturedRecommendationArticlesApi = async ({ page = 1, pageSize = 6 } = {}) => {
    const data = await request(`/recommendations/articles?page=${page}&pageSize=${pageSize}`);
    return (data || []).map(normalizeArticle);
};
