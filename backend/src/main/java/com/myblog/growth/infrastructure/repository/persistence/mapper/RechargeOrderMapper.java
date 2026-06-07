package com.myblog.growth.infrastructure.repository.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 充值订单 MyBatis Mapper.
 * <p>对应表 {@code recharge_order}，XML 在 {@code mapper/growth/RechargeOrderMapper.xml}。</p>
 */
@Mapper
public interface RechargeOrderMapper {

    /**
     * 根据支付单号查询充值订单状态.
     *
     * @param payOrderNo 支付单号
     * @return 状态字符串（PENDING/SUCCESS/FAILED），不存在时返回 null
     */
    String selectStatusByPayOrderNo(@Param("payOrderNo") String payOrderNo);

    /**
     * INSERT IGNORE：uk_pay_order_no 冲突时静默忽略.
     *
     * @param userId         用户 ID
     * @param payOrderNo     支付单号
     * @param amountFen      充值金额（分）
     * @param pointsGranted  对应积分量
     * @return 插入行数（1=成功，0=重复）
     */
    int insertIgnore(@Param("userId") Long userId,
                     @Param("payOrderNo") String payOrderNo,
                     @Param("amountFen") long amountFen,
                     @Param("pointsGranted") int pointsGranted);

    /**
     * 将充值订单状态更新为 SUCCESS，并记录回调次数.
     *
     * @param payOrderNo 支付单号
     */
    void markSuccess(@Param("payOrderNo") String payOrderNo);
}

