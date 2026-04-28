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

const logDevTrace = (type, detail = {}) => {
    if (!import.meta.env.DEV) {
        return;
    }

    const { traceId = '', path = '', status = '', message = '' } = detail;
    const segments = [`[request:${type}]`];

    if (path) {
        segments.push(path);
    }
    if (status) {
        segments.push(`status=${status}`);
    }
    if (traceId) {
        segments.push(`traceId=${traceId}`);
    }
    if (message) {
        segments.push(message);
    }

    console.error(segments.join(' | '));
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
    const traceId = response.headers.get('X-Trace-Id') || '';
    let payload;
    try {
        payload = await response.json();
    } catch (error) {
        const parseError = new TypeError('Failed to fetch');
        if (traceId) {
            parseError.traceId = traceId;
        }
        logDevTrace('parse-error', {
            traceId,
            path,
            status: response.status,
            message: parseError.message
        });
        throw parseError;
    }

    if (!response.ok) {
        if (response.status === 401) {
            localStorage.removeItem(SESSION_KEY);
            window.dispatchEvent(new CustomEvent('my-blog-auth-expired'));
        }
        const requestError = new Error(payload.message || `请求失败 (${response.status})`);
        if (traceId) {
            requestError.traceId = traceId;
        }
        logDevTrace('http-error', {
            traceId,
            path,
            status: response.status,
            message: requestError.message
        });
        throw requestError;
    }

    if (payload.code !== 0) {
        if (payload.code === 401) {
            localStorage.removeItem(SESSION_KEY);
            window.dispatchEvent(new CustomEvent('my-blog-auth-expired'));
        }
        const businessError = new Error(payload.message || '请求失败');
        if (traceId) {
            businessError.traceId = traceId;
        }
        logDevTrace('business-error', {
            traceId,
            path,
            status: response.status,
            message: businessError.message
        });
        throw businessError;
    }

    return payload.data;
};
