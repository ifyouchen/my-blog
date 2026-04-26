package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.NotificationId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class Notification {

    private NotificationId id;
    private UserId receiverUserId;
    private UserId actorUserId;
    private NotificationType type;
    private Long articleId;
    private Long commentId;
    private String payloadJson;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    private Notification() {
    }

    public static Notification create(Long id, Long receiverUserId, Long actorUserId,
                                      NotificationType type, Long articleId, Long commentId,
                                      String payloadJson) {
        Notification notification = new Notification();
        notification.id = new NotificationId(id);
        notification.receiverUserId = new UserId(receiverUserId);
        notification.actorUserId = new UserId(actorUserId);
        notification.type = type;
        notification.articleId = articleId;
        notification.commentId = commentId;
        notification.payloadJson = payloadJson;
        notification.createdAt = LocalDateTime.now();
        notification.updatedAt = notification.createdAt;
        notification.deletedAt = null;
        notification.version = 0L;
        return notification;
    }

    public static Notification restore(Long id, Long receiverUserId, Long actorUserId,
                                       NotificationType type, Long articleId, Long commentId,
                                       String payloadJson, LocalDateTime readAt,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt, Long version) {
        Notification notification = new Notification();
        notification.id = new NotificationId(id);
        notification.receiverUserId = new UserId(receiverUserId);
        notification.actorUserId = new UserId(actorUserId);
        notification.type = type;
        notification.articleId = articleId;
        notification.commentId = commentId;
        notification.payloadJson = payloadJson;
        notification.readAt = readAt;
        notification.createdAt = createdAt;
        notification.updatedAt = updatedAt;
        notification.deletedAt = deletedAt;
        notification.version = version;
        return notification;
    }

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
            this.updatedAt = this.readAt;
        }
    }

    public boolean isRead() {
        return this.readAt != null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public NotificationId getId() {
        return id;
    }

    public UserId getReceiverUserId() {
        return receiverUserId;
    }

    public UserId getActorUserId() {
        return actorUserId;
    }

    public NotificationType getType() {
        return type;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Long getVersion() {
        return version;
    }
}