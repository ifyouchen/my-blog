package com.myblog.growth.application.event;

import java.time.LocalDateTime;

/**
 * 分账流水创建事件.
 * <p>
 * 解锁主事务提交后，异步监听器根据该事件触发作者分成积分结算。
 * </p>
 */
public class RevenueShareCreatedEvent {

    /** 解锁订单号，也是分账流水幂等键. */
    private final String orderNo;

    /** 事件产生时间. */
    private final LocalDateTime occurredAt;

    /**
     * 创建事件.
     *
     * @param orderNo 解锁订单号
     */
    public RevenueShareCreatedEvent(String orderNo) {
        this.orderNo = orderNo;
        this.occurredAt = LocalDateTime.now();
    }

    /** 获取解锁订单号. */
    public String getOrderNo() { return orderNo; }

    /** 获取事件产生时间. */
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
