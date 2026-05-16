package com.myblog.growth.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 等级奖励配置值对象.
 * <p>
 * 封装用户升级到指定等级时可获得的奖励配置。
 * </p>
 *
 * @author czx
 * @since 2026-05-17
 */
public class LevelRewardConfig {

    private Long id;
    private int level;
    private int rewardPoints;
    private String rewardTitle;
    private String description;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    /**
     * 判断用户是否达到该等级.
     *
     * @param userLevel 用户当前等级
     * @return 是否达到
     */
    public boolean isEligible(int userLevel) {
        return enabled && userLevel >= level;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }
    public String getRewardTitle() { return rewardTitle; }
    public void setRewardTitle(String rewardTitle) { this.rewardTitle = rewardTitle; }
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
