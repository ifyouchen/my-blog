package com.myblog.growth.interfaces.rest.dto.request;

/**
 * 管理员积分调整请求体.
 * <p>对应 POST /api/admin/points/adjust 接口的请求体。</p>
 */
public class AdminPointAdjustRequest {

    /** 被调分用户 ID（必填）. */
    private Long targetUserId;

    /** 积分调整量（正=增加，负=减少，不能为 0，必填）. */
    private Integer delta;

    /** 调整原因（必填）. */
    private String reason;

    /** 幂等单号（必填）. */
    private String bizNo;

    /** 默认构造. */
    public AdminPointAdjustRequest() {
    }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
}

