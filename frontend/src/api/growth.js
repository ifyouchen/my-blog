/**
 * 成长与积分模块 API
 *
 * 包含：积分账户、积分流水、签到、邀请、文章解锁、分账查询
 */
import {request} from './http';

// ─────────────────────── 积分账户 ────────────────────────────────

/**
 * 查询我的积分账户.
 * @returns {Promise<{userId, balance, totalEarned, totalSpent}>}
 */
export const getPointAccountApi = () => request('/points/account');

/**
 * 分页查询积分流水.
 * @param {{ sourceType?: string, page?: number, size?: number }} params
 * @returns {Promise<PageResult>}
 */
export const getPointJournalsApi = (params = {}) => {
    const query = new URLSearchParams();
    if (params.sourceType) query.set('sourceType', params.sourceType);
    if (params.page) query.set('page', params.page);
    if (params.size) query.set('size', params.size);
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return request(`/points/journals${suffix}`);
};

// ─────────────────────── 签到 ────────────────────────────────────

/**
 * 执行签到.
 * @returns {Promise<SignInResponse>}
 */
export const signInApi = () =>
    request('/points/sign-in', { method: 'POST' });

/**
 * 查询签到日历.
 * @param {string} month 格式 yyyy-MM，默认当月
 * @returns {Promise<SignInCalendarResponse>}
 */
export const getSignInCalendarApi = (month) => {
    const suffix = month ? `?month=${month}` : '';
    return request(`/points/sign-in/calendar${suffix}`);
};

// ─────────────────────── 邀请 ────────────────────────────────────

/**
 * 查询我的邀请汇总.
 * @returns {Promise<{inviterUserId, totalGrantedCount, totalPointsEarned}>}
 */
export const getInviteSummaryApi = () => request('/points/invite/summary');

// ─────────────────────── 文章解锁 ────────────────────────────────

/**
 * 解锁文章.
 * @param {number|string} articleId 文章 ID
 * @returns {Promise<{articleId, unlocked, pointsCost, balanceAfter, orderNo, message?}>}
 */
export const unlockArticleApi = (articleId) =>
    request(`/articles/${articleId}/unlock`, { method: 'POST' });

/**
 * 查询文章解锁状态.
 * @param {number|string} articleId 文章 ID
 * @returns {Promise<{articleId, needUnlock, unlockPointPrice, unlocked, currentBalance}>}
 */
export const getUnlockStatusApi = (articleId) =>
    request(`/articles/${articleId}/unlock-status`);

// ─────────────────────── 分账收益 ────────────────────────────────

/**
 * 查询我的分账流水（作者视角）.
 * @param {{ page?: number, size?: number }} params
 * @returns {Promise<PageResult>}
 */
export const getMyRevenueApi = (params = {}) => {
    const query = new URLSearchParams();
    if (params.page) query.set('page', params.page);
    if (params.size) query.set('size', params.size);
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return request(`/revenue/my${suffix}`);
};

