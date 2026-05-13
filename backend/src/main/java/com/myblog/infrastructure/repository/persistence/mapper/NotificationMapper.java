package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.NotificationDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface NotificationMapper {

    /**
     * 根据 ID 查询通知。
     *
     * @param id 通知 ID
     * @return 通知数据对象
     */
    NotificationDO selectById(@Param("id") Long id);

    /**
     * 分页查询接收者的通知列表。
     *
     * @param receiverUserId 接收者用户 ID
     * @param filter         过滤类型
     * @param offset         偏移量
     * @param limit          限制数量
     * @return 通知列表
     */
    List<NotificationDO> selectByReceiver(@Param("receiverUserId") Long receiverUserId,
                                         @Param("filter") String filter,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /**
     * 统计接收者的通知数量。
     *
     * @param receiverUserId 接收者用户 ID
     * @param filter         过滤类型
     * @return 通知数量
     */
    long countByReceiver(@Param("receiverUserId") Long receiverUserId, @Param("filter") String filter);

    /**
     * 统计接收者的未读通知数量。
     *
     * @param receiverUserId 接收者用户 ID
     * @return 未读通知数量
     */
    long countUnreadByReceiver(@Param("receiverUserId") Long receiverUserId);

    /**
     * 查询接收者最近的通知列表。
     *
     * @param receiverUserId 接收者用户 ID
     * @param filter         过滤类型
     * @param limit          限制数量
     * @return 通知列表
     */
    List<NotificationDO> selectRecentByReceiver(@Param("receiverUserId") Long receiverUserId,
                                                @Param("filter") String filter,
                                                @Param("limit") int limit);

    /**
     * 插入通知。
     *
     * @param notificationDO 通知数据对象
     * @return 影响行数
     */
    int insert(NotificationDO notificationDO);

    /**
     * 插入或更新通知（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param notificationDO 通知数据对象
     * @return 影响行数
     */
    int insertOrUpdate(NotificationDO notificationDO);

    /**
     * 更新通知。
     *
     * @param notificationDO 通知数据对象
     * @return 影响行数
     */
    int update(NotificationDO notificationDO);

    /**
     * 标记指定通知为已读。
     *
     * @param receiverUserId 接收者用户 ID
     * @param notificationId 通知 ID
     * @return 影响行数
     */
    int markRead(@Param("receiverUserId") Long receiverUserId, @Param("notificationId") Long notificationId);

    /**
     * 将接收者的所有通知标记为已读。
     *
     * @param receiverUserId 接收者用户 ID
     * @return 影响行数
     */
    int markAllRead(@Param("receiverUserId") Long receiverUserId);

    /**
     * 根据主键统计通知数量。
     *
     * @param id 通知 ID
     * @return 通知数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询下一个通知 ID。
     *
     * @return 下一个通知 ID
     */
    Long selectNextId();
}