import {request} from './http';

/**
 * 查询广告位当前生效的广告列表。
 * @param {string} slot 广告位编码，如 home_sidebar
 */
export const getAdsApi = async (slot) => {
    return await request(`/ads?slot=${encodeURIComponent(slot)}`);
};

/**
 * 记录广告曝光事件。
 * @param {number} campaignId 广告 ID
 */
export const recordAdImpressionApi = async (campaignId) => {
    return await request(`/ads/${campaignId}/impression`, { method: 'POST' });
};

/**
 * 记录广告点击事件。
 * @param {number} campaignId 广告 ID
 */
export const recordAdClickApi = async (campaignId) => {
    return await request(`/ads/${campaignId}/click`, { method: 'POST' });
};

// ========================== 后台管理接口 ==========================

/**
 * 后台分页查询广告列表。
 */
export const getAdminAdsApi = async (page = 1, pageSize = 10, params = {}) => {
    const query = new URLSearchParams({ page, pageSize });
    if (params.slotCode) {
        query.set('slotCode', params.slotCode);
    }
    if (params.enabled !== undefined && params.enabled !== null && params.enabled !== '') {
        query.set('enabled', params.enabled);
    }
    return await request(`/admin/ads?${query.toString()}`);
};

/**
 * 后台获取广告详情。
 */
export const getAdminAdApi = async (id) => {
    return await request(`/admin/ads/${id}`);
};

/**
 * 后台创建广告。
 */
export const createAdminAdApi = async (params) => {
    const query = new URLSearchParams();
    query.set('slotCode', params.slotCode || '');
    query.set('title', params.title || '');
    if (params.imageUrl) {
        query.set('imageUrl', params.imageUrl);
    }
    query.set('targetUrl', params.targetUrl || '');
    query.set('label', params.label || '广告');
    if (params.startAt) {
        query.set('startAt', params.startAt);
    }
    if (params.endAt) {
        query.set('endAt', params.endAt);
    }
    query.set('enabled', params.enabled !== undefined ? String(params.enabled) : 'true');
    query.set('sortOrder', params.sortOrder !== undefined ? String(params.sortOrder) : '0');
    return await request(`/admin/ads?${query.toString()}`, { method: 'POST' });
};

/**
 * 后台更新广告。
 */
export const updateAdminAdApi = async (id, params) => {
    const query = new URLSearchParams();
    query.set('title', params.title || '');
    if (params.imageUrl !== undefined) {
        query.set('imageUrl', params.imageUrl || '');
    }
    query.set('targetUrl', params.targetUrl || '');
    query.set('label', params.label || '广告');
    if (params.startAt) {
        query.set('startAt', params.startAt);
    }
    if (params.endAt) {
        query.set('endAt', params.endAt);
    }
    if (params.enabled !== undefined) {
        query.set('enabled', String(params.enabled));
    }
    if (params.sortOrder !== undefined) {
        query.set('sortOrder', String(params.sortOrder));
    }
    return await request(`/admin/ads/${id}?${query.toString()}`, { method: 'PUT' });
};

/**
 * 后台删除广告。
 */
export const deleteAdminAdApi = async (id) => {
    return await request(`/admin/ads/${id}`, { method: 'DELETE' });
};

