package com.myblog.domain.model.aggregate;

import java.time.LocalDateTime;

/**
 * 广告投放聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdCampaign {

    private Long id;
    private String slotCode;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private String label;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean enabled;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private AdCampaign() {
    }

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

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    /**
     * 判断广告是否在指定时间处于有效展示状态。
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

    public Long getId() {
        return id;
    }

    public String getSlotCode() {
        return slotCode;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getLabel() {
        return label;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Integer getVersion() {
        return version;
    }
}

