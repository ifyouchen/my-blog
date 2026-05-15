package com.myblog.growth.application.service;

import com.myblog.growth.domain.repository.RevenueShareRepository;
import com.myblog.growth.infrastructure.repository.persistence.entity.RevenueShareJournalDO;
import com.myblog.growth.infrastructure.repository.persistence.repository.RevenueShareRepositoryImpl;
import com.myblog.shared.result.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分账应用服务.
 *
 * <p>
 * 负责在文章解锁成功后，根据分成比例创建待结算分账流水。
 * </p>
 *
 * <p>默认分成比例：平台 50% : 作者 50%</p>
 *
 * <p>
 * 注意：解锁主事务内只创建 {@code PENDING} 分账流水，作者积分入账由异步结算服务完成。
 * </p>
 */
@Service
public class RevenueShareAppService {

    private static final Logger log = LoggerFactory.getLogger(RevenueShareAppService.class);

    /** 默认平台分成比例（50%）. */
    private static final double PLATFORM_RATIO = 0.5;

    /** 默认分成比例描述. */
    private static final String DEFAULT_SHARE_RATIO = "50:50";

    private final RevenueShareRepository revenueShareRepository;
    private final RevenueShareRepositoryImpl revenueShareRepositoryImpl;

    /**
     * 构造注入.
     *
     * @param revenueShareRepository     分账 Repository 接口
     * @param revenueShareRepositoryImpl 分账 Repository 实现（用于查询方法）
     */
    public RevenueShareAppService(RevenueShareRepository revenueShareRepository,
                                  RevenueShareRepositoryImpl revenueShareRepositoryImpl) {
        this.revenueShareRepository = revenueShareRepository;
        this.revenueShareRepositoryImpl = revenueShareRepositoryImpl;
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
        int platformPoints = (int) (totalPoints * PLATFORM_RATIO);
        int authorPoints = totalPoints - platformPoints;

        int rows = revenueShareRepository.insertIgnore(
                orderNo, articleId, authorId,
                totalPoints, platformPoints, authorPoints,
                DEFAULT_SHARE_RATIO);

        if (rows > 0) {
            log.info("[分账] 待结算记录写入成功。orderNo={}, articleId={}, authorId={}, "
                            + "totalPoints={}, platformPoints={}, authorPoints={}",
                    orderNo, articleId, authorId, totalPoints, platformPoints, authorPoints);
        } else {
            log.info("[分账] 幂等跳过（分账记录已存在）。orderNo={}", orderNo);
        }
    }

    /**
     * 兼容旧调用名：创建待结算分账记录.
     *
     * @param orderNo     解锁订单号
     * @param articleId   文章 ID
     * @param authorId    作者用户 ID
     * @param totalPoints 订单总积分
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
        private final LocalDateTime settledAt;
        private final int retryCount;
        private final String lastError;
        private final LocalDateTime createdAt;

        public RevenueShareVO(RevenueShareJournalDO do_) {
            this.id = do_.getId();
            this.orderNo = do_.getOrderNo();
            this.articleId = do_.getArticleId();
            this.authorId = do_.getAuthorId();
            this.totalPoints = do_.getTotalPoints();
            this.platformPoints = do_.getPlatformPoints();
            this.authorPoints = do_.getAuthorPoints();
            this.shareRatio = do_.getShareRatio();
            this.settlementStatus = do_.getSettlementStatus();
            this.pointJournalBizNo = do_.getPointJournalBizNo();
            this.settledAt = do_.getSettledAt();
            this.retryCount = do_.getRetryCount();
            this.lastError = do_.getLastError();
            this.createdAt = do_.getCreatedAt();
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
        public LocalDateTime getSettledAt() { return settledAt; }
        public int getRetryCount() { return retryCount; }
        public String getLastError() { return lastError; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    /**
     * 分页查询作者分账流水.
     *
     * @param authorId 作者用户 ID
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @return 分账流水 VO 列表
     */
    public List<RevenueShareJournalDO> getAuthorJournals(Long authorId, int page, int size) {
        return revenueShareRepositoryImpl.findPageByAuthorId(authorId, page, size);
    }

    /**
     * 统计作者分账流水总数.
     *
     * @param authorId 作者用户 ID
     * @return 总数
     */
    public long countAuthorJournals(Long authorId) {
        return revenueShareRepositoryImpl.countByAuthorId(authorId);
    }

    /**
     * 分页查询作者分账 VO.
     *
     * @param authorId 作者用户 ID
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @return 分账流水分页结果
     */
    public PageResult<RevenueShareVO> pageAuthorRevenue(Long authorId, int page, int size) {
        long total = countAuthorJournals(authorId);
        List<RevenueShareJournalDO> journals = getAuthorJournals(authorId, page, size);
        return new PageResult<>(toVOList(journals), page, size, total);
    }

    /**
     * 管理端分页查询分账流水.
     *
     * @param authorId         作者用户 ID（可选）
     * @param settlementStatus 结算状态（可选）
     * @param page             页码（从 1 开始）
     * @param size             每页条数
     * @return 分账流水分页结果
     */
    public PageResult<RevenueShareVO> pageAdminRevenue(Long authorId, String settlementStatus, int page, int size) {
        long total = revenueShareRepositoryImpl.countAdmin(authorId, settlementStatus);
        List<RevenueShareJournalDO> journals =
                revenueShareRepositoryImpl.findAdminPage(authorId, settlementStatus, page, size);
        return new PageResult<>(toVOList(journals), page, size, total);
    }

    private List<RevenueShareVO> toVOList(List<RevenueShareJournalDO> journals) {
        return journals.stream()
                .map(RevenueShareVO::new)
                .collect(Collectors.toList());
    }
}

