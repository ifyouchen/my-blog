package com.myblog.application.service;

import com.myblog.application.dto.AdCampaignDTO;
import com.myblog.application.dto.AdStatsDTO;
import com.myblog.domain.model.aggregate.AdCampaign;
import com.myblog.domain.repository.AdCampaignRepository;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.util.BizLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.myblog.infrastructure.repository.persistence.mapper.AdDismissalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 广告应用服务。
 * <p>前台：根据广告位查询当前生效的广告，记录曝光/点击事件。</p>
 * <p>后台：广告投放的增删改查及统计。</p>
 *
 * @author Codex
 * @since 1.0.0
 */
@Service
public class AdAppService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Logger log = LoggerFactory.getLogger(AdAppService.class);

    private final AdCampaignRepository adCampaignRepository;
    private final AdDismissalMapper adDismissalMapper;

    public AdAppService(AdCampaignRepository adCampaignRepository,
                        AdDismissalMapper adDismissalMapper) {
        this.adCampaignRepository = adCampaignRepository;
        this.adDismissalMapper = adDismissalMapper;
    }

    // ==================== 前台接口 ====================

    /**
     * 查询指定广告位当前生效的广告列表（前台展示用）。
     */
    public List<AdCampaignDTO> listActiveBySlot(String slotCode) {
        if (slotCode == null || slotCode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<AdCampaign> campaigns = adCampaignRepository.findActiveBySlot(slotCode.trim(), LocalDateTime.now());
        List<AdCampaignDTO> result = new ArrayList<>(campaigns.size());
        for (AdCampaign campaign : campaigns) {
            result.add(toPublicDTO(campaign));
        }
        return result;
    }

    /**
     * 记录广告曝光事件（异步友好，失败不影响正常流程）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordImpression(Long campaignId, Long userId, String ipAddress, String userAgent) {
        long _start = System.currentTimeMillis();
        adCampaignRepository.findById(campaignId).orElseThrow(
            () -> new ApplicationException(ErrorCode.NOT_FOUND, "广告不存在")
        );
        adCampaignRepository.recordEvent(campaignId, "IMPRESSION", userId, ipAddress, userAgent);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "记录广告曝光",
            BizLogHelper.trace(),
            BizLogHelper.params("campaignId", campaignId, "userId", userId),
            BizLogHelper.result("recorded=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 记录广告点击事件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordClick(Long campaignId, Long userId, String ipAddress, String userAgent) {
        long _start = System.currentTimeMillis();
        adCampaignRepository.findById(campaignId).orElseThrow(
            () -> new ApplicationException(ErrorCode.NOT_FOUND, "广告不存在")
        );
        adCampaignRepository.recordEvent(campaignId, "CLICK", userId, ipAddress, userAgent);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "记录广告点击",
            BizLogHelper.trace(),
            BizLogHelper.params("campaignId", campaignId, "userId", userId),
            BizLogHelper.result("recorded=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 关闭广告（按用户维度，3天后自动过期）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void dismiss(Long campaignId, Long userId) {
        if (userId == null) {
            return;
        }
        long _start = System.currentTimeMillis();
        adDismissalMapper.insert(userId, campaignId, LocalDateTime.now());
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "关闭广告",
            BizLogHelper.trace(),
            BizLogHelper.params("campaignId", campaignId, "userId", userId),
            BizLogHelper.result("dismissed=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 获取当前用户 3 天内关闭过的广告 ID 列表。
     */
    public List<Long> getUserDismissedIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        return adDismissalMapper.selectDismissedAdIds(userId, threeDaysAgo);
    }

    // ==================== 后台接口 ====================

    /**
     * 后台分页查询广告列表（含统计数据）。
     */
    public PageResult<AdCampaignDTO> listForAdmin(int page, int pageSize, String slotCode, Boolean enabled) {
        List<AdCampaign> campaigns = adCampaignRepository.findAll(page, pageSize, slotCode, enabled);
        long total = adCampaignRepository.count(slotCode, enabled);
        List<AdCampaignDTO> items = new ArrayList<>(campaigns.size());
        for (AdCampaign campaign : campaigns) {
            AdCampaignDTO dto = toAdminDTO(campaign);
            dto.setImpressionCount(adCampaignRepository.countEvents(campaign.getId(), "IMPRESSION"));
            dto.setClickCount(adCampaignRepository.countEvents(campaign.getId(), "CLICK"));
            items.add(dto);
        }
        return new PageResult<>(items, page, pageSize, total);
    }

    /**
     * 后台获取广告详情。
     */
    public AdCampaignDTO getForAdmin(Long id) {
        AdCampaign campaign = adCampaignRepository.findById(id).orElseThrow(
            () -> new ApplicationException(ErrorCode.NOT_FOUND, "广告不存在")
        );
        AdCampaignDTO dto = toAdminDTO(campaign);
        dto.setImpressionCount(adCampaignRepository.countEvents(id, "IMPRESSION"));
        dto.setClickCount(adCampaignRepository.countEvents(id, "CLICK"));
        return dto;
    }

    /**
     * 后台创建广告。
     */
    @Transactional(rollbackFor = Exception.class)
    public AdCampaignDTO create(String slotCode, String title, String imageUrl, String targetUrl,
                                 String label, LocalDateTime startAt, LocalDateTime endAt,
                                 Boolean enabled, Integer sortOrder) {
        long _start = System.currentTimeMillis();
        if (slotCode == null || slotCode.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "广告位编码不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "广告标题不能为空");
        }
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "跳转链接不能为空");
        }
        AdCampaign campaign = AdCampaign.create(
            slotCode.trim(), title.trim(), imageUrl, targetUrl.trim(),
            label, startAt, endAt, enabled, sortOrder
        );
        AdCampaign saved = adCampaignRepository.save(campaign);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "创建广告",
            BizLogHelper.trace(),
            BizLogHelper.params("title", title, "slotCode", slotCode),
            BizLogHelper.result("campaignId=" + saved.getId()),
            BizLogHelper.elapsed(_start));
        return toAdminDTO(saved);
    }

    /**
     * 后台更新广告。
     */
    @Transactional(rollbackFor = Exception.class)
    public AdCampaignDTO update(Long id, String slotCode, String title, String imageUrl, String targetUrl,
                                 String label, LocalDateTime startAt, LocalDateTime endAt,
                                 Boolean enabled, Integer sortOrder) {
        long _start = System.currentTimeMillis();
        AdCampaign campaign = adCampaignRepository.findById(id).orElseThrow(
            () -> new ApplicationException(ErrorCode.NOT_FOUND, "广告不存在")
        );
        if (title == null || title.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "广告标题不能为空");
        }
        if (targetUrl == null || targetUrl.trim().isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "跳转链接不能为空");
        }
        campaign.update(slotCode, title.trim(), imageUrl, targetUrl.trim(), label, startAt, endAt, enabled, sortOrder);
        adCampaignRepository.save(campaign);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "更新广告",
            BizLogHelper.trace(),
            BizLogHelper.params("campaignId", id, "title", title),
            BizLogHelper.result("campaignId=" + id),
            BizLogHelper.elapsed(_start));
        return toAdminDTO(campaign);
    }

    /**
     * 后台删除广告（软删除）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        long _start = System.currentTimeMillis();
        AdCampaign campaign = adCampaignRepository.findById(id).orElseThrow(
            () -> new ApplicationException(ErrorCode.NOT_FOUND, "广告不存在")
        );
        campaign.delete();
        adCampaignRepository.save(campaign);
        log.info("{} | {} | 入参({}) | 结果({}) | {}",
            "删除广告",
            BizLogHelper.trace(),
            BizLogHelper.params("campaignId", id),
            BizLogHelper.result("deleted=true"),
            BizLogHelper.elapsed(_start));
    }

    /**
     * 获取广告整体统计概览（后台展示用）。
     */
    public AdStatsDTO getStats() {
        long total = adCampaignRepository.count(null, null);
        long enabled = adCampaignRepository.count(null, true);
        long totalImpressions = adCampaignRepository.countTotalEvents("IMPRESSION");
        long totalClicks = adCampaignRepository.countTotalEvents("CLICK");
        List<AdCampaignRepository.SlotStat> slotRows = adCampaignRepository.findSlotStats();

        List<AdStatsDTO.SlotStat> slotStats = new ArrayList<>(slotRows.size());
        for (AdCampaignRepository.SlotStat row : slotRows) {
            AdStatsDTO.SlotStat s = new AdStatsDTO.SlotStat();
            s.setSlotCode(row.getSlotCode());
            s.setCampaignCount(row.getCampaignCount());
            s.setImpressions(row.getImpressions());
            s.setClicks(row.getClicks());
            slotStats.add(s);
        }

        AdStatsDTO dto = new AdStatsDTO();
        dto.setTotalCampaigns(total);
        dto.setEnabledCampaigns(enabled);
        dto.setTotalImpressions(totalImpressions);
        dto.setTotalClicks(totalClicks);
        dto.setSlotStats(slotStats);
        return dto;
    }

    // ==================== 私有工具方法 ====================

    private AdCampaignDTO toPublicDTO(AdCampaign campaign) {
        AdCampaignDTO dto = new AdCampaignDTO();
        dto.setId(campaign.getId());
        dto.setSlotCode(campaign.getSlotCode());
        dto.setTitle(campaign.getTitle());
        dto.setImageUrl(campaign.getImageUrl());
        dto.setTargetUrl(campaign.getTargetUrl());
        dto.setLabel(campaign.getLabel() != null ? campaign.getLabel() : "广告");
        return dto;
    }

    private AdCampaignDTO toAdminDTO(AdCampaign campaign) {
        AdCampaignDTO dto = new AdCampaignDTO();
        dto.setId(campaign.getId());
        dto.setSlotCode(campaign.getSlotCode());
        dto.setTitle(campaign.getTitle());
        dto.setImageUrl(campaign.getImageUrl());
        dto.setTargetUrl(campaign.getTargetUrl());
        dto.setLabel(campaign.getLabel() != null ? campaign.getLabel() : "广告");
        dto.setStartAt(formatDateTime(campaign.getStartAt()));
        dto.setEndAt(formatDateTime(campaign.getEndAt()));
        dto.setEnabled(campaign.getEnabled());
        dto.setSortOrder(campaign.getSortOrder());
        dto.setCreatedAt(formatDateTime(campaign.getCreatedAt()));
        return dto;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : DATETIME_FORMATTER.format(dateTime);
    }
}

