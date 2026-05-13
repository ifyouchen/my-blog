package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.AdCampaignDO;
import com.myblog.infrastructure.repository.persistence.entity.AdEventCountDO;
import com.myblog.infrastructure.repository.persistence.entity.AdSlotStatDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广告投放 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
@Mapper
public interface AdCampaignMapper {

    /**
     * 插入广告投放记录。
     *
     * @param campaign 广告投放数据对象
     */
    void insert(AdCampaignDO campaign);

    /**
     * 根据 ID 更新广告投放记录。
     *
     * @param campaign 广告投放数据对象
     */
    void updateById(AdCampaignDO campaign);

    /**
     * 根据 ID 查询广告投放记录。
     *
     * @param id 广告 ID
     * @return 广告投放数据对象
     */
    AdCampaignDO selectById(@Param("id") Long id);

    /**
     * 查询指定广告位当前有效的广告列表。
     *
     * @param slotCode 广告位编码
     * @param now      当前时间
     * @return 广告投放列表
     */
    List<AdCampaignDO> selectActiveBySlot(@Param("slotCode") String slotCode,
                                           @Param("now") LocalDateTime now);

    /**
     * 分页查询广告投放列表。
     *
     * @param slotCode 广告位编码
     * @param enabled  是否启用
     * @param offset   偏移量
     * @param limit    限制数量
     * @return 广告投放列表
     */
    List<AdCampaignDO> selectAll(@Param("slotCode") String slotCode,
                                  @Param("enabled") Boolean enabled,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /**
     * 统计广告投放数量。
     *
     * @param slotCode 广告位编码
     * @param enabled  是否启用
     * @return 广告数量
     */
    long countAll(@Param("slotCode") String slotCode,
                  @Param("enabled") Boolean enabled);

    /**
     * 插入广告事件记录。
     *
     * @param campaignId 广告 ID
     * @param eventType  事件类型
     * @param userId     用户 ID
     * @param ipAddress  IP 地址
     * @param userAgent  用户代理
     */
    void insertEvent(@Param("campaignId") Long campaignId,
                     @Param("eventType") String eventType,
                     @Param("userId") Long userId,
                     @Param("ipAddress") String ipAddress,
                     @Param("userAgent") String userAgent);

    /**
     * 统计指定广告的事件数量。
     *
     * @param campaignId 广告 ID
     * @param eventType  事件类型
     * @return 事件数量
     */
    long countEvents(@Param("campaignId") Long campaignId,
                     @Param("eventType") String eventType);

    /**
     * 批量查询广告事件统计。
     *
     * @param campaignIds 广告 ID 列表
     * @return 广告事件统计列表
     */
    List<AdEventCountDO> selectEventStats(@Param("campaignIds") List<Long> campaignIds);

    /**
     * 统计全部广告事件（曝光或点击）总次数。
     */
    long countTotalEvents(@Param("eventType") String eventType);

    /**
     * 按广告位聚合统计广告数、曝光数和点击数。
     */
    List<AdSlotStatDO> selectSlotStats();
}

