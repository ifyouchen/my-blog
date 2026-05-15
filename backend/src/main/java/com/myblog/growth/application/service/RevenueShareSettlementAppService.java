package com.myblog.growth.application.service;

import com.myblog.growth.infrastructure.repository.persistence.entity.RevenueShareJournalDO;
import com.myblog.growth.infrastructure.repository.persistence.repository.RevenueShareRepositoryImpl;
import com.myblog.growth.shared.enums.PointEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 分账异步结算应用服务.
 * <p>
 * 分账流水创建后先处于 {@code PENDING}，本服务负责把作者分成积分异步入账，并将流水标记为
 * {@code SETTLED}。失败时记录 {@code FAILED} 与错误信息，后续由补偿任务继续重试。
 * </p>
 */
@Service
public class RevenueShareSettlementAppService {

    private static final Logger log = LoggerFactory.getLogger(RevenueShareSettlementAppService.class);

    /** 待结算. */
    public static final String STATUS_PENDING = "PENDING";

    /** 已结算入账. */
    public static final String STATUS_SETTLED = "SETTLED";

    /** 结算失败，可由补偿任务或管理员重试. */
    public static final String STATUS_FAILED = "FAILED";

    /** 默认最大自动重试次数. */
    public static final int DEFAULT_MAX_RETRY = 5;

    /** 默认单批补偿扫描数量. */
    public static final int DEFAULT_SCAN_LIMIT = 50;

    private static final int MAX_ERROR_LENGTH = 500;

    private final RevenueShareRepositoryImpl revenueShareRepository;
    private final PointAppService pointAppService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 构造注入.
     *
     * @param revenueShareRepository 分账 Repository
     * @param pointAppService        积分应用服务
     * @param transactionTemplate    编程式事务模板
     */
    public RevenueShareSettlementAppService(RevenueShareRepositoryImpl revenueShareRepository,
                                            PointAppService pointAppService,
                                            TransactionTemplate transactionTemplate) {
        this.revenueShareRepository = revenueShareRepository;
        this.pointAppService = pointAppService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 按订单号结算作者分账.
     *
     * @param orderNo 解锁订单号
     * @return 结算结果
     */
    public SettlementResult settle(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return SettlementResult.failed(orderNo, "orderNo 不能为空");
        }
        try {
            SettlementResult result = transactionTemplate.execute(status -> settleInTransaction(orderNo.trim()));
            return result == null ? SettlementResult.skipped(orderNo, "未执行结算") : result;
        } catch (Exception e) {
            log.warn("[分账结算] 结算失败，准备记录失败状态。orderNo={}", orderNo, e);
            SettlementResult result = transactionTemplate.execute(status -> markFailedInTransaction(orderNo.trim(), e));
            return result == null ? SettlementResult.failed(orderNo, errorMessage(e)) : result;
        }
    }

    /**
     * 扫描并结算待补偿的分账流水.
     *
     * @param maxRetry 最大自动重试次数
     * @param limit    单批数量
     * @return 本批扫描到的记录数
     */
    public int settleRetryable(int maxRetry, int limit) {
        int safeMaxRetry = Math.max(1, maxRetry);
        int safeLimit = Math.min(Math.max(1, limit), DEFAULT_SCAN_LIMIT);
        List<RevenueShareJournalDO> journals =
                revenueShareRepository.findRetryableForSettlement(safeMaxRetry, safeLimit);
        for (RevenueShareJournalDO journal : journals) {
            settle(journal.getOrderNo());
        }
        return journals.size();
    }

    private SettlementResult settleInTransaction(String orderNo) {
        RevenueShareJournalDO share = revenueShareRepository.findByOrderNoForUpdate(orderNo);
        if (share == null) {
            return SettlementResult.failed(orderNo, "分账记录不存在");
        }

        String status = normalizeStatus(share.getSettlementStatus());
        if (STATUS_SETTLED.equals(status)) {
            return SettlementResult.settled(orderNo, share.getPointJournalBizNo(), "分账已结算");
        }
        if (!STATUS_PENDING.equals(status) && !STATUS_FAILED.equals(status)) {
            return SettlementResult.skipped(orderNo, "当前状态无需结算：" + status);
        }

        if (share.getAuthorPoints() <= 0) {
            revenueShareRepository.markSettled(orderNo, null);
            return SettlementResult.settled(orderNo, null, "作者分成为 0，已直接标记结算");
        }

        String pointJournalBizNo = buildPointJournalBizNo(orderNo);
        pointAppService.addPoints(
                share.getAuthorId(),
                share.getAuthorPoints(),
                PointEventType.REVENUE_SHARE.name(),
                pointJournalBizNo,
                "文章解锁分成[" + share.getArticleId() + "]",
                "system"
        );
        revenueShareRepository.markSettled(orderNo, pointJournalBizNo);
        log.info("[分账结算] 作者分成入账成功。orderNo={}, authorId={}, authorPoints={}, pointJournalBizNo={}",
                orderNo, share.getAuthorId(), share.getAuthorPoints(), pointJournalBizNo);
        return SettlementResult.settled(orderNo, pointJournalBizNo, "结算成功");
    }

    private SettlementResult markFailedInTransaction(String orderNo, Exception cause) {
        RevenueShareJournalDO share = revenueShareRepository.findByOrderNoForUpdate(orderNo);
        if (share == null) {
            return SettlementResult.failed(orderNo, "分账记录不存在：" + errorMessage(cause));
        }
        if (STATUS_SETTLED.equals(normalizeStatus(share.getSettlementStatus()))) {
            return SettlementResult.settled(orderNo, share.getPointJournalBizNo(), "分账已结算");
        }
        String error = truncate(errorMessage(cause));
        revenueShareRepository.markFailed(orderNo, error);
        return SettlementResult.failed(orderNo, error);
    }

    private String buildPointJournalBizNo(String orderNo) {
        return PointEventType.REVENUE_SHARE.name() + ":" + orderNo;
    }

    private String normalizeStatus(String status) {
        return status == null || status.trim().isEmpty() ? STATUS_PENDING : status.trim();
    }

    private String errorMessage(Throwable cause) {
        if (cause == null) {
            return "未知错误";
        }
        String message = cause.getMessage();
        return message == null || message.trim().isEmpty() ? cause.getClass().getSimpleName() : message;
    }

    private String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * 分账结算结果.
     */
    public static class SettlementResult {
        private final String orderNo;
        private final String status;
        private final String pointJournalBizNo;
        private final String message;

        private SettlementResult(String orderNo, String status, String pointJournalBizNo, String message) {
            this.orderNo = orderNo;
            this.status = status;
            this.pointJournalBizNo = pointJournalBizNo;
            this.message = message;
        }

        public static SettlementResult settled(String orderNo, String pointJournalBizNo, String message) {
            return new SettlementResult(orderNo, STATUS_SETTLED, pointJournalBizNo, message);
        }

        public static SettlementResult failed(String orderNo, String message) {
            return new SettlementResult(orderNo, STATUS_FAILED, null, message);
        }

        public static SettlementResult skipped(String orderNo, String message) {
            return new SettlementResult(orderNo, "SKIPPED", null, message);
        }

        public String getOrderNo() { return orderNo; }
        public String getStatus() { return status; }
        public String getPointJournalBizNo() { return pointJournalBizNo; }
        public String getMessage() { return message; }
    }
}
