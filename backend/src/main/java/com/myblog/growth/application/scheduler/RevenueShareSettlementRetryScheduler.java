package com.myblog.growth.application.scheduler;

import com.myblog.growth.application.service.RevenueShareSettlementAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 分账结算补偿任务.
 * <p>
 * 定期扫描待结算或失败可重试的分账流水，依靠积分流水业务单号保证作者不会重复入账。
 * </p>
 */
@Component
public class RevenueShareSettlementRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RevenueShareSettlementRetryScheduler.class);

    private final RevenueShareSettlementAppService settlementAppService;

    /**
     * 构造注入.
     *
     * @param settlementAppService 分账结算服务
     */
    public RevenueShareSettlementRetryScheduler(RevenueShareSettlementAppService settlementAppService) {
        this.settlementAppService = settlementAppService;
    }

    /**
     * 扫描补偿待结算分账流水.
     */
    @Scheduled(fixedDelayString = "${my-blog.growth.revenue-settlement-retry-delay-ms:60000}")
    public void retrySettlement() {
        try {
            int scanned = settlementAppService.settleRetryable(
                    RevenueShareSettlementAppService.DEFAULT_MAX_RETRY,
                    RevenueShareSettlementAppService.DEFAULT_SCAN_LIMIT
            );
            if (scanned > 0) {
                log.info("[分账结算] 补偿扫描完成，处理记录数={}", scanned);
            }
        } catch (Exception e) {
            log.warn("[分账结算] 补偿扫描异常", e);
        }
    }
}
