package com.myblog.growth.interfaces.rest.dto.response;

import java.util.List;

/**
 * 用户成长账户视图对象（只读展示）.
 * <p>
 * 对应 GET /api/growth/my 接口的响应体。
 * </p>
 */
public class GrowthAccountVO {

    /** 用户 ID. */
    private Long userId;

    /** 当前累计经验值. */
    private int currentExp;

    /** 当前等级. */
    private int currentLevel;

    /** 当前等级名称（如"青铜"）. */
    private String levelName;

    /** 当前等级升级进度（0~100）. */
    private int progressPercent;

    /** 距下一级还需的经验值（最高级时为 0）. */
    private int expToNextLevel;

    /** 等级奖励列表（仅包含启用配置）. */
    private List<LevelRewardVO> levelRewards;

    // ──────── 构造方法 & getter/setter ────────

    /** 默认构造方法. */
    public GrowthAccountVO() {
    }

    /** 获取用户 ID. */
    public Long getUserId() { return userId; }
    /** 设置用户 ID. */
    public void setUserId(Long userId) { this.userId = userId; }

    /** 获取当前经验值. */
    public int getCurrentExp() { return currentExp; }
    /** 设置当前经验值. */
    public void setCurrentExp(int currentExp) { this.currentExp = currentExp; }

    /** 获取当前等级. */
    public int getCurrentLevel() { return currentLevel; }
    /** 设置当前等级. */
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }

    /** 获取等级名称. */
    public String getLevelName() { return levelName; }
    /** 设置等级名称. */
    public void setLevelName(String levelName) { this.levelName = levelName; }

    /** 获取升级进度百分比（0~100）. */
    public int getProgressPercent() { return progressPercent; }
    /** 设置升级进度百分比. */
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    /** 获取距下一级所需经验. */
    public int getExpToNextLevel() { return expToNextLevel; }
    /** 设置距下一级所需经验. */
    public void setExpToNextLevel(int expToNextLevel) { this.expToNextLevel = expToNextLevel; }

    /** 获取等级奖励列表. */
    public List<LevelRewardVO> getLevelRewards() { return levelRewards; }
    /** 设置等级奖励列表. */
    public void setLevelRewards(List<LevelRewardVO> levelRewards) { this.levelRewards = levelRewards; }

    /**
     * 等级奖励展示对象.
     *
     * @author Codex
     * @since 2026-05-17
     */
    public static class LevelRewardVO {

        /** 等级. */
        private int level;
        /** 奖励积分. */
        private int rewardPoints;
        /** 奖励名称. */
        private String rewardTitle;
        /** 奖励说明. */
        private String description;
        /** 是否已达到该等级. */
        private boolean achieved;

        /** 获取等级. */
        public int getLevel() { return level; }
        /** 设置等级. */
        public void setLevel(int level) { this.level = level; }
        /** 获取奖励积分. */
        public int getRewardPoints() { return rewardPoints; }
        /** 设置奖励积分. */
        public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }
        /** 获取奖励名称. */
        public String getRewardTitle() { return rewardTitle; }
        /** 设置奖励名称. */
        public void setRewardTitle(String rewardTitle) { this.rewardTitle = rewardTitle; }
        /** 获取奖励说明. */
        public String getDescription() { return description; }
        /** 设置奖励说明. */
        public void setDescription(String description) { this.description = description; }
        /** 获取是否已达到. */
        public boolean isAchieved() { return achieved; }
        /** 设置是否已达到. */
        public void setAchieved(boolean achieved) { this.achieved = achieved; }
    }
}

