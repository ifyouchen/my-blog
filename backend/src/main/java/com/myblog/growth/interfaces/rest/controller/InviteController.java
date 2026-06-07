package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.InviteRewardAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拉新邀请奖励接口.
 *
 * <pre>
 * GET /api/points/invite/summary — 查询我的邀请汇总
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

    /**
     * 查询被我邀请的用户列表.
     *
     * @return 被邀请用户列表
     */
    @GetMapping("/api/points/invite/invited-users")
    public Result<List<Map<String, Object>>> getInvitedUsers() {
        Long userId = AuthContext.getRequiredUserId();
        return Result.success(inviteRewardAppService.getInvitedUsers(userId));
    }
}

