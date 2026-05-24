package com.myblog.shared.enums;

/**
 * 通知类型。
 *
 * @author Codex
 * @since 1.0.0
 */
public enum NotificationType {

    /**
     * 文章被点赞
     */
    ARTICLE_LIKE,

    /**
     * 文章被收藏
     */
    ARTICLE_FAVORITE,

    /**
     * 文章被评论
     */
    ARTICLE_COMMENT,

    /**
     * 评论被回复
     */
    COMMENT_REPLY,

    /**
     * 评论被点赞
     */
    COMMENT_LIKE,

    /**
     * 被用户关注
     */
    USER_FOLLOW,

    /**
     * 文章发布
     */
    ARTICLE_PUBLISH,

    /**
     * 举报提交（通知管理员）
     */
    REPORT_SUBMITTED,

    /**
     * 首页推荐申请提交（通知管理员）
     */
    RECOMMENDATION_APPLIED
}