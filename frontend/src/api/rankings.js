import { request } from './http';
import { normalizeArticle, normalizeAuthorRanking } from './transformers';

export const getArticleRankingsApi = async (limit = 10) => {
    const data = await request(`/rankings/articles?limit=${limit}`);
    return (data || []).map(normalizeArticle);
};

export const getAuthorRankingsApi = async (limit = 10) => {
    const data = await request(`/rankings/authors?limit=${limit}`);
    return (data || []).map(normalizeAuthorRanking);
};
