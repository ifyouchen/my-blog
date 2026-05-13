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

    /**
     * 创建通知 MyBatis 仓储。
     *
     * @param notificationMapper 通知 Mapper
     */
    public MyBatisNotificationRepository(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 根据 ID 查询通知。
     *
     * @param id 通知 ID
     * @return 通知 Optional
     */
    @Override
    public Optional<Notification> findById(Long id) {
        NotificationDO notificationDO = notificationMapper.selectById(id);
        return Optional.ofNullable(NotificationPersistenceConverter.toDomain(notificationDO));
    }

    /**
     * 分页查询接收者的通知列表。
     *
     * @param receiverUserId 接收者用户 ID
     * @param filter         过滤类型
     * @param page           页码
     * @param pageSize       每页大小
     * @return 通知列表
     */
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

    /**
     * 统计接收者的通知数量。
     *
     * @param receiverUserId 接收者用户 ID
     * @param filter         过滤类型
     * @return 通知数量
     */
    @Override
    public long countByReceiver(Long receiverUserId, String filter) {
        return notificationMapper.countByReceiver(receiverUserId, filter);
    }

    /**
     * 统计接收者的未读通知数量。
     *
     * @param receiverUserId 接收者用户 ID
     * @return 未读通知数量
     */
    @Override
    public long countUnreadByReceiver(Long receiverUserId) {
        return notificationMapper.countUnreadByReceiver(receiverUserId);
    }

    /**
     * 查询接收者最近的通知列表。
     *
     * @param receiverUserId 接收者用户 ID
     * @param filter         过滤类型
     * @param limit          返回数量限制
     * @return 通知列表
     */
    @Override
    public List<Notification> findRecentByReceiver(Long receiverUserId, String filter, int limit) {
        List<NotificationDO> notificationDOList = notificationMapper.selectRecentByReceiver(receiverUserId, filter, limit);
        List<Notification> notifications = new ArrayList<>(notificationDOList.size());
        for (NotificationDO notificationDO : notificationDOList) {
            notifications.add(NotificationPersistenceConverter.toDomain(notificationDO));
        }
        return notifications;
    }

    /**
     * 保存通知。
     *
     * @param notification 通知聚合根
     * @return 保存后的通知
     */
    @Override
    public Notification save(Notification notification) {
        NotificationDO notificationDO = NotificationPersistenceConverter.toData(notification);
        notificationMapper.insertOrUpdate(notificationDO);
        return notification;
    }

    /**
     * 标记指定通知为已读。
     *
     * @param receiverUserId 接收者用户 ID
     * @param notificationId 通知 ID
     */
    @Override
    public void markRead(Long receiverUserId, Long notificationId) {
        notificationMapper.markRead(receiverUserId, notificationId);
    }

    /**
     * 将接收者的所有通知标记为已读。
     *
     * @param receiverUserId 接收者用户 ID
     */
    @Override
    public void markAllRead(Long receiverUserId) {
        notificationMapper.markAllRead(receiverUserId);
    }

    /**
     * 生成下一个通知 ID。
     *
     * @return 通知 ID
     */
    @Override
    public Long nextId() {
        return notificationMapper.selectNextId();
    }
}
