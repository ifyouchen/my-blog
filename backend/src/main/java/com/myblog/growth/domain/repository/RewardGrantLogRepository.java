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

    /**
     * 管理端分页总数查询.
     *
     * @param userId     用户ID（可选）
     * @param rewardType 奖励类型（可选）
     * @return 总数
     */
    long countForAdmin(Long userId, String rewardType);

    /**
     * 管理端分页查询.
     *
     * @param userId     用户ID（可选）
     * @param rewardType 奖励类型（可选）
     * @param offset     偏移量
     * @param limit      每页数量
     * @return 记录列表
     */
    List<RewardGrantLog> findPageForAdmin(Long userId, String rewardType, int offset, int limit);
}
