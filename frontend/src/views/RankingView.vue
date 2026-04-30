<script setup>
import {onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import {getArticleRankingsApi, getAuthorRankingsApi} from '@/api/rankings';
import {useSession} from '@/stores/session';
import {useStableListRequest} from '@/composables/useStableListRequest';

const router = useRouter();
const articleRankings = ref([]);
const authorRankings = ref([]);
const { state } = useSession();
const {
    initialLoading,
    refreshing,
    hasLoadedOnce,
    errorMessage,
    inlineError,
    runStableRequest
} = useStableListRequest();

const fetchRankings = async () => {
    const response = await runStableRequest(
        () => Promise.all([
            getArticleRankingsApi(10),
            getAuthorRankingsApi(10)
        ]),
        {
            silent: hasLoadedOnce.value,
            initialErrorMessage: '排行榜加载失败',
            refreshErrorMessage: '排行榜刷新失败，请稍后重试'
        }
    );
    if (response?.ignored || response?.error) {
        return;
    }
    const [articleList, authorList] = response.result || [];
    articleRankings.value = articleList || [];
    authorRankings.value = authorList || [];
};

const handleFollowChange = (target, followed) => {
    target.followed = followed;
    target.followerCount = Math.max(0, (target.followerCount || 0) + (followed ? 1 : -1));
};

const openArticle = (articleId) => {
    router.push(`/articles/${articleId}`);
};

onMounted(fetchRankings);
</script>

<template>
    <SiteHeader />
    <main class="page-shell" data-testid="ranking-page">
        <section class="ranking-hero">
            <p class="eyebrow">排行榜</p>
            <h1>看清此刻最有热度的文章和作者</h1>
            <p>先从最值得点开的内容入手，再顺着作者继续深挖，读得更快也更准。</p>
        </section>

        <p v-if="inlineError" class="feed-message">{{ inlineError }}</p>
        <p v-if="errorMessage && !articleRankings.length && !authorRankings.length" class="feed-message">{{ errorMessage }}</p>
        <p v-if="refreshing" class="ranking-refresh-note">正在更新榜单...</p>

        <div class="ranking-layout">
            <section class="ranking-feed" data-testid="ranking-articles">
                <div class="ranking-section-head">
                    <span class="ranking-section-icon">🔥</span>
                    <h2>高热文章</h2>
                    <span class="eyebrow">文章榜</span>
                </div>
                <div v-if="initialLoading && !articleRankings.length" class="ranking-state">加载中...</div>
                <ol v-else class="ranking-post-list">
                    <li
                        v-for="(article, index) in articleRankings"
                        :key="article.id"
                        class="ranking-post-shell"
                    >
                        <article
                            class="ranking-post-item"
                            role="link"
                            tabindex="0"
                            @click="openArticle(article.id)"
                            @keydown.enter="openArticle(article.id)"
                            @keydown.space.prevent="openArticle(article.id)"
                        >
                            <div class="ranking-post-rank" :class="{ top: index < 3 }">
                                <span>TOP</span>
                                <strong>{{ index + 1 }}</strong>
                            </div>
                            <div class="ranking-post-cover" :aria-label="`查看文章：${article.title}`">
                                <img :src="article.cover" :alt="article.coverAlt" loading="lazy" decoding="async">
                            </div>
                            <div class="ranking-post-content">
                                <div class="ranking-post-meta">
                                    <span>{{ article.category || '未分类' }}</span>
                                    <span>{{ article.readingTime }}</span>
                                    <span>{{ article.updatedText }}</span>
                                </div>
                                <h3 class="ranking-post-title">{{ article.title }}</h3>
                                <p class="ranking-post-summary">
                                    {{ article.summary || '这篇文章正在被很多人阅读，适合顺着这条内容线索继续深挖。' }}
                                </p>
                                <div class="ranking-post-footer">
                                    <RouterLink
                                        class="ranking-post-author"
                                        :to="`/users/${article.author.id}`"
                                        @click.stop
                                        @keydown.enter.stop
                                        @keydown.space.stop
                                    >
                                        <img :src="article.author.avatar" alt="作者头像" loading="lazy" decoding="async">
                                        <span>{{ article.author.name }}</span>
                                    </RouterLink>
                                    <div class="ranking-post-stats">
                                        <span>{{ article.stats.views }}</span>
                                        <span>{{ article.stats.likes }}</span>
                                        <span>{{ article.stats.comments }}</span>
                                    </div>
                                </div>
                            </div>
                        </article>
                    </li>
                </ol>
            </section>

            <aside class="ranking-sidebar" data-testid="ranking-authors">
                <section class="ranking-sidebar-card">
                    <div class="ranking-section-head">
                        <span class="ranking-section-icon">✨</span>
                        <h2>影响力作者</h2>
                        <span class="eyebrow">作者榜</span>
                    </div>
                    <div v-if="initialLoading && !authorRankings.length" class="ranking-state">加载中...</div>
                    <div v-else class="ranking-author-list">
                        <div v-for="author in authorRankings" :key="author.user.id" class="ranking-author-card">
                            <span class="ranking-side-rank" :class="`rank-${author.rank}`">{{ author.rank }}</span>
                            <RouterLink class="ranking-author-avatar" :to="`/users/${author.user.id}`">
                                <img :src="author.user.avatar" alt="作者头像" loading="lazy" decoding="async">
                            </RouterLink>
                            <div class="ranking-author-copy">
                                <RouterLink :to="`/users/${author.user.id}`">{{ author.user.name }}</RouterLink>
                                <p>{{ author.articleCount }} 篇 · {{ author.totalLikeCount }} 赞 · {{ author.followerCount }} 粉丝</p>
                            </div>
                            <div class="ranking-author-action">
                                <span v-if="author.user.id === state.user?.id" class="ranking-author-self-tag">我</span>
                                <AuthorFollowButton
                                    v-else
                                    :user-id="author.user.id"
                                    :followed="author.followed"
                                    compact
                                    @change="(followed) => handleFollowChange(author, followed)"
                                />
                            </div>
                        </div>
                    </div>
                </section>
            </aside>
        </div>
    </main>
</template>

<style scoped>
/* ============================================================
   排行榜页 Hero 区
   ============================================================ */
.ranking-hero {
    display: grid;
    gap: 10px;
    max-width: 680px;
    padding-bottom: 8px;
}

.ranking-hero h1,
.ranking-hero p {
    margin: 0;
}

.ranking-hero h1 {
    font-size: clamp(26px, 3.5vw, 38px);
    font-weight: 700;
    line-height: 1.2;
    color: var(--text-strong);
}

.ranking-hero p:last-child {
    color: var(--muted);
    font-size: 15px;
    line-height: 1.8;
}

/* ============================================================
   页面整体布局
   ============================================================ */
.ranking-layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 320px;
    gap: 28px;
    align-items: start;
    margin-top: 24px;
}

.ranking-feed,
.ranking-sidebar {
    min-width: 0;
}

.ranking-feed {
    display: grid;
    gap: 0;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    overflow: hidden;
}

.ranking-sidebar {
    display: grid;
    gap: 0;
}

/* ============================================================
   分区标题
   ============================================================ */
.ranking-section-head {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 16px 20px 14px;
    border-bottom: 1px solid var(--line);
}

.ranking-section-icon {
    font-size: 18px;
    line-height: 1;
}

.ranking-section-head h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--text-strong);
}

.ranking-section-head .eyebrow {
    margin: 0 0 0 auto;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--muted-light);
}

/* ============================================================
   文章榜列表
   ============================================================ */
.ranking-post-list {
    display: grid;
    gap: 0;
    padding: 0;
    margin: 0;
    list-style: none;
}

.ranking-post-shell {
    list-style: none;
    border-bottom: 1px solid var(--line);
}

.ranking-post-shell:last-child {
    border-bottom: none;
}

.ranking-post-item {
    display: grid;
    grid-template-columns: 52px 130px minmax(0, 1fr);
    gap: 0;
    align-items: stretch;
    cursor: pointer;
    transition: background 0.12s;
}

.ranking-post-item:hover,
.ranking-post-item:focus-visible {
    background: var(--surface-soft);
}

.ranking-post-item:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: -2px;
}

/* 排名数字列 */
.ranking-post-rank {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0;
    padding: 0 6px;
    border-right: 1px solid var(--line);
    background: var(--surface-soft);
}

.ranking-post-rank span {
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.12em;
    color: var(--muted-light);
    text-transform: uppercase;
}

.ranking-post-rank strong {
    font-size: 20px;
    font-weight: 800;
    line-height: 1;
    color: var(--muted);
}

/* 前三名高亮排名 */
.ranking-post-rank.top strong {
    color: var(--brand);
}

.ranking-post-shell:nth-child(1) .ranking-post-rank {
    background: #fffbeb;
}

.ranking-post-shell:nth-child(1) .ranking-post-rank strong {
    color: #b45309;
}

.ranking-post-shell:nth-child(2) .ranking-post-rank strong {
    color: #6b7280;
}

.ranking-post-shell:nth-child(3) .ranking-post-rank strong {
    color: #b45309;
    opacity: 0.7;
}

/* 封面图列 */
.ranking-post-cover {
    width: 130px;
    aspect-ratio: 4 / 3;
    overflow: hidden;
    background: var(--surface-muted);
    flex-shrink: 0;
    border-right: 1px solid var(--line);
}

.ranking-post-cover img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.22s ease;
}

.ranking-post-item:hover .ranking-post-cover img {
    transform: scale(1.05);
}

/* 内容列 */
.ranking-post-content {
    display: flex;
    flex-direction: column;
    padding: 14px 16px;
    min-width: 0;
    gap: 6px;
}

.ranking-post-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin: 0;
}

.ranking-post-meta span {
    font-size: 12px;
    color: var(--muted);
}

.ranking-post-meta span + span::before {
    content: '·';
    margin-right: 6px;
}

.ranking-post-title {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    line-height: 1.4;
    color: var(--text-strong);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.ranking-post-item:hover .ranking-post-title {
    color: var(--brand);
}

.ranking-post-summary {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
    line-height: 1.65;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    flex: 1;
}

.ranking-post-footer {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: auto;
    padding-top: 4px;
    flex-wrap: wrap;
}

.ranking-post-author {
    display: inline-flex;
    gap: 6px;
    align-items: center;
    min-width: 0;
    color: var(--text);
    font-size: 13px;
    font-weight: 600;
    text-decoration: none;
    transition: color 0.15s;
    flex-shrink: 0;
}

.ranking-post-author img {
    width: 22px;
    height: 22px;
    object-fit: cover;
    border-radius: 50%;
    flex-shrink: 0;
}

.ranking-post-author:hover {
    color: var(--brand);
}

.ranking-post-stats {
    display: flex;
    gap: 10px;
    margin-left: auto;
    flex-shrink: 0;
}

.ranking-post-stats span {
    font-size: 12px;
    color: var(--muted);
}

/* ============================================================
   作者榜侧边卡片
   ============================================================ */
.ranking-sidebar-card {
    position: sticky;
    top: 72px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--radius-md);
    overflow: hidden;
}

.ranking-author-list {
    display: grid;
    gap: 0;
    padding: 0;
    margin: 0;
    list-style: none;
}

.ranking-author-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--line);
    transition: background 0.12s;
}

.ranking-author-card:last-child {
    border-bottom: none;
}

.ranking-author-card:hover {
    background: var(--surface-soft);
}

/* 排名数字 */
.ranking-side-rank {
    flex-shrink: 0;
    width: 22px;
    text-align: center;
    font-size: 13px;
    font-weight: 800;
    color: var(--muted);
    line-height: 1;
}

.ranking-side-rank.rank-1 {
    color: #b45309;
}

.ranking-side-rank.rank-2 {
    color: #6b7280;
}

.ranking-side-rank.rank-3 {
    color: #c57c3a;
}

/* 头像 */
.ranking-author-avatar {
    flex-shrink: 0;
    display: inline-flex;
}

.ranking-author-avatar img {
    width: 38px;
    height: 38px;
    object-fit: cover;
    border-radius: 50%;
    border: 1px solid var(--line);
}

/* 名字+简介 */
.ranking-author-copy {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.ranking-author-copy a {
    display: block;
    color: var(--text-strong);
    font-weight: 600;
    font-size: 14px;
    text-decoration: none;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: color 0.15s;
}

.ranking-author-copy a:hover {
    color: var(--brand);
}

.ranking-author-copy p {
    margin: 0;
    color: var(--muted);
    font-size: 12px;
    line-height: 1.4;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

/* 关注按钮区 */
.ranking-author-action {
    flex-shrink: 0;
    display: flex;
    align-items: center;
}

.ranking-author-self-tag {
    font-size: 11px;
    font-weight: 600;
    color: var(--brand);
    white-space: nowrap;
    padding: 3px 8px;
    background: var(--brand-soft);
    border: 1px solid var(--brand-hover);
    border-radius: var(--radius-sm);
}

.ranking-author-card :deep(.author-follow-button) {
    min-height: 28px;
    padding-inline: 12px;
    border-radius: var(--radius-sm);
    font-size: 12px;
}

/* ============================================================
   状态 & 加载
   ============================================================ */
.ranking-state {
    padding: 24px 20px;
    color: var(--muted);
    font-size: 14px;
}

.ranking-refresh-note {
    margin: 12px 0 0;
    padding: 8px 12px;
    color: var(--brand);
    font-size: 13px;
    font-weight: 600;
    background: var(--brand-soft);
    border: 1px solid rgba(37, 99, 235, 0.14);
    border-radius: var(--radius-sm);
}

/* ============================================================
   响应式
   ============================================================ */
@media (max-width: 960px) {
    .ranking-layout {
        grid-template-columns: 1fr;
        gap: 20px;
    }

    .ranking-sidebar-card {
        position: static;
    }
}

@media (max-width: 760px) {
    .ranking-layout {
        margin-top: 16px;
    }

    .ranking-post-item {
        grid-template-columns: 44px minmax(0, 1fr);
    }

    .ranking-post-cover {
        display: none;
    }

    .ranking-post-content {
        padding: 12px 14px;
    }

    .ranking-post-title {
        font-size: 15px;
    }

    .ranking-post-summary {
        -webkit-line-clamp: 1;
    }

    .ranking-section-head {
        padding: 12px 16px 10px;
    }
}
</style>
