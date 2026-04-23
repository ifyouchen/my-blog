<script setup>
import { onMounted, ref } from 'vue';
import AuthorFollowButton from '@/components/AuthorFollowButton.vue';
import SiteHeader from '@/components/SiteHeader.vue';
import { getArticleRankingsApi, getAuthorRankingsApi } from '@/api/rankings';

const articleRankings = ref([]);
const authorRankings = ref([]);
const loading = ref(false);
const errorMessage = ref('');

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

onMounted(fetchRankings);
</script>

<template>
    <SiteHeader />
    <main class="page-shell">
        <section class="ranking-hero">
            <p class="eyebrow">排行榜</p>
            <h1>看清此刻最有热度的文章和作者</h1>
            <p>先找到此刻最值得读的内容，再顺着作者和专题继续深挖。</p>
        </section>

        <p v-if="errorMessage" class="feed-message">{{ errorMessage }}</p>

        <div class="ranking-grid">
            <section class="ranking-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">文章榜</p>
                        <h2>高热文章</h2>
                    </div>
                </div>
                <div v-if="loading" class="ranking-state">加载中...</div>
                <ol v-else class="ranking-list">
                    <li v-for="(article, index) in articleRankings" :key="article.id" class="ranking-item">
                        <span class="ranking-no">{{ index + 1 }}</span>
                        <div class="ranking-body">
                            <RouterLink :to="`/articles/${article.id}`">{{ article.title }}</RouterLink>
                            <p>{{ article.category }} · {{ article.viewCount }} 阅读 · {{ article.likeCount }} 赞</p>
                        </div>
                    </li>
                </ol>
            </section>

            <section class="ranking-panel">
                <div class="section-heading compact">
                    <div>
                        <p class="eyebrow">作者榜</p>
                        <h2>影响力作者</h2>
                    </div>
                </div>
                <div v-if="loading" class="ranking-state">加载中...</div>
                <div v-else class="rank-author-list">
                    <div v-for="author in authorRankings" :key="author.user.id" class="rank-author-card">
                        <div class="rank-author-head">
                            <span class="ranking-no">{{ author.rank }}</span>
                            <img :src="author.user.avatar" alt="作者头像">
                            <div>
                                <RouterLink :to="`/users/${author.user.id}`">{{ author.user.name }}</RouterLink>
                                <p>{{ author.articleCount }} 篇文章 · {{ author.followerCount }} 粉丝</p>
                            </div>
                        </div>
                        <div class="rank-author-stats">
                            <span>{{ author.totalViewCount }} 阅读</span>
                            <span>{{ author.totalLikeCount }} 获赞</span>
                        </div>
                        <AuthorFollowButton
                            :user-id="author.user.id"
                            :followed="author.followed"
                            compact
                            @change="(followed) => handleFollowChange(author, followed)"
                        />
                    </div>
                </div>
            </section>
        </div>
    </main>
</template>

<style scoped>
.ranking-hero {
    display: grid;
    gap: 8px;
}

.ranking-hero h1,
.ranking-hero p {
    margin: 0;
}

.ranking-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
}

.ranking-panel {
    display: grid;
    gap: 16px;
    padding: 20px;
    background: #ffffff;
    border: 1px solid rgba(219, 227, 223, 0.92);
    border-radius: 8px;
    box-shadow: 0 18px 36px rgba(15, 23, 42, 0.05);
}

.ranking-list,
.rank-author-list {
    display: grid;
    gap: 14px;
    padding: 0;
    margin: 0;
    list-style: none;
}

.ranking-item,
.rank-author-card {
    display: grid;
    gap: 12px;
    padding: 14px;
    background: #f8fbfa;
    border: 1px solid rgba(219, 227, 223, 0.92);
    border-radius: 8px;
}

.ranking-item {
    grid-template-columns: 32px minmax(0, 1fr);
    align-items: start;
}

.ranking-no {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    color: var(--brand-strong);
    font-size: 13px;
    font-weight: 700;
    background: rgba(17, 153, 132, 0.08);
    border-radius: 8px;
}

.ranking-body a,
.rank-author-head a {
    color: var(--text);
    font-weight: 700;
    text-decoration: none;
}

.ranking-body p,
.rank-author-head p {
    margin: 4px 0 0;
    color: var(--muted);
    font-size: 13px;
}

.rank-author-head {
    display: grid;
    grid-template-columns: 32px 48px minmax(0, 1fr);
    gap: 12px;
    align-items: center;
}

.rank-author-head img {
    width: 48px;
    height: 48px;
    object-fit: cover;
    border-radius: 8px;
}

.rank-author-stats {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    color: var(--muted);
    font-size: 13px;
}

@media (max-width: 960px) {
    .ranking-grid {
        grid-template-columns: 1fr;
    }
}
</style>
