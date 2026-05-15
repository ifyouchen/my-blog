package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDateTime;

/**
 * 经验流水视图对象.
 * <p>
 * 对应 GET /api/growth/my/journals 接口的列表元素。
 * </p>
 */
public class ExpJournalVO {

    /** 流水 ID. */
    private Long id;

    /** 经验变化量（正数为增加）. */
    private int delta;

    /** 变更后经验余额快照. */
    private int balanceAfter;

    /** 行为类型（如 LIKE / PUBLISH）. */
    private String eventType;

    /** 备注（如"ACTOR 行为经验"）. */
    private String remark;

    /** 流水创建时间. */
    private LocalDateTime createdAt;

    // ──────── 构造方法 & getter/setter ────────

    /** 默认构造方法. */
    public ExpJournalVO() {
    }

    /** 获取流水 ID. */
    public Long getId() { return id; }
    /** 设置流水 ID. */
    public void setId(Long id) { this.id = id; }

    /** 获取经验变化量. */
    public int getDelta() { return delta; }
    /** 设置经验变化量. */
    public void setDelta(int delta) { this.delta = delta; }

    /** 获取变更后余额快照. */
    public int getBalanceAfter() { return balanceAfter; }
    /** 设置变更后余额快照. */
    public void setBalanceAfter(int balanceAfter) { this.balanceAfter = balanceAfter; }

    /** 获取行为类型. */
    public String getEventType() { return eventType; }
    /** 设置行为类型. */
    public void setEventType(String eventType) { this.eventType = eventType; }

    /** 获取备注. */
    public String getRemark() { return remark; }
    /** 设置备注. */
    public void setRemark(String remark) { this.remark = remark; }

    /** 获取创建时间. */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** 设置创建时间. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

