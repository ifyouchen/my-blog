package com.myblog.growth.domain.model.valueobject;

/**
 * 等级阈值值对象.
 * <p>
 * 对应数据库 {@code level_threshold_config} 表，描述达到某一等级所需的
 * 最低累计经验值及等级名称。值对象不可变，通过工厂方法构建。
 * </p>
 */
public class LevelThreshold {

    /** 数据库主键 ID. */
    private Long id;

    /** 等级值（从 1 开始连续递增）. */
    private int level;

    /** 达到该等级所需的最低累计经验. */
    private int minExp;

    /** 等级名称（如"新手"、"达人"）. */
    private String levelName;

    /** 等级描述（可选）. */
    private String description;

    /** 是否启用（禁用后该等级不参与计算）. */
    private boolean enabled;

    /** 最后操作人. */
    private String operator;

    /** 乐观锁版本号. */
    private int version;

    /** 禁止外部直接构造，使用工厂方法. */
    private LevelThreshold() {
    }

    /**
     * 工厂方法：构建等级阈值值对象.
     *
     * @param id          主键 ID
     * @param level       等级值
     * @param minExp      达到该等级所需最低经验
     * @param levelName   等级名称
     * @param description 等级描述
     * @param enabled     是否启用
     * @param operator    最后操作人
     * @param version     乐观锁版本号
     * @return 等级阈值值对象
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public static LevelThreshold of(
            @com.fasterxml.jackson.annotation.JsonProperty("id") Long id,
            @com.fasterxml.jackson.annotation.JsonProperty("level") int level,
            @com.fasterxml.jackson.annotation.JsonProperty("minExp") int minExp,
            @com.fasterxml.jackson.annotation.JsonProperty("levelName") String levelName,
            @com.fasterxml.jackson.annotation.JsonProperty("description") String description,
            @com.fasterxml.jackson.annotation.JsonProperty("enabled") boolean enabled,
            @com.fasterxml.jackson.annotation.JsonProperty("operator") String operator,
            @com.fasterxml.jackson.annotation.JsonProperty("version") int version) {
        LevelThreshold threshold = new LevelThreshold();
        threshold.id = id;
        threshold.level = level;
        threshold.minExp = minExp;
        threshold.levelName = levelName;
        threshold.description = description;
        threshold.enabled = enabled;
        threshold.operator = operator;
        threshold.version = version;
        return threshold;
    }

    /**
     * 获取主键 ID.
     *
     * @return 主键 ID
     */
    public Long getId() { return id; }

    /**
     * 获取等级值.
     *
     * @return 等级值（≥ 1）
     */
    public int getLevel() { return level; }

    /**
     * 获取达到该等级所需最低经验.
     *
     * @return 最低经验值（≥ 0）
     */
    public int getMinExp() { return minExp; }

    /**
     * 获取等级名称.
     *
     * @return 等级名称
     */
    public String getLevelName() { return levelName; }

    /**
     * 获取等级描述.
     *
     * @return 等级描述，可为 null
     */
    public String getDescription() { return description; }

    /**
     * 获取是否启用.
     *
     * @return true 表示启用
     */
    public boolean isEnabled() { return enabled; }

    /**
     * 获取最后操作人.
     *
     * @return 操作人用户名
     */
    public String getOperator() { return operator; }

    /**
     * 获取乐观锁版本号.
     *
     * @return 版本号
     */
    public int getVersion() { return version; }
}

