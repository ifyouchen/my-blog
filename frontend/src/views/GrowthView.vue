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
  getInviteSummaryApi,
  signInApi,
} from '@/api/growth';
import {generateInviteCodeApi, getMyInviteCodesApi} from '@/api/auth';
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
        window.dispatchEvent(new CustomEvent('signin:completed'));
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

const FEED_CACHE_PREFIX = 'my-blog:infinite-article-feed:';
function clearFeedCache() {
    try {
        const keys = Object.keys(sessionStorage);
        keys.forEach(key => {
            if (key.startsWith(FEED_CACHE_PREFIX)) {
                sessionStorage.removeItem(key);
            }
        });
    } catch {
        // Ignore storage errors
    }
}

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
        clearFeedCache();
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
    EXCLUSIVE_BADGE: '等级徽章已包含身份标识',
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

// ── 邀请好友 ─────────────────────────────────────────────────────
const inviteCodes = ref([]);
const inviteSummary = ref(null);
const inviteLoading = ref(false);
const inviteGenerating = ref(false);
const inviteError = ref('');
const copiedCode = ref('');

const activeInviteCount = computed(() =>
    inviteCodes.value.filter((c) => c.status === 'ACTIVE').length
);

const buildInviteLink = (code) =>
    `${window.location.origin}/register?invite=${code}`;

const loadInvite = async () => {
    inviteLoading.value = true;
    inviteError.value = '';
    try {
        const [codes, summary] = await Promise.all([
            getMyInviteCodesApi(),
            getInviteSummaryApi().catch(() => null),
        ]);
        inviteCodes.value = Array.isArray(codes) ? codes : [];
        inviteSummary.value = summary;
    } catch (e) {
        inviteError.value = e.message || '加载邀请信息失败';
    } finally {
        inviteLoading.value = false;
    }
};

const generateCode = async () => {
    if (inviteGenerating.value || activeInviteCount.value >= 3) return;
    inviteGenerating.value = true;
    inviteError.value = '';
    try {
        await generateInviteCodeApi();
        await loadInvite();
    } catch (e) {
        inviteError.value = e.message || '生成失败，请稍后重试';
    } finally {
        inviteGenerating.value = false;
    }
};

const copyInviteLink = async (code) => {
    const link = buildInviteLink(code);
    try {
        if (navigator.clipboard?.writeText) {
            await navigator.clipboard.writeText(link);
        } else {
            const el = document.createElement('textarea');
            el.value = link;
            el.style.position = 'fixed';
            el.style.opacity = '0';
            document.body.appendChild(el);
            el.select();
            document.execCommand('copy');
            document.body.removeChild(el);
        }
        copiedCode.value = code;
        window.setTimeout(() => { copiedCode.value = ''; }, 2000);
    } catch {
        inviteError.value = '复制失败，请手动复制';
    }
};

const fmtExpiry = (dateStr) => {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

onMounted(async () => {
    await Promise.all([loadGrowth(), loadAccount(), loadBadges(), loadCalendar(calendarMonth.value), loadInvite()]);
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

            <!-- 邀请好友 -->
            <section class="invite-section">
                <div class="invite-head">
                    <div>
                        <p class="eyebrow">拉新奖励</p>
                        <h2>邀请好友</h2>
                    </div>
                    <div v-if="inviteSummary" class="invite-stats">
                        <span class="invite-stat">
                            <strong>{{ inviteSummary.totalGrantedCount ?? 0 }}</strong>
                            <span>人已加入</span>
                        </span>
                        <span class="invite-stat-divider"></span>
                        <span class="invite-stat">
                            <strong>+{{ inviteSummary.totalPointsEarned ?? 0 }}</strong>
                            <span>积分已到账</span>
                        </span>
                    </div>
                </div>

                <p class="invite-desc">
                    每邀请一位新用户注册，双方均可获得积分奖励。复制下方邀请链接发送给好友即可。
                </p>

                <div v-if="inviteError" class="invite-error">{{ inviteError }}</div>

                <div v-if="inviteLoading" class="invite-loading">加载中...</div>

                <template v-else>
                    <div v-if="inviteCodes.length > 0" class="invite-table-wrap">
                        <table class="invite-table">
                            <thead>
                                <tr>
                                    <th>邀请码</th>
                                    <th>状态</th>
                                    <th>过期时间</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="item in inviteCodes" :key="item.id">
                                    <td>
                                        <code class="invite-code-text">{{ item.code }}</code>
                                    </td>
                                    <td>
                                        <span :class="['invite-status', item.status.toLowerCase()]">
                                            {{ item.status === 'ACTIVE' ? '有效' : item.status === 'USED' ? '已使用' : '已过期' }}
                                        </span>
                                    </td>
                                    <td class="invite-expiry">{{ fmtExpiry(item.expiredAt) }}</td>
                                    <td>
                                        <button
                                            v-if="item.status === 'ACTIVE'"
                                            type="button"
                                            :class="['invite-copy-btn', { copied: copiedCode === item.code }]"
                                            @click="copyInviteLink(item.code)"
                                        >
                                            {{ copiedCode === item.code ? '已复制 ✓' : '复制邀请链接' }}
                                        </button>
                                        <span v-else class="invite-copy-disabled">—</span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div v-else class="invite-empty">暂无邀请码，点击下方按钮生成</div>

                    <div class="invite-actions">
                        <button
                            type="button"
                            class="invite-generate-btn"
                            :disabled="inviteGenerating || activeInviteCount >= 3"
                            @click="generateCode"
                        >
                            {{ inviteGenerating ? '生成中...' : '生成邀请码' }}
                        </button>
                        <span v-if="activeInviteCount >= 3" class="invite-limit-hint">
                            最多同时持有 3 个有效邀请码
                        </span>
                    </div>
                </template>
            </section>

            <section class="badge-wall-section">
                <div class="badge-wall-head">
                    <div>
                        <p class="eyebrow">成就收藏</p>
                        <h2>徽章墙</h2>
                    </div>
                    <span>{{ badgeState.badges.filter((badge) => badge.owned).length }} / {{ badgeState.badges.length }}</span>
                </div>
                <BadgeWall
                    :badges="badgeState.badges"
                    :saving="badgeSaving"
                    @equip="equipBadge"
                />
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

<style scoped src="@/styles/views/GrowthView.part-1.css"></style>
<style scoped src="@/styles/views/GrowthView.part-2.css"></style>
<style scoped src="@/styles/views/GrowthView.part-3.css"></style>
