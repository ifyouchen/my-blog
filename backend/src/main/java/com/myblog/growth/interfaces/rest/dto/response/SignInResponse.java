package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 签到响应 DTO.
 * <p>对应 POST /api/points/sign-in 接口的响应体。</p>
 *
 * @author czx
 * @since 2026-05-17
 */
public class SignInResponse {

    private LocalDate signDate;
    private int consecutiveDays;
    private int totalSignDays;
    private int basePoints;
    private int consecutiveBonus;
    private int milestoneBonus;
    private int pointsGranted;
    private int totalBalance;
    private String rewardTier;
    private String rewardDesc;
    private List<MilestoneRewardInfo> newMilestones;

    public SignInResponse() {
    }

    public LocalDate getSignDate() { return signDate; }
    public void setSignDate(LocalDate signDate) { this.signDate = signDate; }

    public int getConsecutiveDays() { return consecutiveDays; }
    public void setConsecutiveDays(int consecutiveDays) { this.consecutiveDays = consecutiveDays; }

    public int getTotalSignDays() { return totalSignDays; }
    public void setTotalSignDays(int totalSignDays) { this.totalSignDays = totalSignDays; }

    public int getBasePoints() { return basePoints; }
    public void setBasePoints(int basePoints) { this.basePoints = basePoints; }

    public int getConsecutiveBonus() { return consecutiveBonus; }
    public void setConsecutiveBonus(int consecutiveBonus) { this.consecutiveBonus = consecutiveBonus; }

    public int getMilestoneBonus() { return milestoneBonus; }
    public void setMilestoneBonus(int milestoneBonus) { this.milestoneBonus = milestoneBonus; }

    public int getPointsGranted() { return pointsGranted; }
    public void setPointsGranted(int pointsGranted) { this.pointsGranted = pointsGranted; }

    public int getTotalBalance() { return totalBalance; }
    public void setTotalBalance(int totalBalance) { this.totalBalance = totalBalance; }

    public String getRewardTier() { return rewardTier; }
    public void setRewardTier(String rewardTier) { this.rewardTier = rewardTier; }

    public String getRewardDesc() { return rewardDesc; }
    public void setRewardDesc(String rewardDesc) { this.rewardDesc = rewardDesc; }

    public List<MilestoneRewardInfo> getNewMilestones() { return newMilestones; }
    public void setNewMilestones(List<MilestoneRewardInfo> newMilestones) { this.newMilestones = newMilestones; }

    /**
     * 里程碑奖励信息 DTO.
     */
    public static class MilestoneRewardInfo {
        private int milestoneDays;
        private int rewardPoints;
        private String rewardTitle;
        private String description;

        public MilestoneRewardInfo() {
        }

        public int getMilestoneDays() { return milestoneDays; }
        public void setMilestoneDays(int milestoneDays) { this.milestoneDays = milestoneDays; }

        public int getRewardPoints() { return rewardPoints; }
        public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

        public String getRewardTitle() { return rewardTitle; }
        public void setRewardTitle(String rewardTitle) { this.rewardTitle = rewardTitle; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
