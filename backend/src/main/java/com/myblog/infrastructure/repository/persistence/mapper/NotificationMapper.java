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

    NotificationDO selectById(@Param("id") Long id);

    List<NotificationDO> selectByReceiver(@Param("receiverUserId") Long receiverUserId,
                                         @Param("filter") String filter,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    long countByReceiver(@Param("receiverUserId") Long receiverUserId, @Param("filter") String filter);

    long countUnreadByReceiver(@Param("receiverUserId") Long receiverUserId);

    List<NotificationDO> selectRecentByReceiver(@Param("receiverUserId") Long receiverUserId,
                                                @Param("filter") String filter,
                                                @Param("limit") int limit);

    int insert(NotificationDO notificationDO);

    int update(NotificationDO notificationDO);

    int markRead(@Param("receiverUserId") Long receiverUserId, @Param("notificationId") Long notificationId);

    int markAllRead(@Param("receiverUserId") Long receiverUserId);

    int countById(@Param("id") Long id);

    Long selectNextId();
}