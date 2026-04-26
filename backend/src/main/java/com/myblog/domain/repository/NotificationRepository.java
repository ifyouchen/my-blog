package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Notification;

import java.util.List;
import java.util.Optional;

/**
 * 通知仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface NotificationRepository {

    /**
     * 根据通知ID查询通知。
     *
     * @param id 通知ID
     * @return 通知Optional
     */
    Optional<Notification> findById(Long id);

    /**
     * 分页查询接收者的通知列表。
     *
     * @param receiverUserId 接收者用户ID
     * @param filter 过滤条件：all 或 unread
     * @param page 页码
     * @param pageSize 每页大小
     * @return 通知列表
     */
    List<Notification> findByReceiver(Long receiverUserId, String filter, int page, int pageSize);

    /**
     * 统计接收者的通知数量。
     *
     * @param receiverUserId 接收者用户ID
     * @param filter 过滤条件：all 或 unread
     * @return 通知数量
     */
    long countByReceiver(Long receiverUserId, String filter);

    /**
     * 统计接收者的未读通知数量。
     *
     * @param receiverUserId 接收者用户ID
     * @return 未读通知数量
     */
    long countUnreadByReceiver(Long receiverUserId);

    /**
     * 查询接收者的最近通知。
     *
     * @param receiverUserId 接收者用户ID
     * @param filter 过滤条件：all 或 unread
     * @param limit 限制条数
     * @return 通知列表
     */
    List<Notification> findRecentByReceiver(Long receiverUserId, String filter, int limit);

    /**
     * 保存通知。
     *
     * @param notification 通知聚合根
     * @return 保存后的通知
     */
    Notification save(Notification notification);

    /**
     * 标记单条通知为已读。
     *
     * @param receiverUserId 接收者用户ID
     * @param notificationId 通知ID
     */
    void markRead(Long receiverUserId, Long notificationId);

    /**
     * 标记所有通知为已读。
     *
     * @param receiverUserId 接收者用户ID
     */
    void markAllRead(Long receiverUserId);

    /**
     * 生成下一个通知ID。
     *
     * @return 通知ID
     */
    Long nextId();
}