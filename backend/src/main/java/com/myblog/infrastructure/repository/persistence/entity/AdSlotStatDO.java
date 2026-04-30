package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 广告位维度统计 DO（用于 slot 级别聚合查询结果映射）。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdSlotStatDO {

    private String slotCode;
    private Long campaignCount;
    private Long impressionCount;
    private Long clickCount;

    public String getSlotCode() {
        return slotCode;
    }

    public void setSlotCode(String slotCode) {
        this.slotCode = slotCode;
    }

    public Long getCampaignCount() {
        return campaignCount;
    }

    public void setCampaignCount(Long campaignCount) {
        this.campaignCount = campaignCount;
    }

    public Long getImpressionCount() {
        return impressionCount;
    }

    public void setImpressionCount(Long impressionCount) {
        this.impressionCount = impressionCount;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }
}

