<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { getArticleRankingsApi, getAuthorRankingsApi } from '@/api/rankings';
import { useSession } from '@/stores/session';

const router = useRouter();
const articleRankings = ref([]);
const authorRankings = ref([]);
const loading = ref(false);
const errorMessage = ref('');
const { state } = useSession();

const fetchRankings = async () => {
    loading.value = true;
    errorMessage.value = '';
    try {
        const [articleList, authorList] = await Promise.all([
            getArticleRankingsApi(10),
            getAuthorRankingsApi(10)
        ]);
        articleRankings.value = articleList || [];
        authorRankings.value = authorList || [];
    } catch (error) {
        articleRankings.value = [];
        authorRankings.value = [];
        errorMessage.value = error.message || '排行榜加载失败';
    } finally {
        loading.value = false;
    }
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

        <p v-if="errorMessage" class="feed-message">{{ errorMessage }}</p>

        <div class="ranking-layout">
            <section class="ranking-feed" data-testid="ranking-articles">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">文章榜</p>
                        <h2>高热文章</h2>
                    </div>
                </div>
                <div v-if="loading" class="ranking-state">加载中...</div>
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
                                <img :src="article.cover" :alt="article.coverAlt" loading="lazy">
                            </div>
                            <div class="ranking-post-content">
                                <div class="post-meta ranking-post-meta">
                                    <span>{{ article.category || '未分类' }}</span>
                                    <span>{{ article.readingTime }}</span>
                                    <span>{{ article.updatedText }}</span>
                                </div>
                                <h3 class="ranking-post-title">{{ article.title }}</h3>
                                <p class="ranking-post-summary">
                                    {{ article.summary || '这篇文章正在被很多人阅读，适合顺着这条内容线索继续深入。' }}
                                </p>
                                <div class="post-footer ranking-post-footer">
                                    <RouterLink
                                        class="ranking-post-author"
                                        :to="`/users/${article.author.id}`"
                                        @click.stop
                                        @keydown.enter.stop
                                        @keydown.space.stop
                                    >
                                        <img :src="article.author.avatar" alt="作者头像" loading="lazy">
                                        <span>{{ article.author.name }}</span>
                                    </RouterLink>
                                    <span>{{ article.stats.views }}</span>
                                    <span>{{ article.stats.likes }}</span>
                                    <span>{{ article.stats.comments }}</span>
                                </div>
                            </div>
                        </article>
                    </li>
                </ol>
            </section>

            <aside class="ranking-sidebar" data-testid="ranking-authors">
                <section class="ranking-sidebar-card">
                    <div class="section-heading compact ranking-side-heading">
                        <div>
                            <p class="eyebrow">作者榜</p>
                            <h2>影响力作者</h2>
                        </div>
                    </div>
                    <div v-if="loading" class="ranking-state">加载中...</div>
                    <div v-else class="ranking-author-list">
                        <div v-for="author in authorRankings" :key="author.user.id" class="ranking-author-card">
                            <div class="ranking-author-head">
                                <span class="ranking-side-rank">{{ author.rank }}</span>
                                <RouterLink class="ranking-author-avatar" :to="`/users/${author.user.id}`">
                                    <img :src="author.user.avatar" alt="作者头像" loading="lazy">
                                </RouterLink>
                                <div class="ranking-author-copy">
                                    <RouterLink :to="`/users/${author.user.id}`">{{ author.user.name }}</RouterLink>
                                    <p>{{ author.articleCount }} 篇文章 · {{ author.followerCount }} 粉丝</p>
                                </div>
                            </div>
                            <div class="ranking-author-footer">
                                <div class="ranking-author-meta">
                                    <span>{{ author.totalViewCount }} 阅读</span>
                                    <span>{{ author.totalLikeCount }} 获赞</span>
                                </div>
                                <span v-if="author.user.id === state.user?.id" class="ranking-author-self-tag">当前账号</span>
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
.ranking-hero {
    display: grid;
    gap: 10px;
    max-width: 760px;
}

.ranking-hero h1,
.ranking-hero p {
    margin: 0;
}

.ranking-hero h1 {
    font-size: clamp(32px, 4vw, 44px);
    line-height: 1.14;
}

.ranking-hero p:last-child {
    color: var(--muted);
    line-height: 1.8;
}

.ranking-layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) var(--layout-sidebar-width);
    gap: 24px;
    align-items: start;
}

.ranking-feed,
.ranking-sidebar {
    min-width: 0;
}

.ranking-feed {
    display: grid;
    gap: 16px;
}

.ranking-sidebar {
    display: grid;
    gap: 16px;
}

.ranking-sidebar-card {
    position: sticky;
    top: 96px;
    display: grid;
    gap: 14px;
    padding: 18px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
}

.ranking-post-list,
.ranking-author-list {
    display: grid;
    gap: 16px;
    padding: 0;
    margin: 0;
    list-style: none;
}

.ranking-post-shell {
    list-style: none;
}

.ranking-post-item {
    display: grid;
    grid-template-columns: 52px 190px minmax(0, 1fr);
    gap: 18px;
    align-items: center;
    padding: 18px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 8px;
    transition: transform 0.18s ease, background-color 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
    cursor: pointer;
}

.ranking-post-item:hover,
.ranking-post-item:focus-visible {
    background: color-mix(in srgb, var(--surface-soft) 42%, white);
    border-color: rgba(15, 143, 117, 0.22);
    box-shadow: 0 14px 30px rgba(24, 32, 31, 0.08);
    transform: translateY(-2px);
}

.ranking-post-item:focus-visible {
    outline: 2px solid color-mix(in srgb, var(--brand) 56%, white);
    outline-offset: 4px;
}

.ranking-post-rank {
    display: grid;
    place-items: center;
    gap: 2px;
    width: 52px;
    min-height: 72px;
    color: var(--brand-strong);
    background: linear-gradient(180deg, rgba(15, 143, 117, 0.13), rgba(15, 143, 117, 0.04));
    border: 1px solid color-mix(in srgb, var(--brand) 14%, var(--line));
    border-radius: 8px;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.ranking-post-rank.top {
    background: linear-gradient(180deg, rgba(15, 143, 117, 0.18), rgba(19, 167, 134, 0.06));
}

.ranking-post-rank span {
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.16em;
}

.ranking-post-rank strong {
    font-size: 22px;
    line-height: 1;
}

.ranking-post-cover {
    min-height: 140px;
    overflow: hidden;
    background: var(--surface-soft);
    border-radius: 8px;
}

.ranking-post-cover img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.24s ease;
}

.ranking-post-item:hover .ranking-post-cover img,
.ranking-post-item:focus-visible .ranking-post-cover img {
    transform: scale(1.04);
}

.ranking-post-content {
    display: flex;
    min-width: 0;
    flex-direction: column;
}

.ranking-post-meta {
    margin: 0;
}

.ranking-post-title {
    margin: 10px 0 8px;
    font-size: 22px;
    line-height: 1.35;
    min-width: 0;
    color: var(--text);
    font-weight: 700;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.ranking-post-item:hover .ranking-post-title,
.ranking-post-item:focus-visible .ranking-post-title,
.ranking-post-item:hover .ranking-post-author,
.ranking-post-item:focus-visible .ranking-post-author,
.ranking-author-copy a:hover,
.ranking-author-avatar:hover + .ranking-author-copy a {
    color: var(--brand-strong);
}

.ranking-post-summary {
    margin: 0 0 18px;
    color: var(--muted);
    font-size: 14px;
    line-height: 1.7;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.ranking-post-footer {
    margin-top: auto;
}

.ranking-post-author {
    display: inline-flex;
    gap: 8px;
    align-items: center;
    min-width: 0;
    color: var(--text);
    text-decoration: none;
    transition: color 0.18s ease;
    font-weight: 700;
    position: relative;
    z-index: 1;
}

.ranking-post-author img {
    width: 28px;
    height: 28px;
    object-fit: cover;
    border-radius: 50%;
}

.ranking-post-author:hover,
.ranking-post-author:focus-visible {
    color: var(--brand-strong);
}

.ranking-side-heading {
    margin-bottom: 4px;
}

.ranking-author-card {
    display: grid;
    gap: 12px;
    padding: 14px 0;
    border-bottom: 1px solid color-mix(in srgb, var(--line) 68%, transparent);
    transition: transform 0.18s ease, background-color 0.18s ease, border-color 0.18s ease;
}

.ranking-author-card:last-child {
    padding-bottom: 0;
    border-bottom: 0;
}

.ranking-author-card:hover {
    background: color-mix(in srgb, var(--surface-soft) 38%, white);
    transform: translateY(-1px);
}

.rank-author-head p,
.ranking-author-copy p {
    margin: 4px 0 0;
    color: var(--muted);
    font-size: 13px;
}

.ranking-author-head {
    display: grid;
    grid-template-columns: 28px 42px minmax(0, 1fr);
    gap: 12px;
    align-items: center;
}

.ranking-author-avatar {
    display: inline-flex;
    border-radius: 14px;
}

.ranking-author-avatar img {
    width: 42px;
    height: 42px;
    object-fit: cover;
    border-radius: 14px;
}

.ranking-side-rank {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    color: var(--brand-strong);
    font-size: 12px;
    font-weight: 700;
    background: color-mix(in srgb, var(--surface-soft) 82%, white);
    border-radius: 999px;
}

.ranking-author-copy {
    min-width: 0;
}

.ranking-author-copy a {
    color: var(--text);
    font-weight: 700;
    text-decoration: none;
}

.ranking-author-meta {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.ranking-author-footer {
    display: flex;
    gap: 10px;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
}

.ranking-author-meta span {
    display: inline-flex;
    align-items: center;
    min-height: 24px;
    padding: 0 8px;
    color: var(--muted);
    font-size: 12px;
    background: color-mix(in srgb, var(--surface-soft) 74%, white);
    border-radius: 999px;
}

.ranking-author-self-tag {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0 10px;
    color: var(--muted);
    font-size: 12px;
    background: color-mix(in srgb, var(--surface-soft) 74%, white);
    border-radius: 999px;
}

.ranking-author-card :deep(.author-follow-button) {
    min-height: 32px;
    padding-inline: 12px;
    border-radius: 999px;
}

.ranking-state {
    color: var(--muted);
    line-height: 1.7;
}

@media (max-width: 960px) {
    .ranking-layout {
        grid-template-columns: 1fr;
        gap: 24px;
    }

    .ranking-sidebar-card {
        position: static;
    }
}

@media (max-width: 760px) {
    .ranking-post-item {
        grid-template-columns: 40px minmax(0, 1fr);
        gap: 14px;
    }

    .ranking-post-rank {
        width: 40px;
        min-height: 56px;
        border-radius: 8px;
    }

    .ranking-post-rank span {
        display: none;
    }

    .ranking-post-rank strong {
        font-size: 16px;
    }

    .ranking-post-cover {
        grid-column: 1 / -1;
        min-height: 0;
        aspect-ratio: 16 / 9;
    }

    .ranking-sidebar-card {
        padding: 16px;
    }

    .ranking-post-title {
        font-size: 19px;
    }

    .ranking-post-content {
        grid-column: 2;
    }

    .ranking-author-footer {
        align-items: start;
    }

    .ranking-post-footer {
        gap: 8px;
    }

    .ranking-post-footer,
    .ranking-author-footer {
        flex-wrap: wrap;
    }
}
</style>
