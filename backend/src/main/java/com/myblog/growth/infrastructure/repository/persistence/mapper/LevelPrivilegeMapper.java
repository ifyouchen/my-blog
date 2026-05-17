package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 等级权益配置 MyBatis Mapper.
 *
 * @author Codex
 * @since 2026-05-17
 */
@Mapper
public interface LevelPrivilegeMapper {

    /**
     * 查询指定等级的启用权益.
     *
     * @param level 等级
     * @return 权益列表
     */
    List<LevelPrivilegeConfig> selectEnabledByLevel(@Param("level") int level);

    /**
     * 查询全部启用权益.
     *
     * @return 权益列表
     */
    List<LevelPrivilegeConfig> selectAllEnabled();
}
