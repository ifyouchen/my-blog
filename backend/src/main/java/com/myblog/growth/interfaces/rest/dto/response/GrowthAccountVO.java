package com.myblog.growth.interfaces.rest.dto.response;

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
}

