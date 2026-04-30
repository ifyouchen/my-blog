import {request} from './http';
import {normalizeArticle, normalizeUser} from './transformers';

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
        user: normalizeUser(data.user || {}),
        followerCount: data.followerCount || 0,
        followingCount: data.followingCount || 0,
        following: Boolean(data.following)
    };
};

export const getUserHotArticlesApi = async (userId, limit = 3) => {
    const data = await request(`/users/${userId}/articles/hot?limit=${limit}`);
    return (data || []).map(normalizeArticle);
};

export const forgotPasswordApi = async (email) => {
    return await request('/auth/password/forgot', {
        method: 'POST',
        body: JSON.stringify({ email })
    });
};

export const resetPasswordApi = async (token, newPassword) => {
    return await request('/auth/password/reset', {
        method: 'POST',
        body: JSON.stringify({ token, newPassword })
    });
};

export const changePasswordApi = async (currentPassword, newPassword) => {
    return await request('/users/me/password', {
        method: 'POST',
        body: JSON.stringify({ currentPassword, newPassword })
    });
};

export const getSecurityInfoApi = async () => normalizeUser(await request('/users/me/security'));
