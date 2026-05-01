<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { getAdminLogsApi, getAdminStatsApi } from '@/api/admin';
import { formatAdminDateTime, useAdminRefresh } from '@/views/admin/adminShared';

const loading = ref(false);
const error = ref('');
const stats = ref(null);
const recentLogs = ref([]);

const statCards = computed(() => {
    const data = stats.value || {};
    return [
        { label: '用户总数', value: data.totalUsers || 0, helper: `今日新增 ${data.todayNewUsers || 0}`, color: 'blue' },
        { label: '正常用户', value: data.normalUsers || 0, helper: `已禁用 ${data.disabledUsers || 0}`, color: 'green' },
        { label: '文章总数', value: data.totalArticles || 0, helper: `近 7 天新增 ${data.sevenDayArticles || 0}`, color: 'purple' },
        { label: '草稿文章', value: data.draftArticles || 0, helper: '待继续编辑的内容池', color: 'orange' },
        { label: '已发布文章', value: data.publishedArticles || 0, helper: `已下架 ${data.offlineArticles || 0}`, color: 'teal' },
        { label: '已删除文章', value: data.deletedArticles || 0, helper: '重点关注误删与回收策略', color: 'red' },
        { label: '评论总数', value: data.totalComments || 0, helper: `今日新增 ${data.todayNewComments || 0}`, color: 'indigo' },
        { label: '近 7 天用户', value: data.sevenDayUsers || 0, helper: `近 7 天评论 ${data.sevenDayComments || 0}`, color: 'pink' }
    ];
});

const quickLinks = [
    { title: '管理用户状态', description: '快速处理禁用、恢复和账号检索。', to: '/admin/users' },
    { title: '管理文章状态', description: '筛选已发布、草稿和下架文章。', to: '/admin/articles' },
    { title: '处理评论与审计', description: '查看评论删除和管理员操作记录。', to: '/admin/comments' }
];

// ==================== 7 日趋势折线图 ====================

const trendMetric = ref('newArticles');

const trendSeries = computed(() => {
    const data = stats.value || {};
    const trend = data.sevenDayTrend || [];
    return trend.map(p => ({
        date: p.date ? p.date.slice(5) : '',  // MM-DD
        value: p[trendMetric.value] || 0
    }));
});

const trendMax = computed(() => {
    const vals = trendSeries.value.map(p => p.value);
    return Math.max(...vals, 1);
});

const trendPolyline = computed(() => {
    const w = 420;
    const h = 90;
    const pts = trendSeries.value;
    if (!pts.length) return '';
    return pts.map((p, i) => {
        const x = pts.length === 1 ? w / 2 : (i / (pts.length - 1)) * w;
        const y = h - (p.value / trendMax.value) * h;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
});

const trendMetricOptions = [
    { key: 'newArticles', label: '新增文章' },
    { key: 'newUsers', label: '新增用户' },
    { key: 'newComments', label: '新增评论' }
];

// ==================== 分类分布 ====================

const categoryStats = computed(() => {
    const data = stats.value || {};
    return data.categoryStats || [];
});

const categoryTotal = computed(() => {
    return categoryStats.value.reduce((sum, c) => sum + (c.articleCount || 0), 0) || 1;
});

const categoryColors = [
    '#6366f1', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444',
    '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#84cc16'
];

// ==================== Top 作者 ====================

const topAuthors = computed(() => {
    const data = stats.value || {};
    return data.topAuthors || [];
});

// ==================== 加载 ====================

const loadOverview = async () => {
    loading.value = true;
    error.value = '';
    try {
        const [statsResult, logsResult] = await Promise.all([
            getAdminStatsApi(),
            getAdminLogsApi(1, 5)
        ]);
        stats.value = statsResult;
        recentLogs.value = logsResult.items || [];
    } catch (loadError) {
        error.value = loadError.message || '管理概览加载失败';
    } finally {
        loading.value = false;
    }
};

useAdminRefresh(loadOverview);

onMounted(loadOverview);
</script>

<template>
    <section class="admin-page-grid">
        <!-- ===== 数字统计卡片 ===== -->
        <section class="dashboard-content-panel">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">概览</p>
                    <h2>站点运行概况</h2>
                </div>
            </div>

            <p v-if="error && stats" class="backend-state-text error-text subtle">{{ error }}</p>
            <p v-if="error && !stats" class="backend-state-text error-text">{{ error }}</p>
            <div v-else-if="stats" class="admin-overview-grid" data-testid="admin-overview-stats">
                <article
                    v-for="card in statCards"
                    :key="card.label"
                    class="admin-overview-card"
                    :class="`card-accent-${card.color}`"
                >
                    <span>{{ card.label }}</span>
                    <strong>{{ card.value.toLocaleString() }}</strong>
                    <p>{{ card.helper }}</p>
                </article>
            </div>
        </section>

        <!-- ===== 7 日趋势折线图 ===== -->
        <section v-if="stats" class="dashboard-content-panel">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">趋势</p>
                    <h2>近 7 日新增趋势</h2>
                </div>
                <div class="trend-metric-tabs">
                    <button
                        v-for="opt in trendMetricOptions"
                        :key="opt.key"
                        class="trend-tab"
                        :class="{ active: trendMetric === opt.key }"
                        @click="trendMetric = opt.key"
                    >{{ opt.label }}</button>
                </div>
            </div>
            <div class="trend-chart-wrap">
                <svg viewBox="0 0 420 110" class="trend-svg" preserveAspectRatio="none">
                    <!-- 网格线 -->
                    <line x1="0" y1="30" x2="420" y2="30" stroke="#f0f0f0" stroke-width="1"/>
                    <line x1="0" y1="60" x2="420" y2="60" stroke="#f0f0f0" stroke-width="1"/>
                    <line x1="0" y1="90" x2="420" y2="90" stroke="#f0f0f0" stroke-width="1"/>
                    <!-- 折线 -->
                    <polyline
                        v-if="trendPolyline"
                        :points="trendPolyline"
                        fill="none"
                        stroke="#6366f1"
                        stroke-width="2.5"
                        stroke-linejoin="round"
                        stroke-linecap="round"
                    />
                    <!-- 数据点 -->
                    <g v-for="(p, i) in trendSeries" :key="i">
                        <circle
                            :cx="trendSeries.length === 1 ? 210 : (i / (trendSeries.length - 1)) * 420"
                            :cy="90 - (p.value / trendMax) * 90"
                            r="3.5"
                            fill="#6366f1"
                        />
                    </g>
                </svg>
                <!-- X 轴日期标签 -->
                <div class="trend-x-labels">
                    <span v-for="p in trendSeries" :key="p.date">{{ p.date }}</span>
                </div>
            </div>
            <!-- 数值汇总行 -->
            <div class="trend-summary-row">
                <div v-for="p in trendSeries" :key="p.date" class="trend-summary-cell">
                    <span class="trend-val">{{ p.value }}</span>
                </div>
            </div>
        </section>

        <!-- ===== 分类分布 + Top 作者 两列并排 ===== -->
        <section v-if="stats" class="dashboard-content-panel overview-two-col">
            <!-- 分类分布 -->
            <div class="overview-sub-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">内容</p>
                        <h2>文章分类分布</h2>
                    </div>
                </div>
                <div v-if="categoryStats.length" class="category-dist-list">
                    <div
                        v-for="(cat, idx) in categoryStats"
                        :key="cat.category"
                        class="category-dist-item"
                    >
                        <span class="cat-dot" :style="{ background: categoryColors[idx % categoryColors.length] }"></span>
                        <span class="cat-name">{{ cat.category }}</span>
                        <div class="cat-bar-wrap">
                            <div
                                class="cat-bar"
                                :style="{
                                    width: ((cat.articleCount / categoryTotal) * 100).toFixed(1) + '%',
                                    background: categoryColors[idx % categoryColors.length]
                                }"
                            ></div>
                        </div>
                        <span class="cat-count">{{ cat.articleCount }}</span>
                    </div>
                </div>
                <p v-else class="backend-state-text">暂无分类数据</p>
            </div>

            <!-- Top 作者 -->
            <div class="overview-sub-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">创作者</p>
                        <h2>活跃作者 Top 5</h2>
                    </div>
                </div>
                <div v-if="topAuthors.length" class="top-author-list">
                    <div
                        v-for="(author, idx) in topAuthors"
                        :key="author.authorId"
                        class="top-author-item"
                    >
                        <span class="top-rank">{{ idx + 1 }}</span>
                        <img
                            :src="author.avatarUrl || '/default-avatar.png'"
                            :alt="author.nickname || author.username"
                            class="top-author-avatar"
                        decoding="async">
                        <div class="top-author-info">
                            <strong>{{ author.nickname || author.username }}</strong>
                            <span>{{ author.articleCount }} 篇 · {{ (author.totalViews || 0).toLocaleString() }} 阅读</span>
                        </div>
                        <span class="top-author-likes">{{ (author.totalLikes || 0).toLocaleString() }} 赞</span>
                    </div>
                </div>
                <p v-else class="backend-state-text">暂无作者数据</p>
            </div>
        </section>

        <!-- ===== 快捷入口 ===== -->
        <section class="dashboard-content-panel">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">快捷入口</p>
                    <h2>今天先处理什么</h2>
                </div>
            </div>

            <div class="admin-quick-grid">
                <RouterLink
                    v-for="link in quickLinks"
                    :key="link.to"
                    :to="link.to"
                    class="admin-quick-card"
                >
                    <strong>{{ link.title }}</strong>
                    <span>{{ link.description }}</span>
                </RouterLink>
            </div>
        </section>

        <!-- ===== 管理员日志 ===== -->
        <section class="dashboard-content-panel">
            <div class="section-heading compact">
                <div>
                    <p class="eyebrow">最近操作</p>
                    <h2>管理员日志摘要</h2>
                </div>
                <RouterLink class="text-link" to="/admin/logs">查看全部</RouterLink>
            </div>

            <div v-if="recentLogs.length" class="admin-log-list">
                <article v-for="log in recentLogs" :key="log.id" class="admin-log-item">
                    <div>
                        <strong>{{ log.operation }}</strong>
                        <p>{{ log.detail }}</p>
                    </div>
                    <span>{{ formatAdminDateTime(log.createdAt) }}</span>
                </article>
            </div>
            <p v-else-if="!loading && !error" class="backend-state-text">最近还没有管理员操作记录</p>
        </section>
    </section>
</template>

<style scoped>
/* ===== 数字卡片颜色变体 ===== */
.admin-overview-card { border-top: 3px solid transparent; }
.card-accent-blue    { border-top-color: #3b82f6; }
.card-accent-green   { border-top-color: #10b981; }
.card-accent-purple  { border-top-color: #8b5cf6; }
.card-accent-orange  { border-top-color: #f59e0b; }
.card-accent-teal    { border-top-color: #14b8a6; }
.card-accent-red     { border-top-color: #ef4444; }
.card-accent-indigo  { border-top-color: #6366f1; }
.card-accent-pink    { border-top-color: #ec4899; }

/* ===== 趋势图 ===== */
.trend-metric-tabs {
    display: flex;
    gap: 4px;
}
.trend-tab {
    padding: 4px 10px;
    border: 1px solid var(--border-color, #e5e7eb);
    border-radius: 20px;
    font-size: 12px;
    background: transparent;
    cursor: pointer;
    color: var(--text-secondary, #6b7280);
    transition: all .15s;
}
.trend-tab.active,
.trend-tab:hover {
    background: #6366f1;
    color: #fff;
    border-color: #6366f1;
}
.trend-chart-wrap {
    margin-top: 12px;
    padding: 0 4px;
}
.trend-svg {
    width: 100%;
    height: 110px;
    display: block;
    overflow: visible;
}
.trend-x-labels {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: var(--text-secondary, #9ca3af);
    margin-top: 4px;
}
.trend-summary-row {
    display: flex;
    justify-content: space-between;
    margin-top: 2px;
}
.trend-summary-cell {
    flex: 1;
    text-align: center;
}
.trend-val {
    font-size: 12px;
    font-weight: 600;
    color: #6366f1;
}

/* ===== 两列面板 ===== */
.overview-two-col {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
}
.overview-sub-panel { min-width: 0; }

/* ===== 分类分布 ===== */
.category-dist-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-top: 12px;
}
.category-dist-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
}
.cat-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex-shrink: 0;
}
.cat-name {
    width: 80px;
    truncate: ellipsis;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: var(--text-primary);
}
.cat-bar-wrap {
    flex: 1;
    height: 6px;
    background: var(--border-color, #f3f4f6);
    border-radius: 3px;
    overflow: hidden;
}
.cat-bar {
    height: 100%;
    border-radius: 3px;
    transition: width .4s;
}
.cat-count {
    width: 28px;
    text-align: right;
    font-weight: 600;
    color: var(--text-primary);
    font-size: 12px;
}

/* ===== Top 作者 ===== */
.top-author-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 12px;
}
.top-author-item {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
}
.top-rank {
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: #6366f1;
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}
.top-author-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
}
.top-author-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
}
.top-author-info strong {
    font-size: 13px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.top-author-info span {
    font-size: 11px;
    color: var(--text-secondary, #6b7280);
}
.top-author-likes {
    font-size: 11px;
    color: #ef4444;
    white-space: nowrap;
}

@media (max-width: 640px) {
    .overview-two-col {
        grid-template-columns: 1fr;
    }
}
</style>
