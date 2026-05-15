package com.myblog.growth.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 每日奖励计数器 MyBatis Mapper.
 * <p>
 * 对应数据库表 {@code daily_reward_counter}，
 * XML 定义于 {@code mapper/growth/DailyRewardCounterMapper.xml}。
 * </p>
 */
@Mapper
public interface DailyRewardCounterMapper {

    /**
     * 查询指定用户当日已发经验量.
     *
     * @param userId     用户 ID
     * @param rewardType 奖励类型
     * @param statDate   统计日期
     * @return 当日已发经验量，记录不存在时返回 null（调用方应处理 null）
     */
    Integer selectGrantedExp(@Param("userId") Long userId,
                             @Param("rewardType") String rewardType,
                             @Param("statDate") LocalDate statDate);

    /**
     * 原子地递增计数器.
     * <p>
     * 使用 {@code INSERT ... ON DUPLICATE KEY UPDATE} 实现并发安全的递增。
     * 唯一索引为 {@code uk_user_type_date(user_id, reward_type, stat_date)}。
     * </p>
     *
     * @param userId     用户 ID
     * @param rewardType 奖励类型
     * @param statDate   统计日期
     * @param countDelta 次数增量（通常为 1）
     * @param expDelta   已发经验增量
     */
    void upsertCounter(@Param("userId") Long userId,
                       @Param("rewardType") String rewardType,
                       @Param("statDate") LocalDate statDate,
                       @Param("countDelta") int countDelta,
                       @Param("expDelta") int expDelta);
}

