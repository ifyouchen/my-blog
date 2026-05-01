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

/**
 * 关闭广告（用户维度，3天有效）。
 * @param {number} campaignId 广告 ID
 */
export const dismissAdApi = async (campaignId) => {
    return await request(`/ads/${campaignId}/dismiss`, { method: 'POST' });
};

/**
 * 获取当前用户 3 天内关闭过的广告 ID 列表。
 */
export const getDismissedAdIdsApi = async () => {
    return await request('/ads/dismissed-ids');
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
    return await request('/admin/ads', {
        method: 'POST',
        body: {
            slotCode: params.slotCode || '',
            title: params.title || '',
            imageUrl: params.imageUrl || '',
            targetUrl: params.targetUrl || '',
            label: params.label || '广告',
            startAt: params.startAt || null,
            endAt: params.endAt || null,
            enabled: params.enabled !== undefined ? params.enabled : true,
            sortOrder: params.sortOrder !== undefined ? params.sortOrder : 0
        }
    });
};

/**
 * 后台更新广告。
 */
export const updateAdminAdApi = async (id, params) => {
    return await request(`/admin/ads/${id}`, {
        method: 'PUT',
        body: {
            slotCode: params.slotCode || 'home_sidebar',
            title: params.title || '',
            imageUrl: params.imageUrl !== undefined ? params.imageUrl : '',
            targetUrl: params.targetUrl || '',
            label: params.label || '广告',
            startAt: params.startAt || null,
            endAt: params.endAt || null,
            enabled: params.enabled !== undefined ? params.enabled : null,
            sortOrder: params.sortOrder !== undefined ? params.sortOrder : null
        }
    });
};

/**
 * 后台删除广告。
 */
export const deleteAdminAdApi = async (id) => {
    return await request(`/admin/ads/${id}`, { method: 'DELETE' });
};

/**
 * 后台获取广告统计概览。
 */
export const getAdminAdStatsApi = async () => {
    return await request('/admin/ads/stats');
};

