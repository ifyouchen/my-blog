package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 管理员积分调整日志数据库实体.
 * <p>对应表 {@code admin_point_adjust_log}。</p>
 */
public class AdminPointAdjustLogDO {

    /** 主键 ID. */
    private Long id;
    /** 被调分用户 ID. */
    private Long targetUserId;
    /** 调整量（正=增加，负=减少）. */
    private int delta;
    /** 调整原因（必填）. */
    private String reason;
    /** 幂等单号. */
    private String bizNo;
    /** 管理员用户 ID. */
    private Long operatorUserId;
    /** 管理员用户名. */
    private String operatorUsername;
    /** 创建时间. */
    private LocalDateTime createdAt;
    /** 软删除时间. */
    private LocalDateTime deletedAt;
    /** 乐观锁版本号. */
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public int getDelta() { return delta; }
    public void setDelta(int delta) { this.delta = delta; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public Long getOperatorUserId() { return operatorUserId; }
    public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}

