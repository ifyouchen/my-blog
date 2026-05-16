package com.myblog.growth.interfaces.rest.dto.response;

import java.time.LocalDate;

/**
 * 签到统计响应 DTO.
 * <p>对应 GET /api/points/sign-in/stats 接口的响应体。</p>
 *
 * @author czx
 * @since 2026-05-17
 */
public class SignInStatsResponse {

    private int totalSignDays;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastSignDate;
    private NextMilestoneInfo nextMilestone;

    public SignInStatsResponse() {
    }

    public int getTotalSignDays() { return totalSignDays; }
    public void setTotalSignDays(int totalSignDays) { this.totalSignDays = totalSignDays; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public LocalDate getLastSignDate() { return lastSignDate; }
    public void setLastSignDate(LocalDate lastSignDate) { this.lastSignDate = lastSignDate; }

    public NextMilestoneInfo getNextMilestone() { return nextMilestone; }
    public void setNextMilestone(NextMilestoneInfo nextMilestone) { this.nextMilestone = nextMilestone; }

    /**
     * 下一个里程碑信息 DTO.
     */
    public static class NextMilestoneInfo {
        private int milestoneDays;
        private int rewardPoints;
        private String rewardTitle;
        private int progress;
        private int remaining;

        public NextMilestoneInfo() {
        }

        public int getMilestoneDays() { return milestoneDays; }
        public void setMilestoneDays(int milestoneDays) { this.milestoneDays = milestoneDays; }

        public int getRewardPoints() { return rewardPoints; }
        public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

        public String getRewardTitle() { return rewardTitle; }
        public void setRewardTitle(String rewardTitle) { this.rewardTitle = rewardTitle; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }

        public int getRemaining() { return remaining; }
        public void setRemaining(int remaining) { this.remaining = remaining; }
    }
}
