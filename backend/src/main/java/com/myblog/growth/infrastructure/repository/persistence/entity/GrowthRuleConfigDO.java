package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 经验规则配置数据库实体（MyBatis DO）.
 * <p>
 * 对应数据库表 {@code growth_rule_config}。
 * </p>
 */
public class GrowthRuleConfigDO {

    /** 主键 ID（自增）. */
    private Long id;

    /** 行为类型（LIKE / READ / PUBLISH 等）. */
    private String eventType;

    /** 作用角色：ACTOR（操作者）/ AUTHOR（被操作内容作者）. */
    private String role;

    /** 单次发放经验量. */
    private Integer expAmount;

    /** 每日上限次数（0 = 无上限）. */
    private Integer dailyLimit;

    /** 超限策略：SKIP / PARTIAL. */
    private String dailyLimitStrategy;

    /** 是否启用（1=是，0=否）. */
    private Boolean enabled;

    /** 生效时间（null = 立即生效）. */
    private LocalDateTime effectiveAt;

    /** 最后操作人. */
    private String operator;

    /** 变更原因. */
    private String reason;

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

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getExpAmount() { return expAmount; }
    public void setExpAmount(Integer expAmount) { this.expAmount = expAmount; }

    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }

    public String getDailyLimitStrategy() { return dailyLimitStrategy; }
    public void setDailyLimitStrategy(String dailyLimitStrategy) { this.dailyLimitStrategy = dailyLimitStrategy; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(LocalDateTime effectiveAt) { this.effectiveAt = effectiveAt; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}

