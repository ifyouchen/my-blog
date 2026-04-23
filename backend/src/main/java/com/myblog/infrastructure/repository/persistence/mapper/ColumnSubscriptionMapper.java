package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ColumnSubscriptionDO;
import org.apache.ibatis.annotations.Param;

/**
 * 专栏订阅 Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ColumnSubscriptionMapper {

    ColumnSubscriptionDO selectByColumnAndUser(@Param("columnId") Long columnId, @Param("userId") Long userId);

    ColumnSubscriptionDO selectAnyByColumnAndUser(@Param("columnId") Long columnId, @Param("userId") Long userId);

    int countByColumnAndUser(@Param("columnId") Long columnId, @Param("userId") Long userId);

    Long selectNextId();

    int insert(ColumnSubscriptionDO columnSubscriptionDO);

    int update(ColumnSubscriptionDO columnSubscriptionDO);
}
