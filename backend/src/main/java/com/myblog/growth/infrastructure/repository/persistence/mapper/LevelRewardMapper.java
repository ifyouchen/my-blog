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
     * 根据等级查询.
     */
    Optional<LevelRewardConfig> selectByLevel(@Param("level") int level);

    /**
     * 查询所有启用的配置.
     */
    List<LevelRewardConfig> selectAllEnabled();

    /**
     * 更新配置.
     */
    int updateById(LevelRewardConfig config);
}
