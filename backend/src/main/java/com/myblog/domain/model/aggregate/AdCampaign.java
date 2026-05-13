package com.myblog.domain.model.aggregate;

import java.time.LocalDateTime;

/**
 * 广告投放聚合根。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class AdCampaign {

    /**
     * 广告投放 ID
     */
    private Long id;

    /**
     * 广告位编码（对应页面中的广告槽）
     */
    private String slotCode;

    /**
     * 广告标题
     */
    private String title;

    /**
     * 广告图片地址
     */
    private String imageUrl;

    /**
     * 广告点击跳转目标 URL
     */
    private String targetUrl;

    /**
     * 广告标签（如"广告"、"赞助"等展示文案）
     */
    private String label;

    /**
     * 广告展示开始时间，为 null 表示不限开始时间
     */
    private LocalDateTime startAt;

    /**
     * 广告展示结束时间，为 null 表示不限结束时间
     */
    private LocalDateTime endAt;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 广告排序权重，值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 广告创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 广告最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    private AdCampaign() {
    }

    /**
     * 创建广告投放聚合根。
     *
     * @param slotCode  广告位编码
     * @param title     广告标题
     * @param imageUrl  广告图片地址
     * @param targetUrl 点击跳转目标 URL
     * @param label     广告标签
     * @param startAt   展示开始时间
     * @param endAt     展示结束时间
     * @param enabled   是否启用
     * @param sortOrder 排序权重
     * @return 广告投放聚合根
     */
    public static AdCampaign create(String slotCode, String title, String imageUrl,
                                     String targetUrl, String label,
                                     LocalDateTime startAt, LocalDateTime endAt,
                                     Boolean enabled, Integer sortOrder) {
        AdCampaign campaign = new AdCampaign();
        campaign.slotCode = slotCode;
        campaign.title = title;
        campaign.imageUrl = imageUrl;
        campaign.targetUrl = targetUrl;
        campaign.label = label != null ? label : "广告";
        campaign.startAt = startAt;
        campaign.endAt = endAt;
        campaign.enabled = enabled != null ? enabled : Boolean.TRUE;
        campaign.sortOrder = sortOrder != null ? sortOrder : 0;
        campaign.createdAt = LocalDateTime.now();
        campaign.updatedAt = campaign.createdAt;
        campaign.deletedAt = null;
        campaign.version = 0;
        return campaign;
    }

    /**
     * 从持久化数据还原广告投放聚合根。
     *
     * @param id        广告投放 ID
     * @param slotCode  广告位编码
     * @param title     广告标题
     * @param imageUrl  广告图片地址
     * @param targetUrl 点击跳转目标 URL
     * @param label     广告标签
     * @param startAt   展示开始时间
     * @param endAt     展示结束时间
     * @param enabled   是否启用
     * @param sortOrder 排序权重
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @param deletedAt 删除时间
     * @param version   乐观锁版本号
     * @return 广告投放聚合根
     */
    public static AdCampaign restore(Long id, String slotCode, String title, String imageUrl,
                                      String targetUrl, String label,
                                      LocalDateTime startAt, LocalDateTime endAt,
                                      Boolean enabled, Integer sortOrder,
                                      LocalDateTime createdAt, LocalDateTime updatedAt,
                                      LocalDateTime deletedAt, Integer version) {
        AdCampaign campaign = new AdCampaign();
        campaign.id = id;
        campaign.slotCode = slotCode;
        campaign.title = title;
        campaign.imageUrl = imageUrl;
        campaign.targetUrl = targetUrl;
        campaign.label = label != null ? label : "广告";
        campaign.startAt = startAt;
        campaign.endAt = endAt;
        campaign.enabled = enabled != null ? enabled : Boolean.TRUE;
        campaign.sortOrder = sortOrder != null ? sortOrder : 0;
        campaign.createdAt = createdAt;
        campaign.updatedAt = updatedAt;
        campaign.deletedAt = deletedAt;
        campaign.version = version;
        return campaign;
    }

    /**
     * 更新广告投放信息。
     *
     * @param slotCode  广告位编码
     * @param title     广告标题
     * @param imageUrl  广告图片地址
     * @param targetUrl 点击跳转目标 URL
     * @param label     广告标签
     * @param startAt   展示开始时间
     * @param endAt     展示结束时间
     * @param enabled   是否启用
     * @param sortOrder 排序权重
     */
    public void update(String slotCode, String title, String imageUrl, String targetUrl, String label,
                       LocalDateTime startAt, LocalDateTime endAt,
                       Boolean enabled, Integer sortOrder) {
        this.slotCode = slotCode;
        this.title = title;
        this.imageUrl = imageUrl;
        this.targetUrl = targetUrl;
        if (label != null) {
            this.label = label;
        }
        this.startAt = startAt;
        this.endAt = endAt;
        if (enabled != null) {
            this.enabled = enabled;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 软删除广告投放。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    /**
     * 判断广告在指定时间是否处于有效展示状态。
     *
     * @param time 指定时间
     * @return 有效展示返回 true，否则返回 false
     */
    public boolean isActiveAt(LocalDateTime time) {
        if (Boolean.FALSE.equals(enabled) || deletedAt != null) {
            return false;
        }
        if (startAt != null && time.isBefore(startAt)) {
            return false;
        }
        if (endAt != null && time.isAfter(endAt)) {
            return false;
        }
        return true;
    }

    /**
     * 获取广告投放 ID。
     *
     * @return 广告投放 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取广告位编码。
     *
     * @return 广告位编码
     */
    public String getSlotCode() {
        return slotCode;
    }

    /**
     * 获取广告标题。
     *
     * @return 广告标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取广告图片地址。
     *
     * @return 广告图片地址
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 获取广告点击跳转目标 URL。
     *
     * @return 目标 URL
     */
    public String getTargetUrl() {
        return targetUrl;
    }

    /**
     * 获取广告标签文案。
     *
     * @return 广告标签
     */
    public String getLabel() {
        return label;
    }

    /**
     * 获取广告展示开始时间。
     *
     * @return 展示开始时间
     */
    public LocalDateTime getStartAt() {
        return startAt;
    }

    /**
     * 获取广告展示结束时间。
     *
     * @return 展示结束时间
     */
    public LocalDateTime getEndAt() {
        return endAt;
    }

    /**
     * 获取是否启用。
     *
     * @return 是否启用
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * 获取广告排序权重。
     *
     * @return 排序权重
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 获取广告创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取广告最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}

