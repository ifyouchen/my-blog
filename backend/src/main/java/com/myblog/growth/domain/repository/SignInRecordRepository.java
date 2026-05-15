package com.myblog.growth.domain.repository;

import java.time.LocalDate;

/**
 * 签到记录 Repository 接口.
 */
public interface SignInRecordRepository {

    /**
     * 检查用户在指定日期是否已签到.
     *
     * @param userId   用户 ID
     * @param signDate 签到日期
     * @return {@code true} 表示已签到
     */
    boolean existsByUserIdAndDate(Long userId, LocalDate signDate);

    /**
     * 插入签到记录（INSERT IGNORE，防止重复签到）.
     *
     * @param userId         用户 ID
     * @param signDate       签到日期
     * @param continuousDays 连续签到天数（含今日）
     * @return 插入行数（1=成功，0=已签到）
     */
    int insertIgnore(Long userId, LocalDate signDate, int continuousDays);

    /**
     * 查询用户最近一次签到日期.
     *
     * @param userId 用户 ID
     * @return 最近签到日期，未签到过时返回 null
     */
    LocalDate findLastSignDate(Long userId);

    /**
     * 查询用户当前连续签到天数（取最近一条记录的 continuous_days）.
     *
     * @param userId 用户 ID
     * @return 连续签到天数，无记录时返回 0
     */
    int findContinuousDays(Long userId);
}

