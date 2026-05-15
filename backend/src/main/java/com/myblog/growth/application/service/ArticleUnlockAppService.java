package com.myblog.growth.application.service;

import com.myblog.growth.application.event.RevenueShareCreatedEvent;
import com.myblog.growth.domain.model.aggregate.UnlockOrder;
import com.myblog.growth.domain.repository.UnlockOrderRepository;
import com.myblog.growth.domain.repository.UnlockRelationRepository;
import com.myblog.growth.infrastructure.repository.persistence.mapper.ArticleUnlockInfoMapper;
import com.myblog.growth.infrastructure.repository.persistence.mapper.ArticleUnlockInfoMapper.ArticleUnlockInfoVO;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
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
 * 负责处理用户解锁付费文章的全流程：检查文章锁态 → 幂等校验 → 积分扣减 → 写入凭证 → 待结算分账记录。
 * 整个流程在一个本地事务内完成，任一步骤失败整体回滚。
 * </p>
 *
 * <pre>
 * 核心流程 unlock()：
 *   ① 查 blog_article（need_unlock / unlock_point_price / author_id / status）
 *       └── need_unlock=0 → 返回 400（无需解锁）
 *       └── 文章未发布    → 返回 404
 *   ②  查 article_unlock_relation → 已解锁返回幂等成功
 *   ③  查积分余额是否足够 → 不足返回 400
 *   ④  生成 orderNo = "UNLOCK-{userId}-{articleId}-{timestamp}"
 *   ⑤  INSERT IGNORE article_unlock_order(PENDING)
 *   ⑥  PointAppService.deductPoints(userId, pointsCost, "UNLOCK", orderNo)
 *   ⑦  INSERT IGNORE article_unlock_relation
 *   ⑧  RevenueShareAppService.createPendingShare(orderNo, articleId, authorId, pointsCost)
 *   ⑨  UPDATE article_unlock_order SET status=SUCCESS
 *   ⑩  发布 RevenueShareCreatedEvent，事务提交后异步结算作者分成
 * </pre>
 */
@Service
public class ArticleUnlockAppService {

    private static final Logger log = LoggerFactory.getLogger(ArticleUnlockAppService.class);

    private final ArticleUnlockInfoMapper articleUnlockInfoMapper;
    private final UnlockOrderRepository unlockOrderRepository;
    private final UnlockRelationRepository unlockRelationRepository;
    private final PointAppService pointAppService;
    private final RevenueShareAppService revenueShareAppService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 构造注入.
     *
     * @param articleUnlockInfoMapper  文章解锁信息只读 Mapper
     * @param unlockOrderRepository    解锁订单 Repository
     * @param unlockRelationRepository 解锁关系 Repository
     * @param pointAppService          积分应用服务
     * @param revenueShareAppService   分账应用服务
     * @param eventPublisher           应用事件发布器
     */
    public ArticleUnlockAppService(ArticleUnlockInfoMapper articleUnlockInfoMapper,
                                   UnlockOrderRepository unlockOrderRepository,
                                   UnlockRelationRepository unlockRelationRepository,
                                   PointAppService pointAppService,
                                   RevenueShareAppService revenueShareAppService,
                                   ApplicationEventPublisher eventPublisher) {
        this.articleUnlockInfoMapper = articleUnlockInfoMapper;
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

        public UnlockStatusResult(Long articleId, boolean needUnlock, int unlockPointPrice,
                                  boolean unlocked, int currentBalance) {
            this.articleId = articleId;
            this.needUnlock = needUnlock;
            this.unlockPointPrice = unlockPointPrice;
            this.unlocked = unlocked;
            this.currentBalance = currentBalance;
        }

        public Long getArticleId() { return articleId; }
        public boolean isNeedUnlock() { return needUnlock; }
        public int getUnlockPointPrice() { return unlockPointPrice; }
        public boolean isUnlocked() { return unlocked; }
        public int getCurrentBalance() { return currentBalance; }
    }

    /**
     * 解锁文章.
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @return 解锁结果
     * @throws GrowthBusinessException 文章无需解锁、积分不足等情况时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public UnlockResult unlock(Long userId, Long articleId) {
        // ① 查文章解锁信息
        ArticleUnlockInfoVO articleInfo = articleUnlockInfoMapper.selectUnlockInfoById(articleId);
        if (articleInfo == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "文章不存在");
        }
        if (!"PUBLISHED".equals(articleInfo.getStatus())) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "文章未发布，无法解锁");
        }
        if (!articleInfo.isNeedUnlock()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "该文章无需解锁（免费文章）");
        }

        int pointsCost = articleInfo.getUnlockPointPrice();

        // ② 已解锁则返回幂等成功
        if (unlockRelationRepository.existsByUserIdAndArticleId(userId, articleId)) {
            log.info("[解锁] 幂等跳过，userId={}, articleId={}", userId, articleId);
            return new UnlockResult(articleId, true, pointsCost, 0, null, "已解锁");
        }

        // ③ 检查积分余额
        int currentBalance = pointAppService.getAccount(userId).getBalance();
        if (currentBalance < pointsCost) {
            throw new GrowthBusinessException(GrowthErrorCode.INSUFFICIENT_POINTS,
                    "积分不足，当前余额=" + currentBalance + "，需要=" + pointsCost);
        }

        // ④ 生成订单号
        String orderNo = "UNLOCK-" + userId + "-" + articleId + "-" + System.currentTimeMillis();

        // ⑤ INSERT IGNORE article_unlock_order(PENDING)
        UnlockOrder order = UnlockOrder.create(orderNo, userId, articleId, pointsCost);
        try {
            int rows = unlockOrderRepository.insertIgnore(order);
            if (rows == 0) {
                // 并发场景下订单已存在（uk_user_article 冲突）
                log.info("[解锁] 订单并发幂等跳过，userId={}, articleId={}", userId, articleId);
                return new UnlockResult(articleId, true, pointsCost, currentBalance, orderNo, "已解锁");
            }
        } catch (DuplicateKeyException e) {
            log.info("[解锁] 订单并发幂等跳过（DuplicateKey），userId={}, articleId={}", userId, articleId);
            return new UnlockResult(articleId, true, pointsCost, currentBalance, orderNo, "已解锁");
        }

        // ⑥ 积分扣减
        int balanceAfter;
        try {
            balanceAfter = pointAppService.deductPoints(userId, pointsCost, "UNLOCK", orderNo,
                    "解锁文章[" + articleId + "]", null);
        } catch (Exception e) {
            unlockOrderRepository.markFailed(orderNo, e.getMessage());
            throw e;
        }

        // ⑦ INSERT IGNORE article_unlock_relation
        unlockRelationRepository.insertIgnore(userId, articleId, orderNo);

        // ⑧ 分账记录：只创建待结算流水，作者入账由事务提交后的异步事件处理
        Long authorId = articleInfo.getAuthorId();
        revenueShareAppService.createPendingShare(orderNo, articleId, authorId, pointsCost);

        // ⑨ 订单状态改为 SUCCESS
        unlockOrderRepository.markSuccess(orderNo);

        // ⑩ 发布分账创建事件，监听器在 AFTER_COMMIT 阶段异步结算
        eventPublisher.publishEvent(new RevenueShareCreatedEvent(orderNo));

        log.info("[解锁] 成功。userId={}, articleId={}, pointsCost={}, balanceAfter={}",
                userId, articleId, pointsCost, balanceAfter);

        return new UnlockResult(articleId, true, pointsCost, balanceAfter, orderNo, null);
    }

    /**
     * 查询解锁状态.
     *
     * @param userId    用户 ID
     * @param articleId 文章 ID
     * @return 解锁状态结果
     */
    public UnlockStatusResult getUnlockStatus(Long userId, Long articleId) {
        ArticleUnlockInfoVO articleInfo = articleUnlockInfoMapper.selectUnlockInfoById(articleId);
        if (articleInfo == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "文章不存在");
        }

        boolean unlocked = unlockRelationRepository.existsByUserIdAndArticleId(userId, articleId);
        int currentBalance = pointAppService.getAccount(userId).getBalance();

        return new UnlockStatusResult(
                articleId,
                articleInfo.isNeedUnlock(),
                articleInfo.getUnlockPointPrice(),
                unlocked,
                currentBalance
        );
    }
}

