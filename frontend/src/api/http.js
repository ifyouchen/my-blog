const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const SESSION_KEY = 'my-blog-session';

const getToken = () => {
    try {
        const raw = localStorage.getItem(SESSION_KEY);
        const session = raw ? JSON.parse(raw) : null;
        return session ? session.token : '';
    } catch (error) {
        return '';
    }
};

export const request = async (path, options = {}) => {
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };
    const token = getToken();

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers
    });
    let payload;
    try {
        payload = await response.json();
    } catch (error) {
        throw new TypeError('Failed to fetch');
    }

    if (!response.ok || payload.code !== 0) {
        throw new Error(payload.message || '请求失败');
    }

    return payload.data;
};
