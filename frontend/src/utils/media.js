const ABSOLUTE_URL_PATTERN = /^(?:https?:)?\/\//i;
const SPECIAL_SCHEME_PATTERN = /^(?:data|blob):/i;

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
    return new URL(normalizedPath, window.location.origin).toString();
};
