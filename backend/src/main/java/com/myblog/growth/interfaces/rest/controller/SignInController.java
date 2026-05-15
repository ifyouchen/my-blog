package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.SignInAppService;
import com.myblog.growth.interfaces.rest.dto.response.SignInCalendarResponse;
import com.myblog.growth.interfaces.rest.dto.response.SignInResponse;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 签到接口.
 *
 * <pre>
 * POST /api/points/sign-in          — 执行签到
 * GET  /api/points/sign-in/calendar — 查询签到日历
 * </pre>
 */
@RestController
@RequestMapping("/api/points/sign-in")
public class SignInController {

    private final SignInAppService signInAppService;

    /**
     * 构造注入.
     *
     * @param signInAppService 签到应用服务
     */
    public SignInController(SignInAppService signInAppService) {
        this.signInAppService = signInAppService;
    }

    /**
     * 执行签到.
     *
     * @return 签到结果（签到日期、连续天数、积分发放量、总余额、奖励档位等）
     */
    @PostMapping
    public Result<SignInResponse> signIn() {
        Long userId = AuthContext.getRequiredUserId();
        SignInAppService.SignInResult result = signInAppService.signIn(userId);

        SignInResponse resp = new SignInResponse();
        resp.setSignDate(result.getSignDate());
        resp.setConsecutiveDays(result.getConsecutiveDays());
        resp.setPointsGranted(result.getPointsGranted());
        resp.setTotalBalance(result.getTotalBalance());
        resp.setRewardTier(result.getRewardTier());
        resp.setRewardDesc(result.getRewardDesc());
        return Result.success(resp);
    }

    /**
     * 查询签到日历.
     *
     * @param month 月份（格式 yyyy-MM，默认当月）
     * @return 签到日历（已签到日期列表、连续天数、今日是否签到等）
     */
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
}

