package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.SignInAppService;
import com.myblog.growth.interfaces.rest.dto.response.SignInCalendarResponse;
import com.myblog.growth.interfaces.rest.dto.response.SignInResponse;
import com.myblog.growth.interfaces.rest.dto.response.SignInStatsResponse;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 签到接口.
 *
 * <pre>
 * POST /api/points/sign-in          — 执行签到
 * GET  /api/points/sign-in/calendar — 查询签到日历
 * GET  /api/points/sign-in/stats    — 查询签到统计
 * GET  /api/points/sign-in/milestones — 查询里程碑列表
 * </pre>
 *
 * @author czx
 * @since 2026-05-17
 */
@RestController
@RequestMapping("/api/points/sign-in")
public class SignInController {

    private final SignInAppService signInAppService;

    public SignInController(SignInAppService signInAppService) {
        this.signInAppService = signInAppService;
    }

    @PostMapping
    public Result<SignInResponse> signIn() {
        Long userId = AuthContext.getRequiredUserId();
        SignInAppService.SignInResult result = signInAppService.signIn(userId);

        SignInResponse resp = new SignInResponse();
        resp.setSignDate(result.getSignDate());
        resp.setConsecutiveDays(result.getConsecutiveDays());
        resp.setTotalSignDays(result.getTotalSignDays());
        resp.setBasePoints(result.getBasePoints());
        resp.setConsecutiveBonus(result.getConsecutiveBonus());
        resp.setMilestoneBonus(result.getMilestoneBonus());
        resp.setPointsGranted(result.getPointsGranted());
        resp.setTotalBalance(result.getTotalBalance());
        resp.setRewardTier(result.getRewardTier());
        resp.setRewardDesc(result.getRewardDesc());
        if (result.getNewMilestones() != null) {
            List<SignInResponse.MilestoneRewardInfo> milestones = result.getNewMilestones().stream()
                    .map(m -> {
                        SignInResponse.MilestoneRewardInfo info = new SignInResponse.MilestoneRewardInfo();
                        info.setMilestoneDays(m.getMilestoneDays());
                        info.setRewardPoints(m.getRewardPoints());
                        info.setRewardTitle(m.getRewardTitle());
                        info.setDescription(m.getDescription());
                        return info;
                    })
                    .collect(Collectors.toList());
            resp.setNewMilestones(milestones);
        }
        return Result.success(resp);
    }

    @GetMapping("/calendar")
    public Result<SignInCalendarResponse> getCalendar(
            @RequestParam(required = false) String month) {
        Long userId = AuthContext.getRequiredUserId();
        SignInAppService.SignInCalendarResult result = signInAppService.getCalendar(userId, month);

        SignInCalendarResponse resp = new SignInCalendarResponse();
        resp.setMonth(result.getMonth());
        resp.setSignedDates(result.getSignedDates());
        resp.setCurrentConsecutiveDays(result.getCurrentConsecutiveDays());
        resp.setTotalSignDaysThisMonth(result.getTotalSignDaysThisMonth());
        resp.setTodaySigned(result.isTodaySigned());
        return Result.success(resp);
    }

    @GetMapping("/stats")
    public Result<SignInStatsResponse> getStats() {
        Long userId = AuthContext.getRequiredUserId();
        SignInAppService.SignInStatsResult result = signInAppService.getSignInStats(userId);

        SignInStatsResponse resp = new SignInStatsResponse();
        resp.setTotalSignDays(result.getTotalSignDays());
        resp.setCurrentStreak(result.getCurrentStreak());
        resp.setLongestStreak(result.getLongestStreak());
        resp.setLastSignDate(result.getLastSignDate());

        SignInAppService.NextMilestoneInfo next = result.getNextMilestone();
        if (next != null) {
            SignInStatsResponse.NextMilestoneInfo nextResp = new SignInStatsResponse.NextMilestoneInfo();
            nextResp.setMilestoneDays(next.getMilestoneDays());
            nextResp.setRewardPoints(next.getRewardPoints());
            nextResp.setRewardTitle(next.getRewardTitle());
            nextResp.setProgress(next.getProgress());
            nextResp.setRemaining(next.getRemaining());
            resp.setNextMilestone(nextResp);
        }

        return Result.success(resp);
    }

    @GetMapping("/milestones")
    public Result<List<SignInResponse.MilestoneRewardInfo>> getMilestones() {
        Long userId = AuthContext.getRequiredUserId();
        List<SignInAppService.MilestoneRewardInfo> result = signInAppService.getMilestones(userId);

        List<SignInResponse.MilestoneRewardInfo> resp = result.stream()
                .map(m -> {
                    SignInResponse.MilestoneRewardInfo info = new SignInResponse.MilestoneRewardInfo();
                    info.setMilestoneDays(m.getMilestoneDays());
                    info.setRewardPoints(m.getRewardPoints());
                    info.setRewardTitle(m.getRewardTitle());
                    info.setDescription(m.getDescription());
                    return info;
                })
                .collect(Collectors.toList());

        return Result.success(resp);
    }
}
