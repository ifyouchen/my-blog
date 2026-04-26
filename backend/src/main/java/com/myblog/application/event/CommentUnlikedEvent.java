package com.myblog.application.event;

import java.time.LocalDateTime;

public class CommentUnlikedEvent {
    private final Long commentId;
    private final Long userId;
    private final LocalDateTime occurredOn;

    public CommentUnlikedEvent(Long commentId, Long userId) {
        this.commentId = commentId;
        this.userId = userId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getCommentId() { return commentId; }
    public Long getUserId() { return userId; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
}