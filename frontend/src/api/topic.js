import {request} from './http';
import {normalizeArticle, normalizeTopic} from './transformers';

export const getTopicsApi = async ({ page = 1, pageSize = 10 } = {}) => {
    const data = await request(`/topics?page=${page}&pageSize=${pageSize}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeTopic)
    };
};

export const getTopicDetailApi = async (topicId) => normalizeTopic(await request(`/topics/${topicId}`));

export const getTopicArticlesApi = async (topicId, { page = 1, pageSize = 10 } = {}) => {
    const data = await request(`/topics/${topicId}/articles?page=${page}&pageSize=${pageSize}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

export const getTopicArticleNeighborsApi = async (topicId, articleId) => {
    const data = await request(`/topics/${topicId}/articles/${articleId}/neighbors`, {
        suppressAuthPrompt: true
    });
    return {
        prev: data?.prev ? normalizeArticle(data.prev) : null,
        next: data?.next ? normalizeArticle(data.next) : null
    };
};
