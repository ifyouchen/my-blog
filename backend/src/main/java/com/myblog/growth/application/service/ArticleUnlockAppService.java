package com.myblog.growth.application.service;

import com.myblog.growth.application.event.RevenueShareCreatedEvent;
import com.myblog.growth.domain.model.aggregate.UnlockOrder;
import com.myblog.growth.domain.model.valueobject.ArticleUnlockInfo;
import com.myblog.growth.domain.repository.ArticleUnlockInfoRepository;
import com.myblog.growth.domain.repository.UnlockOrderRepository;
import com.myblog.growth.domain.repository.UnlockRelationRepository;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import com.myblog.shared.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文章积分解锁应用服务.
 *
 * <p>
 * 负责处理用户解锁付费文章的全流程。
 * <ul>
 *   <li>作者本人查看自己的付费文章不扣积分、不生成订单、不生成分账</li>
 *   <li>普通用户解锁：检查文章锁态 → 幂等校验 → 积分扣减 → 写入凭证 → 待结算分账记录</li>
 * </ul>
 * </p>
 */
@Service
public class ArticleUnlockAppService {

    private static final Logger log = LoggerFactory.getLogger(ArticleUnlockAppService.class);

    /** 作者自解锁原因标识. */
    private static final String REASON_AUTHOR_SELF = "AUTHOR_SELF";

    /** 管理员免积分查看原因标识. */
    private static final String REASON_ADMIN_BYPASS = "ADMIN_BYPASS";

    /** 正常已解锁标识. */
    private static final String REASON_UNLOCKED = "UNLOCKED";

    private final ArticleUnlockInfoRepository articleUnlockInfoRepository;
    private final UnlockOrderRepository unlockOrderRepository;
    private final UnlockRelationRepository unlockRelationRepository;
    private final PointAppService pointAppService;
    private final RevenueShareAppService revenueShareAppService;
    private final ApplicationEventPublisher eventPublisher;

    public ArticleUnlockAppService(ArticleUnlockInfoRepository articleUnlockInfoRepository,
                                   UnlockOrderRepository unlockOrderRepository,
                                   UnlockRelationRepository unlockRelationRepository,
                                   PointAppService pointAppService,
                                   RevenueShareAppService revenueShareAppService,
                                   ApplicationEventPublisher eventPublisher) {
        this.articleUnlockInfoRepository = articleUnlockInfoRepository;
        this.unlockOrderRepository = unlockOrderRepository;
        this.unlockRelationRepository = unlockRelationRepository;
        this.pointAppService = pointAppService;
        this.revenueShareAppService = revenueShareAppService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 解锁结果值对象.
     */
    public static class UnlockResult {
        private final Long articleId;
        private final boolean unlocked;
        private final int pointsCost;
        private final int balanceAfter;
        private final String orderNo;
        private final String message;

        public UnlockResult(Long articleId, boolean unlocked, int pointsCost,
                            int balanceAfter, String orderNo, String message) {
            this.articleId = articleId;
            this.unlocked = unlocked;
            this.pointsCost = pointsCost;
            this.balanceAfter = balanceAfter;
            this.orderNo = orderNo;
            this.message = message;
        }

        public Long getArticleId() { return articleId; }
        public boolean isUnlocked() { return unlocked; }
        public int getPointsCost() { return pointsCost; }
        public int getBalanceAfter() { return balanceAfter; }
        public String getOrderNo() { return orderNo; }
        public String getMessage() { return message; }
    }

    /**
     * 解锁状态查询结果值对象.
     */
    public static class UnlockStatusResult {
        private final Long articleId;
        private final boolean needUnlock;
        private final int unlockPointPrice;
        private final boolean unlocked;
        private final int currentBalance;
        private final String reason;

        public UnlockStatusResult(Long articleId, boolean needUnlock, int unlockPointPrice,
                                  boolean unlocked, int currentBalance, String reason) {
            this.articleId = articleId;
            this.needUnlock = needUnlock;
            this.unlockPointPrice = unlockPointPrice;
            this.unlocked = unlocked;
            this.currentBalance = currentBalance;
            this.reason = reason;
        }

        public Long getArticleId() { return articleId; }
        public boolean isNeedUnlock() { return needUnlock; }
        public int getUnlockPointPrice() { return unlockPointPrice; }
        public boolean isUnlocked() { return unlocked; }
        public int getCurrentBalance() { return currentBalance; }
        public String getReason() { return reason; }

        /** 前端判断是否需要展示解锁按钮. */
        public boolean isUnlockRequired() {
            return needUnlock && !unlocked;
        }
    }

    /**
     * 解锁文章.
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @param role      当前用户角色（可为 null）
     * @return 解锁结果
     */
    @Transactional(rollbackFor = Exception.class)
    public UnlockResult unlock(Long userId, Long articleId, String role) {
        // ① 查文章解锁信息
        ArticleUnlockInfo articleInfo = articleUnlockInfoRepository.findByArticleId(articleId)
                .orElseThrow(() -> new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "文章不存在"));
        if (!"PUBLISHED".equals(articleInfo.getStatus())) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "文章未发布，无法解锁");
        }
        if (!articleInfo.isNeedUnlock()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "该文章无需解锁（免费文章）");
        }

        // ② 作者本人无需解锁
        if (userId.equals(articleInfo.getAuthorId())) {
            log.info("[解锁] 作者本人无需解锁，userId={}, articleId={}", userId, articleId);
            return new UnlockResult(articleId, true, 0, 0, null, "作者本人无需解锁");
        }

        // ③ 管理员免积分查看
        if (UserRole.ADMIN.name().equals(role)) {
            log.info("[解锁] 管理员免积分查看，userId={}, articleId={}", userId, articleId);
            return new UnlockResult(articleId, true, 0, 0, null, "管理员可直接查看");
        }

        int pointsCost = articleInfo.getUnlockPointPrice();

        // ③ 已解锁则返回幂等成功
        if (unlockRelationRepository.existsByUserIdAndArticleId(userId, articleId)) {
            log.info("[解锁] 幂等跳过，userId={}, articleId={}", userId, articleId);
            return new UnlockResult(articleId, true, pointsCost, 0, null, "已解锁");
        }

        // ④ 检查积分余额
        int currentBalance = pointAppService.getAccount(userId).getBalance();
        if (currentBalance < pointsCost) {
            throw new GrowthBusinessException(GrowthErrorCode.INSUFFICIENT_POINTS,
                    "积分不足，当前余额=" + currentBalance + "，需要=" + pointsCost);
        }

        // ⑤ 生成订单号
        String orderNo = "UNLOCK-" + userId + "-" + articleId + "-" + System.currentTimeMillis();

        // ⑥ INSERT IGNORE article_unlock_order(PENDING)
        UnlockOrder order = UnlockOrder.create(orderNo, userId, articleId, pointsCost);
        try {
            int rows = unlockOrderRepository.insertIgnore(order);
            if (rows == 0) {
                log.info("[解锁] 订单并发幂等跳过，userId={}, articleId={}", userId, articleId);
                return new UnlockResult(articleId, true, pointsCost, currentBalance, orderNo, "已解锁");
            }
        } catch (DuplicateKeyException e) {
            log.info("[解锁] 订单并发幂等跳过（DuplicateKey），userId={}, articleId={}", userId, articleId);
            return new UnlockResult(articleId, true, pointsCost, currentBalance, orderNo, "已解锁");
        }

        // ⑦ 积分扣减
        int balanceAfter;
        try {
            balanceAfter = pointAppService.deductPoints(userId, pointsCost, "UNLOCK", orderNo,
                    "解锁文章[" + articleId + "]", null);
        } catch (Exception e) {
            unlockOrderRepository.markFailed(orderNo, e.getMessage());
            throw e;
        }

        // ⑧ INSERT IGNORE article_unlock_relation
        unlockRelationRepository.insertIgnore(userId, articleId, orderNo);

        // ⑨ 分账记录：只创建待结算流水
        Long authorId = articleInfo.getAuthorId();
        revenueShareAppService.createPendingShare(orderNo, articleId, authorId, pointsCost);

        // ⑩ 订单状态改为 SUCCESS
        unlockOrderRepository.markSuccess(orderNo);

        // ⑪ 发布分账创建事件
        eventPublisher.publishEvent(new RevenueShareCreatedEvent(orderNo));

        log.info("[解锁] 成功。userId={}, articleId={}, pointsCost={}, balanceAfter={}",
                userId, articleId, pointsCost, balanceAfter);

        return new UnlockResult(articleId, true, pointsCost, balanceAfter, orderNo, null);
    }

    /**
     * 查询解锁状态.
     * <p>作者本人查看自己的文章时，直接返回已解锁状态；管理员无需解锁即可查看。</p>
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @param role      当前用户角色（可为 null）
     * @return 解锁状态结果
     */
    public UnlockStatusResult getUnlockStatus(Long userId, Long articleId, String role) {
        ArticleUnlockInfo articleInfo = articleUnlockInfoRepository.findByArticleId(articleId)
                .orElseThrow(() -> new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "文章不存在"));

        // 作者本人 → 直接已解锁
        if (userId.equals(articleInfo.getAuthorId())) {
            return new UnlockStatusResult(
                    articleId, articleInfo.isNeedUnlock(), articleInfo.getUnlockPointPrice(),
                    true, 0, REASON_AUTHOR_SELF
            );
        }

        // 管理员 → 免积分查看
        if (UserRole.ADMIN.name().equals(role)) {
            return new UnlockStatusResult(
                    articleId, articleInfo.isNeedUnlock(), articleInfo.getUnlockPointPrice(),
                    true, 0, REASON_ADMIN_BYPASS
            );
        }

        boolean unlocked = unlockRelationRepository.existsByUserIdAndArticleId(userId, articleId);
        int currentBalance = pointAppService.getAccount(userId).getBalance();

        return new UnlockStatusResult(
                articleId,
                articleInfo.isNeedUnlock(),
                articleInfo.getUnlockPointPrice(),
                unlocked,
                currentBalance,
                unlocked ? REASON_UNLOCKED : null
        );
    }
}
