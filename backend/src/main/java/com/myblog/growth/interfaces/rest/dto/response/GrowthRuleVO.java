package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDateTime;

/**
 * 经验规则视图对象（管理端列表 / 详情展示）.
 */
public class GrowthRuleVO {

    /** 规则 ID. */
    private Long id;

    /** 行为类型（如 LIKE / PUBLISH）. */
    private String eventType;

    /** 角色（ACTOR / AUTHOR）. */
    private String role;

    /** 每次发放经验量. */
    private int expAmount;

    /** 每日总发放上限（0 = 无限制）. */
    private int dailyLimit;

    /** 每日限额策略（TOTAL / COUNT）. */
    private String dailyLimitStrategy;

    /** 是否启用. */
    private boolean enabled;

    /** 规则生效时间（null = 立即生效）. */
    private LocalDateTime effectiveAt;

    /** 修改操作人. */
    private String operator;

    /** 修改原因. */
    private String reason;

    /** 乐观锁版本号（修改时回传）. */
    private int version;

    // ──────── 构造方法 & getter/setter ────────

    /** 默认构造方法. */
    public GrowthRuleVO() {
    }

    /** 获取规则 ID. */
    public Long getId() { return id; }
    /** 设置规则 ID. */
    public void setId(Long id) { this.id = id; }

    /** 获取行为类型. */
    public String getEventType() { return eventType; }
    /** 设置行为类型. */
    public void setEventType(String eventType) { this.eventType = eventType; }

    /** 获取角色. */
    public String getRole() { return role; }
    /** 设置角色. */
    public void setRole(String role) { this.role = role; }

    /** 获取经验量. */
    public int getExpAmount() { return expAmount; }
    /** 设置经验量. */
    public void setExpAmount(int expAmount) { this.expAmount = expAmount; }

    /** 获取每日限额. */
    public int getDailyLimit() { return dailyLimit; }
    /** 设置每日限额. */
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }

    /** 获取每日限额策略. */
    public String getDailyLimitStrategy() { return dailyLimitStrategy; }
    /** 设置每日限额策略. */
    public void setDailyLimitStrategy(String dailyLimitStrategy) { this.dailyLimitStrategy = dailyLimitStrategy; }

    /** 获取是否启用. */
    public boolean isEnabled() { return enabled; }
    /** 设置是否启用. */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** 获取生效时间. */
    public LocalDateTime getEffectiveAt() { return effectiveAt; }
    /** 设置生效时间. */
    public void setEffectiveAt(LocalDateTime effectiveAt) { this.effectiveAt = effectiveAt; }

    /** 获取操作人. */
    public String getOperator() { return operator; }
    /** 设置操作人. */
    public void setOperator(String operator) { this.operator = operator; }

    /** 获取修改原因. */
    public String getReason() { return reason; }
    /** 设置修改原因. */
    public void setReason(String reason) { this.reason = reason; }

    /** 获取版本号. */
    public int getVersion() { return version; }
    /** 设置版本号. */
    public void setVersion(int version) { this.version = version; }
}

