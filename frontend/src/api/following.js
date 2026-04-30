import {request} from './http';
import {normalizeArticle, normalizeUser} from './transformers';

export const followUserApi = async (userId) => request(`/users/${userId}/follow`, {
    method: 'POST'
});

export const unfollowUserApi = async (userId) => request(`/users/${userId}/follow`, {
    method: 'DELETE'
});

export const getMyFollowingApi = async () => {
    const data = await request('/users/me/following');
    return (data || []).map(normalizeUser);
};

export const getFollowingFeedApi = async ({ page = 1, pageSize = 10, sort = 'latest' } = {}) => {
    const data = await request(`/users/me/feed?page=${page}&pageSize=${pageSize}&sort=${sort}`);
    return {
        ...data,
        items: (data.items || []).map(normalizeArticle)
    };
};

export const getUserFollowersApi = async (userId) => {
    const data = await request(`/users/${userId}/followers`);
    return (data || []).map(normalizeUser);
};

export const getUserFollowingListApi = async (userId) => {
    const data = await request(`/users/${userId}/following`);
    return (data || []).map(normalizeUser);
};

export const getFollowStatusApi = async (userId) => {
    const data = await request(`/users/${userId}/follow-status`);
    return data || {};
};
