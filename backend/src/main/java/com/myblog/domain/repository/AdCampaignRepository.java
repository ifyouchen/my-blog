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
     *
     * @param campaign 广告投放聚合根
     * @return 保存后的广告投放
     */
    AdCampaign save(AdCampaign campaign);

    /**
     * 根据 ID 查询广告投放。
     *
     * @param id 广告投放 ID
     * @return 广告投放 Optional
     */
    Optional<AdCampaign> findById(Long id);

    /**
     * 查询指定广告位在指定时间生效的广告列表，按 sort_order 排序。
     *
     * @param slotCode 广告位编码
     * @param time     指定时间
     * @return 生效的广告列表
     */
    List<AdCampaign> findActiveBySlot(String slotCode, LocalDateTime time);

    /**
     * 分页查询广告投放（后台管理用）。
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param slotCode 广告位编码筛选
     * @param enabled  是否启用筛选
     * @return 广告投放列表
     */
    List<AdCampaign> findAll(int page, int pageSize, String slotCode, Boolean enabled);

    /**
     * 统计广告投放总数（后台管理用）。
     *
     * @param slotCode 广告位编码筛选
     * @param enabled  是否启用筛选
     * @return 广告投放数量
     */
    long count(String slotCode, Boolean enabled);

    /**
     * 记录广告事件（曝光或点击）。
     *
     * @param campaignId 广告投放 ID
     * @param eventType  事件类型（IMPRESSION / CLICK）
     * @param userId     触发用户 ID
     * @param ipAddress  来源 IP 地址
     * @param userAgent  浏览器 User-Agent
     */
    void recordEvent(Long campaignId, String eventType, Long userId, String ipAddress, String userAgent);

    /**
     * 统计广告事件数（后台统计用）。
     *
     * @param campaignId 广告投放 ID
     * @param eventType  事件类型
     * @return 事件数量
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
     *
     * @return 广告位维度统计列表
     */
    List<SlotStat> findSlotStats();

    /**
     * 广告位维度统计值对象。
     */
    class SlotStat {

        /** 广告位编码 */
        private final String slotCode;
        /** 该广告位的投放数量 */
        private final long campaignCount;
        /** 曝光总数 */
        private final long impressions;
        /** 点击总数 */
        private final long clicks;

        /**
         * 创建广告位统计值对象。
         *
         * @param slotCode      广告位编码
         * @param campaignCount 投放数量
         * @param impressions   曝光总数
         * @param clicks        点击总数
         */
        public SlotStat(String slotCode, long campaignCount, long impressions, long clicks) {
            this.slotCode = slotCode;
            this.campaignCount = campaignCount;
            this.impressions = impressions;
            this.clicks = clicks;
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
         * 获取该广告位的投放数量。
         *
         * @return 投放数量
         */
        public long getCampaignCount() {
            return campaignCount;
        }

        /**
         * 获取曝光总数。
         *
         * @return 曝光总数
         */
        public long getImpressions() {
            return impressions;
        }

        /**
         * 获取点击总数。
         *
         * @return 点击总数
         */
        public long getClicks() {
            return clicks;
        }
    }
}

