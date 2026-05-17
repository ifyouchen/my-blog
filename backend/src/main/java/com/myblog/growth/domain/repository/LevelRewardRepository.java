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
     * 根据主键查询配置.
     *
     * @param id 配置ID
     * @return 配置（可能为空）
     */
    Optional<LevelRewardConfig> findById(Long id);

    /**
     * 根据等级查询奖励配置.
     *
     * @param level 等级
     * @return 奖励配置（可能为空）
     */
    Optional<LevelRewardConfig> findByLevel(int level);

    /**
     * 根据等级查询已软删除配置.
     *
     * @param level 等级
     * @return 已软删除配置（可能为空）
     */
    Optional<LevelRewardConfig> findDeletedByLevel(int level);

    /**
     * 查询所有启用的配置.
     *
     * @return 配置列表
     */
    List<LevelRewardConfig> findAllEnabled();

    /**
     * 查询全部未删除配置（管理端）.
     *
     * @return 配置列表
     */
    List<LevelRewardConfig> findAll();

    /**
     * 更新配置（CAS）.
     *
     * @param config 配置
     * @return 是否更新成功
     */
    boolean update(LevelRewardConfig config);

    /**
     * 新增配置.
     *
     * @param config 配置
     * @return 新增配置ID
     */
    Long insert(LevelRewardConfig config);

    /**
     * 恢复软删除配置并更新内容.
     *
     * @param config 配置
     * @return 是否恢复成功
     */
    boolean restore(LevelRewardConfig config);

    /**
     * 软删除配置（CAS）.
     *
     * @param id      配置ID
     * @param version 当前版本
     * @return 是否删除成功
     */
    boolean softDelete(Long id, int version);
}
