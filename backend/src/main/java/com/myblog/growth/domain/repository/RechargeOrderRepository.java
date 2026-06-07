package com.myblog.growth.domain.repository;

/**
 * 充值订单 Repository 接口.
 */
public interface RechargeOrderRepository {

    /**
     * 根据支付订单号查询充值订单状态.
     * <p>用于幂等检查：同一 payOrderNo 只入账一次。</p>
     *
     * @param payOrderNo 支付订单号（第三方支付平台流水号）
     * @return 充值订单状态字符串（如 PENDING / SUCCESS / FAILED），不存在时返回 null
     */
    String findStatusByPayOrderNo(String payOrderNo);

    /**
     * 插入充值订单（INSERT IGNORE，payOrderNo 唯一键冲突时静默忽略）.
     *
     * @param userId      用户 ID
     * @param payOrderNo  支付订单号
     * @param amount      支付金额（分）
     * @param pointAmount 对应积分量
     * @param channel     支付渠道（如 WECHAT / ALIPAY）
     * @return 插入行数（1=成功，0=已存在）
     */
    int insertIgnore(Long userId, String payOrderNo, long amount, int pointAmount, String channel);

    /**
     * 将充值订单标记为已到账（SUCCESS）.
     *
     * @param payOrderNo 支付订单号
     */
    void markSuccess(String payOrderNo);
}

