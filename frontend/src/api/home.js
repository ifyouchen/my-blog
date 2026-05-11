import { request } from './http';
import { normalizeArticle, normalizeAuthorRanking, normalizeColumn, normalizeTopic } from './transformers';

export const getHomeStatsApi = async () => {
    return await request('/home/stats', {
        suppressAuthPrompt: true
    });
};

export const getHomeBootstrapApi = async () => {
    const data = await request('/home/bootstrap', {
        suppressAuthPrompt: true
    });
    return {
        stats: data?.stats || null,
        categories: data?.categories || [],
        recommendedColumns: (data?.recommendedColumns || []).map(normalizeColumn),
        authorRankings: (data?.authorRankings || []).map(normalizeAuthorRanking),
        featuredArticles: (data?.featuredArticles || []).map(normalizeArticle),
        weeklyArticles: (data?.weeklyArticles || []).map(normalizeArticle),
        hotTopics: (data?.hotTopics || []).map(normalizeTopic),
        todayFocus: data?.todayFocus ? normalizeArticle(data.todayFocus) : null,
        learningTopics: (data?.learningTopics || []).map(normalizeTopic),
        hotTags: data?.hotTags || [],
        hotKeywords: data?.hotKeywords || []
    };
};
