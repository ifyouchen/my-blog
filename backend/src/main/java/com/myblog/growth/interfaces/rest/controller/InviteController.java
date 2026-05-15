package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.InviteRewardAppService;
import com.myblog.growth.interfaces.rest.dto.request.InviteRewardTriggerRequest;
import com.myblog.growth.shared.exception.GrowthBusinessException;
import com.myblog.growth.shared.exception.GrowthErrorCode;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 拉新邀请奖励接口.
 *
 * <pre>
 * POST /api/open/invite/reward/trigger  — 触发拉新奖励（开放接口）
 * GET  /api/points/invite/summary       — 查询我的邀请汇总
 * </pre>
 */
@RestController
public class InviteController {

    private final InviteRewardAppService inviteRewardAppService;

    /**
     * 构造注入.
     *
     * @param inviteRewardAppService 拉新奖励应用服务
     */
    public InviteController(InviteRewardAppService inviteRewardAppService) {
        this.inviteRewardAppService = inviteRewardAppService;
    }

    /**
     * 触发拉新奖励（由注册流程完成后内部调用，开放接口）.
     *
     * @param request 触发请求（inviterUserId、inviteeUserId 必填）
     * @return 奖励触发结果
     */
    @PostMapping("/api/open/invite/reward/trigger")
    public Result<Map<String, Object>> triggerReward(@RequestBody InviteRewardTriggerRequest request) {
        if (request.getInviterUserId() == null || request.getInviteeUserId() == null) {
            throw new GrowthBusinessException(GrowthErrorCode.PARAM_INVALID,
                    "inviterUserId 和 inviteeUserId 不能为空");
        }

        InviteRewardAppService.InviteRewardResult result =
                inviteRewardAppService.triggerReward(request.getInviterUserId(), request.getInviteeUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("inviterUserId", result.getInviterUserId());
        data.put("inviteeUserId", result.getInviteeUserId());
        data.put("pointsGranted", result.getPointsGranted());
        data.put("rewardStatus", result.getRewardStatus());
        if (result.getMessage() != null) {
            data.put("message", result.getMessage());
        }
        return Result.success(data);
    }

    /**
     * 查询我的邀请汇总.
     *
     * @return 邀请汇总（总邀请数、累计获得奖励次数等）
     */
    @GetMapping("/api/points/invite/summary")
    public Result<Map<String, Object>> getMySummary() {
        Long userId = AuthContext.getRequiredUserId();
        InviteRewardAppService.InviteSummary summary = inviteRewardAppService.getSummary(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("inviterUserId", summary.getInviterUserId());
        data.put("totalGrantedCount", summary.getTotalGrantedCount());
        data.put("totalPointsEarned", summary.getTotalGrantedCount() * 50);
        return Result.success(data);
    }
}

