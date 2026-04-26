import { request } from './http';
import { normalizeAuthorRanking, normalizeColumn } from './transformers';

export const getHomeStatsApi = async () => {
    return await request('/home/stats');
};

export const getHomeBootstrapApi = async () => {
    const data = await request('/home/bootstrap');
    return {
        stats: data?.stats || null,
        categories: data?.categories || [],
        recommendedColumns: (data?.recommendedColumns || []).map(normalizeColumn),
        authorRankings: (data?.authorRankings || []).map(normalizeAuthorRanking)
    };
};
