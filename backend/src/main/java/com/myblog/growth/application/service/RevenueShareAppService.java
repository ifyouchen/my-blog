package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.domain.model.valueobject.RevenueShareJournal;
import com.myblog.growth.domain.repository.PointRuleRepository;
import com.myblog.growth.domain.repository.RevenueShareRepository;
import com.myblog.shared.result.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分账应用服务.
 *
 * <p>
 * 负责在文章解锁成功后，根据分成比例创建待结算分账流水。
 * 分成比例从 {@code point_rule_config.UNLOCK_SHARE_RATIO} 读取，回退 50%。
 * </p>
 */
@Service
public class RevenueShareAppService {

    private static final Logger log = LoggerFactory.getLogger(RevenueShareAppService.class);

    /** 积分规则来源类型：平台分成比例. */
    private static final String SOURCE_UNLOCK_SHARE_RATIO = "UNLOCK_SHARE_RATIO";

    /** 默认平台分成比例（50%），当规则未配置或无效时回退. */
    private static final double DEFAULT_PLATFORM_RATIO = 0.5;

    private final RevenueShareRepository revenueShareRepository;
    private final PointRuleRepository pointRuleRepository;
    private final GrowthJournalContextService journalContextService;

    public RevenueShareAppService(RevenueShareRepository revenueShareRepository,
                                  PointRuleRepository pointRuleRepository,
                                  GrowthJournalContextService journalContextService) {
        this.revenueShareRepository = revenueShareRepository;
        this.pointRuleRepository = pointRuleRepository;
        this.journalContextService = journalContextService;
    }

    /**
     * 获取当前平台分成比例.
     * <p>从 {@code point_rule_config.UNLOCK_SHARE_RATIO} 读取，缺失或非法时回退 50%。</p>
     */
    private double getPlatformRatio() {
        Optional<PointRule> ruleOpt = pointRuleRepository.findBySourceType(SOURCE_UNLOCK_SHARE_RATIO);
        if (ruleOpt.isPresent() && ruleOpt.get().isEffective()) {
            int ratio = ruleOpt.get().getPointAmount();
            if (ratio >= 0 && ratio <= 100) {
                return ratio / 100.0;
            }
        }
        return DEFAULT_PLATFORM_RATIO;
    }

    /**
     * 获取当前分成比例描述字符串.
     */
    private String getShareRatioDesc(double platformRatio) {
        int platformPct = (int) Math.round(platformRatio * 100);
        int authorPct = 100 - platformPct;
        return platformPct + ":" + authorPct;
    }

    /**
     * 创建待结算分账记录（在解锁成功后调用）.
     *
     * @param orderNo     解锁订单号
     * @param articleId   文章 ID
     * @param authorId    作者用户 ID
     * @param totalPoints 订单总积分（即解锁价格）
     */
    public void createPendingShare(String orderNo, Long articleId, Long authorId, int totalPoints) {
        double platformRatio = getPlatformRatio();
        int platformPoints = (int) (totalPoints * platformRatio);
        int authorPoints = totalPoints - platformPoints;
        String shareRatio = getShareRatioDesc(platformRatio);

        int rows = revenueShareRepository.insertIgnore(
                orderNo, articleId, authorId,
                totalPoints, platformPoints, authorPoints,
                shareRatio);

        if (rows > 0) {
            log.info("[分账] 待结算记录写入成功。orderNo={}, articleId={}, authorId={}, "
                            + "totalPoints={}, platformPoints={}, authorPoints={}, shareRatio={}",
                    orderNo, articleId, authorId, totalPoints, platformPoints, authorPoints, shareRatio);
        } else {
            log.info("[分账] 幂等跳过（分账记录已存在）。orderNo={}", orderNo);
        }
    }

    /**
     * 兼容旧调用名：创建待结算分账记录.
     */
    public void createShareRecord(String orderNo, Long articleId, Long authorId, int totalPoints) {
        createPendingShare(orderNo, articleId, authorId, totalPoints);
    }

    /**
     * 分账流水展示 VO.
     */
    public static class RevenueShareVO {
        private final Long id;
        private final String orderNo;
        private final Long articleId;
        private final Long authorId;
        private final int totalPoints;
        private final int platformPoints;
        private final int authorPoints;
        private final String shareRatio;
        private final String settlementStatus;
        private final String pointJournalBizNo;
        private final String articleTitle;
        private final String articleStatus;
        private final boolean articleAccessible;
        private final LocalDateTime settledAt;
        private final int retryCount;
        private final String lastError;
        private final LocalDateTime createdAt;

        public RevenueShareVO(RevenueShareJournal journal) {
            this(journal, null);
        }

        public RevenueShareVO(RevenueShareJournal journal,
                              GrowthJournalContextService.ArticleContext articleContext) {
            this.id = journal.getId();
            this.orderNo = journal.getOrderNo();
            this.articleId = journal.getArticleId();
            this.authorId = journal.getAuthorId();
            this.totalPoints = journal.getTotalPoints();
            this.platformPoints = journal.getPlatformPoints();
            this.authorPoints = journal.getAuthorPoints();
            this.shareRatio = journal.getShareRatio();
            this.settlementStatus = journal.getSettlementStatus();
            this.pointJournalBizNo = journal.getPointJournalBizNo();
            this.articleTitle = articleContext == null ? null : articleContext.getArticleTitle();
            this.articleStatus = articleContext == null ? null : articleContext.getArticleStatus();
            this.articleAccessible = articleContext != null && articleContext.isArticleAccessible();
            this.settledAt = journal.getSettledAt();
            this.retryCount = journal.getRetryCount();
            this.lastError = journal.getLastError();
            this.createdAt = journal.getCreatedAt();
        }

        public Long getId() { return id; }
        public String getOrderNo() { return orderNo; }
        public Long getArticleId() { return articleId; }
        public Long getAuthorId() { return authorId; }
        public int getTotalPoints() { return totalPoints; }
        public int getPlatformPoints() { return platformPoints; }
        public int getAuthorPoints() { return authorPoints; }
        public String getShareRatio() { return shareRatio; }
        public String getSettlementStatus() { return settlementStatus; }
        public String getPointJournalBizNo() { return pointJournalBizNo; }
        public String getArticleTitle() { return articleTitle; }
        public String getArticleStatus() { return articleStatus; }
        public boolean isArticleAccessible() { return articleAccessible; }
        public LocalDateTime getSettledAt() { return settledAt; }
        public int getRetryCount() { return retryCount; }
        public String getLastError() { return lastError; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    /**
     * 分页查询作者分账流水.
     */
    public List<RevenueShareJournal> getAuthorJournals(Long authorId, int page, int size) {
        return revenueShareRepository.findPageByAuthorId(authorId, page, size);
    }

    /**
     * 统计作者分账流水总数.
     */
    public long countAuthorJournals(Long authorId) {
        return revenueShareRepository.countByAuthorId(authorId);
    }

    /**
     * 分页查询作者分账 VO.
     */
    public PageResult<RevenueShareVO> pageAuthorRevenue(Long authorId, int page, int size) {
        long total = countAuthorJournals(authorId);
        List<RevenueShareJournal> journals = getAuthorJournals(authorId, page, size);
        return new PageResult<>(toVOList(journals), page, size, total);
    }

    /**
     * 管理端分页查询分账流水.
     */
    public PageResult<RevenueShareVO> pageAdminRevenue(Long authorId, String settlementStatus, int page, int size) {
        long total = revenueShareRepository.countAdmin(authorId, settlementStatus);
        List<RevenueShareJournal> journals =
                revenueShareRepository.findAdminPage(authorId, settlementStatus, page, size);
        return new PageResult<>(toVOList(journals), page, size, total);
    }

    private List<RevenueShareVO> toVOList(List<RevenueShareJournal> journals) {
        Set<Long> articleIds = new LinkedHashSet<>();
        for (RevenueShareJournal journal : journals) {
            if (journal.getArticleId() != null) {
                articleIds.add(journal.getArticleId());
            }
        }
        Map<Long, GrowthJournalContextService.ArticleContext> articleContexts =
                journalContextService.resolveArticleContexts(articleIds);
        return journals.stream()
                .map(journal -> new RevenueShareVO(journal, articleContexts.get(journal.getArticleId())))
                .collect(Collectors.toList());
    }
}
