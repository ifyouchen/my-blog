package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 广告事件统计 DO（用于聚合查询结果映射）。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdEventCountDO {

    private Long campaignId;
    private String eventType;
    private Long eventCount;

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getEventCount() {
        return eventCount;
    }

    public void setEventCount(Long eventCount) {
        this.eventCount = eventCount;
    }
}

