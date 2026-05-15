/**
 * 成长与积分 Pinia Store
 *
 * 管理积分账户、签到状态、文章解锁状态等响应式数据。
 */
import {reactive, readonly} from 'vue';
import {getPointAccountApi, getSignInCalendarApi, getUnlockStatusApi, signInApi, unlockArticleApi,} from '@/api/growth';

// ─────────────────────── 状态 ─────────────────────────────────────

const state = reactive({
    /** 积分账户信息 */
    account: null,
    /** 账户加载状态 */
    accountLoading: false,
    /** 账户加载错误 */
    accountError: null,

    /** 签到日历（按月缓存，key=yyyy-MM） */
    calendarCache: {},
    /** 签到操作进行中 */
    signingIn: false,

    /** 文章解锁状态缓存（key=articleId） */
    unlockStatusCache: {},
    /** 解锁操作进行中（key=articleId） */
    unlockingMap: {},
});

// ─────────────────────── Actions ──────────────────────────────────

/**
 * 加载积分账户信息.
 */
const loadAccount = async () => {
    if (state.accountLoading) return;
    state.accountLoading = true;
    state.accountError = null;
    try {
        state.account = await getPointAccountApi();
    } catch (err) {
        state.accountError = err?.message || '加载积分账户失败';
    } finally {
        state.accountLoading = false;
    }
};

/**
 * 执行签到.
 * @returns {Promise<SignInResponse>} 签到结果
 */
const signIn = async () => {
    if (state.signingIn) return null;
    state.signingIn = true;
    try {
        const result = await signInApi();
        // 签到成功后刷新积分账户
        await loadAccount();
        // 清除当月日历缓存，下次查询时重新加载
        const monthKey = new Date().toISOString().slice(0, 7); // yyyy-MM
        delete state.calendarCache[monthKey];
        return result;
    } finally {
        state.signingIn = false;
    }
};

/**
 * 查询签到日历（带缓存）.
 * @param {string} month yyyy-MM 格式，默认当月
 * @returns {Promise<SignInCalendarResponse>}
 */
const getCalendar = async (month) => {
    const key = month || new Date().toISOString().slice(0, 7);
    if (state.calendarCache[key]) {
        return state.calendarCache[key];
    }
    const data = await getSignInCalendarApi(month);
    state.calendarCache[key] = data;
    return data;
};

/**
 * 查询文章解锁状态（带缓存）.
 * @param {number|string} articleId 文章 ID
 * @returns {Promise<UnlockStatusResponse>}
 */
const fetchUnlockStatus = async (articleId) => {
    if (state.unlockStatusCache[articleId]) {
        return state.unlockStatusCache[articleId];
    }
    const data = await getUnlockStatusApi(articleId);
    state.unlockStatusCache[articleId] = data;
    return data;
};

/**
 * 解锁文章.
 * @param {number|string} articleId 文章 ID
 * @returns {Promise<UnlockResult>}
 */
const unlockArticle = async (articleId) => {
    if (state.unlockingMap[articleId]) return null;
    state.unlockingMap[articleId] = true;
    try {
        const result = await unlockArticleApi(articleId);
        // 解锁成功后更新缓存
        if (result?.unlocked) {
            state.unlockStatusCache[articleId] = {
                ...state.unlockStatusCache[articleId],
                unlocked: true,
                currentBalance: result.balanceAfter,
            };
            if (state.account) {
                state.account.balance = result.balanceAfter;
            }
        }
        return result;
    } finally {
        delete state.unlockingMap[articleId];
    }
};

/**
 * 使文章解锁状态缓存失效（解锁后或刷新时）.
 * @param {number|string} articleId 文章 ID
 */
const invalidateUnlockCache = (articleId) => {
    delete state.unlockStatusCache[articleId];
};

// ─────────────────────── 导出 ─────────────────────────────────────

export const useGrowthStore = () => ({
    state: readonly(state),
    loadAccount,
    signIn,
    getCalendar,
    fetchUnlockStatus,
    unlockArticle,
    invalidateUnlockCache,
});

