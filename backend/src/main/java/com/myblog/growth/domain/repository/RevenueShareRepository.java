package com.myblog.growth.domain.repository;

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
}

