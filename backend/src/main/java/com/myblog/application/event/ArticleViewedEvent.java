package com.myblog.application.event;

import java.time.LocalDateTime;

public class ArticleViewedEvent {
    private final Long articleId;
    private final LocalDateTime occurredOn;

    public ArticleViewedEvent(Long articleId) {
        this.articleId = articleId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getArticleId() {
        return articleId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}