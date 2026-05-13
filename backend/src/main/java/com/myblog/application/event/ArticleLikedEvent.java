package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 文章点赞事件.
 * <p>
 * 当用户对文章进行点赞操作时触发，用于更新文章点赞数和通知作者.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleLikedEvent {

    /** 文章ID. */
    private final Long articleId;

    /** 点赞用户ID. */
    private final Long userId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造文章点赞事件.
     *
     * @param articleId 文章ID
     * @param userId    点赞用户ID
     */
    public ArticleLikedEvent(Long articleId, Long userId) {
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
     * 获取点赞用户ID.
     *
     * @return 点赞用户ID
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