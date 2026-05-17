package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;

import java.util.List;
import java.util.Optional;

/**
 * 累计签到里程碑奖励配置 Repository 接口.
 *
 * @author czx
 * @since 2026-05-17
 */
public interface CumulativeSignInRewardRepository {

    /**
     * 根据主键查询配置.
     *
     * @param id 配置ID
     * @return 配置（可能为空）
     */
    Optional<CumulativeSignInRewardConfig> findById(Long id);

    /**
     * 查询所有启用的配置.
     *
     * @return 配置列表
     */
    List<CumulativeSignInRewardConfig> findAllEnabled();

    /**
     * 查询所有里程碑天数小于等于指定天数的配置.
     *
     * @param days 累计签到天数
     * @return 配置列表
     */
    List<CumulativeSignInRewardConfig> findByMilestoneLessThanEqual(int days);

    /**
     * 根据里程碑天数查询已软删除配置.
     *
     * @param milestoneDays 里程碑天数
     * @return 已软删除配置（可能为空）
     */
    Optional<CumulativeSignInRewardConfig> findDeletedByMilestoneDays(int milestoneDays);

    /**
     * 查询全部未删除配置（管理端）.
     *
     * @return 配置列表
     */
    List<CumulativeSignInRewardConfig> findAll();

    /**
     * 更新配置（CAS）.
     *
     * @param config 配置
     * @return 是否更新成功
     */
    boolean update(CumulativeSignInRewardConfig config);

    /**
     * 新增配置.
     *
     * @param config 配置
     * @return 新增配置ID
     */
    Long insert(CumulativeSignInRewardConfig config);

    /**
     * 恢复软删除配置并更新内容.
     *
     * @param config 配置
     * @return 是否恢复成功
     */
    boolean restore(CumulativeSignInRewardConfig config);

    /**
     * 软删除配置（CAS）.
     *
     * @param id      配置ID
     * @param version 当前版本
     * @return 是否删除成功
     */
    boolean softDelete(Long id, int version);
}
