package com.myblog.growth.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 签到记录 MyBatis Mapper.
 * <p>对应表 {@code sign_in_record}，XML 在 {@code mapper/growth/SignInRecordMapper.xml}。</p>
 */
@Mapper
public interface SignInRecordMapper {

    /**
     * 检查用户在指定日期是否已签到.
     *
     * @param userId   用户 ID
     * @param signDate 签到日期
     * @return 记录数（0 或 1）
     */
    int countByUserIdAndDate(@Param("userId") Long userId,
                              @Param("signDate") LocalDate signDate);

    /**
     * INSERT IGNORE：uk_user_sign_date 冲突时静默忽略.
     *
     * @param userId         用户 ID
     * @param signDate       签到日期
     * @param consecutiveDays 连续签到天数
     * @param pointsGranted  本次发放积分
     * @return 插入行数（1=成功，0=重复）
     */
    int insertIgnore(@Param("userId") Long userId,
                     @Param("signDate") LocalDate signDate,
                     @Param("consecutiveDays") int consecutiveDays,
                     @Param("pointsGranted") int pointsGranted);

    /**
     * 查询用户最近一次签到日期.
     *
     * @param userId 用户 ID
     * @return 最近签到日期，无记录时返回 null
     */
    LocalDate selectLastSignDate(@Param("userId") Long userId);

    /**
     * 查询用户最近一条签到记录的连续天数.
     *
     * @param userId 用户 ID
     * @return 连续签到天数，无记录时返回 null
     */
    Integer selectLastConsecutiveDays(@Param("userId") Long userId);
}

