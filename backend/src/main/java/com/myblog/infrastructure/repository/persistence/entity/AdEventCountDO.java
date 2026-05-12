package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 广告事件统计 DO（用于聚合查询结果映射）。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdEventCountDO {

    /**
     * 广告投放计划 ID
     */
    private Long campaignId;

    /**
     * 事件类型（如：IMPRESSION 曝光、CLICK 点击）
     */
    private String eventType;

    /**
     * 事件发生次数
     */
    private Long eventCount;

    /**
     * 获取广告投放计划 ID。
     *
     * @return 投放计划 ID
     */
    public Long getCampaignId() {
        return campaignId;
    }

    /**
     * 设置广告投放计划 ID。
     *
     * @param campaignId 投放计划 ID
     */
    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    /**
     * 获取事件类型。
     *
     * @return 事件类型
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 设置事件类型。
     *
     * @param eventType 事件类型
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * 获取事件发生次数。
     *
     * @return 事件次数
     */
    public Long getEventCount() {
        return eventCount;
    }

    /**
     * 设置事件发生次数。
     *
     * @param eventCount 事件次数
     */
    public void setEventCount(Long eventCount) {
        this.eventCount = eventCount;
    }
}

