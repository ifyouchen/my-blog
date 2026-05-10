import {request} from './http';

export const updateLearningProgressApi = async ({ assetType, assetId, articleId, completed = true }) => {
    return request('/learning/progress', {
        method: 'POST',
        body: JSON.stringify({ assetType, assetId, articleId, completed })
    });
};
