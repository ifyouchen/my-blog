const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const SESSION_KEY = 'my-blog-session';

const getToken = () => {
    try {
        const raw = localStorage.getItem(SESSION_KEY);
        const session = raw ? JSON.parse(raw) : null;
        if (!session || !session.token || session.token === 'local-dev-token') {
            return '';
        }
        return session.token;
    } catch (error) {
        return '';
    }
};

export const request = async (path, options = {}) => {
    const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
    const headers = {
        ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
        ...(options.headers || {})
    };
    const token = getToken();

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const body = headers['Content-Type'] === 'application/json'
        && options.body
        && typeof options.body !== 'string'
        ? JSON.stringify(options.body)
        : options.body;

    const url = path.startsWith('http') ? path : `${API_BASE_URL}${path}`;
    const response = await fetch(url, {
        ...options,
        body,
        headers
    });
    let payload;
    try {
        payload = await response.json();
    } catch (error) {
        throw new TypeError('Failed to fetch');
    }

    if (!response.ok) {
        if (response.status === 401) {
            localStorage.removeItem(SESSION_KEY);
            window.dispatchEvent(new CustomEvent('my-blog-auth-expired'));
        }
        throw new Error(payload.message || `请求失败 (${response.status})`);
    }

    if (payload.code !== 0) {
        if (payload.code === 401) {
            localStorage.removeItem(SESSION_KEY);
            window.dispatchEvent(new CustomEvent('my-blog-auth-expired'));
        }
        throw new Error(payload.message || '请求失败');
    }

    return payload.data;
};
