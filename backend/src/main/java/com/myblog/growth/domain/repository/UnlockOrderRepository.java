package com.myblog.growth.domain.repository;

import com.myblog.growth.domain.model.aggregate.UnlockOrder;

import java.util.Optional;

/**
 * 解锁订单 Repository 接口.
 */
public interface UnlockOrderRepository {

    /**
     * 保存解锁订单（INSERT IGNORE，uk_user_article 或 uk_order_no 冲突时忽略）.
     *
     * @param order 解锁订单
     * @return 插入行数（1=成功，0=已存在）
     */
    int insertIgnore(UnlockOrder order);

    /**
     * 根据订单号查询订单.
     *
     * @param orderNo 订单号
     * @return 订单（Optional 包装）
     */
    Optional<UnlockOrder> findByOrderNo(String orderNo);

    /**
     * 根据用户 ID + 文章 ID 查询订单.
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @return 订单（Optional 包装）
     */
    Optional<UnlockOrder> findByUserIdAndArticleId(Long userId, Long articleId);

    /**
     * 将订单状态更新为 SUCCESS.
     *
     * @param orderNo 订单号
     */
    void markSuccess(String orderNo);

    /**
     * 将订单状态更新为 FAILED.
     *
     * @param orderNo   订单号
     * @param failReason 失败原因
     */
    void markFailed(String orderNo, String failReason);
}

