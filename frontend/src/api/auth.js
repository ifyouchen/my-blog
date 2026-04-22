import { request } from './http';
import { normalizeArticle, normalizeUser } from './transformers';

export const loginApi = async (payload) => {
    const data = await request('/auth/login', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

    return {
        token: data.token,
        user: normalizeUser(data.user)
    };
};

export const registerApi = async (payload) => {
    const data = await request('/auth/register', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

    return {
        token: data.token,
        user: normalizeUser(data.user)
    };
};

export const currentUserApi = async () => normalizeUser(await request('/users/me'));

export const updateProfileApi = async (payload) => normalizeUser(await request('/users/me/profile', {
    method: 'PUT',
    body: payload
}));

export const getUserProfileApi = async (userId) => {
    const data = await request(`/users/${userId}`);
    return {
        ...data,
        user: normalizeUser(data.user || {})
    };
};

export const getUserHotArticlesApi = async (userId, limit = 3) => {
    const data = await request(`/users/${userId}/articles/hot?limit=${limit}`);
    return (data || []).map(normalizeArticle);
};
