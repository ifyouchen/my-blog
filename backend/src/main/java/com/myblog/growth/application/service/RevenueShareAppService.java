package com.myblog.growth.application.service;

import com.myblog.growth.domain.repository.RevenueShareRepository;
import com.myblog.growth.infrastructure.repository.persistence.entity.RevenueShareJournalDO;
import com.myblog.growth.infrastructure.repository.persistence.repository.RevenueShareRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分账应用服务.
 *
 * <p>
 * 负责在文章解锁成功后，根据分成比例将积分分配给平台和作者，
 * 并记录分账流水到 {@code revenue_share_journal} 表。
 * </p>
 *
 * <p>默认分成比例：平台 50% : 作者 50%</p>
 *
 * <p>
 * 注意：分账仅记录流水，不实际向作者账户入积分（积分账户由 PointAppService 管理）。
 * 作者积分入账需另行实现。当前版本仅记录分账数据，可按需扩展。
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
     * 创建分账记录（在解锁成功后调用）.
     *
     * @param orderNo   解锁订单号
     * @param articleId 文章 ID
     * @param authorId  作者用户 ID
     * @param totalPoints 订单总积分（即解锁价格）
     */
    public void createShareRecord(String orderNo, Long articleId, Long authorId, int totalPoints) {
        int platformPoints = (int) (totalPoints * PLATFORM_RATIO);
        int authorPoints = totalPoints - platformPoints;

        int rows = revenueShareRepository.insertIgnore(
                orderNo, articleId, authorId,
                totalPoints, platformPoints, authorPoints,
                DEFAULT_SHARE_RATIO);

        if (rows > 0) {
            log.info("[分账] 记录写入成功。orderNo={}, articleId={}, authorId={}, "
                            + "totalPoints={}, platformPoints={}, authorPoints={}",
                    orderNo, articleId, authorId, totalPoints, platformPoints, authorPoints);
        } else {
            log.info("[分账] 幂等跳过（分账记录已存在）。orderNo={}", orderNo);
        }
    }

    /**
     * 分账流水展示 VO.
     */
    public static class RevenueShareVO {
        private final String orderNo;
        private final Long articleId;
        private final int totalPoints;
        private final int platformPoints;
        private final int authorPoints;
        private final String shareRatio;
        private final java.time.LocalDateTime createdAt;

        public RevenueShareVO(RevenueShareJournalDO do_) {
            this.orderNo = do_.getOrderNo();
            this.articleId = do_.getArticleId();
            this.totalPoints = do_.getTotalPoints();
            this.platformPoints = do_.getPlatformPoints();
            this.authorPoints = do_.getAuthorPoints();
            this.shareRatio = do_.getShareRatio();
            this.createdAt = do_.getCreatedAt();
        }

        public String getOrderNo() { return orderNo; }
        public Long getArticleId() { return articleId; }
        public int getTotalPoints() { return totalPoints; }
        public int getPlatformPoints() { return platformPoints; }
        public int getAuthorPoints() { return authorPoints; }
        public String getShareRatio() { return shareRatio; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
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
}

