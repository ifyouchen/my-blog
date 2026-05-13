package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 文章浏览事件.
 * <p>
 * 当用户查看文章详情时触发，用于统计文章阅读量、更新热门排行等.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleViewedEvent {

    /** 文章ID. */
    private final Long articleId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造文章浏览事件.
     *
     * @param articleId 文章ID
     */
    public ArticleViewedEvent(Long articleId) {
        this.articleId = articleId;
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
     * 获取事件发生时间.
     *
     * @return 事件发生时间
     */
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}