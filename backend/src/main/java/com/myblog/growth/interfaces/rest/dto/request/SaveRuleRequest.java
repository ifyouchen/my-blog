package com.myblog.growth.interfaces.rest.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 新增 / 更新经验规则请求体.
 * <p>
 * 新增时 id 和 version 可为 null；更新时必须传入 id 和 version（乐观锁）。
 * </p>
 */
public class SaveRuleRequest {

    /** 规则 ID（更新时必传）. */
    private Long id;

    /** 行为类型（如 LIKE、PUBLISH），不能为空. */
    @NotBlank(message = "eventType 不能为空")
    private String eventType;

    /** 角色（ACTOR / AUTHOR），不能为空. */
    @NotBlank(message = "role 不能为空")
    private String role;

    /** 每次发放经验量（≥ 1）. */
    @NotNull(message = "expAmount 不能为空")
    @Min(value = 1, message = "expAmount 最小值为 1")
    private Integer expAmount;

    /** 每日总发放上限（0 = 无限制）. */
    @Min(value = 0, message = "dailyLimit 最小值为 0")
    private int dailyLimit = 0;

    /**
     * 每日限额策略.
     * <ul>
     *   <li>TOTAL — 每日发放经验总量不超过 dailyLimit</li>
     *   <li>COUNT — 每日触发次数不超过 dailyLimit</li>
     * </ul>
     */
    private String dailyLimitStrategy = "TOTAL";

    /** 是否启用（默认 true）. */
    private boolean enabled = true;

    /** 规则生效时间（null = 立即生效）. */
    private LocalDateTime effectiveAt;

    /** 操作人（不能为空，通常由网关注入）. */
    @NotBlank(message = "operator 不能为空")
    private String operator;

    /** 修改原因. */
    private String reason;

    /** 乐观锁版本号（更新时必传）. */
    private Integer version;

    // ──────── getter/setter ────────

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
    public Integer getExpAmount() { return expAmount; }
    /** 设置经验量. */
    public void setExpAmount(Integer expAmount) { this.expAmount = expAmount; }

    /** 获取每日限额. */
    public int getDailyLimit() { return dailyLimit; }
    /** 设置每日限额. */
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }

    /** 获取限额策略. */
    public String getDailyLimitStrategy() { return dailyLimitStrategy; }
    /** 设置限额策略. */
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
    public Integer getVersion() { return version; }
    /** 设置版本号. */
    public void setVersion(Integer version) { this.version = version; }
}

