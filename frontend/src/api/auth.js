import { request } from './http';
import { normalizeUser } from './transformers';

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
