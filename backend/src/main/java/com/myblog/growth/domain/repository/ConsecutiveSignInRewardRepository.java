package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.ConsecutiveSignInRewardConfig;

import java.util.List;
import java.util.Optional;

/**
 * 连续签到奖励配置 Repository 接口.
 *
 * @author czx
 * @since 2026-05-17
 */
public interface ConsecutiveSignInRewardRepository {

    /**
     * 根据主键查询配置.
     *
     * @param id 配置ID
     * @return 配置（可能为空）
     */
    Optional<ConsecutiveSignInRewardConfig> findById(Long id);

    /**
     * 查询所有启用的配置.
     *
     * @return 配置列表
     */
    List<ConsecutiveSignInRewardConfig> findAllEnabled();

    /**
     * 根据连续天数查找匹配的配置.
     *
     * @param consecutiveDays 连续签到天数
     * @return 匹配的配置（可能为空）
     */
    Optional<ConsecutiveSignInRewardConfig> findByConsecutiveDays(int consecutiveDays);

    /**
     * 查询全部未删除配置（管理端）.
     *
     * @return 配置列表
     */
    List<ConsecutiveSignInRewardConfig> findAll();

    /**
     * 更新配置（CAS）.
     *
     * @param config 配置
     * @return 是否更新成功
     */
    boolean update(ConsecutiveSignInRewardConfig config);

    /**
     * 新增配置.
     *
     * @param config 配置
     * @return 新增配置ID
     */
    Long insert(ConsecutiveSignInRewardConfig config);

    /**
     * 软删除配置（CAS）.
     *
     * @param id      配置ID
     * @param version 当前版本
     * @return 是否删除成功
     */
    boolean softDelete(Long id, int version);
}
