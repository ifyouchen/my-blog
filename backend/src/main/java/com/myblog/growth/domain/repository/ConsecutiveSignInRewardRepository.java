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
}
