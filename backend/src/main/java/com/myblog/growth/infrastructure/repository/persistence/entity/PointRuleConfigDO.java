package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 积分规则配置数据库实体.
 * <p>对应表 {@code point_rule_config}。</p>
 */
public class PointRuleConfigDO {

    /** 主键 ID. */
    private Long id;
    /** 来源类型（source_type），唯一键. */
    private String sourceType;
    /** 单次积分量. */
    private int pointAmount;
    /** 单日上限次数（0=不限）. */
    private int dailyLimit;
    /** 是否启用. */
    private Boolean enabled;
    /** 最后操作人. */
    private String operator;
    /** 变更原因. */
    private String reason;
    /** 创建时间. */
    private LocalDateTime createdAt;
    /** 更新时间. */
    private LocalDateTime updatedAt;
    /** 软删除时间. */
    private LocalDateTime deletedAt;
    /** 乐观锁版本号. */
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public int getPointAmount() { return pointAmount; }
    public void setPointAmount(int pointAmount) { this.pointAmount = pointAmount; }
    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
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
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}

