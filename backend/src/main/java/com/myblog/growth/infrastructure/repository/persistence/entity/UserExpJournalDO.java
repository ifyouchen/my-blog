package com.myblog.growth.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 用户经验流水数据库实体（MyBatis DO）.
 * <p>
 * 对应数据库表 {@code user_exp_journal}。只允许 INSERT，禁止 UPDATE / DELETE。
 * </p>
 */
public class UserExpJournalDO {

    /** 主键 ID（自增）. */
    private Long id;

    /** 用户 ID. */
    private Long userId;

    /** 经验变化量（正数 = 增加）. */
    private Integer delta;

    /** 变更后余额快照. */
    private Integer balanceAfter;

    /** 行为类型（LIKE / READ / PUBLISH 等）. */
    private String eventType;

    /** 来源 ID（文章 ID / 评论 ID，可为 null）. */
    private Long sourceId;

    /** 备注（可为 null）. */
    private String remark;

    /** 幂等键，唯一索引 uk_idempotent_key. */
    private String idempotentKey;

    /** 流水创建时间. */
    private LocalDateTime createdAt;

    /** 软删除时间. */
    private LocalDateTime deletedAt;

    /** 乐观锁版本号. */
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }

    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getIdempotentKey() { return idempotentKey; }
    public void setIdempotentKey(String idempotentKey) { this.idempotentKey = idempotentKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}

