package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.NotificationId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * 通知聚合根。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class Notification {

    /**
     * 通知 ID
     */
    private NotificationId id;

    /**
     * 通知接收人用户 ID
     */
    private UserId receiverUserId;

    /**
     * 触发通知的行为人用户 ID
     */
    private UserId actorUserId;

    /**
     * 通知类型
     */
    private NotificationType type;

    /**
     * 关联文章 ID（可为 null）
     */
    private Long articleId;

    /**
     * 关联评论 ID（可为 null）
     */
    private Long commentId;

    /**
     * 扩展数据 JSON（可为 null）
     */
    private String payloadJson;

    /**
     * 通知已读时间，为 null 表示未读
     */
    private LocalDateTime readAt;

    /**
     * 通知创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 通知最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 通知软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Long version;

    private Notification() {
    }

    /**
     * 创建通知聚合根。
     *
     * @param id             通知 ID
     * @param receiverUserId 接收人用户 ID
     * @param actorUserId    行为人用户 ID
     * @param type           通知类型
     * @param articleId      关联文章 ID
     * @param commentId      关联评论 ID
     * @param payloadJson    扩展数据 JSON
     * @return 通知聚合根
     */
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

    /**
     * 从持久化数据还原通知聚合根。
     *
     * @param id             通知 ID
     * @param receiverUserId 接收人用户 ID
     * @param actorUserId    行为人用户 ID
     * @param type           通知类型
     * @param articleId      关联文章 ID
     * @param commentId      关联评论 ID
     * @param payloadJson    扩展数据 JSON
     * @param readAt         已读时间
     * @param createdAt      创建时间
     * @param updatedAt      更新时间
     * @param deletedAt      删除时间
     * @param version        乐观锁版本号
     * @return 通知聚合根
     */
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

    /**
     * 将通知标记为已读，幂等操作（已读则跳过）。
     */
    public void markRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
            this.updatedAt = this.readAt;
        }
    }

    /**
     * 判断通知是否已读。
     *
     * @return 已读返回 true，否则返回 false
     */
    public boolean isRead() {
        return this.readAt != null;
    }

    /**
     * 软删除通知。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    /**
     * 判断通知是否已被删除。
     *
     * @return 已删除返回 true，否则返回 false
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 获取通知 ID。
     *
     * @return 通知 ID
     */
    public NotificationId getId() {
        return id;
    }

    /**
     * 获取通知接收人用户 ID。
     *
     * @return 接收人用户 ID
     */
    public UserId getReceiverUserId() {
        return receiverUserId;
    }

    /**
     * 获取触发通知的行为人用户 ID。
     *
     * @return 行为人用户 ID
     */
    public UserId getActorUserId() {
        return actorUserId;
    }

    /**
     * 获取通知类型。
     *
     * @return 通知类型
     */
    public NotificationType getType() {
        return type;
    }

    /**
     * 获取关联文章 ID。
     *
     * @return 关联文章 ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 获取关联评论 ID。
     *
     * @return 关联评论 ID
     */
    public Long getCommentId() {
        return commentId;
    }

    /**
     * 获取扩展数据 JSON。
     *
     * @return 扩展数据 JSON
     */
    public String getPayloadJson() {
        return payloadJson;
    }

    /**
     * 获取通知已读时间。
     *
     * @return 已读时间，未读则为 null
     */
    public LocalDateTime getReadAt() {
        return readAt;
    }

    /**
     * 获取通知创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取通知最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取通知软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Long getVersion() {
        return version;
    }
}