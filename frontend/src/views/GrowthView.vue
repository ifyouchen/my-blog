<script setup>
import {computed, onMounted, ref} from 'vue';
import {useHead} from '@unhead/vue';
import {RouterLink} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import CreatorSidebar from '@/components/CreatorSidebar.vue';
import BadgePicker from '@/components/BadgePicker.vue';
import BadgeWall from '@/components/BadgeWall.vue';
import UserEquippedBadge from '@/components/UserEquippedBadge.vue';
import {
  equipMyBadgeApi,
  getMyExpJournalsApi,
  getMyBadgesApi,
  getMyGrowthApi,
  getMyRevenueApi,
  getPointAccountApi,
  getPointJournalsApi,
  getSignInCalendarApi,
  signInApi,
} from '@/api/growth';
import {useSession} from '@/stores/session';

useHead({title: '经验积分 - DevNotes'});

const {state: session, updateCurrentUser} = useSession();

// ── 成长账户（等级 / 经验） ──────────────────────────────────────
const growth = ref(null);
const growthLoading = ref(false);
const growthError = ref('');

const loadGrowth = async () => {
    growthLoading.value = true;
    growthError.value = '';
    try {
        growth.value = await getMyGrowthApi();
    } catch (e) {
        growthError.value = e.message || '加载失败';
    } finally {
        growthLoading.value = false;
    }
};

// ── 积分账户 ─────────────────────────────────────────────────────
const pointAccount = ref(null);
const accountLoading = ref(false);

const loadAccount = async () => {
    accountLoading.value = true;
    try {
        pointAccount.value = await getPointAccountApi();
    } catch {
        // ignore
    } finally {
        accountLoading.value = false;
    }
};

// ── 签到 ─────────────────────────────────────────────────────────
const signInResult = ref(null);
const signingIn = ref(false);
const signInError = ref('');
const formatLocalDate = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};
const formatLocalMonth = (date) => formatLocalDate(date).slice(0, 7);
const shiftMonth = (monthValue, delta) => {
    const [year, month] = monthValue.split('-').map(Number);
    const date = new Date(year, month - 1, 1);
    date.setMonth(date.getMonth() + delta);
    return formatLocalMonth(date);
};
const calendarMonth = ref(formatLocalMonth(new Date())); // yyyy-MM
const calendarData = ref({});
const calendarLoading = ref(false);
const calendarInitialLoading = computed(() => calendarLoading.value && !calendarData.value[calendarMonth.value]);

const signedDates = computed(() => {
    const currentCalendar = calendarData.value[calendarMonth.value];
    if (!currentCalendar?.signedDates) return new Set();
    return new Set(currentCalendar.signedDates);
});

const todayStr = computed(() => formatLocalDate(new Date())); // yyyy-MM-dd
const todaySigned = computed(() => signedDates.value.has(todayStr.value));
const isCurrentMonth = computed(() => calendarMonth.value === formatLocalMonth(new Date()));
const canSignInToday = computed(() => isCurrentMonth.value && !calendarLoading.value && !todaySigned.value);

const calendarDays = computed(() => {
    const [year, month] = calendarMonth.value.split('-').map(Number);
    const firstDay = new Date(year, month - 1, 1).getDay(); // 0=Sun
    const daysInMonth = new Date(year, month, 0).getDate();
    const days = [];
    // 填充前置空白格
    for (let i = 0; i < firstDay; i++) {
        days.push(null);
    }
    for (let d = 1; d <= daysInMonth; d++) {
        const dateStr = `${calendarMonth.value}-${String(d).padStart(2, '0')}`;
        days.push({
            date: d,
            dateStr,
            signed: signedDates.value.has(dateStr),
            isToday: dateStr === todayStr.value,
        });
    }
    while (days.length < 42) {
        days.push(null);
    }
    return days;
});

const loadCalendar = async (month) => {
    calendarLoading.value = true;
    try {
        const result = await getSignInCalendarApi(month);
        calendarData.value = {
            ...calendarData.value,
            [month]: result
        };
    } catch {
        // ignore
    } finally {
        calendarLoading.value = false;
    }
};

const prevMonth = () => {
    calendarMonth.value = shiftMonth(calendarMonth.value, -1);
    loadCalendar(calendarMonth.value);
};

const nextMonth = () => {
    const next = shiftMonth(calendarMonth.value, 1);
    if (next <= formatLocalMonth(new Date())) {
        calendarMonth.value = next;
        loadCalendar(calendarMonth.value);
    }
};

const doSignIn = async () => {
    if (signingIn.value || !canSignInToday.value) return;
    signingIn.value = true;
    signInError.value = '';
    try {
        signInResult.value = await signInApi();
        await Promise.all([loadAccount(), loadCalendar(calendarMonth.value)]);
    } catch (e) {
        signInError.value = e.message || '签到失败，请稍后重试';
    } finally {
        signingIn.value = false;
    }
};

// ── 积分流水 ─────────────────────────────────────────────────────
const journalTab = ref('points'); // 'points' | 'exp' | 'revenue'
const journalDisplayTab = ref('points');
const journals = ref([]);
const journalTotal = ref(0);
const journalPage = ref(1);
const journalSize = 15;
const journalLoading = ref(false);
const journalPendingTab = ref('');
const journalError = ref('');
const journalCache = ref({});

const SOURCE_TYPE_LABELS = {
    REGISTER_BONUS: '注册奖励',
    SIGN_IN: '每日签到',
    RECHARGE: '积分充值',
    INVITE: '邀请奖励',
    INVITE_REWARD: '邀请奖励',
    UNLOCK: '文章解锁',
    ARTICLE_UNLOCK: '文章解锁',
    ADMIN_ADJUST: '管理员调整',
    PUBLISH: '发布文章',
    COMMENT: '发表评论',
    READ: '阅读文章',
    FAVORITE: '收藏文章',
    SHARE: '分享文章',
    FOLLOW: '关注用户',
    LIKE: '点赞文章',
    LEVEL_UP: '等级升级',
    LIKE_RECEIVED: '获得点赞',
    FOLLOW_RECEIVED: '获得关注',
    REVENUE_SHARE: '文章解锁分成',
};

const sourceLabel = (type) => SOURCE_TYPE_LABELS[type] || type || '-';

const ARTICLE_POINT_SOURCE_TYPES = ['UNLOCK', 'ARTICLE_UNLOCK', 'REVENUE_SHARE'];

const AUTHOR_EVENT_LABELS = {
    COMMENT: '评论你的文章',
    READ: '阅读你的文章',
    FAVORITE: '收藏你的文章',
    SHARE: '分享你的文章',
    LIKE: '点赞你的文章',
    FOLLOW: '关注你',
    PUBLISH: '作为作者发布文章',
};

const hasArticleContext = (item) =>
    item?.articleId && ARTICLE_POINT_SOURCE_TYPES.includes(item.sourceType);

const articleDisplayTitle = (item) => item.articleTitle || '文章已不可用';

const pointJournalArticleLabel = (item) => {
    if (!hasArticleContext(item)) {
        return '';
    }
    return `${sourceLabel(item.sourceType)}[${item.articleId}-${articleDisplayTitle(item)}]`;
};

const revenueArticleLabel = (item) => {
    if (!item?.articleId) {
        return '-';
    }
    return `[${item.articleId}-${articleDisplayTitle(item)}]`;
};

const articleRoute = (item) => `/articles/${item.articleId}`;

const expJournalDesc = (item) => {
    const role = item.grantRole;
    if (role === 'AUTHOR') {
        const eventLabel = AUTHOR_EVENT_LABELS[item.eventType] || sourceLabel(item.eventType);
        return `${item.grantRoleLabel || '别人互动后你获得'} · ${eventLabel}`;
    }
    if (role === 'ACTOR') {
        return `${item.grantRoleLabel || '你操作获得'} · ${sourceLabel(item.eventType)}`;
    }
    return item.remark || item.description || sourceLabel(item.eventType);
};

const formatShareRatio = (ratio) => {
    if (!ratio) {
        return '-';
    }
    const parts = String(ratio).split(':');
    if (parts.length === 2) {
        return `平台 ${parts[0]}% / 作者 ${parts[1]}%`;
    }
    return ratio;
};

const REVENUE_STATUS_META = {
    PENDING: { label: '待结算', className: 'pending' },
    SETTLED: { label: '已入账', className: 'settled' },
    FAILED: { label: '失败待处理', className: 'failed' },
};

const revenueStatusMeta = (status) => REVENUE_STATUS_META[status] || {
    label: status || '待结算',
    className: 'pending',
};

const getJournalCacheKey = (tab = journalTab.value, page = journalPage.value) => `${tab}:${page}`;

const applyJournalData = (tab, page, items, total) => {
    journalDisplayTab.value = tab;
    journalPage.value = page;
    journals.value = items;
    journalTotal.value = total;
};

// ── 徽章 ─────────────────────────────────────────────────────────
const badgeState = ref({
    badges: [],
    equippedBadge: null
});
const badgeLoading = ref(false);
const badgeSaving = ref(false);
const badgeError = ref('');

const loadBadges = async () => {
    badgeLoading.value = true;
    badgeError.value = '';
    try {
        const result = await getMyBadgesApi();
        badgeState.value = {
            badges: result?.badges || [],
            equippedBadge: result?.equippedBadge || null
        };
    } catch (e) {
        badgeError.value = e.message || '徽章加载失败';
    } finally {
        badgeLoading.value = false;
    }
};

const syncSessionEquippedBadge = (equippedBadge) => {
    if (!session.user) {
        return;
    }
    updateCurrentUser({
        ...session.user,
        equippedBadge: equippedBadge || null
    });
};

const equipBadge = async (badgeCode) => {
    if (badgeSaving.value) {
        return;
    }
    badgeSaving.value = true;
    badgeError.value = '';
    try {
        const result = await equipMyBadgeApi(badgeCode);
        badgeState.value = {
            badges: result?.badges || [],
            equippedBadge: result?.equippedBadge || null
        };
        syncSessionEquippedBadge(badgeState.value.equippedBadge);
    } catch (e) {
        badgeError.value = e.message || '佩戴徽章失败';
    } finally {
        badgeSaving.value = false;
    }
};

const fetchJournalData = async (tab, page) => {
    let nextItems = [];
    let nextTotal = 0;
    if (tab === 'points') {
        const result = await getPointJournalsApi({page, size: journalSize});
        nextItems = result.items || result.records || result || [];
        nextTotal = result.total || nextItems.length;
    } else if (tab === 'exp') {
        const data = await getMyExpJournalsApi(50);
        nextItems = Array.isArray(data) ? data : [];
        nextTotal = nextItems.length;
    } else if (tab === 'revenue') {
        const result = await getMyRevenueApi({page, size: journalSize});
        nextItems = result.items || result.records || result || [];
        nextTotal = result.total || nextItems.length;
    }
    return {items: nextItems, total: nextTotal};
};

const loadJournals = async (options = {}) => {
    const {
        force = false,
        tab = journalTab.value,
        page = journalPage.value,
        background = false,
    } = options;
    const requestedTab = tab;
    const requestedPage = page;
    const cacheKey = getJournalCacheKey(requestedTab, requestedPage);
    if (!force && journalCache.value[cacheKey]) {
        const cached = journalCache.value[cacheKey];
        if (!background) {
            applyJournalData(requestedTab, requestedPage, cached.items, cached.total);
            journalError.value = '';
            journalLoading.value = false;
            journalPendingTab.value = '';
        }
        return;
    }
    if (!background) {
        journalLoading.value = true;
        journalPendingTab.value = requestedTab;
        journalError.value = '';
    }
    try {
        const {items: nextItems, total: nextTotal} = await fetchJournalData(requestedTab, requestedPage);
        journalCache.value = {
            ...journalCache.value,
            [cacheKey]: {items: nextItems, total: nextTotal}
        };
        if (!background && journalTab.value === requestedTab) {
            applyJournalData(requestedTab, requestedPage, nextItems, nextTotal);
        }
    } catch (e) {
        if (!background && journalTab.value === requestedTab) {
            journalError.value = e.message || '加载失败';
            applyJournalData(requestedTab, requestedPage, [], 0);
        }
    } finally {
        if (!background && journalPendingTab.value === requestedTab) {
            journalLoading.value = false;
            journalPendingTab.value = '';
        }
    }
};

const switchTab = (tab) => {
    if (journalTab.value === tab && !journalLoading.value) return;
    journalTab.value = tab;
    loadJournals({tab, page: 1});
};

const warmJournalCache = () => {
    ['exp', 'revenue'].forEach((tab) => {
        loadJournals({tab, page: 1, background: true}).catch(() => {});
    });
};

const journalTotalPages = computed(() => Math.ceil(journalTotal.value / journalSize) || 1);
const journalInitialLoading = computed(() =>
    journalLoading.value && journalDisplayTab.value === journalTab.value && journals.value.length === 0
);

const prevPage = () => {
    if (journalPage.value > 1 && !journalLoading.value) {
        loadJournals({tab: journalTab.value, page: journalPage.value - 1});
    }
};

const nextPage = () => {
    if (journalPage.value < journalTotalPages.value && !journalLoading.value) {
        loadJournals({tab: journalTab.value, page: journalPage.value + 1});
    }
};

// ── 格式化 ───────────────────────────────────────────────────────
const fmtTime = (t) => {
    if (!t) return '-';
    const d = new Date(t);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const date = String(d.getDate()).padStart(2, '0');
    const dateText = `${year}-${month}-${date}`;
    const timeText = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    return `${dateText} ${timeText}`;
};

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六'];

const compactCalendarDays = computed(() =>
    calendarDays.value.filter(Boolean).map((day) => {
        const [year, month, date] = day.dateStr.split('-').map(Number);
        return {
            ...day,
            weekday: WEEKDAYS[new Date(year, month - 1, date).getDay()]
        };
    })
);

const currentLevel = computed(() => growth.value?.currentLevel ?? growth.value?.level ?? 1);
const currentExp = computed(() => growth.value?.currentExp ?? growth.value?.exp ?? 0);
const expToNextLevel = computed(() => Number(growth.value?.expToNextLevel || 0));
const nextLevelExpTarget = computed(() => currentExp.value + expToNextLevel.value);
const levelRewards = computed(() => Array.isArray(growth.value?.levelRewards) ? growth.value.levelRewards : []);
const LEVEL_PRIVILEGE_LABELS = {
    PAID_ARTICLE_PUBLISH: '解锁付费文章发布权限',
    EXCLUSIVE_BADGE: '解锁专属徽章',
    HOMEPAGE_RECOMMEND_ELIGIBLE: '解锁首页推荐申请资格',
    ANNUAL_CREATOR_ELIGIBLE: '获得年度创作者候选资格',
};
const rewardDialogOpen = ref(false);
const rewardStatusText = (reward) => {
    if (reward?.status === 'GRANTED') {
        return reward?.rewardKind === 'POINTS' ? '已发放' : '已获得';
    }
    return '未解锁';
};
const rewardStatusClass = (reward) => reward?.status === 'GRANTED' ? 'claimed' : 'locked';
const rewardGranted = (reward) => reward?.status === 'GRANTED' || reward?.achieved;
const rewardPrivilegeSummary = (reward) => {
    const codes = Array.isArray(reward?.privilegeCodes) ? reward.privilegeCodes : [];
    if (!codes.length) {
        return '';
    }
    return codes.map((code) => LEVEL_PRIVILEGE_LABELS[code] || code).join(' · ');
};
const rewardPointsSummary = (reward) => Number(reward?.rewardPoints || 0) > 0
    ? `+${Number(reward.rewardPoints || 0)} 积分`
    : '';
const rewardBenefitSummary = (reward) => {
    const parts = [rewardPointsSummary(reward), rewardPrivilegeSummary(reward)].filter(Boolean);
    return parts.join(' · ');
};
const nextLevelReward = computed(() =>
    levelRewards.value.find((reward) => Number(reward.level || 0) > currentLevel.value && Number(reward.rewardPoints || 0) > 0)
        || levelRewards.value.find((reward) => Number(reward.level || 0) > currentLevel.value)
        || null
);
const rewardSummaryText = computed(() => {
    if (!levelRewards.value.length) return '暂无奖励';
    if (!nextLevelReward.value) return '已达成全部等级奖励';
    const benefit = rewardBenefitSummary(nextLevelReward.value);
    return benefit
        ? `下级 Lv.${nextLevelReward.value.level} 可得 ${benefit}`
        : `下级 Lv.${nextLevelReward.value.level} 解锁新权益`;
});
const signInPoints = computed(() => signInResult.value?.pointsGranted ?? signInResult.value?.pointsEarned ?? 0);
const signInDays = computed(() => {
    if (signInResult.value?.consecutiveDays) return signInResult.value.consecutiveDays;
    const currentCalendar = calendarData.value[calendarMonth.value];
    return currentCalendar?.currentConsecutiveDays ?? 0;
});

// ── 签到统计 ──────────────────────────────────────────────────────
const signedDaysThisMonth = computed(() => {
    const currentCalendar = calendarData.value[calendarMonth.value];
    if (!currentCalendar?.signedDates) return 0;
    return currentCalendar.signedDates.length;
});

const totalDaysThisMonth = computed(() => {
    const [year, month] = calendarMonth.value.split('-').map(Number);
    return new Date(year, month, 0).getDate();
});

// ── 判断日期是否为未来日期 ────────────────────────────────────────
const isFutureDate = (dateStr) => {
    const date = new Date(dateStr);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date > today;
};

let rewardDialogPointerDownOnSelf = false;
const openRewardDialog = () => {
    rewardDialogOpen.value = true;
};

const closeRewardDialog = () => {
    rewardDialogOpen.value = false;
    rewardDialogPointerDownOnSelf = false;
};

const handleRewardDialogPointerDown = (event) => {
    rewardDialogPointerDownOnSelf = event.target === event.currentTarget;
};

const handleRewardDialogPointerUp = (event) => {
    if (rewardDialogPointerDownOnSelf && event.target === event.currentTarget) {
        closeRewardDialog();
    }
    rewardDialogPointerDownOnSelf = false;
};

onMounted(async () => {
    await Promise.all([loadGrowth(), loadAccount(), loadBadges(), loadCalendar(calendarMonth.value)]);
    await loadJournals({tab: 'points', page: 1});
    warmJournalCache();
});
</script>

<template>
    <SiteHeader />

    <main class="page-shell dashboard-layout growth-page">
        <CreatorSidebar />

        <section class="dashboard-main growth-main">
            <div class="section-heading growth-heading">
                <div>
                    <p class="eyebrow">个人账户</p>
                    <h1>经验积分</h1>
                </div>
            </div>

            <!-- 顶部卡片区 -->
            <div class="growth-cards">
                <!-- 等级 & 经验卡片 -->
                <div class="growth-card level-card">
                    <div v-if="growthLoading" class="card-skeleton"></div>
                    <template v-else-if="growth">
                        <button
                            type="button"
                            class="reward-entry"
                            aria-label="查看等级奖励"
                            @click="openRewardDialog"
                        >
                            <span class="reward-entry-icon" aria-hidden="true">奖</span>
                            <span>奖励</span>
                        </button>
                        <div class="level-badge">Lv.{{ currentLevel }}</div>
                        <div class="level-info">
                            <p class="level-name">{{ growth.levelName || `${currentLevel} 级` }}</p>
                            <p class="level-exp">
                                <span class="exp-label">EXP</span>
                                <strong>{{ currentExp }}</strong>
                                <span v-if="expToNextLevel > 0" class="exp-target">
                                    / {{ nextLevelExpTarget }}
                                </span>
                                <span v-else class="max-level">· 已满级</span>
                            </p>
                            <div class="exp-bar">
                                <div
                                    class="exp-fill"
                                    :style="{ width: `${Math.min(growth.progressPercent || 0, 100)}%` }"
                                ></div>
                            </div>
                            <p class="exp-percent">{{ growth.progressPercent || 0 }}%</p>
                            <p class="reward-summary">{{ rewardSummaryText }}</p>
                        </div>
                    </template>
                    <div v-else-if="growthError" class="card-error">{{ growthError }}</div>
                    <div v-else class="card-empty">暂无成长数据</div>
                </div>

                <!-- 积分账户卡片 -->
                <div class="growth-card point-card">
                    <div v-if="accountLoading" class="card-skeleton"></div>
                    <template v-else-if="pointAccount">
                        <div class="point-balance">
                            <span class="point-num">{{ pointAccount.balance }}</span>
                            <span class="point-unit">积分</span>
                        </div>
                        <div class="point-stats">
                            <div class="point-stat-item">
                                <span class="stat-label">累计获得</span>
                                <span class="stat-val">{{ pointAccount.totalEarned }}</span>
                            </div>
                            <div class="point-stat-item">
                                <span class="stat-label">累计消耗</span>
                                <span class="stat-val">{{ pointAccount.totalSpent }}</span>
                            </div>
                        </div>
                    </template>
                    <div v-else class="card-empty">暂无积分信息</div>
                </div>

                <!-- 徽章卡片 -->
                <div class="growth-card badge-card">
                    <div v-if="badgeLoading" class="card-skeleton"></div>
                    <template v-else>
                        <div class="badge-card-head">
                            <div>
                                <span class="stat-label">当前佩戴</span>
                                <h3>我的徽章</h3>
                            </div>
                            <UserEquippedBadge :badge="badgeState.equippedBadge" />
                        </div>
                        <BadgePicker
                            :badges="badgeState.badges"
                            :saving="badgeSaving"
                            @equip="equipBadge"
                        />
                        <p v-if="badgeError" class="badge-error">{{ badgeError }}</p>
                    </template>
                </div>

                <!-- 签到卡片 -->
                <div class="growth-card signin-card">
                    <div class="signin-header">
                        <h3>每日签到</h3>
                        <div v-if="signInResult && !signInError" class="signin-reward">
                            🎉 +{{ signInPoints }} 积分（连续 {{ signInDays }} 天）
                        </div>
                        <div v-if="signInError" class="signin-error">{{ signInError }}</div>
                    </div>

                    <!-- 签到统计摘要 -->
                    <div class="signin-summary">
                        <span class="summary-item">
                            <span class="summary-icon">🔥</span>
                            连续 <strong>{{ signInDays }}</strong> 天
                        </span>
                        <span class="summary-item">
                            <span class="summary-icon">📅</span>
                            本月 <strong>{{ signedDaysThisMonth }}/{{ totalDaysThisMonth }}</strong> 天
                        </span>
                    </div>

                    <!-- 横向滚动日历条 -->
                    <div
                        class="calendar-strip"
                        :class="{ 'calendar-grid--loading': calendarLoading && !calendarInitialLoading }"
                        aria-label="签到日期"
                    >
                        <div
                            v-for="day in compactCalendarDays"
                            :key="day.dateStr"
                            :class="['cal-strip-day', {
                                'cal-day--signed': day.signed,
                                'cal-day--today': day.isToday,
                                'cal-day--missed': !day.signed && !day.isToday
                            }]"
                        >
                            <span>{{ day.date }}</span>
                        </div>
                    </div>

                    <!-- 签到按钮 -->
                    <button
                        type="button"
                        :class="['signin-btn', { 'signed': isCurrentMonth && todaySigned }]"
                        :disabled="signingIn || !canSignInToday"
                        @click="doSignIn"
                    >
                        <template v-if="signingIn">签到中...</template>
                        <template v-else-if="!isCurrentMonth">仅本月可签到</template>
                        <template v-else-if="todaySigned">今日已签到 ✓</template>
                        <template v-else>立即签到</template>
                    </button>
                </div>
            </div>

            <section class="badge-wall-section">
                <div class="badge-wall-head">
                    <div>
                        <p class="eyebrow">成就收藏</p>
                        <h2>徽章墙</h2>
                    </div>
                    <span>{{ badgeState.badges.filter((badge) => badge.owned).length }} / {{ badgeState.badges.length }}</span>
                </div>
                <BadgeWall :badges="badgeState.badges" />
            </section>

            <!-- 流水记录区 -->
            <section class="journal-section">
                <div class="journal-tabs">
                    <button
                        type="button"
                        :class="['tab-btn', {
                            active: journalTab === 'points',
                            loading: journalLoading && journalPendingTab === 'points'
                        }]"
                        @click="switchTab('points')"
                    >积分流水</button>
                    <button
                        type="button"
                        :class="['tab-btn', {
                            active: journalTab === 'exp',
                            loading: journalLoading && journalPendingTab === 'exp'
                        }]"
                        @click="switchTab('exp')"
                    >经验流水</button>
                    <button
                        type="button"
                        :class="['tab-btn', {
                            active: journalTab === 'revenue',
                            loading: journalLoading && journalPendingTab === 'revenue'
                        }]"
                        @click="switchTab('revenue')"
                    >分账收益</button>
                </div>

                <div class="journal-content" :aria-busy="journalLoading">
                    <div v-if="journalInitialLoading" class="journal-loading">加载中...</div>
                    <div v-else-if="journalError" class="journal-error">{{ journalError }}</div>
                    <template v-else-if="journals.length > 0">
                        <table class="journal-table">
                            <thead>
                                <tr v-if="journalDisplayTab !== 'revenue'">
                                    <th>类型</th>
                                    <th v-if="journalDisplayTab === 'points'">变动</th>
                                    <th v-if="journalDisplayTab === 'points'">余额</th>
                                    <th v-if="journalDisplayTab === 'exp'">经验值</th>
                                    <th>说明</th>
                                    <th>时间</th>
                                </tr>
                                <tr v-else>
                                    <th>文章</th>
                                    <th>解锁积分</th>
                                    <th>作者分成</th>
                                    <th>平台分成</th>
                                    <th>分成比例</th>
                                    <th>结算状态</th>
                                    <th>订单号</th>
                                    <th>时间</th>
                                </tr>
                            </thead>
                            <tbody>
                                <!-- 积分流水 -->
                                <template v-if="journalDisplayTab === 'points'">
                                    <tr v-for="item in journals" :key="item.id || item.bizNo">
                                        <td><span class="tag-chip">{{ sourceLabel(item.sourceType) }}</span></td>
                                        <td :class="['delta', item.delta > 0 ? 'plus' : 'minus']">
                                            {{ item.delta > 0 ? '+' : '' }}{{ item.delta }}
                                        </td>
                                        <td>{{ item.balanceAfter ?? '-' }}</td>
                                        <td class="desc-cell">
                                            <template v-if="pointJournalArticleLabel(item)">
                                                <RouterLink
                                                    v-if="item.articleAccessible"
                                                    class="journal-article-link"
                                                    :to="articleRoute(item)"
                                                >
                                                    {{ pointJournalArticleLabel(item) }}
                                                </RouterLink>
                                                <span v-else class="journal-article-link disabled">
                                                    {{ pointJournalArticleLabel(item) }}
                                                </span>
                                            </template>
                                            <template v-else>
                                                {{ item.remark || item.description || '-' }}
                                            </template>
                                        </td>
                                        <td class="time-cell">{{ fmtTime(item.createdAt) }}</td>
                                    </tr>
                                </template>
                                <!-- 经验流水 -->
                                <template v-if="journalDisplayTab === 'exp'">
                                    <tr v-for="item in journals" :key="item.id">
                                        <td><span class="tag-chip">{{ sourceLabel(item.eventType) }}</span></td>
                                        <td :class="['delta', 'plus']">+{{ item.delta ?? item.expAmount ?? 0 }}</td>
                                        <td class="desc-cell">{{ expJournalDesc(item) }}</td>
                                        <td class="time-cell">{{ fmtTime(item.createdAt) }}</td>
                                    </tr>
                                </template>
                                <!-- 分账收益 -->
                                <template v-if="journalDisplayTab === 'revenue'">
                                    <tr v-for="item in journals" :key="item.id || item.orderNo">
                                        <td class="article-cell">
                                            <RouterLink
                                                v-if="item.articleAccessible"
                                                class="journal-article-link"
                                                :to="articleRoute(item)"
                                            >
                                                {{ revenueArticleLabel(item) }}
                                            </RouterLink>
                                            <span v-else class="journal-article-link disabled">
                                                {{ revenueArticleLabel(item) }}
                                            </span>
                                        </td>
                                        <td>{{ item.totalPoints }}</td>
                                        <td :class="['delta', 'plus']">+{{ item.authorPoints }}</td>
                                        <td>{{ item.platformPoints }}</td>
                                        <td>{{ formatShareRatio(item.shareRatio) }}</td>
                                        <td>
                                            <span
                                                :class="[
                                                    'status-chip',
                                                    revenueStatusMeta(item.settlementStatus).className
                                                ]"
                                            >
                                                {{ revenueStatusMeta(item.settlementStatus).label }}
                                            </span>
                                        </td>
                                        <td class="desc-cell">
                                            <span class="order-code">{{ item.orderNo || '-' }}</span>
                                        </td>
                                        <td class="time-cell">{{ fmtTime(item.settledAt || item.createdAt) }}</td>
                                    </tr>
                                </template>
                            </tbody>
                        </table>

                        <!-- 分页 -->
                        <div v-if="journalDisplayTab !== 'exp'" class="journal-pagination">
                            <button
                                type="button"
                                :disabled="journalLoading || journalPage <= 1"
                                @click="prevPage"
                            >上一页</button>
                            <span>第 {{ journalPage }} / {{ journalTotalPages }} 页</span>
                            <button
                                type="button"
                                :disabled="journalLoading || journalPage >= journalTotalPages"
                                @click="nextPage"
                            >下一页</button>
                        </div>
                    </template>
                    <div v-else class="journal-empty">暂无记录</div>
                    <div v-if="journalLoading && !journalInitialLoading" class="journal-loading-overlay">
                        加载中...
                    </div>
                </div>
            </section>
        </section>
    </main>

    <Teleport to="body">
        <div
            v-if="rewardDialogOpen"
            class="level-reward-overlay"
            role="presentation"
            @pointerdown="handleRewardDialogPointerDown"
            @pointerup="handleRewardDialogPointerUp"
        >
            <section
                class="level-reward-dialog"
                role="dialog"
                aria-modal="true"
                aria-labelledby="level-reward-title"
            >
                <button
                    type="button"
                    class="level-reward-close"
                    aria-label="关闭等级奖励弹窗"
                    @click="closeRewardDialog"
                >×</button>
                <p class="reward-dialog-eyebrow">等级权益</p>
                <h2 id="level-reward-title">等级奖励</h2>
                <p class="reward-dialog-desc">
                    {{ rewardSummaryText }}
                </p>
                <div v-if="levelRewards.length" class="level-reward-list">
                    <article
                        v-for="reward in levelRewards"
                        :key="reward.level"
                        :class="['level-reward-row', {achieved: rewardGranted(reward)}]"
                    >
                        <div class="level-reward-rank">
                            <span>Lv.{{ reward.level }}</span>
                        </div>
                        <div class="level-reward-body">
                            <div class="level-reward-main">
                                <strong>{{ reward.rewardTitle || '等级奖励' }}</strong>
                                <span v-if="rewardBenefitSummary(reward)">{{ rewardBenefitSummary(reward) }}</span>
                            </div>
                            <p>{{ reward.description || '达到该等级后自动发放奖励或解锁权益' }}</p>
                        </div>
                        <span :class="['reward-state', rewardStatusClass(reward)]">
                            {{ rewardStatusText(reward) }}
                        </span>
                    </article>
                </div>
                <div v-else class="level-reward-empty">暂无等级奖励配置</div>
            </section>
        </div>
    </Teleport>
</template>

<style scoped>
.growth-main {
    min-width: 0;
    max-width: 100%;
}

.growth-heading h1 {
    font-weight: 700;
    margin: 0;
    color: var(--text-strong);
}

/* ── 卡片区 ─────────────────────────────────── */
.growth-cards {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 24px;
    min-width: 0;
}

.growth-card {
    min-width: 0;
    max-width: 100%;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    padding: 16px 20px;
    box-shadow: var(--shadow);
}

.signin-card {
    grid-column: 1 / -1;
}

.card-skeleton {
    height: 100%;
    min-height: 120px;
    border-radius: 8px;
    background: linear-gradient(90deg, var(--surface-soft) 25%, var(--line) 50%, var(--surface-soft) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}

.card-error { color: #dc2626; font-size: 14px; }
.card-empty { color: var(--text-muted); font-size: 14px; }

/* ── 等级卡片 ───────────────────────────────── */
.level-card {
    position: relative;
    display: flex;
    gap: 16px;
    align-items: flex-start;
    padding-right: 112px;
}

.level-badge {
    flex-shrink: 0;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--accent, #6c63ff), #a78bfa);
    color: #fff;
    font-size: 16px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
}

.level-info {
    flex: 1;
    min-width: 0;
}

.level-name {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-strong);
    margin: 0 0 6px;
}

.level-exp {
    display: flex;
    align-items: baseline;
    gap: 4px;
    flex-wrap: wrap;
    font-size: 13px;
    color: var(--text-muted);
    margin: 0 0 10px;
    overflow-wrap: anywhere;
}

.level-exp strong { color: var(--text-strong); }

.exp-label {
    color: var(--text-strong);
    font-size: 12px;
    font-weight: 800;
}

.exp-target {
    color: var(--text-muted);
    font-weight: 600;
}

.max-level { color: var(--accent); }

.exp-bar {
    height: 8px;
    background: var(--surface-soft);
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 4px;
}

.exp-fill {
    height: 100%;
    background: linear-gradient(90deg, var(--accent, #6c63ff), #a78bfa);
    border-radius: 4px;
    transition: width 0.6s ease;
}

.exp-percent {
    font-size: 12px;
    color: var(--text-muted);
    text-align: right;
    margin: 0;
}

.reward-entry {
    position: absolute;
    top: 14px;
    right: 16px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 32px;
    padding: 0 10px 0 8px;
    color: #2563eb;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.15s, border-color 0.15s, color 0.15s, transform 0.15s;
}

.reward-entry:hover {
    color: #1d4ed8;
    background: #dbeafe;
    border-color: #93c5fd;
    transform: translateY(-1px);
}

.reward-entry-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    color: #fff;
    background: linear-gradient(135deg, #2563eb, #8b5cf6);
    border-radius: 50%;
    font-size: 11px;
    line-height: 1;
}

.reward-summary {
    margin: 8px 0 0;
    color: var(--text-muted);
    font-size: 12px;
    line-height: 1.45;
    overflow-wrap: anywhere;
}

.level-reward-overlay {
    position: fixed;
    inset: 0;
    z-index: 3000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: rgba(15, 23, 42, 0.42);
}

.level-reward-dialog {
    position: relative;
    width: min(560px, calc(100vw - 40px));
    max-height: min(680px, calc(100vh - 40px));
    overflow: hidden;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 12px;
    box-shadow: 0 24px 80px rgba(15, 23, 42, 0.24);
    padding: 24px;
}

.level-reward-close {
    position: absolute;
    top: 14px;
    right: 14px;
    width: 32px;
    height: 32px;
    border: 1px solid var(--line);
    border-radius: 50%;
    color: var(--text-muted);
    background: var(--surface);
    font-size: 22px;
    line-height: 1;
    cursor: pointer;
}

.level-reward-close:hover {
    color: var(--text-strong);
    background: var(--surface-soft);
}

.reward-dialog-eyebrow {
    margin: 0 0 6px;
    color: var(--brand);
    font-size: 12px;
    font-weight: 700;
}

.level-reward-dialog h2 {
    margin: 0;
    color: var(--text-strong);
    font-size: 22px;
    line-height: 1.25;
}

.reward-dialog-desc {
    margin: 8px 40px 18px 0;
    color: var(--text-muted);
    font-size: 14px;
}

.level-reward-list {
    display: grid;
    gap: 10px;
    max-height: 480px;
    overflow-y: auto;
    padding-right: 4px;
}

.level-reward-row {
    display: grid;
    grid-template-columns: 58px minmax(0, 1fr) auto;
    gap: 12px;
    align-items: center;
    padding: 12px;
    background: var(--surface-soft);
    border: 1px solid var(--line);
    border-radius: 10px;
}

.level-reward-row.achieved {
    background: #f0fdf4;
    border-color: #bbf7d0;
}

.level-reward-rank {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    border-radius: 14px;
    color: #fff;
    background: linear-gradient(135deg, #64748b, #94a3b8);
    font-size: 13px;
    font-weight: 800;
}

.level-reward-row.achieved .level-reward-rank {
    background: linear-gradient(135deg, #16a34a, #22c55e);
}

.level-reward-body {
    min-width: 0;
}

.level-reward-main {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
}

.level-reward-main strong {
    min-width: 0;
    color: var(--text-strong);
    font-size: 14px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.level-reward-main span {
    flex-shrink: 0;
    color: #16a34a;
    font-size: 13px;
    font-weight: 800;
}

.level-reward-body p {
    margin: 4px 0 0;
    color: var(--text-muted);
    font-size: 12px;
    line-height: 1.45;
    overflow-wrap: anywhere;
}

.reward-state {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 64px;
    height: 28px;
    padding: 0 10px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
}

.reward-state.claimed {
    color: #047857;
    background: #d1fae5;
}

.reward-state.locked {
    color: #64748b;
    background: #e2e8f0;
}

.level-reward-empty {
    min-height: 160px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-muted);
    background: var(--surface-soft);
    border-radius: 10px;
}

/* ── 积分卡片 ───────────────────────────────── */
.point-card {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 16px;
}

.point-balance {
    display: flex;
    align-items: baseline;
    gap: 6px;
}

.point-num {
    font-size: 36px;
    font-weight: 800;
    color: var(--accent, #6c63ff);
    line-height: 1;
}

.point-unit {
    font-size: 14px;
    color: var(--text-muted);
}

.point-stats {
    display: flex;
    gap: 20px;
    min-width: 0;
}

.point-stat-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.stat-label {
    font-size: 12px;
    color: var(--text-muted);
}

.stat-val {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-strong);
}

/* ── 徽章 ───────────────────────────────────── */
.badge-card {
    grid-column: 1 / -1;
    display: grid;
    gap: 14px;
}

.badge-card-head,
.badge-wall-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    min-width: 0;
}

.badge-card-head h3,
.badge-wall-head h2,
.badge-wall-head p {
    margin: 0;
}

.badge-card-head h3,
.badge-wall-head h2 {
    color: var(--text-strong);
    font-size: 16px;
}

.badge-error {
    margin: 0;
    color: #dc2626;
    font-size: 12px;
}

.badge-wall-section {
    display: grid;
    gap: 14px;
    margin-bottom: 24px;
    padding: 20px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow);
}

.badge-wall-head > span {
    flex: none;
    color: var(--brand);
    font-size: 13px;
    font-weight: 800;
}

/* ── 签到卡片 ───────────────────────────────── */
.signin-card {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.signin-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.signin-header h3 {
    font-size: 14px;
    font-weight: 600;
    margin: 0;
    color: var(--text-strong);
}

.signin-reward {
    font-size: 12px;
    color: #16a34a;
    font-weight: 500;
}

.signin-error {
    font-size: 12px;
    color: #dc2626;
}

/* 签到统计摘要 */
.signin-summary {
    display: flex;
    gap: 12px;
    padding: 6px 10px;
    background: var(--surface-soft);
    border-radius: var(--radius-sm);
    font-size: 12px;
}

.summary-item {
    display: flex;
    align-items: center;
    gap: 4px;
    color: var(--text);
}

.summary-icon {
    font-size: 12px;
}

.summary-item strong {
    color: var(--text-strong);
    font-weight: 700;
}

/* 横向滚动日历条 */
.calendar-strip {
    display: flex;
    max-width: 100%;
    gap: 6px;
    padding-bottom: 2px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
}

.calendar-strip::-webkit-scrollbar {
    display: none;
}

.cal-strip-day {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 0 0 36px;
    min-height: 44px;
    color: var(--text);
    background: var(--surface);
    border: 1px dashed var(--line-strong);
    border-radius: 8px;
    transition: all 0.15s;
}

.cal-strip-day span {
    font-size: 13px;
    font-weight: 700;
}

/* 已签到 - 实心紫色背景 */
.cal-strip-day.cal-day--signed {
    background: var(--accent, #6c63ff);
    border: 1px solid var(--accent, #6c63ff);
    color: #fff;
}

/* 今天未签到 - 脉冲动画 */
.cal-strip-day.cal-day--today:not(.cal-day--signed) {
    border: 2px solid var(--accent, #6c63ff);
    background: rgba(108, 99, 255, 0.08);
    color: var(--accent, #6c63ff);
    animation: pulse-today-mobile 2s infinite;
}

@keyframes pulse-today-mobile {
    0%, 100% { box-shadow: 0 0 0 0 rgba(108, 99, 255, 0.3); }
    50% { box-shadow: 0 0 0 4px rgba(108, 99, 255, 0); }
}

/* 错过签到（过去但未签到） */
.cal-strip-day.cal-day--missed {
    background: var(--surface-soft);
    border: 1px dashed var(--line-strong);
    color: var(--muted);
}

.signin-btn {
    margin-top: 4px;
    padding: 10px;
    background: var(--accent, #6c63ff);
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: opacity 0.2s;
    width: 100%;
}

.signin-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.signin-btn.signed { background: #16a34a; }
.signin-btn:not(:disabled):not(.signed):hover { opacity: 0.88; }

/* ── 流水区 ─────────────────────────────────── */
.journal-section {
    min-width: 0;
    max-width: 100%;
    overflow: hidden;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    padding: 24px;
    box-shadow: var(--shadow);
}

.journal-tabs {
    display: flex;
    gap: 4px;
    min-width: 0;
    margin-bottom: 20px;
    border-bottom: 1px solid var(--line);
    padding-bottom: 0;
}

.tab-btn {
    padding: 8px 18px;
    border: none;
    background: none;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-muted);
    cursor: pointer;
    border-bottom: 2px solid transparent;
    margin-bottom: -1px;
    transition: color 0.15s, border-color 0.15s;
}

.tab-btn.active {
    color: var(--accent, #6c63ff);
    border-bottom-color: var(--accent, #6c63ff);
}

.tab-btn.loading {
    opacity: 0.72;
}

.tab-btn:not(.active):hover { color: var(--text); }

.journal-content {
    position: relative;
    width: 100%;
    max-width: 100%;
    min-width: 0;
    min-height: 360px;
    overflow-x: auto;
    overscroll-behavior-x: contain;
}

.journal-loading,
.journal-empty {
    min-height: 280px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-muted);
    font-size: 14px;
}

.journal-loading-overlay {
    position: absolute;
    top: 8px;
    right: 8px;
    display: inline-flex;
    align-items: center;
    height: 28px;
    padding: 0 10px;
    color: var(--brand);
    font-size: 12px;
    font-weight: 600;
    background: rgba(239, 246, 255, 0.96);
    border: 1px solid #bfdbfe;
    border-radius: 999px;
    box-shadow: 0 6px 16px rgba(37, 99, 235, 0.12);
    pointer-events: none;
}

.journal-error {
    min-height: 280px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #dc2626;
    font-size: 14px;
}

.journal-table {
    width: 100%;
    min-width: 900px;
    border-collapse: collapse;
    font-size: 14px;
}

.journal-table th {
    text-align: left;
    padding: 8px 12px;
    font-size: 12px;
    font-weight: 600;
    color: var(--text-muted);
    border-bottom: 1px solid var(--line);
}

.journal-table td {
    padding: 10px 12px;
    border-bottom: 1px solid var(--line);
    color: var(--text);
    vertical-align: middle;
}

.journal-table tr:last-child td { border-bottom: none; }
.journal-table tr:hover td { background: var(--surface-soft); }

.tag-chip {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    background: var(--surface-soft);
    color: var(--text-muted);
    white-space: nowrap;
}

.status-chip {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
}

.status-chip.pending {
    background: #fff7ed;
    color: #c2410c;
}

.status-chip.settled {
    background: #ecfdf5;
    color: #047857;
}

.status-chip.failed {
    background: #fef2f2;
    color: #b91c1c;
}

.delta { font-weight: 700; }
.delta.plus { color: #16a34a; }
.delta.minus { color: #dc2626; }

.article-cell {
    min-width: 220px;
}

.desc-cell {
    max-width: 280px;
    color: var(--text-muted);
    font-size: 13px;
}

.journal-article-link {
    color: var(--brand);
    font-weight: 600;
    text-decoration: none;
    overflow-wrap: anywhere;
}

.journal-article-link:hover {
    color: var(--brand-strong);
    text-decoration: underline;
}

.journal-article-link.disabled {
    color: var(--text-muted);
    cursor: not-allowed;
    text-decoration: none;
}

.order-code {
    font-family: Consolas, 'Liberation Mono', monospace;
    color: var(--text-muted);
}

.time-cell { color: var(--text-muted); font-size: 12px; white-space: nowrap; }

.journal-pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin-top: 16px;
    font-size: 14px;
    color: var(--text-muted);
}

.journal-pagination button {
    padding: 6px 16px;
    border: 1px solid var(--line);
    border-radius: 6px;
    background: var(--surface);
    cursor: pointer;
    font-size: 13px;
    color: var(--text);
    transition: background 0.15s;
}

.journal-pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.journal-pagination button:not(:disabled):hover { background: var(--surface-soft); }

/* ── 响应式 ─────────────────────────────────── */
@media (max-width: 640px) {
    .growth-cards {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 10px;
        padding: 0;
        margin-bottom: 14px;
    }

    .signin-card {
        grid-column: 1 / -1;
    }

    .growth-heading {
        padding: 0;
        margin-bottom: 8px;
    }

    .growth-card {
        padding: 12px;
        min-height: 0;
        border-radius: var(--radius-sm);
    }

    .level-card {
        gap: 10px;
        align-items: flex-start;
        text-align: left;
        overflow: hidden;
        padding-right: 48px;
    }

    .level-badge {
        width: 42px;
        height: 42px;
        font-size: 13px;
    }

    .reward-entry {
        top: 10px;
        right: 10px;
        width: 32px;
        height: 32px;
        padding: 0;
        justify-content: center;
    }

    .reward-entry span:not(.reward-entry-icon) {
        display: none;
    }

    .reward-entry-icon {
        width: 20px;
        height: 20px;
        font-size: 12px;
    }

    .level-name {
        margin-bottom: 4px;
        font-size: 15px;
    }

    .level-exp {
        margin-bottom: 8px;
        font-size: 12px;
        line-height: 1.45;
    }

    .reward-summary {
        display: -webkit-box;
        margin-top: 6px;
        font-size: 11px;
        line-height: 1.35;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .level-reward-overlay {
        align-items: flex-end;
        padding: 12px;
    }

    .level-reward-dialog {
        width: 100%;
        max-height: min(620px, calc(100vh - 24px));
        padding: 20px 16px 16px;
        border-radius: 14px 14px 10px 10px;
    }

    .level-reward-dialog h2 {
        font-size: 20px;
    }

    .reward-dialog-desc {
        margin-right: 36px;
        font-size: 13px;
    }

    .level-reward-list {
        max-height: 430px;
    }

    .level-reward-row {
        grid-template-columns: 46px minmax(0, 1fr);
        gap: 10px;
    }

    .level-reward-rank {
        width: 42px;
        height: 42px;
        border-radius: 12px;
        font-size: 12px;
    }

    .level-reward-main {
        flex-wrap: wrap;
        gap: 4px 8px;
    }

    .level-reward-main strong {
        flex: 1 1 100%;
        white-space: normal;
    }

    .reward-state {
        grid-column: 2;
        justify-self: start;
        min-width: 58px;
        height: 24px;
        padding: 0 8px;
        font-size: 11px;
    }

    .point-num {
        font-size: 26px;
    }

    .point-card {
        gap: 10px;
        justify-content: flex-start;
    }

    .point-stats {
        gap: 8px;
        flex-wrap: wrap;
    }

    .point-stat-item {
        flex: 1 1 56px;
        min-width: 0;
    }

    .stat-val {
        font-size: 14px;
    }

    .signin-card {
        gap: 10px;
    }

    .signin-header {
        align-items: center;
    }

    .signin-header h3 {
        margin-bottom: 0;
    }

    .signin-reward,
    .signin-error {
        max-width: 190px;
        font-size: 12px;
        text-align: right;
    }

    .signin-btn {
        margin-top: 0;
        padding: 9px 10px;
        border-radius: 7px;
        font-size: 13px;
    }

    .journal-section {
        margin: 0 -14px;
        padding: 14px;
        border-radius: 0;
        border-left: none;
        border-right: none;
    }

    .journal-tabs {
        max-width: 100%;
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
        scrollbar-width: none;
    }

    .journal-tabs::-webkit-scrollbar {
        display: none;
    }

    .tab-btn {
        padding: 8px 14px;
        font-size: 13px;
        white-space: nowrap;
        flex-shrink: 0;
    }

    .journal-content {
        min-height: 260px;
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
    }

    .journal-table {
        min-width: 720px;
    }

    .journal-table th,
    .journal-table td {
        padding: 8px 8px;
        font-size: 12px;
    }
}
</style>

