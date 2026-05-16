package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.LevelRewardConfig;

import java.util.List;
import java.util.Optional;

/**
 * 等级奖励配置 Repository 接口.
 *
 * @author czx
 * @since 2026-05-17
 */
public interface LevelRewardRepository {

    /**
     * 根据等级查询奖励配置.
     *
     * @param level 等级
     * @return 奖励配置（可能为空）
     */
    Optional<LevelRewardConfig> findByLevel(int level);

    /**
     * 查询所有启用的配置.
     *
     * @return 配置列表
     */
    List<LevelRewardConfig> findAllEnabled();
}
