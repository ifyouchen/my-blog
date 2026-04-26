package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Notification;
import com.myblog.infrastructure.repository.persistence.entity.NotificationDO;
import com.myblog.shared.enums.NotificationType;

/**
 * 通知持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class NotificationPersistenceConverter {

    private NotificationPersistenceConverter() {
    }

    public static Notification toDomain(NotificationDO notificationDO) {
        if (notificationDO == null) {
            return null;
        }
        return Notification.restore(
            notificationDO.getId(),
            notificationDO.getReceiverUserId(),
            notificationDO.getActorUserId(),
            NotificationType.valueOf(notificationDO.getType()),
            notificationDO.getArticleId(),
            notificationDO.getCommentId(),
            notificationDO.getPayloadJson(),
            notificationDO.getReadAt(),
            notificationDO.getCreatedAt(),
            notificationDO.getUpdatedAt(),
            notificationDO.getDeletedAt(),
            notificationDO.getVersion()
        );
    }

    public static NotificationDO toData(Notification notification) {
        NotificationDO notificationDO = new NotificationDO();
        notificationDO.setId(notification.getId().getValue());
        notificationDO.setReceiverUserId(notification.getReceiverUserId().getValue());
        notificationDO.setActorUserId(notification.getActorUserId().getValue());
        notificationDO.setType(notification.getType().name());
        notificationDO.setArticleId(notification.getArticleId());
        notificationDO.setCommentId(notification.getCommentId());
        notificationDO.setPayloadJson(notification.getPayloadJson());
        notificationDO.setReadAt(notification.getReadAt());
        notificationDO.setCreatedAt(notification.getCreatedAt());
        notificationDO.setUpdatedAt(notification.getUpdatedAt());
        notificationDO.setDeletedAt(notification.getDeletedAt());
        notificationDO.setVersion(notification.getVersion());
        return notificationDO;
    }
}