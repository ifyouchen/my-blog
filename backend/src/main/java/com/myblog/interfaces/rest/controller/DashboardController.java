package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleStatsDTO;
import com.myblog.application.dto.DashboardArticlePerformanceDTO;
import com.myblog.application.dto.DashboardOverviewDTO;
import com.myblog.application.dto.DashboardTrendPointDTO;
import com.myblog.application.dto.NotificationDTO;
import com.myblog.application.service.DashboardAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 创作者工作台接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardAppService dashboardAppService;

    public DashboardController(DashboardAppService dashboardAppService) {
        this.dashboardAppService = dashboardAppService;
    }

    @GetMapping("/overview")
    public Result<DashboardOverviewDTO> overview() {
        return Result.success(dashboardAppService.getOverview(requiredUserId()));
    }

    @GetMapping("/trends")
    public Result<List<DashboardTrendPointDTO>> trends(@RequestParam(defaultValue = "7d") String range) {
        return Result.success(dashboardAppService.getTrends(requiredUserId(), range));
    }

    @GetMapping("/articles/performance")
    public Result<List<DashboardArticlePerformanceDTO>> articlePerformance(
        @RequestParam(defaultValue = "view") String sort) {
        return Result.success(dashboardAppService.getArticlePerformance(requiredUserId(), sort));
    }

    @GetMapping("/interactions")
    public Result<List<NotificationDTO>> interactions() {
        return Result.success(dashboardAppService.getInteractions(requiredUserId()));
    }

    @GetMapping("/content-opportunities")
    public Result<List<Map<String, Object>>> contentOpportunities() {
        return Result.success(dashboardAppService.getContentOpportunities(requiredUserId()));
    }

    @GetMapping("/articles/{id}/stats")
    public Result<ArticleStatsDTO> articleStats(@PathVariable("id") Long articleId,
                                                @RequestParam(defaultValue = "7d") String range) {
        return Result.success(dashboardAppService.getArticleStats(requiredUserId(), articleId, range));
    }

    private Long requiredUserId() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }
}
