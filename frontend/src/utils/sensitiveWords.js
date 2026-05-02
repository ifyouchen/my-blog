import {getWarnSensitiveWordsApi} from '@/api/sensitiveWords';

const WARN_WORD_CACHE_KEY = 'my-blog-sensitive-warn-words';
const WARN_WORD_CACHE_TTL_MS = 5 * 60 * 1000;

const normalizeWords = (words) => {
    const seen = new Set();
    const result = [];
    for (const word of words || []) {
        const value = String(word || '').trim();
        if (!value) {
            continue;
        }
        const key = value.toLowerCase();
        if (seen.has(key)) {
            continue;
        }
        seen.add(key);
        result.push(value);
    }
    return result.sort((left, right) => right.length - left.length);
};

const readWarnWordCache = (allowExpired = false) => {
    if (typeof localStorage === 'undefined') {
        return null;
    }
    try {
        const raw = localStorage.getItem(WARN_WORD_CACHE_KEY);
        const cached = raw ? JSON.parse(raw) : null;
        if (!cached || !Array.isArray(cached.words) || !cached.expiresAt) {
            return null;
        }
        if (!allowExpired && Date.now() > cached.expiresAt) {
            return null;
        }
        return normalizeWords(cached.words);
    } catch (error) {
        return null;
    }
};

const writeWarnWordCache = (words) => {
    if (typeof localStorage === 'undefined') {
        return;
    }
    try {
        localStorage.setItem(WARN_WORD_CACHE_KEY, JSON.stringify({
            words,
            expiresAt: Date.now() + WARN_WORD_CACHE_TTL_MS
        }));
    } catch (error) {
        // 缓存失败不影响提交，服务端仍会兜底处理敏感词。
    }
};

export const getCachedWarnSensitiveWords = async () => {
    const cached = readWarnWordCache();
    if (cached) {
        return cached;
    }
    try {
        const words = normalizeWords(await getWarnSensitiveWordsApi());
        writeWarnWordCache(words);
        return words;
    } catch (error) {
        return readWarnWordCache(true) || [];
    }
};

export const findWarnSensitiveWords = async (text) => {
    const source = String(text || '').toLowerCase();
    if (!source) {
        return [];
    }
    const words = await getCachedWarnSensitiveWords();
    return words.filter((word) => source.includes(word.toLowerCase()));
};

export const findArticleWarnSensitiveWords = async (draft) => {
    const source = [
        draft?.title,
        draft?.summary,
        draft?.content
    ].map((item) => String(item || '')).join('\n');
    return findWarnSensitiveWords(source);
};

export const formatWarnSensitiveWords = (hits, limit = 8) => {
    const words = normalizeWords(hits);
    if (words.length <= limit) {
        return words.join('、');
    }
    return `${words.slice(0, limit).join('、')} 等 ${words.length} 个`;
};
