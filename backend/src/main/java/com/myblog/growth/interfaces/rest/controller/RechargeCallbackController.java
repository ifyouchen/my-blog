package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.RechargeCallbackAppService;
import com.myblog.growth.interfaces.rest.dto.request.RechargeCallbackRequest;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 充值回调接收接口.
 *
 * <p>
 * 该接口由支付平台异步回调，不暴露给前端，需要 IP 白名单或签名验证。
 * </p>
 *
 * <pre>
 * POST /api/open/pay/recharge/callback — 充值回调入账（无需登录，需验签）
 * </pre>
 */
@RestController
@RequestMapping("/api/open/pay/recharge")
public class RechargeCallbackController {

    private final RechargeCallbackAppService rechargeCallbackAppService;

    /**
     * 构造注入.
     *
     * @param rechargeCallbackAppService 充值回调应用服务
     */
    public RechargeCallbackController(RechargeCallbackAppService rechargeCallbackAppService) {
        this.rechargeCallbackAppService = rechargeCallbackAppService;
    }

    /**
     * 接收充值回调.
     *
     * @param request 回调请求体（payOrderNo、userId、amountFen、status、sign、notifyTime 必填）
     * @return 处理结果
     */
    @PostMapping("/callback")
    public Result<Map<String, Object>> handleCallback(@RequestBody RechargeCallbackRequest request) {
        // 参数校验
        if (request.getPayOrderNo() == null || request.getPayOrderNo().trim().isEmpty()) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "payOrderNo 不能为空");
        }
        if (request.getUserId() == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "userId 不能为空");
        }
        if (request.getAmountFen() == null || request.getAmountFen() <= 0) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID, "amountFen 必须大于 0");
        }
        // 非成功回调直接跳过（支付平台可能回调 FAILED）
        if (!"SUCCESS".equalsIgnoreCase(request.getStatus())) {
            Map<String, Object> data = new HashMap<>();
            data.put("payOrderNo", request.getPayOrderNo());
            data.put("status", request.getStatus());
            data.put("message", "非成功回调，已忽略");
            return Result.success(data);
        }

        RechargeCallbackAppService.CallbackResult result = rechargeCallbackAppService.handleCallback(
                request.getPayOrderNo(),
                request.getUserId(),
                request.getAmountFen(),
                request.getNotifyTime(),
                request.getSign()
        );

        Map<String, Object> data = new HashMap<>();
        data.put("payOrderNo", result.getPayOrderNo());
        data.put("pointsGranted", result.getPointsGranted());
        data.put("status", result.getStatus());
        if (result.getMessage() != null) {
            data.put("message", result.getMessage());
        }
        return Result.success(data);
    }
}

