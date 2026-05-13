package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.AdCampaign;
import com.myblog.infrastructure.repository.persistence.entity.AdCampaignDO;

/**
 * 广告投放 领域对象 ↔ DO 转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class AdCampaignPersistenceConverter {

    private AdCampaignPersistenceConverter() {
    }

    /**
     * 将广告投放领域对象转换为数据对象。
     *
     * @param campaign 广告投放领域对象
     * @return 广告投放数据对象，若 campaign 为 null 则返回 null
     */
    public static AdCampaignDO toDO(AdCampaign campaign) {
        if (campaign == null) {
            return null;
        }
        AdCampaignDO dO = new AdCampaignDO();
        dO.setId(campaign.getId());
        dO.setSlotCode(campaign.getSlotCode());
        dO.setTitle(campaign.getTitle());
        dO.setImageUrl(campaign.getImageUrl());
        dO.setTargetUrl(campaign.getTargetUrl());
        dO.setLabel(campaign.getLabel());
        dO.setStartAt(campaign.getStartAt());
        dO.setEndAt(campaign.getEndAt());
        dO.setEnabled(campaign.getEnabled());
        dO.setSortOrder(campaign.getSortOrder());
        dO.setCreatedAt(campaign.getCreatedAt());
        dO.setUpdatedAt(campaign.getUpdatedAt());
        dO.setDeletedAt(campaign.getDeletedAt());
        dO.setVersion(campaign.getVersion());
        return dO;
    }

    /**
     * 将广告投放数据对象转换为领域对象。
     *
     * @param dO 广告投放数据对象
     * @return 广告投放领域对象，若 dO 为 null 则返回 null
     */
    public static AdCampaign toDomain(AdCampaignDO dO) {
        if (dO == null) {
            return null;
        }
        return AdCampaign.restore(
            dO.getId(),
            dO.getSlotCode(),
            dO.getTitle(),
            dO.getImageUrl(),
            dO.getTargetUrl(),
            dO.getLabel(),
            dO.getStartAt(),
            dO.getEndAt(),
            dO.getEnabled(),
            dO.getSortOrder(),
            dO.getCreatedAt(),
            dO.getUpdatedAt(),
            dO.getDeletedAt(),
            dO.getVersion()
        );
    }
}

