package com.myblog.growth.application.service;

import com.myblog.growth.domain.model.valueobject.PointRule;
import com.myblog.growth.domain.repository.PointRuleRepository;
import com.myblog.growth.domain.repository.RechargeOrderRepository;
import com.myblog.growth.domain.service.RechargeSignVerifyService;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 充值回调应用服务.
 *
 * <p>
 * 接收支付平台的充值成功回调，计算积分并入账，保证幂等性。
 * 积分兑换比例：1元（100分）= 1积分，可通过 point_rule_config.source_type=RECHARGE 配置。
 * </p>
 *
 * <pre>
 * 核心流程 handleCallback()：
 *   ① RechargeSignVerifyService.verify(request) → 验签失败则抛异常
 *   ② 查 recharge_order → 已 SUCCESS 直接返回（幂等）
 *   ③ INSERT IGNORE recharge_order(PENDING) — 唯一索引兜底
 *   ④ 计算 pointsGranted = amountFen / 100 × ratio
 *   ⑤ PointAppService.addPoints(userId, pointsGranted, "RECHARGE", payOrderNo)
 *   ⑥ UPDATE recharge_order SET status=SUCCESS
 * </pre>
 */
@Service
public class RechargeCallbackAppService {

    private static final Logger log = LoggerFactory.getLogger(RechargeCallbackAppService.class);

    /** 积分规则来源类型：充值. */
    private static final String SOURCE_RECHARGE = "RECHARGE";

    /** 默认兑换比例：100分=1积分（若无规则配置）. */
    private static final int DEFAULT_FEN_PER_POINT = 100;

    private final RechargeOrderRepository rechargeOrderRepository;
    private final PointRuleRepository pointRuleRepository;
    private final RechargeSignVerifyService rechargeSignVerifyService;
    private final PointAppService pointAppService;

    /**
     * 构造注入.
     *
     * @param rechargeOrderRepository   充值订单 Repository
     * @param pointRuleRepository       积分规则 Repository
     * @param rechargeSignVerifyService 验签服务
     * @param pointAppService           积分应用服务
     */
    public RechargeCallbackAppService(RechargeOrderRepository rechargeOrderRepository,
                                      PointRuleRepository pointRuleRepository,
                                      RechargeSignVerifyService rechargeSignVerifyService,
                                      PointAppService pointAppService) {
        this.rechargeOrderRepository = rechargeOrderRepository;
        this.pointRuleRepository = pointRuleRepository;
        this.rechargeSignVerifyService = rechargeSignVerifyService;
        this.pointAppService = pointAppService;
    }

    /**
     * 充值回调结果值对象.
     */
    public static class CallbackResult {
        private final String payOrderNo;
        private final int pointsGranted;
        private final String status;
        private final String message;

        public CallbackResult(String payOrderNo, int pointsGranted, String status, String message) {
            this.payOrderNo = payOrderNo;
            this.pointsGranted = pointsGranted;
            this.status = status;
            this.message = message;
        }

        public String getPayOrderNo() { return payOrderNo; }
        public int getPointsGranted() { return pointsGranted; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    /**
     * 处理充值回调.
     *
     * @param payOrderNo  支付订单号
     * @param userId      用户 ID
     * @param amountFen   充值金额（分）
     * @param notifyTime  回调时间（字符串）
     * @param sign        支付平台签名
     * @return 处理结果
     * @throws GrowthBusinessException 验签失败或参数非法时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public CallbackResult handleCallback(String payOrderNo, Long userId, long amountFen,
                                         String notifyTime, String sign) {
        if (payOrderNo == null || payOrderNo.trim().isEmpty()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "payOrderNo 不能为空");
        }
        if (userId == null || amountFen <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "userId 和 amountFen 必须合法");
        }

        // ① 验签
        rechargeSignVerifyService.verify(payOrderNo, userId, amountFen, notifyTime, sign);

        // ② 幂等检查：已 SUCCESS 直接返回
        String existingStatus = rechargeOrderRepository.findStatusByPayOrderNo(payOrderNo);
        if ("SUCCESS".equals(existingStatus)) {
            log.info("[充值回调] 幂等跳过，payOrderNo={}", payOrderNo);
            return new CallbackResult(payOrderNo, 0, "SUCCESS", "已处理，不重复入账");
        }

        // ④ 计算积分
        Optional<PointRule> ruleOpt = pointRuleRepository.findBySourceType(SOURCE_RECHARGE);
        int fenPerPoint = DEFAULT_FEN_PER_POINT;
        if (ruleOpt.isPresent() && ruleOpt.get().isEffective() && ruleOpt.get().getPointAmount() > 0) {
            // pointAmount 在 RECHARGE 规则中代表"每元（100分）获得的积分数"
            // pointsGranted = amountFen / 100 * pointAmount
            int pointsPerYuan = ruleOpt.get().getPointAmount();
            long pointsGranted = amountFen / 100 * pointsPerYuan;
            return doInsertAndGrantPoints(payOrderNo, userId, amountFen, pointsGranted);
        } else {
            long pointsGranted = amountFen / fenPerPoint;
            return doInsertAndGrantPoints(payOrderNo, userId, amountFen, pointsGranted);
        }
    }

    // ────────────────────────── 私有辅助方法 ──────────────────────────────

    private CallbackResult doInsertAndGrantPoints(String payOrderNo, Long userId,
                                                  long amountFen, long pointsGranted) {
        // ③ INSERT IGNORE recharge_order(PENDING)
        int inserted = rechargeOrderRepository.insertIgnore(userId, payOrderNo,
                amountFen, (int) pointsGranted, null);
        if (inserted == 0) {
            // 已存在记录（并发场景），直接幂等返回
            log.info("[充值回调] 并发幂等跳过，payOrderNo={}", payOrderNo);
            return new CallbackResult(payOrderNo, 0, "SUCCESS", "已处理，不重复入账");
        }

        // ⑤ 积分入账
        pointAppService.addPoints(userId, (int) pointsGranted, SOURCE_RECHARGE, payOrderNo,
                "充值 " + amountFen + " 分，获得 " + pointsGranted + " 积分", null);

        // ⑥ 更新状态为 SUCCESS
        rechargeOrderRepository.markSuccess(payOrderNo);

        log.info("[充值回调] 成功。payOrderNo={}, userId={}, amountFen={}, pointsGranted={}",
                payOrderNo, userId, amountFen, pointsGranted);

        return new CallbackResult(payOrderNo, (int) pointsGranted, "SUCCESS", null);
    }
}

