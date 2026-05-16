package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 连续签到奖励配置值对象.
 * <p>
 * 封装连续签到天数区间对应的奖励配置。
 * </p>
 *
 * @author czx
 * @since 2026-05-17
 */
public class ConsecutiveSignInRewardConfig {

    private Long id;
    private int minDays;
    private Integer maxDays;
    private int bonusPoints;
    private String rewardTier;
    private String rewardDesc;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    /**
     * 判断给定连续天数是否匹配此配置区间.
     *
     * @param consecutiveDays 连续签到天数
     * @return 是否匹配
     */
    public boolean matches(int consecutiveDays) {
        return enabled && consecutiveDays >= minDays
                && (maxDays == null || consecutiveDays <= maxDays);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getMinDays() { return minDays; }
    public void setMinDays(int minDays) { this.minDays = minDays; }
    public Integer getMaxDays() { return maxDays; }
    public void setMaxDays(Integer maxDays) { this.maxDays = maxDays; }
    public int getBonusPoints() { return bonusPoints; }
    public void setBonusPoints(int bonusPoints) { this.bonusPoints = bonusPoints; }
    public String getRewardTier() { return rewardTier; }
    public void setRewardTier(String rewardTier) { this.rewardTier = rewardTier; }
    public String getRewardDesc() { return rewardDesc; }
    public void setRewardDesc(String rewardDesc) { this.rewardDesc = rewardDesc; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
