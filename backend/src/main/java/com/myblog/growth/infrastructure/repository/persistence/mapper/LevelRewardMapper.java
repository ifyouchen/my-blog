package com.myblog.growth.infrastructure.repository.persistence.mapper;

import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 等级奖励配置 MyBatis Mapper.
 *
 * @author czx
 * @since 2026-05-17
 */
@Mapper
public interface LevelRewardMapper {

    /**
     * 根据主键查询.
     */
    Optional<LevelRewardConfig> selectById(@Param("id") Long id);

    /**
     * 根据等级查询.
     */
    Optional<LevelRewardConfig> selectByLevel(@Param("level") int level);

    /**
     * 根据等级查询已软删除配置.
     */
    Optional<LevelRewardConfig> selectDeletedByLevel(@Param("level") int level);

    /**
     * 查询所有启用的配置.
     */
    List<LevelRewardConfig> selectAllEnabled();

    /**
     * 查询全部未删除配置.
     */
    List<LevelRewardConfig> selectAll();

    /**
     * 更新配置.
     */
    int updateById(LevelRewardConfig config);

    /**
     * 插入配置.
     */
    int insert(LevelRewardConfig config);

    /**
     * 恢复软删除配置.
     */
    int restoreById(LevelRewardConfig config);

    /**
     * 软删除配置.
     */
    int softDelete(@Param("id") Long id, @Param("version") int version);
}
