package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.RewardGrantLog;

import java.util.List;

/**
 * 用户奖励领取记录 Repository 接口.
 *
 * @author czx
 * @since 2026-05-17
 */
public interface RewardGrantLogRepository {

    /**
     * 检查用户是否已领取指定奖励.
     *
     * @param userId     用户ID
     * @param rewardType 奖励类型
     * @param rewardId   关联配置ID
     * @return 是否已领取
     */
    boolean existsByUserAndReward(Long userId, String rewardType, Long rewardId);

    /**
     * 保存奖励领取记录.
     *
     * @param log 领取记录
     */
    void save(RewardGrantLog log);

    /**
     * 查询用户的所有领取记录.
     *
     * @param userId 用户ID
     * @return 领取记录列表
     */
    List<RewardGrantLog> findByUserId(Long userId);
}
