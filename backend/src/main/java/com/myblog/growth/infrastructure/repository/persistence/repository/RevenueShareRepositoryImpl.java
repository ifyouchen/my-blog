package com.myblog.growth.infrastructure.repository.persistence.repository;

import com.myblog.growth.domain.model.valueobject.RevenueShareJournal;
import com.myblog.growth.domain.repository.RevenueShareRepository;
import com.myblog.growth.infrastructure.repository.persistence.entity.RevenueShareJournalDO;
import com.myblog.growth.infrastructure.repository.persistence.mapper.RevenueShareJournalMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分账流水 Repository 实现.
 */
@Repository
public class RevenueShareRepositoryImpl implements RevenueShareRepository {

    private final RevenueShareJournalMapper mapper;

    /**
     * 构造注入 Mapper.
     *
     * @param mapper 分账流水 Mapper
     */
    public RevenueShareRepositoryImpl(RevenueShareJournalMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int insertIgnore(String orderNo, Long articleId, Long authorId,
                            int totalPoints, int platformPoints, int authorPoints, String shareRatio) {
        RevenueShareJournalDO do_ = new RevenueShareJournalDO();
        do_.setOrderNo(orderNo);
        do_.setArticleId(articleId);
        do_.setAuthorId(authorId);
        do_.setTotalPoints(totalPoints);
        do_.setPlatformPoints(platformPoints);
        do_.setAuthorPoints(authorPoints);
        do_.setShareRatio(shareRatio);
        return mapper.insertIgnore(do_);
    }

    /**
     * 根据订单号查询并锁定分账流水.
     *
     * @param orderNo 解锁订单号
     * @return 分账流水，不存在时返回 null
     */
    @Override
    public RevenueShareJournal findByOrderNoForUpdate(String orderNo) {
        return toDomain(mapper.selectByOrderNoForUpdate(orderNo));
    }

    /**
     * 标记分账已结算.
     *
     * @param orderNo           解锁订单号
     * @param pointJournalBizNo 作者积分流水业务单号
     * @return 更新行数
     */
    @Override
    public int markSettled(String orderNo, String pointJournalBizNo) {
        return mapper.markSettled(orderNo, pointJournalBizNo);
    }

    /**
     * 标记分账结算失败.
     *
     * @param orderNo   解锁订单号
     * @param lastError 最近一次错误信息
     * @return 更新行数
     */
    @Override
    public int markFailed(String orderNo, String lastError) {
        return mapper.markFailed(orderNo, lastError);
    }

    /**
     * 查询待补偿结算的分账流水.
     *
     * @param maxRetry 最大重试次数
     * @param limit    最大返回条数
     * @return 分账流水 DO 列表
     */
    @Override
    public List<RevenueShareJournal> findRetryableForSettlement(int maxRetry, int limit) {
        return toDomainList(mapper.selectRetryableForSettlement(maxRetry, limit));
    }

    /**
     * 管理端分页查询分账流水.
     *
     * @param authorId         作者用户 ID（可选）
     * @param authorKeyword    作者用户名或邮箱（可选）
     * @param settlementStatus 结算状态（可选）
     * @param page             页码（从 1 开始）
     * @param size             每页条数
     * @return 分账流水 DO 列表
     */
    @Override
    public List<RevenueShareJournal> findAdminPage(Long authorId,
                                                   String authorKeyword,
                                                   String settlementStatus,
                                                   int page,
                                                   int size) {
        int offset = (page - 1) * size;
        return toDomainList(mapper.selectAdminPage(authorId, authorKeyword, settlementStatus, size, offset));
    }

    /**
     * 管理端统计分账流水总数.
     *
     * @param authorId         作者用户 ID（可选）
     * @param authorKeyword    作者用户名或邮箱（可选）
     * @param settlementStatus 结算状态（可选）
     * @return 总数
     */
    @Override
    public long countAdmin(Long authorId, String authorKeyword, String settlementStatus) {
        return mapper.countAdmin(authorId, authorKeyword, settlementStatus);
    }

    /**
     * 分页查询作者分账流水.
     *
     * @param authorId 作者用户 ID
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @return 分账流水 DO 列表
     */
    @Override
    public List<RevenueShareJournal> findPageByAuthorId(Long authorId, int page, int size) {
        int offset = (page - 1) * size;
        return toDomainList(mapper.selectPageByAuthorId(authorId, size, offset));
    }

    /**
     * 统计作者分账流水总数.
     *
     * @param authorId 作者用户 ID
     * @return 总数
     */
    @Override
    public long countByAuthorId(Long authorId) {
        return mapper.countByAuthorId(authorId);
    }

    private List<RevenueShareJournal> toDomainList(List<RevenueShareJournalDO> journals) {
        return journals.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private RevenueShareJournal toDomain(RevenueShareJournalDO do_) {
        if (do_ == null) {
            return null;
        }
        return RevenueShareJournal.builder()
                .id(do_.getId())
                .orderNo(do_.getOrderNo())
                .articleId(do_.getArticleId())
                .authorId(do_.getAuthorId())
                .totalPoints(do_.getTotalPoints())
                .platformPoints(do_.getPlatformPoints())
                .authorPoints(do_.getAuthorPoints())
                .shareRatio(do_.getShareRatio())
                .settlementStatus(do_.getSettlementStatus())
                .pointJournalBizNo(do_.getPointJournalBizNo())
                .settledAt(do_.getSettledAt())
                .retryCount(do_.getRetryCount())
                .lastError(do_.getLastError())
                .createdAt(do_.getCreatedAt())
                .build();
    }
}

