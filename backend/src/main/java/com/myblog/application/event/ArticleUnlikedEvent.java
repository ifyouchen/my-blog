package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 文章取消点赞事件.
 * <p>
 * 当用户取消对文章的点赞时触发，用于更新文章点赞数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleUnlikedEvent {

    /** 文章ID. */
    private final Long articleId;

    /** 操作用户ID. */
    private final Long userId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造文章取消点赞事件.
     *
     * @param articleId 文章ID
     * @param userId    操作用户ID
     */
    public ArticleUnlikedEvent(Long articleId, Long userId) {
        this.articleId = articleId;
        this.userId = userId;
        this.occurredOn = LocalDateTime.now();
    }

    /**
     * 获取文章ID.
     *
     * @return 文章ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 获取操作用户ID.
     *
     * @return 操作用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取事件发生时间.
     *
     * @return 事件发生时间
     */
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}