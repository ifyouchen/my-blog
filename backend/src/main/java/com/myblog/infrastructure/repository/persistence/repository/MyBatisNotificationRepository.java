package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Notification;
import com.myblog.domain.repository.NotificationRepository;
import com.myblog.infrastructure.repository.persistence.converter.NotificationPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.NotificationDO;
import com.myblog.infrastructure.repository.persistence.mapper.NotificationMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
/**
 * 通知 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisNotificationRepository implements NotificationRepository {

    private final NotificationMapper notificationMapper;

    public MyBatisNotificationRepository(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public Optional<Notification> findById(Long id) {
        NotificationDO notificationDO = notificationMapper.selectById(id);
        return Optional.ofNullable(NotificationPersistenceConverter.toDomain(notificationDO));
    }

    @Override
    public List<Notification> findByReceiver(Long receiverUserId, String filter, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<NotificationDO> notificationDOList = notificationMapper.selectByReceiver(receiverUserId, filter, offset, pageSize);
        List<Notification> notifications = new ArrayList<>(notificationDOList.size());
        for (NotificationDO notificationDO : notificationDOList) {
            notifications.add(NotificationPersistenceConverter.toDomain(notificationDO));
        }
        return notifications;
    }

    @Override
    public long countByReceiver(Long receiverUserId, String filter) {
        return notificationMapper.countByReceiver(receiverUserId, filter);
    }

    @Override
    public long countUnreadByReceiver(Long receiverUserId) {
        return notificationMapper.countUnreadByReceiver(receiverUserId);
    }

    @Override
    public List<Notification> findRecentByReceiver(Long receiverUserId, String filter, int limit) {
        List<NotificationDO> notificationDOList = notificationMapper.selectRecentByReceiver(receiverUserId, filter, limit);
        List<Notification> notifications = new ArrayList<>(notificationDOList.size());
        for (NotificationDO notificationDO : notificationDOList) {
            notifications.add(NotificationPersistenceConverter.toDomain(notificationDO));
        }
        return notifications;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationDO notificationDO = NotificationPersistenceConverter.toData(notification);
        notificationMapper.insertOrUpdate(notificationDO);        return notification;
    }

    @Override
    public void markRead(Long receiverUserId, Long notificationId) {
        notificationMapper.markRead(receiverUserId, notificationId);
    }

    @Override
    public void markAllRead(Long receiverUserId) {
        notificationMapper.markAllRead(receiverUserId);
    }

    @Override
    public Long nextId() {
        return notificationMapper.selectNextId();
    }
}
