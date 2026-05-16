<script setup>
import {computed, onMounted, ref} from 'vue';
import {useHead} from '@unhead/vue';
import {RouterLink} from 'vue-router';
import SiteHeader from '@/components/SiteHeader.vue';
import CreatorSidebar from '@/components/CreatorSidebar.vue';
import {
  getMyExpJournalsApi,
  getMyGrowthApi,
  getMyRevenueApi,
  getPointAccountApi,
  getPointJournalsApi,
  getSignInCalendarApi,
  signInApi,
} from '@/api/growth';
import {useSession} from '@/stores/session';

useHead({title: '积分与成长 - DevNotes'});

const {state: session} = useSession();

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
const journals = ref([]);
const journalTotal = ref(0);
const journalPage = ref(1);
const journalSize = 15;
const journalLoading = ref(false);
const journalError = ref('');
const journalCache = ref({});

const SOURCE_TYPE_LABELS = {
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

const applyJournalData = (items, total) => {
    journals.value = items;
    journalTotal.value = total;
};

const loadJournals = async (options = {}) => {
    const { force = false } = options;
    const requestedTab = journalTab.value;
    const requestedPage = journalPage.value;
    const cacheKey = getJournalCacheKey(requestedTab, requestedPage);
    if (!force && journalCache.value[cacheKey]) {
        const cached = journalCache.value[cacheKey];
        applyJournalData(cached.items, cached.total);
        journalError.value = '';
        journalLoading.value = false;
        return;
    }
    journalLoading.value = true;
    journalError.value = '';
    try {
        let nextItems = [];
        let nextTotal = 0;
        if (requestedTab === 'points') {
            const result = await getPointJournalsApi({page: requestedPage, size: journalSize});
            nextItems = result.items || result.records || result || [];
            nextTotal = result.total || nextItems.length;
        } else if (requestedTab === 'exp') {
            const data = await getMyExpJournalsApi(50);
            nextItems = Array.isArray(data) ? data : [];
            nextTotal = nextItems.length;
        } else if (requestedTab === 'revenue') {
            const result = await getMyRevenueApi({page: requestedPage, size: journalSize});
            nextItems = result.items || result.records || result || [];
            nextTotal = result.total || nextItems.length;
        }
        journalCache.value = {
            ...journalCache.value,
            [cacheKey]: {items: nextItems, total: nextTotal}
        };
        if (journalTab.value === requestedTab && journalPage.value === requestedPage) {
            applyJournalData(nextItems, nextTotal);
        }
    } catch (e) {
        if (journalTab.value === requestedTab && journalPage.value === requestedPage) {
            journalError.value = e.message || '加载失败';
            applyJournalData([], 0);
        }
    } finally {
        if (journalTab.value === requestedTab && journalPage.value === requestedPage) {
            journalLoading.value = false;
        }
    }
};

const switchTab = (tab) => {
    journalTab.value = tab;
    journalPage.value = 1;
    loadJournals();
};

const journalTotalPages = computed(() => Math.ceil(journalTotal.value / journalSize) || 1);

const prevPage = () => {
    if (journalPage.value > 1) {
        journalPage.value--;
        loadJournals();
    }
};

const nextPage = () => {
    if (journalPage.value < journalTotalPages.value) {
        journalPage.value++;
        loadJournals();
    }
};

// ── 格式化 ───────────────────────────────────────────────────────
const fmtTime = (t) => {
    if (!t) return '-';
    const d = new Date(t);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六'];

const currentLevel = computed(() => growth.value?.currentLevel ?? growth.value?.level ?? 1);
const currentExp = computed(() => growth.value?.currentExp ?? growth.value?.exp ?? 0);
const signInPoints = computed(() => signInResult.value?.pointsGranted ?? signInResult.value?.pointsEarned ?? 0);
const signInDays = computed(() => signInResult.value?.consecutiveDays ?? signInResult.value?.continuousDays ?? 0);

onMounted(async () => {
    await Promise.all([loadGrowth(), loadAccount(), loadCalendar(calendarMonth.value)]);
    loadJournals();
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
                    <h1>积分与成长</h1>
                </div>
            </div>

            <!-- 顶部卡片区 -->
            <div class="growth-cards">
                <!-- 等级 & 经验卡片 -->
                <div class="growth-card level-card">
                    <div v-if="growthLoading" class="card-skeleton"></div>
                    <template v-else-if="growth">
                        <div class="level-badge">Lv.{{ currentLevel }}</div>
                        <div class="level-info">
                            <p class="level-name">{{ growth.levelName || `${currentLevel} 级` }}</p>
                            <p class="level-exp">
                                经验值 <strong>{{ currentExp }}</strong>
                                <span v-if="growth.expToNextLevel > 0">
                                    / 距升级还需 <strong>{{ growth.expToNextLevel }}</strong> 经验
                                </span>
                                <span v-else class="max-level">（已达最高等级）</span>
                            </p>
                            <div class="exp-bar">
                                <div
                                    class="exp-fill"
                                    :style="{ width: `${Math.min(growth.progressPercent || 0, 100)}%` }"
                                ></div>
                            </div>
                            <p class="exp-percent">{{ growth.progressPercent || 0 }}%</p>
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

                <!-- 签到卡片 -->
                <div class="growth-card signin-card">
                    <div class="signin-header">
                        <h3>每日签到</h3>
                        <div v-if="signInResult && !signInError" class="signin-reward">
                            🎉 +{{ signInPoints }} 积分（连续 {{ signInDays }} 天）
                        </div>
                        <div v-if="signInError" class="signin-error">{{ signInError }}</div>
                    </div>

                    <!-- 日历月份导航 -->
                    <div class="calendar-nav">
                        <button type="button" class="cal-nav-btn" @click="prevMonth">‹</button>
                        <span class="cal-month">{{ calendarMonth }}</span>
                        <button type="button" class="cal-nav-btn" :disabled="isCurrentMonth" @click="nextMonth">›</button>
                    </div>

                    <!-- 日历格子 -->
                    <div
                        class="calendar-grid"
                        :class="{ 'calendar-grid--loading': calendarLoading && !calendarInitialLoading }"
                    >
                        <div v-for="w in WEEKDAYS" :key="w" class="cal-weekday">{{ w }}</div>
                        <template v-if="calendarInitialLoading">
                            <div v-for="i in 42" :key="i" class="cal-day cal-day--skeleton"></div>
                        </template>
                        <template v-else>
                            <div
                                v-for="(day, idx) in calendarDays"
                                :key="idx"
                                :class="['cal-day', {
                                    'cal-day--empty': !day,
                                    'cal-day--signed': day && day.signed,
                                    'cal-day--today': day && day.isToday,
                                }]"
                            >
                                <span v-if="day">{{ day.date }}</span>
                            </div>
                        </template>
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

            <!-- 流水记录区 -->
            <section class="journal-section">
                <div class="journal-tabs">
                    <button
                        type="button"
                        :class="['tab-btn', {active: journalTab === 'points'}]"
                        @click="switchTab('points')"
                    >积分流水</button>
                    <button
                        type="button"
                        :class="['tab-btn', {active: journalTab === 'exp'}]"
                        @click="switchTab('exp')"
                    >经验流水</button>
                    <button
                        type="button"
                        :class="['tab-btn', {active: journalTab === 'revenue'}]"
                        @click="switchTab('revenue')"
                    >分账收益</button>
                </div>

                <div class="journal-content" :aria-busy="journalLoading">
                    <div v-if="journalLoading" class="journal-loading">加载中...</div>
                    <div v-else-if="journalError" class="journal-error">{{ journalError }}</div>
                    <template v-else-if="journals.length > 0">
                        <table class="journal-table">
                            <thead>
                                <tr v-if="journalTab !== 'revenue'">
                                    <th>类型</th>
                                    <th v-if="journalTab === 'points'">变动</th>
                                    <th v-if="journalTab === 'points'">余额</th>
                                    <th v-if="journalTab === 'exp'">经验值</th>
                                    <th v-if="journalTab === 'revenue'">总积分</th>
                                    <th v-if="journalTab === 'revenue'">作者分成</th>
                                    <th v-if="journalTab === 'revenue'">结算状态</th>
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
                                <template v-if="journalTab === 'points'">
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
                                <template v-if="journalTab === 'exp'">
                                    <tr v-for="item in journals" :key="item.id">
                                        <td><span class="tag-chip">{{ sourceLabel(item.eventType) }}</span></td>
                                        <td :class="['delta', 'plus']">+{{ item.delta ?? item.expAmount ?? 0 }}</td>
                                        <td class="desc-cell">{{ expJournalDesc(item) }}</td>
                                        <td class="time-cell">{{ fmtTime(item.createdAt) }}</td>
                                    </tr>
                                </template>
                                <!-- 分账收益 -->
                                <template v-if="journalTab === 'revenue'">
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
                        <div v-if="journalTab !== 'exp'" class="journal-pagination">
                            <button type="button" :disabled="journalPage <= 1" @click="prevPage">上一页</button>
                            <span>第 {{ journalPage }} / {{ journalTotalPages }} 页</span>
                            <button
                                type="button"
                                :disabled="journalPage >= journalTotalPages"
                                @click="nextPage"
                            >下一页</button>
                        </div>
                    </template>
                    <div v-else class="journal-empty">暂无记录</div>
                </div>
            </section>
        </section>
    </main>
</template>

<style scoped>
.growth-main {
    min-width: 0;
}

.growth-heading h1 {
    font-weight: 700;
    margin: 0;
    color: var(--text-strong);
}

/* ── 卡片区 ─────────────────────────────────── */
.growth-cards {
    display: grid;
    grid-template-columns: 1fr 1fr 1.4fr;
    gap: 20px;
    margin-bottom: 32px;
}

.growth-card {
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    padding: 24px;
    min-height: 180px;
    box-shadow: var(--shadow);
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
    display: flex;
    gap: 16px;
    align-items: flex-start;
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
    font-size: 13px;
    color: var(--text-muted);
    margin: 0 0 10px;
}

.level-exp strong { color: var(--text-strong); }

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

/* ── 签到卡片 ───────────────────────────────── */
.signin-card {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.signin-header h3 {
    font-size: 15px;
    font-weight: 600;
    margin: 0 0 4px;
    color: var(--text-strong);
}

.signin-reward {
    font-size: 13px;
    color: #16a34a;
    font-weight: 500;
}

.signin-error {
    font-size: 13px;
    color: #dc2626;
}

.calendar-nav {
    display: flex;
    align-items: center;
    gap: 10px;
}

.cal-nav-btn {
    width: 24px;
    height: 24px;
    border: 1px solid var(--line);
    background: var(--surface);
    border-radius: 6px;
    cursor: pointer;
    font-size: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text);
    padding: 0;
}

.cal-nav-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.cal-month {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-strong);
    flex: 1;
    text-align: center;
}

.calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 3px;
}

.calendar-grid--loading .cal-day {
    opacity: 0.62;
}

.cal-weekday {
    font-size: 11px;
    color: var(--text-muted);
    text-align: center;
    padding: 2px 0;
}

.cal-day {
    aspect-ratio: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    font-size: 12px;
    color: var(--text);
    background: var(--surface-soft);
    transition: background 0.15s;
}

.cal-day--empty { background: transparent; }
.cal-day--skeleton { background: var(--line); animation: shimmer 1.5s infinite; }
.cal-day--signed { background: var(--accent, #6c63ff); color: #fff; font-weight: 600; }
.cal-day--today:not(.cal-day--signed) { border: 2px solid var(--accent, #6c63ff); font-weight: 600; color: var(--accent, #6c63ff); }

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
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    padding: 24px;
    box-shadow: var(--shadow);
}

.journal-tabs {
    display: flex;
    gap: 4px;
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

.tab-btn:not(.active):hover { color: var(--text); }

.journal-content {
    min-height: 360px;
    overflow-x: auto;
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
@media (max-width: 1024px) {
    .growth-cards {
        grid-template-columns: 1fr 1fr;
    }
    .signin-card {
        grid-column: 1 / -1;
    }
}

@media (max-width: 768px) {
    .growth-cards {
        grid-template-columns: 1fr;
    }
    .signin-card {
        grid-column: auto;
    }
}
</style>

