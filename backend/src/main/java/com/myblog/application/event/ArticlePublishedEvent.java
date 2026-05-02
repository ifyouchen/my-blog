package com.myblog.application.event;

import java.time.LocalDateTime;

public class ArticlePublishedEvent {
    private final Long articleId;
    private final Long authorId;
    private final LocalDateTime occurredOn;

    public ArticlePublishedEvent(Long articleId, Long authorId) {
        this.articleId = articleId;
        this.authorId = authorId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
