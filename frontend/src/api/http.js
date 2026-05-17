const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const SESSION_KEY = 'my-blog-session';

export const getToken = () => {
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

const clearSession = () => {
    localStorage.removeItem(SESSION_KEY);
    window.dispatchEvent(new CustomEvent('my-blog-auth-expired'));
};

const eventCooldowns = {};
const dispatchWithCooldown = (eventName, detail, cooldownMs = 1500) => {
    const now = Date.now();
    if (eventCooldowns[eventName] && now - eventCooldowns[eventName] < cooldownMs) {
        return;
    }
    eventCooldowns[eventName] = now;
    window.dispatchEvent(new CustomEvent(eventName, { detail }));
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

const resolveUrl = (path) => path.startsWith('http') ? path : `${API_BASE_URL}${path}`;
const shouldSuppressAuthPrompt = (path) => String(path || '').startsWith('/auth/');

const parseDownloadFilename = (disposition, fallbackFilename) => {
    if (!disposition) {
        return fallbackFilename;
    }

    const encodedMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (encodedMatch?.[1]) {
        try {
            return decodeURIComponent(encodedMatch[1].trim());
        } catch (error) {
            return encodedMatch[1].trim();
        }
    }

    const normalMatch = disposition.match(/filename="?([^";]+)"?/i);
    return normalMatch?.[1]?.trim() || fallbackFilename;
};

const saveBlob = (blob, filename) => {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
};

const createRequestError = (payload, response, traceId, fallbackMessage, options = {}) => {
    const authFailed = payload?.code === 401 || response.status === 401;
    if (authFailed) {
        clearSession();
        if (!options.suppressAuthPrompt) {
            const message = payload?.message || '登录已过期，请重新登录';
            dispatchWithCooldown('my-blog-auth-required', { message });
        }
    }
    if (payload?.code === 403 || response.status === 403) {
        const message = payload?.message || '无权限访问';
        dispatchWithCooldown('my-blog-forbidden', { message });
    }
    const message = payload?.code !== 0 && payload?.message
        ? payload.message
        : (fallbackMessage || `请求失败 (${response.status})`);
    const requestError = new Error(message);
    if (traceId) {
        requestError.traceId = traceId;
    }
    return requestError;
};

export const request = async (path, options = {}) => {
    const { suppressAuthPrompt = false, timeout = 15000, ...fetchOptions } = options;
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

    const url = resolveUrl(path);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    let response;
    try {
        response = await fetch(url, {
            ...fetchOptions,
            body,
            headers,
            signal: controller.signal
        });
    } catch (error) {
        clearTimeout(timeoutId);
        if (error.name === 'AbortError') {
            throw new Error(`请求超时（${timeout / 1000}s），请检查网络连接`);
        }
        throw error;
    }
    clearTimeout(timeoutId);

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
        const requestError = createRequestError(payload, response, traceId, undefined, {
            suppressAuthPrompt: suppressAuthPrompt || shouldSuppressAuthPrompt(path)
        });
        logDevTrace('http-error', {
            traceId,
            path,
            status: response.status,
            message: requestError.message
        });
        throw requestError;
    }

    if (payload.code !== 0) {
        const businessError = createRequestError(payload, response, traceId, '请求失败', {
            suppressAuthPrompt: suppressAuthPrompt || shouldSuppressAuthPrompt(path)
        });
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

export const downloadFile = async (path, fallbackFilename = 'download') => {
    const headers = {};
    const token = getToken();

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(resolveUrl(path), { headers });
    const traceId = response.headers.get('X-Trace-Id') || '';
    const contentType = response.headers.get('Content-Type') || '';

    if (contentType.includes('application/json')) {
        let payload = null;
        try {
            payload = await response.json();
        } catch (error) {
            throw createRequestError(null, response, traceId, '下载失败');
        }

        if (!response.ok || payload.code !== 0) {
            throw createRequestError(payload, response, traceId, '下载失败');
        }
        throw createRequestError(payload, response, traceId, '导出接口未返回文件');
    }

    if (!response.ok) {
        throw createRequestError(null, response, traceId, '下载失败');
    }

    const blob = await response.blob();
    const filename = parseDownloadFilename(
        response.headers.get('Content-Disposition'),
        fallbackFilename
    );
    saveBlob(blob, filename);
};
