import { resolveMediaUrl } from '@/utils/media';

export const normalizeUser = (user) => ({
    id: user.id,
    username: user.username,
    name: user.nickname || user.username,
    nickname: user.nickname || user.username,
    email: user.email,
    role: user.role,
    status: user.status,
    bio: user.bio,
    followed: Boolean(user.followed),
    avatar: resolveMediaUrl(
        user.avatar || user.avatarUrl,
        'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=96&q=80'
    ),
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
    let rawContent;
    if (Array.isArray(article.content)) {
        rawContent = article.content.join('\n\n');
    } else if (typeof article.content === 'string') {
        rawContent = article.content;
    } else {
        rawContent = '';
    }

    return {
        id: article.id,
        title: article.title,
        summary: article.summary,
        category: article.category,
        status: article.status,
        rawContent,
        readingTime: `${Math.max(5, Math.ceil(rawContent.length / 120))} 分钟阅读`,
        publishedText: article.publishedAt || '刚刚更新',
        updatedAt: article.updatedAt || article.publishedAt || '',
        updatedText: article.updatedAt || article.publishedAt || '刚刚更新',
        cover: resolveMediaUrl(article.cover || article.coverUrl),
        coverUrl: article.coverUrl || article.cover,
        coverAlt: `${article.title} 封面图`,
        author: normalizeUser(article.author || {}),
        tags: article.tags || [],
        content: splitContent(rawContent),
        viewCount: article.viewCount || 0,
        likeCount: article.likeCount || 0,
        favoriteCount: article.favoriteCount || 0,
        commentCount: article.commentCount || 0,
        favoritedAt: article.favoritedAt || '',
        liked: Boolean(article.liked),
        favorited: Boolean(article.favorited),
        slug: article.slug || '',
        seoTitle: article.seoTitle || '',
        seoDescription: article.seoDescription || '',
        stats: {
            views: `${article.viewCount || 0} 阅读`,
            likes: `${article.likeCount || 0} 赞`,
            comments: `${article.commentCount || 0} 评论`
        }
    };
};

export const normalizeColumn = (column) => ({
    id: column.id,
    title: column.title,
    summary: column.summary,
    coverUrl: resolveMediaUrl(
        column.coverUrl,
        'https://images.unsplash.com/photo-1526379095098-d400fd0bf935?auto=format&fit=crop&w=900&q=80'
    ),
    subscriberCount: column.subscriberCount || 0,
    articleCount: column.articleCount || 0,
    subscribed: Boolean(column.subscribed),
    author: normalizeUser(column.author || {})
});

export const normalizeTopic = (topic) => ({
    id: topic.id,
    title: topic.title,
    summary: topic.summary,
    coverUrl: resolveMediaUrl(
        topic.coverUrl,
        'https://images.unsplash.com/photo-1526379095098-d400fd0bf935?auto=format&fit=crop&w=900&q=80'
    ),
    articleCount: topic.articleCount || 0,
    status: topic.status
});

export const normalizeAuthorRanking = (item) => ({
    rank: item.rank || 0,
    user: normalizeUser(item.user || {}),
    articleCount: item.articleCount || 0,
    totalViewCount: item.totalViewCount || 0,
    totalLikeCount: item.totalLikeCount || 0,
    followerCount: item.followerCount || 0,
    followed: Boolean(item.followed)
});

export const normalizeComment = (comment) => ({
    id: comment.id,
    articleId: comment.articleId,
    userId: comment.userId,
    rootCommentId: comment.rootCommentId,
    parentId: comment.parentId,
    content: comment.content,
    createdAt: comment.createdAt,
    time: formatCommentTime(comment.createdAt),
    replyCount: comment.replyCount || 0,
    likeCount: comment.likeCount || 0,
    liked: Boolean(comment.liked),
    pinned: Boolean(comment.pinned),
    canDelete: Boolean(comment.canDelete),
    canPin: Boolean(comment.canPin),
    author: Boolean(comment.author),
    user: normalizeUser(comment.user || {}),
    replyToUser: comment.replyToUser ? normalizeUser(comment.replyToUser) : null,
    replyPreview: (comment.replyPreview || []).map((reply) => normalizeComment(reply))
});

export const normalizeCommentPage = (pageData = {}) => ({
    items: (pageData.items || []).map((item) => normalizeComment(item)),
    page: pageData.page || 1,
    pageSize: pageData.pageSize || 10,
    total: pageData.total || 0
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
