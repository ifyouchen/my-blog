package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 广告位维度统计 DO（用于 slot 级别聚合查询结果映射）。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdSlotStatDO {

    /**
     * 广告位编码
     */
    private String slotCode;

    /**
     * 该广告位下的投放计划数量
     */
    private Long campaignCount;

    /**
     * 曝光次数
     */
    private Long impressionCount;

    /**
     * 点击次数
     */
    private Long clickCount;

    /**
     * 获取广告位编码。
     *
     * @return 广告位编码
     */
    public String getSlotCode() {
        return slotCode;
    }

    /**
     * 设置广告位编码。
     *
     * @param slotCode 广告位编码
     */
    public void setSlotCode(String slotCode) {
        this.slotCode = slotCode;
    }

    /**
     * 获取该广告位下的投放计划数量。
     *
     * @return 投放计划数量
     */
    public Long getCampaignCount() {
        return campaignCount;
    }

    /**
     * 设置该广告位下的投放计划数量。
     *
     * @param campaignCount 投放计划数量
     */
    public void setCampaignCount(Long campaignCount) {
        this.campaignCount = campaignCount;
    }

    /**
     * 获取曝光次数。
     *
     * @return 曝光次数
     */
    public Long getImpressionCount() {
        return impressionCount;
    }

    /**
     * 设置曝光次数。
     *
     * @param impressionCount 曝光次数
     */
    public void setImpressionCount(Long impressionCount) {
        this.impressionCount = impressionCount;
    }

    /**
     * 获取点击次数。
     *
     * @return 点击次数
     */
    public Long getClickCount() {
        return clickCount;
    }

    /**
     * 设置点击次数。
     *
     * @param clickCount 点击次数
     */
    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }
}

