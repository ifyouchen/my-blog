package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDate;

/**
 * 签到响应 DTO.
 * <p>对应 POST /api/points/sign-in 接口的响应体。</p>
 */
public class SignInResponse {

    /** 签到日期. */
    private LocalDate signDate;

    /** 连续签到天数（含今日）. */
    private int consecutiveDays;

    /** 本次发放积分. */
    private int pointsGranted;

    /** 签到后积分总余额. */
    private int totalBalance;

    /** 奖励档位（NORMAL / TRIPLE / WEEK / BIWEEK / MONTH）. */
    private String rewardTier;

    /** 奖励描述文案. */
    private String rewardDesc;

    /** 默认构造. */
    public SignInResponse() {
    }

    public LocalDate getSignDate() { return signDate; }
    public void setSignDate(LocalDate signDate) { this.signDate = signDate; }

    public int getConsecutiveDays() { return consecutiveDays; }
    public void setConsecutiveDays(int consecutiveDays) { this.consecutiveDays = consecutiveDays; }

    public int getPointsGranted() { return pointsGranted; }
    public void setPointsGranted(int pointsGranted) { this.pointsGranted = pointsGranted; }

    public int getTotalBalance() { return totalBalance; }
    public void setTotalBalance(int totalBalance) { this.totalBalance = totalBalance; }

    public String getRewardTier() { return rewardTier; }
    public void setRewardTier(String rewardTier) { this.rewardTier = rewardTier; }

    public String getRewardDesc() { return rewardDesc; }
    public void setRewardDesc(String rewardDesc) { this.rewardDesc = rewardDesc; }
}

