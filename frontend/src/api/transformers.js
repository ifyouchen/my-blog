export const normalizeUser = (user) => ({
    id: user.id,
    username: user.username,
    nickname: user.nickname || user.username,
    email: user.email,
    role: user.role,
    bio: user.bio,
    avatar: user.avatar || user.avatarUrl || 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80',
    avatarUrl: user.avatarUrl || user.avatar
});

const splitContent = (content) => {
    if (!content) {
        return [];
    }
    return String(content)
        .split(/\n+/)
        .map((item) => item.trim())
        .filter(Boolean);
};

export const normalizeArticle = (article) => ({
    id: article.id,
    title: article.title,
    summary: article.summary,
    category: article.category,
    readingTime: `${Math.max(5, Math.ceil((article.content || '').length / 120))} 分钟阅读`,
    publishedText: article.publishedAt || '刚刚更新',
    cover: article.cover || article.coverUrl,
    coverUrl: article.coverUrl || article.cover,
    coverAlt: `${article.title} 封面图`,
    author: normalizeUser(article.author || {}),
    tags: article.tags || [],
    content: splitContent(article.content),
    stats: {
        views: `${article.viewCount || 0} 阅读`,
        likes: `${article.likeCount || 0} 赞`,
        comments: `${article.commentCount || 0} 评论`
    }
});
