package com.myblog.application.dto;

import java.util.List;

/**
 * 广告统计概览 DTO（后台管理用）。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdStatsDTO {

    /** 广告投放总数（未删除） */
    private long totalCampaigns;
    /** 当前启用中的广告投放数 */
    private long enabledCampaigns;
    /** 全部广告的总曝光次数 */
    private long totalImpressions;
    /** 全部广告的总点击次数 */
    private long totalClicks;
    /** 按广告位汇总的统计数据 */
    private List<SlotStat> slotStats;

    public long getTotalCampaigns() {
        return totalCampaigns;
    }

    public void setTotalCampaigns(long totalCampaigns) {
        this.totalCampaigns = totalCampaigns;
    }

    public long getEnabledCampaigns() {
        return enabledCampaigns;
    }

    public void setEnabledCampaigns(long enabledCampaigns) {
        this.enabledCampaigns = enabledCampaigns;
    }

    public long getTotalImpressions() {
        return totalImpressions;
    }

    public void setTotalImpressions(long totalImpressions) {
        this.totalImpressions = totalImpressions;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public List<SlotStat> getSlotStats() {
        return slotStats;
    }

    public void setSlotStats(List<SlotStat> slotStats) {
        this.slotStats = slotStats;
    }

    /**
     * 单个广告位的汇总统计。
     */
    public static class SlotStat {

        /** 广告位编码 */
        private String slotCode;
        /** 该广告位的广告投放数 */
        private long campaignCount;
        /** 该广告位的曝光次数 */
        private long impressions;
        /** 该广告位的点击次数 */
        private long clicks;

        public String getSlotCode() {
            return slotCode;
        }

        public void setSlotCode(String slotCode) {
            this.slotCode = slotCode;
        }

        public long getCampaignCount() {
            return campaignCount;
        }

        public void setCampaignCount(long campaignCount) {
            this.campaignCount = campaignCount;
        }

        public long getImpressions() {
            return impressions;
        }

        public void setImpressions(long impressions) {
            this.impressions = impressions;
        }

        public long getClicks() {
            return clicks;
        }

        public void setClicks(long clicks) {
            this.clicks = clicks;
        }
    }
}

