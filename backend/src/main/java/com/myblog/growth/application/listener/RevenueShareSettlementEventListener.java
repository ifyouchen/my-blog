package com.myblog.growth.application.listener;

import com.myblog.growth.application.event.RevenueShareCreatedEvent;
import com.myblog.growth.application.service.RevenueShareSettlementAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 分账结算事件监听器.
 * <p>
 * 解锁事务提交后异步触发作者分成结算，避免作者入账失败影响买家解锁主流程。
 * </p>
 */
@Component
public class RevenueShareSettlementEventListener {

    private static final Logger log = LoggerFactory.getLogger(RevenueShareSettlementEventListener.class);

    private final RevenueShareSettlementAppService settlementAppService;

    /**
     * 构造注入.
     *
     * @param settlementAppService 分账结算服务
     */
    public RevenueShareSettlementEventListener(RevenueShareSettlementAppService settlementAppService) {
        this.settlementAppService = settlementAppService;
    }

    /**
     * 解锁事务提交后异步结算分账.
     *
     * @param event 分账流水创建事件
     */
    @Async("growthAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRevenueShareCreated(RevenueShareCreatedEvent event) {
        try {
            settlementAppService.settle(event.getOrderNo());
        } catch (Exception e) {
            log.warn("[分账结算] 事件处理异常，等待补偿任务重试。orderNo={}", event.getOrderNo(), e);
        }
    }
}
