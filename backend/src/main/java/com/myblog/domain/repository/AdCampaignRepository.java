package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.AdCampaign;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 广告投放仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface AdCampaignRepository {

    /**
     * 保存广告投放（新增或更新）。
     */
    AdCampaign save(AdCampaign campaign);

    /**
     * 根据 ID 查询广告投放。
     */
    Optional<AdCampaign> findById(Long id);

    /**
     * 查询指定广告位在指定时间生效的广告列表，按 sort_order 排序。
     */
    List<AdCampaign> findActiveBySlot(String slotCode, LocalDateTime time);

    /**
     * 分页查询广告投放（后台管理用）。
     */
    List<AdCampaign> findAll(int page, int pageSize, String slotCode, Boolean enabled);

    /**
     * 统计广告投放总数（后台管理用）。
     */
    long count(String slotCode, Boolean enabled);

    /**
     * 记录广告事件（曝光或点击）。
     */
    void recordEvent(Long campaignId, String eventType, Long userId, String ipAddress, String userAgent);

    /**
     * 统计广告事件数（后台统计用）。
     */
    long countEvents(Long campaignId, String eventType);

    /**
     * 统计指定类型的广告事件总次数（全部投放汇总）。
     *
     * @param eventType 事件类型，IMPRESSION 或 CLICK
     */
    long countTotalEvents(String eventType);

    /**
     * 按广告位聚合统计投放数、曝光数、点击数。
     */
    List<SlotStat> findSlotStats();

    /**
     * 广告位维度统计值对象。
     */
    class SlotStat {

        private final String slotCode;
        private final long campaignCount;
        private final long impressions;
        private final long clicks;

        public SlotStat(String slotCode, long campaignCount, long impressions, long clicks) {
            this.slotCode = slotCode;
            this.campaignCount = campaignCount;
            this.impressions = impressions;
            this.clicks = clicks;
        }

        public String getSlotCode() {
            return slotCode;
        }

        public long getCampaignCount() {
            return campaignCount;
        }

        public long getImpressions() {
            return impressions;
        }

        public long getClicks() {
            return clicks;
        }
    }
}

