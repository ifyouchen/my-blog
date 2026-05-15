package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 积分流水数据库实体.
 * <p>对应表 {@code user_point_journal}。</p>
 */
public class UserPointJournalDO {

    /** 主键 ID. */
    private Long id;
    /** 用户 ID. */
    private Long userId;
    /** 积分变化量（正=入账，负=扣减）. */
    private int delta;
    /** 变更后余额快照. */
    private int balanceAfter;
    /** 来源类型（source_type）. */
    private String sourceType;
    /** 业务单号（biz_no），唯一幂等键. */
    private String bizNo;
    /** 备注. */
    private String remark;
    /** 操作人（管理员调分时填写）. */
    private String operator;
    /** 创建时间. */
    private LocalDateTime createdAt;
    /** 软删除时间. */
    private LocalDateTime deletedAt;
    /** 乐观锁版本号. */
    private int version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getDelta() { return delta; }
    public void setDelta(int delta) { this.delta = delta; }
    public int getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(int balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}

