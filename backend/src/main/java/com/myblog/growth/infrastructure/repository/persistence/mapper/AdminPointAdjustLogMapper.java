package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.infrastructure.repository.persistence.entity.AdminPointAdjustLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员积分调整日志 MyBatis Mapper.
 * <p>对应表 {@code admin_point_adjust_log}，XML 在 {@code mapper/growth/AdminPointAdjustLogMapper.xml}。</p>
 */
@Mapper
public interface AdminPointAdjustLogMapper {

    /**
     * 插入管理员调分日志，回填自增主键.
     *
     * @param log 日志 DO
     */
    void insert(AdminPointAdjustLogDO log);
}

