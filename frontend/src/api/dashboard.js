import { request } from './http';

export const getDashboardOverviewApi = async () => {
    return await request('/dashboard/overview');
};

export const getDashboardTrendsApi = async (range = '7d') => {
    return await request(`/dashboard/trends?range=${encodeURIComponent(range)}`);
};

export const getDashboardArticlePerformanceApi = async (sort = 'view') => {
    return await request(`/dashboard/articles/performance?sort=${encodeURIComponent(sort)}`);
};

export const getDashboardInteractionsApi = async () => {
    return await request('/dashboard/interactions');
};
