package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.AdminLogDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理员操作日志 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface AdminLogMapper {

    /**
     * 插入日志。
     *
     * @param adminLogDO 日志数据对象
     * @return 影响行数
     */
    int insert(AdminLogDO adminLogDO);

    /**
     * 分页查询日志。
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 日志列表
     */
    List<AdminLogDO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计日志数量。
     *
     * @return 日志数量
     */
    int countAll();

    /**
     * 获取下一个日志 ID。
     *
     * @return 日志 ID
     */
    Long selectNextId();
}
