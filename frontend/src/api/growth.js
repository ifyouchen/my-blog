/**
 * 成长与积分模块 API
 *
 * 包含：等级经验、积分账户、积分流水、签到、邀请、文章解锁、分账查询
 */
import {request} from './http';

// ─────────────────────── 等级 & 经验 ──────────────────────────────

/**
 * 查询我的成长账户（等级/经验/进度）.
 * @returns {Promise<{userId, level, levelName, exp, expToNextLevel, progressPercent}>}
 */
export const getMyGrowthApi = () => request('/growth/my');

/**
 * 查询我的经验流水.
 * @param {number} limit 最多返回条数（默认 20，最大 50）
 * @returns {Promise<Array>}
 */
export const getMyExpJournalsApi = (limit = 20) =>
    request(`/growth/my/journals?limit=${limit}`);

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

// ─────────────────────── 管理员接口 ──────────────────────────────

/**
 * 管理员查询经验规则配置.
 * @returns {Promise<Array>}
 */
export const getAdminGrowthRulesApi = () =>
    request('/admin/growth/rules');

/**
 * 管理员新增经验规则.
 * @param {{eventType: string, role: string, expAmount: number, dailyLimit: number,
 * dailyLimitStrategy: string, enabled: boolean, effectiveAt?: string, reason?: string}} payload
 * @returns {Promise<number>}
 */
export const createAdminGrowthRuleApi = (payload) =>
    request('/admin/growth/rules', {
        method: 'POST',
        body: JSON.stringify({
            ...payload,
            operator: payload.operator || 'ADMIN_UI'
        })
    });

/**
 * 管理员更新经验规则.
 * @param {{id: number, version: number, eventType: string, role: string, expAmount: number,
 * dailyLimit: number, dailyLimitStrategy: string, enabled: boolean, effectiveAt?: string, reason?: string}} payload
 * @returns {Promise<void>}
 */
export const updateAdminGrowthRuleApi = (payload) =>
    request('/admin/growth/rules', {
        method: 'PUT',
        body: JSON.stringify({
            ...payload,
            operator: payload.operator || 'ADMIN_UI'
        })
    });

/**
 * 管理员软删除经验规则.
 * @param {number} id 规则 ID
 * @param {number} version 当前版本号（乐观锁）
 * @returns {Promise<void>}
 */
export const deleteAdminGrowthRuleApi = (id, version) =>
    request(`/admin/growth/rules/${id}?version=${version}`, {
        method: 'DELETE'
    });

/**
 * 管理员查询等级阈值.
 * @returns {Promise<Array>}
 */
export const getAdminLevelThresholdsApi = () =>
    request('/admin/growth/thresholds');

/**
 * 管理员批量保存等级阈值.
 * @param {Array<{level: number, minExp: number, levelName: string, description?: string, version?: number}>} thresholds
 * @returns {Promise<void>}
 */
export const saveAdminLevelThresholdsApi = (thresholds) =>
    request('/admin/growth/thresholds', {
        method: 'PUT',
        body: JSON.stringify({
            operator: 'ADMIN_UI',
            thresholds
        })
    });

/**
 * 管理员调整用户积分.
 * @param {{ targetUserId: number, delta: number, reason: string, bizNo: string }} payload
 * @returns {Promise<{targetUserId, balanceAfter}>}
 */
export const adminAdjustPointsApi = (payload) =>
    request('/admin/points/adjust', {method: 'POST', body: JSON.stringify(payload)});

/**
 * 查询指定用户的积分账户（管理员视角）.
 * @param {number} userId
 * @returns {Promise<{userId, balance, totalEarned, totalSpent}>}
 */
export const adminGetPointAccountApi = (userId) =>
    request(`/admin/points/account?userId=${userId}`);

/**
 * 管理员分页查询指定用户积分流水.
 * @param {{ userId: number|string, sourceType?: string, page?: number, size?: number }} params
 * @returns {Promise<PageResult>}
 */
export const adminGetPointJournalsApi = (params = {}) => {
    const query = new URLSearchParams();
    query.set('userId', params.userId);
    if (params.sourceType) query.set('sourceType', params.sourceType);
    if (params.page) query.set('page', params.page);
    if (params.size) query.set('size', params.size);
    return request(`/admin/points/journals?${query.toString()}`);
};

/**
 * 管理员分页查询分账流水.
 * @param {{ authorId?: number|string, settlementStatus?: string, page?: number, size?: number }} params
 * @returns {Promise<PageResult>}
 */
export const getAdminRevenueSharesApi = (params = {}) => {
    const query = new URLSearchParams();
    if (params.authorId) query.set('authorId', params.authorId);
    if (params.settlementStatus) query.set('settlementStatus', params.settlementStatus);
    if (params.page) query.set('page', params.page);
    if (params.size) query.set('size', params.size);
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return request(`/admin/revenue-shares${suffix}`);
};

/**
 * 管理员手动重试分账结算.
 * @param {string} orderNo 解锁订单号
 * @returns {Promise<{orderNo, status, pointJournalBizNo, message}>}
 */
export const retryRevenueShareSettlementApi = (orderNo) =>
    request(`/admin/revenue-shares/${encodeURIComponent(orderNo)}/retry`, {method: 'POST'});

// ─────────────────────── 管理员接口：奖励配置 ─────────────────────

/**
 * 管理员查询等级奖励配置.
 * @returns {Promise<Array>}
 */
export const getAdminLevelRewardsApi = () =>
    request('/admin/rewards/level');

/**
 * 管理员新增等级奖励配置.
 * @param {{level: number, rewardPoints: number, rewardTitle?: string, description?: string, enabled: boolean}} payload
 * @returns {Promise<number>}
 */
export const createAdminLevelRewardApi = (payload) =>
    request('/admin/rewards/level', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

/**
 * 管理员更新等级奖励配置.
 * @param {{id: number, level: number, rewardPoints: number, rewardTitle?: string, description?: string, enabled: boolean, version: number}} payload
 * @returns {Promise<void>}
 */
export const updateAdminLevelRewardApi = (payload) =>
    request(`/admin/rewards/level/${payload.id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
    });

/**
 * 管理员软删除等级奖励配置.
 * @param {number} id 配置 ID
 * @param {number} version 当前版本
 * @returns {Promise<void>}
 */
export const deleteAdminLevelRewardApi = (id, version) =>
    request(`/admin/rewards/level/${id}?version=${version}`, {method: 'DELETE'});

/**
 * 管理员查询连续签到奖励配置.
 * @returns {Promise<Array>}
 */
export const getAdminConsecutiveRewardsApi = () =>
    request('/admin/rewards/sign-in/consecutive');

/**
 * 管理员新增连续签到奖励配置.
 * @param {{minDays: number, maxDays?: number|null, bonusPoints: number, rewardTier?: string, rewardDesc?: string, enabled: boolean}} payload
 * @returns {Promise<number>}
 */
export const createAdminConsecutiveRewardApi = (payload) =>
    request('/admin/rewards/sign-in/consecutive', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

/**
 * 管理员更新连续签到奖励配置.
 * @param {{id: number, minDays: number, maxDays?: number|null, bonusPoints: number, rewardTier?: string, rewardDesc?: string, enabled: boolean, version: number}} payload
 * @returns {Promise<void>}
 */
export const updateAdminConsecutiveRewardApi = (payload) =>
    request(`/admin/rewards/sign-in/consecutive/${payload.id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
    });

/**
 * 管理员软删除连续签到奖励配置.
 * @param {number} id 配置 ID
 * @param {number} version 当前版本
 * @returns {Promise<void>}
 */
export const deleteAdminConsecutiveRewardApi = (id, version) =>
    request(`/admin/rewards/sign-in/consecutive/${id}?version=${version}`, {method: 'DELETE'});

/**
 * 管理员查询累计签到里程碑配置.
 * @returns {Promise<Array>}
 */
export const getAdminCumulativeRewardsApi = () =>
    request('/admin/rewards/sign-in/cumulative');

/**
 * 管理员新增累计签到里程碑配置.
 * @param {{milestoneDays: number, rewardPoints: number, rewardTitle?: string, badgeCode?: string, description?: string, enabled: boolean}} payload
 * @returns {Promise<number>}
 */
export const createAdminCumulativeRewardApi = (payload) =>
    request('/admin/rewards/sign-in/cumulative', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

/**
 * 管理员更新累计签到里程碑配置.
 * @param {{id: number, milestoneDays: number, rewardPoints: number, rewardTitle?: string, badgeCode?: string, description?: string, enabled: boolean, version: number}} payload
 * @returns {Promise<void>}
 */
export const updateAdminCumulativeRewardApi = (payload) =>
    request(`/admin/rewards/sign-in/cumulative/${payload.id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
    });

/**
 * 管理员软删除累计签到里程碑配置.
 * @param {number} id 配置 ID
 * @param {number} version 当前版本
 * @returns {Promise<void>}
 */
export const deleteAdminCumulativeRewardApi = (id, version) =>
    request(`/admin/rewards/sign-in/cumulative/${id}?version=${version}`, {method: 'DELETE'});

/**
 * 管理员分页查询奖励领取记录.
 * @param {{userId?: number|string, rewardType?: string, page?: number, size?: number}} params
 * @returns {Promise<PageResult>}
 */
export const getAdminRewardGrantLogsApi = (params = {}) => {
    const query = new URLSearchParams();
    if (params.userId) query.set('userId', params.userId);
    if (params.rewardType) query.set('rewardType', params.rewardType);
    if (params.page) query.set('page', params.page);
    if (params.size) query.set('size', params.size);
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return request(`/admin/rewards/grant-logs${suffix}`);
};

export const getAdminRecommendationApplicationsApi = (params = {}) => {
    const query = new URLSearchParams();
    if (params.status) query.set('status', params.status);
    if (params.page) query.set('page', params.page);
    if (params.size) query.set('size', params.size);
    const suffix = query.toString() ? `?${query.toString()}` : '';
    return request(`/admin/recommendation-applications${suffix}`);
};

export const approveAdminRecommendationApplicationApi = (id, reviewNote = '') =>
    request(`/admin/recommendation-applications/${id}/approve`, {
        method: 'POST',
        body: { reviewNote }
    });

export const rejectAdminRecommendationApplicationApi = (id, reviewNote = '') =>
    request(`/admin/recommendation-applications/${id}/reject`, {
        method: 'POST',
        body: { reviewNote }
    });

export const getAdminAnnualCreatorCandidatesApi = () =>
    request('/admin/growth/annual-creator-candidates');

export const backfillAdminGrowthRewardsApi = ({ mode = 'ALL', userId } = {}) =>
    request('/admin/growth/rewards/backfill', {
        method: 'POST',
        body: {
            mode,
            userId
        }
    });

// ─────────────────────── 管理员接口：积分规则 ─────────────────────

/**
 * 管理员查询积分规则配置.
 * @returns {Promise<Array>}
 */
export const getAdminPointRulesApi = () =>
    request('/admin/growth/point-rules');

/**
 * 管理员新增积分规则.
 * @param {{sourceType: string, pointAmount: number, dailyLimit: number, enabled: boolean, reason?: string}} payload
 * @returns {Promise<number>}
 */
export const createAdminPointRuleApi = (payload) =>
    request('/admin/growth/point-rules', {
        method: 'POST',
        body: JSON.stringify({
            ...payload,
            operator: payload.operator || 'ADMIN_UI'
        })
    });

/**
 * 管理员更新积分规则.
 * @param {{id: number, version: number, sourceType: string, pointAmount: number, dailyLimit: number, enabled: boolean, reason?: string}} payload
 * @returns {Promise<void>}
 */
export const updateAdminPointRuleApi = (payload) =>
    request(`/admin/growth/point-rules/${payload.id}`, {
        method: 'PUT',
        body: JSON.stringify({
            ...payload,
            operator: payload.operator || 'ADMIN_UI'
        })
    });

/**
 * 管理员软删除积分规则.
 * @param {number} id 规则 ID
 * @param {number} version 当前版本号（乐观锁）
 * @returns {Promise<void>}
 */
export const deleteAdminPointRuleApi = (id, version) =>
    request(`/admin/growth/point-rules/${id}?version=${version}`, {
        method: 'DELETE'
    });

