package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDateTime;

/**
 * 管理端邀请记录展示对象.
 *
 * @since 2026-06-07
 */
public class AdminInviteRecordVO {

    private Long id;
    private Long inviterUserId;
    private String inviterUsername;
    private Long inviteeUserId;
    private String inviteeUsername;
    private String inviteCode;
    private String rewardStatus;
    private LocalDateTime rewardGrantedAt;
    private String skipReason;
    private String triggerBizNo;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInviterUserId() { return inviterUserId; }
    public void setInviterUserId(Long inviterUserId) { this.inviterUserId = inviterUserId; }

    public String getInviterUsername() { return inviterUsername; }
    public void setInviterUsername(String inviterUsername) { this.inviterUsername = inviterUsername; }

    public Long getInviteeUserId() { return inviteeUserId; }
    public void setInviteeUserId(Long inviteeUserId) { this.inviteeUserId = inviteeUserId; }

    public String getInviteeUsername() { return inviteeUsername; }
    public void setInviteeUsername(String inviteeUsername) { this.inviteeUsername = inviteeUsername; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getRewardStatus() { return rewardStatus; }
    public void setRewardStatus(String rewardStatus) { this.rewardStatus = rewardStatus; }

    public LocalDateTime getRewardGrantedAt() { return rewardGrantedAt; }
    public void setRewardGrantedAt(LocalDateTime rewardGrantedAt) { this.rewardGrantedAt = rewardGrantedAt; }

    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }

    public String getTriggerBizNo() { return triggerBizNo; }
    public void setTriggerBizNo(String triggerBizNo) { this.triggerBizNo = triggerBizNo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
