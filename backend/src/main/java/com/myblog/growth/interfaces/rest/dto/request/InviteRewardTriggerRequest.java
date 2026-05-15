package com.myblog.growth.interfaces.rest.dto.request;

/**
 * 触发拉新奖励请求体.
 * <p>对应 POST /api/open/invite/reward/trigger 接口。</p>
 */
public class InviteRewardTriggerRequest {

    /** 邀请人用户 ID（必填）. */
    private Long inviterUserId;

    /** 被邀请人用户 ID（必填）. */
    private Long inviteeUserId;

    /** 邀请码（可选，备用）. */
    private String inviteCode;

    /** 触发类型（如 REGISTER_COMPLETE）. */
    private String triggerType;

    /** 客户端 IP（风控用）. */
    private String clientIp;

    /** 设备指纹（风控用）. */
    private String deviceId;

    /** 默认构造. */
    public InviteRewardTriggerRequest() {
    }

    public Long getInviterUserId() { return inviterUserId; }
    public void setInviterUserId(Long inviterUserId) { this.inviterUserId = inviterUserId; }

    public Long getInviteeUserId() { return inviteeUserId; }
    public void setInviteeUserId(Long inviteeUserId) { this.inviteeUserId = inviteeUserId; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}

