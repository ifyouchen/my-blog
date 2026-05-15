package com.myblog.growth.interfaces.rest.dto.response;

/**
 * 等级阈值视图对象.
 */
public class LevelThresholdVO {

    /** 等级值. */
    private int level;

    /** 达到此等级所需的最低经验值. */
    private int minExp;

    /** 等级名称（如"铂金"）. */
    private String levelName;

    /** 等级描述. */
    private String description;

    /** 乐观锁版本号（批量保存时回传）. */
    private int version;

    // ──────── 构造方法 & getter/setter ────────

    /** 默认构造方法. */
    public LevelThresholdVO() {
    }

    /** 获取等级值. */
    public int getLevel() { return level; }
    /** 设置等级值. */
    public void setLevel(int level) { this.level = level; }

    /** 获取最低经验值. */
    public int getMinExp() { return minExp; }
    /** 设置最低经验值. */
    public void setMinExp(int minExp) { this.minExp = minExp; }

    /** 获取等级名称. */
    public String getLevelName() { return levelName; }
    /** 设置等级名称. */
    public void setLevelName(String levelName) { this.levelName = levelName; }

    /** 获取等级描述. */
    public String getDescription() { return description; }
    /** 设置等级描述. */
    public void setDescription(String description) { this.description = description; }

    /** 获取版本号. */
    public int getVersion() { return version; }
    /** 设置版本号. */
    public void setVersion(int version) { this.version = version; }
}

