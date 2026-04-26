package com.myblog.application.event;

import java.time.LocalDateTime;

public class ArticleUnfavoritedEvent {
    private final Long articleId;
    private final Long userId;
    private final LocalDateTime occurredOn;

    public ArticleUnfavoritedEvent(Long articleId, Long userId) {
        this.articleId = articleId;
        this.userId = userId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getArticleId() { return articleId; }
    public Long getUserId() { return userId; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
}