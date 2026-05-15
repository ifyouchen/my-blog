package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 等级阈值配置数据库实体（MyBatis DO）.
 * <p>
 * 对应数据库表 {@code level_threshold_config}。
 * </p>
 */
public class LevelThresholdConfigDO {

    /** 主键 ID（自增）. */
    private Long id;

    /** 等级值（1, 2, 3...连续递增）. */
    private Integer level;

    /** 达到该等级所需的最低累计经验. */
    private Integer minExp;

    /** 等级名称（如"新手"、"达人"）. */
    private String levelName;

    /** 等级描述（可为 null）. */
    private String description;

    /** 是否启用（1=是，0=否）. */
    private Boolean enabled;

    /** 最后操作人. */
    private String operator;

    /** 创建时间. */
    private LocalDateTime createdAt;

    /** 最后更新时间. */
    private LocalDateTime updatedAt;

    /** 软删除时间（null 表示未删除）. */
    private LocalDateTime deletedAt;

    /** 乐观锁版本号. */
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getMinExp() { return minExp; }
    public void setMinExp(Integer minExp) { this.minExp = minExp; }

    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}

