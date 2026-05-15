package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 用户成长账户数据库实体（MyBatis DO）.
 * <p>
 * 对应数据库表 {@code user_growth_account}。
 * 仅用于持久化层数据传输，不包含业务逻辑。
 * </p>
 */
public class UserGrowthAccountDO {

    /** 主键 ID（自增）. */
    private Long id;

    /** 用户 ID，关联 blog_user.id，唯一索引 uk_user_id. */
    private Long userId;

    /** 当前累计经验值. */
    private Integer currentExp;

    /** 当前等级（1 起步）. */
    private Integer currentLevel;

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

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getCurrentExp() { return currentExp; }
    public void setCurrentExp(Integer currentExp) { this.currentExp = currentExp; }

    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}

