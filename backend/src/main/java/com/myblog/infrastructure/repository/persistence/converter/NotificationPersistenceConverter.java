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

    /**
     * 将通知数据对象转换为领域对象。
     *
     * @param notificationDO 通知数据对象
     * @return 通知领域对象，若 notificationDO 为 null 则返回 null
     */
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

    /**
     * 将通知领域对象转换为数据对象。
     *
     * @param notification 通知领域对象
     * @return 通知数据对象
     */
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