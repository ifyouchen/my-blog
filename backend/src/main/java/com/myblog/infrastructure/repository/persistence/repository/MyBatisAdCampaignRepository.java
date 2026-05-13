package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.AdCampaign;
import com.myblog.domain.repository.AdCampaignRepository;
import com.myblog.infrastructure.repository.persistence.converter.AdCampaignPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.AdCampaignDO;
import com.myblog.infrastructure.repository.persistence.entity.AdSlotStatDO;
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

    /**
     * 创建广告投放 MyBatis 仓储。
     *
     * @param adCampaignMapper 广告投放 Mapper
     */
    public MyBatisAdCampaignRepository(AdCampaignMapper adCampaignMapper) {
        this.adCampaignMapper = adCampaignMapper;
    }

    /**
     * 保存广告投放记录（ID 为 null 则插入，否则更新）。
     *
     * @param campaign 广告投放聚合根
     * @return 保存后的广告投放聚合根
     */
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

    /**
     * 根据 ID 查询广告投放。
     *
     * @param id 广告投放 ID
     * @return 广告投放 Optional
     */
    @Override
    public Optional<AdCampaign> findById(Long id) {
        AdCampaignDO dO = adCampaignMapper.selectById(id);
        return Optional.ofNullable(AdCampaignPersistenceConverter.toDomain(dO));
    }

    /**
     * 查询指定广告位在指定时间点有效的广告列表。
     *
     * @param slotCode 广告位编码
     * @param time     查询时间点
     * @return 有效广告列表
     */
    @Override
    public List<AdCampaign> findActiveBySlot(String slotCode, LocalDateTime time) {
        List<AdCampaignDO> rows = adCampaignMapper.selectActiveBySlot(slotCode, time);
        List<AdCampaign> result = new ArrayList<>(rows.size());
        for (AdCampaignDO dO : rows) {
            result.add(AdCampaignPersistenceConverter.toDomain(dO));
        }
        return result;
    }

    /**
     * 分页查询所有广告投放列表。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param slotCode 广告位编码（可为 null）
     * @param enabled  是否启用（可为 null）
     * @return 广告投放列表
     */
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

    /**
     * 统计广告投放数量。
     *
     * @param slotCode 广告位编码（可为 null）
     * @param enabled  是否启用（可为 null）
     * @return 广告投放数量
     */
    @Override
    public long count(String slotCode, Boolean enabled) {
        return adCampaignMapper.countAll(slotCode, enabled);
    }

    /**
     * 记录广告事件（曝光或点击）。
     *
     * @param campaignId 广告投放 ID
     * @param eventType  事件类型
     * @param userId     用户 ID
     * @param ipAddress  客户端 IP
     * @param userAgent  客户端 UA
     */
    @Override
    public void recordEvent(Long campaignId, String eventType, Long userId,
                             String ipAddress, String userAgent) {
        adCampaignMapper.insertEvent(campaignId, eventType, userId, ipAddress, userAgent);
    }

    /**
     * 统计广告事件数量。
     *
     * @param campaignId 广告投放 ID
     * @param eventType  事件类型
     * @return 事件数量
     */
    @Override
    public long countEvents(Long campaignId, String eventType) {
        return adCampaignMapper.countEvents(campaignId, eventType);
    }

    /**
     * 统计全部广告事件总次数。
     *
     * @param eventType 事件类型
     * @return 事件总次数
     */
    @Override
    public long countTotalEvents(String eventType) {
        return adCampaignMapper.countTotalEvents(eventType);
    }

    /**
     * 按广告位聚合统计广告数、曝光数和点击数。
     *
     * @return 广告位统计列表
     */
    @Override
    public List<AdCampaignRepository.SlotStat> findSlotStats() {
        List<AdSlotStatDO> rows = adCampaignMapper.selectSlotStats();
        List<AdCampaignRepository.SlotStat> result = new ArrayList<>(rows.size());
        for (AdSlotStatDO row : rows) {
            long impressions = row.getImpressionCount() == null ? 0L : row.getImpressionCount();
            long clicks = row.getClickCount() == null ? 0L : row.getClickCount();
            long campaigns = row.getCampaignCount() == null ? 0L : row.getCampaignCount();
            result.add(new AdCampaignRepository.SlotStat(row.getSlotCode(), campaigns, impressions, clicks));
        }
        return result;
    }
}

