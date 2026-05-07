import { request } from './http';
import { normalizeArticle, normalizeAuthorRanking } from './transformers';

const buildRankingQuery = ({ limit = 10, period = '7d', category = '' } = {}) => {
    const params = new URLSearchParams();
    params.set('limit', String(limit));
    params.set('period', period || '7d');
    if (category) {
        params.set('category', category);
    }
    return params.toString();
};

export const getArticleRankingsApi = async (limit = 10, options = {}) => {
    const data = await request(`/rankings/articles?${buildRankingQuery({ ...options, limit })}`);
    return (data || []).map(normalizeArticle);
};

export const getAuthorRankingsApi = async (limit = 10, options = {}) => {
    const data = await request(`/rankings/authors?${buildRankingQuery({ ...options, limit })}`);
    return (data || []).map(normalizeAuthorRanking);
};
