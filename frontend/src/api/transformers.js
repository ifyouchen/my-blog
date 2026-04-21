export const normalizeUser = (user) => ({
    id: user.id,
    username: user.username,
    name: user.nickname || user.username,
    nickname: user.nickname || user.username,
    email: user.email,
    role: user.role,
    status: user.status,
    bio: user.bio,
    avatar: user.avatar || user.avatarUrl || 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80',
    avatarUrl: user.avatarUrl || user.avatar,
    createdAt: user.createdAt
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

export const extractToc = (content) => {
    if (!content) {
        return [];
    }
    const source = Array.isArray(content) ? content.join('\n') : String(content);
    const lines = source.split('\n');
    const toc = [];
    let headingIndex = 0;

    for (const line of lines) {
        const trimmed = line.trim();
        const matched = trimmed.match(/^(#{1,5})\s+(.+)$/);
        if (matched) {
            headingIndex++;
            const level = matched[1].length;
            const text = matched[2].trim();
            const id = `toc-heading-${headingIndex}`;
            toc.push({ id, text, level });
        }
    }

    return toc;
};

export const normalizeArticle = (article) => {
    const rawContent = Array.isArray(article.content)
        ? article.content.join('\n\n')
        : (article.content || '');

    return {
        id: article.id,
        title: article.title,
        summary: article.summary,
        category: article.category,
        status: article.status,
        rawContent,
        readingTime: `${Math.max(5, Math.ceil(rawContent.length / 120))} 分钟阅读`,
        publishedText: article.publishedAt || '刚刚更新',
        cover: article.cover || article.coverUrl,
        coverUrl: article.coverUrl || article.cover,
        coverAlt: `${article.title} 封面图`,
        author: normalizeUser(article.author || {}),
        tags: article.tags || [],
        content: splitContent(rawContent),
        viewCount: article.viewCount || 0,
        likeCount: article.likeCount || 0,
        favoriteCount: article.favoriteCount || 0,
        commentCount: article.commentCount || 0,
        stats: {
            views: `${article.viewCount || 0} 阅读`,
            likes: `${article.likeCount || 0} 赞`,
            comments: `${article.commentCount || 0} 评论`
        }
    };
};

export const normalizeComment = (comment, parentUser = null) => ({
    id: comment.id,
    articleId: comment.articleId,
    userId: comment.userId,
    parentId: comment.parentId,
    content: comment.content,
    createdAt: comment.createdAt,
    time: formatCommentTime(comment.createdAt),
    user: normalizeUser(comment.user || {}),
    parentUser: parentUser,
    replies: (comment.replies || []).map((reply) => normalizeComment(reply, comment.user))
});

const formatCommentTime = (timeStr) => {
    if (!timeStr) return '刚刚';
    const date = new Date(timeStr);
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes} 分钟前`;
    if (hours < 24) return `${hours} 小时前`;
    if (days < 30) return `${days} 天前`;
    return timeStr.substring(0, 10);
};
