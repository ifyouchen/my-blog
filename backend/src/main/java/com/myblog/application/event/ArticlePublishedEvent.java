package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 文章发布事件.
 * <p>
 * 当文章状态变更为已发布时触发，用于通知相关系统（如缓存刷新、通知推送等）.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticlePublishedEvent {

    /** 文章ID. */
    private final Long articleId;

    /** 作者用户ID. */
    private final Long authorId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造文章发布事件.
     *
     * @param articleId 文章ID
     * @param authorId  作者用户ID
     */
    public ArticlePublishedEvent(Long articleId, Long authorId) {
        this.articleId = articleId;
        this.authorId = authorId;
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
     * 获取作者用户ID.
     *
     * @return 作者用户ID
     */
    public Long getAuthorId() {
        return authorId;
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
