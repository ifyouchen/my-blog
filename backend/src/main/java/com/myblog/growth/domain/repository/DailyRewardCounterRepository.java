package com.myblog.growth.domain.repository;

import java.time.LocalDate;

/**
 * 每日奖励计数器 Repository 接口.
 * <p>
 * 用于记录每个用户每天每种行为的经验发放次数和累计量，
 * 实现每日上限（dailyLimit）控制。
 * </p>
 */
public interface DailyRewardCounterRepository {

    /**
     * 查询指定用户当日已发经验量.
     *
     * @param userId     用户 ID
     * @param rewardType 奖励类型（对应 event_type）
     * @param statDate   统计日期
     * @return 当日已发经验量，记录不存在时返回 0
     */
    int getGrantedExp(Long userId, String rewardType, LocalDate statDate);

    /**
     * 原子地递增计数器（使用 ON DUPLICATE KEY UPDATE，并发安全）.
     *
     * @param userId     用户 ID
     * @param rewardType 奖励类型
     * @param statDate   统计日期
     * @param countDelta 累计次数增量（通常为 1）
     * @param expDelta   已发经验增量
     */
    void incrementCounter(Long userId, String rewardType, LocalDate statDate,
                          int countDelta, int expDelta);
}

