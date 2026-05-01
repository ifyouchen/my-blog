export function getNotificationText(type) {
    const textMap = {
        ARTICLE_LIKE: '点赞了你的文章',
        ARTICLE_FAVORITE: '收藏了你的文章',
        ARTICLE_COMMENT: '评论了你的文章',
        COMMENT_REPLY: '回复了你的评论',
        COMMENT_LIKE: '点赞了你的评论',
        USER_FOLLOW: '关注了你'
    };
    return textMap[type] || '有一条新通知';
}

export function formatNotificationTime(timeStr) {
    if (!timeStr) {
        return '';
    }
    const date = new Date(timeStr);
    const now = new Date();
    const diff = now - date;

    if (diff < 60000) {
        return '刚刚';
    }
    if (diff < 3600000) {
        return `${Math.floor(diff / 60000)}分钟前`;
    }
    if (diff < 86400000) {
        return `${Math.floor(diff / 3600000)}小时前`;
    }
    if (diff < 604800000) {
        return `${Math.floor(diff / 86400000)}天前`;
    }
    const y = date.getFullYear();
    const M = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const h = String(date.getHours()).padStart(2, '0');
    const m = String(date.getMinutes()).padStart(2, '0');
    const s = String(date.getSeconds()).padStart(2, '0');
    return `${y}-${M}-${d} ${h}:${m}:${s}`;
}

export function getNotificationDetail(notification) {
    const payload = notification?.payload || {};
    if (payload.articleTitle) {
        return payload.articleTitle;
    }
    if (payload.commentExcerpt) {
        return payload.commentExcerpt;
    }
    return '点击查看详情';
}
