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

    /**
     * 查询有效的专栏订阅记录（未取消）。
     *
     * @param columnId 专栏 ID
     * @param userId   用户 ID
     * @return 专栏订阅数据对象
     */
    ColumnSubscriptionDO selectByColumnAndUser(@Param("columnId") Long columnId, @Param("userId") Long userId);

    /**
     * 查询专栏订阅记录（包含已逻辑删除的记录）。
     *
     * @param columnId 专栏 ID
     * @param userId   用户 ID
     * @return 专栏订阅数据对象
     */
    ColumnSubscriptionDO selectAnyByColumnAndUser(@Param("columnId") Long columnId, @Param("userId") Long userId);

    /**
     * 统计有效的专栏订阅数量。
     *
     * @param columnId 专栏 ID
     * @param userId   用户 ID
     * @return 订阅数量
     */
    int countByColumnAndUser(@Param("columnId") Long columnId, @Param("userId") Long userId);

    /**
     * 查询下一个订阅记录 ID。
     *
     * @return 下一个订阅记录 ID
     */
    Long selectNextId();

    /**
     * 插入专栏订阅记录。
     *
     * @param columnSubscriptionDO 专栏订阅数据对象
     * @return 影响行数
     */
    int insert(ColumnSubscriptionDO columnSubscriptionDO);

    /**
     * 更新专栏订阅记录。
     *
     * @param columnSubscriptionDO 专栏订阅数据对象
     * @return 影响行数
     */
    int update(ColumnSubscriptionDO columnSubscriptionDO);
}
