package com.myblog.interfaces.rest.dto.request;

/**
 * 创建广告活动请求.
 * <p>
 * 用于后台管理端创建或编辑广告位活动配置.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CreateAdCampaignRequest {

    /** 广告位编码. */
    private String slotCode;

    /** 广告标题. */
    private String title;

    /** 广告图片URL. */
    private String imageUrl;

    /** 点击跳转目标URL. */
    private String targetUrl;

    /** 广告标签文案. */
    private String label;

    /** 开始时间（ISO格式）. */
    private String startAt;

    /** 结束时间（ISO格式）. */
    private String endAt;

    /** 是否启用. */
    private Boolean enabled;

    /** 排序权重，值越大越靠前. */
    private Integer sortOrder;

    /**
     * 获取广告位编码.
     *
     * @return 广告位编码
     */
    public String getSlotCode() {
        return slotCode;
    }

    /**
     * 设置广告位编码.
     *
     * @param slotCode 广告位编码
     */
    public void setSlotCode(String slotCode) {
        this.slotCode = slotCode;
    }

    /**
     * 获取广告标题.
     *
     * @return 广告标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置广告标题.
     *
     * @param title 广告标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取广告图片URL.
     *
     * @return 图片URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 设置广告图片URL.
     *
     * @param imageUrl 图片URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 获取点击跳转目标URL.
     *
     * @return 目标URL
     */
    public String getTargetUrl() {
        return targetUrl;
    }

    /**
     * 设置点击跳转目标URL.
     *
     * @param targetUrl 目标URL
     */
    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    /**
     * 获取广告标签文案.
     *
     * @return 标签文案
     */
    public String getLabel() {
        return label;
    }

    /**
     * 设置广告标签文案.
     *
     * @param label 标签文案
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * 获取开始时间.
     *
     * @return 开始时间（ISO格式）
     */
    public String getStartAt() {
        return startAt;
    }

    /**
     * 设置开始时间.
     *
     * @param startAt 开始时间（ISO格式）
     */
    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    /**
     * 获取结束时间.
     *
     * @return 结束时间（ISO格式）
     */
    public String getEndAt() {
        return endAt;
    }

    /**
     * 设置结束时间.
     *
     * @param endAt 结束时间（ISO格式）
     */
    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }

    /**
     * 获取是否启用.
     *
     * @return 是否启用
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用.
     *
     * @param enabled 是否启用
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取排序权重.
     *
     * @return 排序权重
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 设置排序权重.
     *
     * @param sortOrder 排序权重
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
