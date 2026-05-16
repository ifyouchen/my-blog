package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.CumulativeSignInRewardConfig;

import java.util.List;

/**
 * 累计签到里程碑奖励配置 Repository 接口.
 *
 * @author czx
 * @since 2026-05-17
 */
public interface CumulativeSignInRewardRepository {

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
}
