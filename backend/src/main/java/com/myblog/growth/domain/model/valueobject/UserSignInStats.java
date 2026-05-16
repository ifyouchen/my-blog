package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户签到统计聚合根.
 * <p>
 * 封装用户累计签到数据，所有状态变更必须通过业务方法进行。
 * </p>
 *
 * @author czx
 * @since 2026-05-17
 */
public class UserSignInStats {

    private Long id;
    private Long userId;
    private int totalSignDays;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastSignDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    private UserSignInStats() {
    }

    /**
     * 创建新的签到统计.
     *
     * @param userId 用户ID
     * @return 新创建的统计对象
     */
    public static UserSignInStats create(Long userId) {
        UserSignInStats stats = new UserSignInStats();
        stats.userId = userId;
        stats.totalSignDays = 0;
        stats.currentStreak = 0;
        stats.longestStreak = 0;
        stats.lastSignDate = null;
        stats.createdAt = LocalDateTime.now();
        stats.updatedAt = LocalDateTime.now();
        stats.version = 0;
        return stats;
    }

    /**
     * 从持久化状态恢复.
     *
     * @param id            主键ID
     * @param userId        用户ID
     * @param totalSignDays 累计签到天数
     * @param currentStreak 当前连续天数
     * @param longestStreak 最长连续天数
     * @param lastSignDate  最后签到日期
     * @param createdAt     创建时间
     * @param updatedAt     更新时间
     * @param version       版本号
     * @return 恢复后的统计对象
     */
    public static UserSignInStats restore(Long id, Long userId, int totalSignDays,
                                           int currentStreak, int longestStreak,
                                           LocalDate lastSignDate,
                                           LocalDateTime createdAt, LocalDateTime updatedAt, int version) {
        UserSignInStats stats = new UserSignInStats();
        stats.id = id;
        stats.userId = userId;
        stats.totalSignDays = totalSignDays;
        stats.currentStreak = currentStreak;
        stats.longestStreak = longestStreak;
        stats.lastSignDate = lastSignDate;
        stats.createdAt = createdAt;
        stats.updatedAt = updatedAt;
        stats.version = version;
        return stats;
    }

    /**
     * 记录一次签到.
     *
     * @param today           今日日期
     * @param newConsecutiveDays 今日签到后的连续天数
     */
    public void recordSignIn(LocalDate today, int newConsecutiveDays) {
        this.totalSignDays++;
        this.currentStreak = newConsecutiveDays;
        if (newConsecutiveDays > this.longestStreak) {
            this.longestStreak = newConsecutiveDays;
        }
        this.lastSignDate = today;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 重置连续签到天数（断签时调用）.
     */
    public void resetStreak() {
        this.currentStreak = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public int getTotalSignDays() { return totalSignDays; }
    public int getCurrentStreak() { return currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    public LocalDate getLastSignDate() { return lastSignDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }
}
