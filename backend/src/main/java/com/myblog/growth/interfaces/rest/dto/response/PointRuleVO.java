package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDateTime;

/**
 * 积分规则视图对象（管理端列表 / 详情展示）.
 */
public class PointRuleVO {

    private Long id;
    private String sourceType;
    private int pointAmount;
    private int dailyLimit;
    private boolean enabled;
    private String enabledLabel;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private String statusLabel;
    private String operator;
    private String reason;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public int getPointAmount() { return pointAmount; }
    public void setPointAmount(int pointAmount) { this.pointAmount = pointAmount; }

    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getEnabledLabel() { return enabledLabel; }
    public void setEnabledLabel(String enabledLabel) { this.enabledLabel = enabledLabel; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
