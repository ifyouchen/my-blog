const ABSOLUTE_URL_PATTERN = /^(?:https?:)?\/\//i;
const SPECIAL_SCHEME_PATTERN = /^(?:data|blob):/i;
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
export const DEFAULT_ARTICLE_COVER_URL = '/api/uploads/files/default/article-cover.svg';

const resolveApiOrigin = () => {
    if (!ABSOLUTE_URL_PATTERN.test(API_BASE_URL)) {
        return '';
    }
    try {
        return new URL(API_BASE_URL).origin;
    } catch (error) {
        return '';
    }
};

export const resolveMediaUrl = (source, fallback = '') => {
    const value = String(source || '').trim();
    if (!value) {
        return fallback;
    }
    if (ABSOLUTE_URL_PATTERN.test(value) || SPECIAL_SCHEME_PATTERN.test(value)) {
        return value;
    }
    if (typeof window === 'undefined') {
        return value;
    }
    const normalizedPath = value.startsWith('/') ? value : `/${value}`;
    if (normalizedPath.startsWith('/api/')) {
        const apiOrigin = resolveApiOrigin();
        if (apiOrigin) {
            return new URL(normalizedPath, apiOrigin).toString();
        }
    }
    return new URL(normalizedPath, window.location.origin).toString();
};
