package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.LevelPrivilegeConfig;

import java.util.List;

/**
 * 等级权益配置仓储接口.
 *
 * @author Codex
 * @since 2026-05-17
 */
public interface LevelPrivilegeRepository {

    /**
     * 查询指定等级的启用权益.
     *
     * @param level 等级
     * @return 权益列表
     */
    List<LevelPrivilegeConfig> findEnabledByLevel(int level);

    /**
     * 查询全部启用权益.
     *
     * @return 权益列表
     */
    List<LevelPrivilegeConfig> findAllEnabled();
}
