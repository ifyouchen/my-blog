package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.AdCampaign;
import com.myblog.domain.repository.AdCampaignRepository;
import com.myblog.infrastructure.repository.persistence.converter.AdCampaignPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.AdCampaignDO;
import com.myblog.infrastructure.repository.persistence.mapper.AdCampaignMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 广告投放仓储 MyBatis 实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisAdCampaignRepository implements AdCampaignRepository {

    private final AdCampaignMapper adCampaignMapper;

    public MyBatisAdCampaignRepository(AdCampaignMapper adCampaignMapper) {
        this.adCampaignMapper = adCampaignMapper;
    }

    @Override
    public AdCampaign save(AdCampaign campaign) {
        AdCampaignDO dO = AdCampaignPersistenceConverter.toDO(campaign);
        if (dO.getId() == null) {
            adCampaignMapper.insert(dO);
            return AdCampaignPersistenceConverter.toDomain(dO);
        }
        adCampaignMapper.updateById(dO);
        return campaign;
    }

    @Override
    public Optional<AdCampaign> findById(Long id) {
        AdCampaignDO dO = adCampaignMapper.selectById(id);
        return Optional.ofNullable(AdCampaignPersistenceConverter.toDomain(dO));
    }

    @Override
    public List<AdCampaign> findActiveBySlot(String slotCode, LocalDateTime time) {
        List<AdCampaignDO> rows = adCampaignMapper.selectActiveBySlot(slotCode, time);
        List<AdCampaign> result = new ArrayList<>(rows.size());
        for (AdCampaignDO dO : rows) {
            result.add(AdCampaignPersistenceConverter.toDomain(dO));
        }
        return result;
    }

    @Override
    public List<AdCampaign> findAll(int page, int pageSize, String slotCode, Boolean enabled) {
        int offset = (page - 1) * pageSize;
        List<AdCampaignDO> rows = adCampaignMapper.selectAll(slotCode, enabled, offset, pageSize);
        List<AdCampaign> result = new ArrayList<>(rows.size());
        for (AdCampaignDO dO : rows) {
            result.add(AdCampaignPersistenceConverter.toDomain(dO));
        }
        return result;
    }

    @Override
    public long count(String slotCode, Boolean enabled) {
        return adCampaignMapper.countAll(slotCode, enabled);
    }

    @Override
    public void recordEvent(Long campaignId, String eventType, Long userId,
                             String ipAddress, String userAgent) {
        adCampaignMapper.insertEvent(campaignId, eventType, userId, ipAddress, userAgent);
    }

    @Override
    public long countEvents(Long campaignId, String eventType) {
        return adCampaignMapper.countEvents(campaignId, eventType);
    }
}

