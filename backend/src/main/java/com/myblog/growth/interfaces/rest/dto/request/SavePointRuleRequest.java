package com.myblog.growth.interfaces.rest.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 新增 / 更新积分规则请求体.
 * <p>
 * 新增时 id 和 version 可为 null；更新时必须传入 id 和 version（乐观锁）。
 * </p>
 */
public class SavePointRuleRequest {

    /** 规则 ID（更新时必传）. */
    private Long id;

    /** 来源类型（SIGN_IN / INVITE / RECHARGE / PUBLISH / UNLOCK_SHARE_RATIO）. */
    @NotBlank(message = "sourceType 不能为空")
    private String sourceType;

    /** 积分量或比例值. */
    @NotNull(message = "pointAmount 不能为空")
    private Integer pointAmount;

    /** 每日上限（0=不限制）. */
    @Min(value = 0, message = "dailyLimit 最小值为 0")
    private int dailyLimit = 0;

    /** 是否启用. */
    private boolean enabled = true;

    /** 操作人（由服务端注入）. */
    private String operator;

    /** 变更原因. */
    private String reason;

    /** 乐观锁版本号（更新时必传）. */
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Integer getPointAmount() { return pointAmount; }
    public void setPointAmount(Integer pointAmount) { this.pointAmount = pointAmount; }

    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
