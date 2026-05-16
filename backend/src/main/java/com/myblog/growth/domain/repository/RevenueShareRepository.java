package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.valueobject.RevenueShareJournal;

import java.util.List;

/**
 * 分账流水 Repository 接口.
 * <p>
 * 分账流水是每次成功解锁时生成的积分分成记录，对应 {@code revenue_share_journal} 表。
 * </p>
 */
public interface RevenueShareRepository {

    /**
     * 保存分账流水（INSERT IGNORE，uk_order_no 冲突时忽略）.
     *
     * @param orderNo       关联解锁订单号
     * @param articleId     文章 ID
     * @param authorId      作者用户 ID
     * @param totalPoints   订单总积分
     * @param platformPoints 平台分成积分
     * @param authorPoints  作者分成积分
     * @param shareRatio    分成比例（如 "50:50"）
     * @return 插入行数（1=成功，0=已存在）
     */
    int insertIgnore(String orderNo, Long articleId, Long authorId,
                     int totalPoints, int platformPoints, int authorPoints, String shareRatio);

    /**
     * 根据订单号查询并锁定分账流水.
     *
     * @param orderNo 解锁订单号
     * @return 分账流水，不存在时返回 null
     */
    RevenueShareJournal findByOrderNoForUpdate(String orderNo);

    /**
     * 标记分账已结算.
     *
     * @param orderNo           解锁订单号
     * @param pointJournalBizNo 作者积分流水业务单号
     * @return 更新行数
     */
    int markSettled(String orderNo, String pointJournalBizNo);

    /**
     * 标记分账结算失败.
     *
     * @param orderNo   解锁订单号
     * @param lastError 最近一次错误信息
     * @return 更新行数
     */
    int markFailed(String orderNo, String lastError);

    /**
     * 查询待补偿结算的分账流水.
     *
     * @param maxRetry 最大重试次数
     * @param limit    最大返回条数
     * @return 分账流水列表
     */
    List<RevenueShareJournal> findRetryableForSettlement(int maxRetry, int limit);

    /**
     * 管理端分页查询分账流水.
     *
     * @param authorId         作者用户 ID（可选）
     * @param settlementStatus 结算状态（可选）
     * @param page             页码（从 1 开始）
     * @param size             每页条数
     * @return 分账流水列表
     */
    List<RevenueShareJournal> findAdminPage(Long authorId, String settlementStatus, int page, int size);

    /**
     * 管理端统计分账流水总数.
     *
     * @param authorId         作者用户 ID（可选）
     * @param settlementStatus 结算状态（可选）
     * @return 总数
     */
    long countAdmin(Long authorId, String settlementStatus);

    /**
     * 分页查询作者分账流水.
     *
     * @param authorId 作者用户 ID
     * @param page     页码（从 1 开始）
     * @param size     每页条数
     * @return 分账流水列表
     */
    List<RevenueShareJournal> findPageByAuthorId(Long authorId, int page, int size);

    /**
     * 统计作者分账流水总数.
     *
     * @param authorId 作者用户 ID
     * @return 总数
     */
    long countByAuthorId(Long authorId);
}

