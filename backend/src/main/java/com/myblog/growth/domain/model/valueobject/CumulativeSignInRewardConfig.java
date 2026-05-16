package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 累计签到里程碑奖励配置值对象.
 * <p>
 * 封装累计签到天数达到里程碑时的奖励配置。
 * </p>
 *
 * @author czx
 * @since 2026-05-17
 */
public class CumulativeSignInRewardConfig {

    private Long id;
    private int milestoneDays;
    private int rewardPoints;
    private String rewardTitle;
    private String badgeCode;
    private String description;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getMilestoneDays() { return milestoneDays; }
    public void setMilestoneDays(int milestoneDays) { this.milestoneDays = milestoneDays; }
    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }
    public String getRewardTitle() { return rewardTitle; }
    public void setRewardTitle(String rewardTitle) { this.rewardTitle = rewardTitle; }
    public String getBadgeCode() { return badgeCode; }
    public void setBadgeCode(String badgeCode) { this.badgeCode = badgeCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
