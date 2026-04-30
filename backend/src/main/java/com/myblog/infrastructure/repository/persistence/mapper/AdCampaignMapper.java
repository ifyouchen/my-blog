package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.AdCampaignDO;
import com.myblog.infrastructure.repository.persistence.entity.AdEventCountDO;
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

    void insert(AdCampaignDO campaign);

    void updateById(AdCampaignDO campaign);

    AdCampaignDO selectById(@Param("id") Long id);

    List<AdCampaignDO> selectActiveBySlot(@Param("slotCode") String slotCode,
                                           @Param("now") LocalDateTime now);

    List<AdCampaignDO> selectAll(@Param("slotCode") String slotCode,
                                  @Param("enabled") Boolean enabled,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    long countAll(@Param("slotCode") String slotCode,
                  @Param("enabled") Boolean enabled);

    void insertEvent(@Param("campaignId") Long campaignId,
                     @Param("eventType") String eventType,
                     @Param("userId") Long userId,
                     @Param("ipAddress") String ipAddress,
                     @Param("userAgent") String userAgent);

    long countEvents(@Param("campaignId") Long campaignId,
                     @Param("eventType") String eventType);

    List<AdEventCountDO> selectEventStats(@Param("campaignIds") List<Long> campaignIds);
}

