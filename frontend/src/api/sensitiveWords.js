import {request} from './http';

export const getWarnSensitiveWordsApi = async () => {
    const data = await request('/sensitive-words/warn');
    return Array.isArray(data) ? data : [];
};
