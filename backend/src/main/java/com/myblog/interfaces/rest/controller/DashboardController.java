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

    /**
     * 获取创作者概览数据。
     *
     * @return 居山概览 DTO
     */
    @GetMapping("/overview")
    public Result<DashboardOverviewDTO> overview() {
        return Result.success(dashboardAppService.getOverview(requiredUserId()));
    }

    /**
     * 获取创作者成长趋势数据。
     *
     * @param range 时间范围（7d/30d）
     * @return 趋势数据列表
     */
    @GetMapping("/trends")
    public Result<List<DashboardTrendPointDTO>> trends(@RequestParam(defaultValue = "7d") String range) {
        return Result.success(dashboardAppService.getTrends(requiredUserId(), range));
    }

    /**
     * 获取文章表现数据列表。
     *
     * @param sort 排序方式（view/like/comment）
     * @return 文章表现列表
     */
    @GetMapping("/articles/performance")
    public Result<List<DashboardArticlePerformanceDTO>> articlePerformance(
        @RequestParam(defaultValue = "view") String sort) {
        return Result.success(dashboardAppService.getArticlePerformance(requiredUserId(), sort));
    }

    /**
     * 获取近期互动消息列表。
     *
     * @return 最近收到的评论、点赞等通知
     */
    @GetMapping("/interactions")
    public Result<List<NotificationDTO>> interactions() {
        return Result.success(dashboardAppService.getInteractions(requiredUserId()));
    }

    /**
     * 获取内容创作机会建议（热门关键词、剪芒内容等）。
     *
     * @return 内容机会建议列表
     */
    @GetMapping("/content-opportunities")
    public Result<List<Map<String, Object>>> contentOpportunities() {
        return Result.success(dashboardAppService.getContentOpportunities(requiredUserId()));
    }

    /**
     * 获取单篇文章的详细统计数据。
     *
     * @param articleId 文章 ID
     * @param range     统计时间范围（7d/30d）
     * @return 文章统计 DTO
     */
    @GetMapping("/articles/{id}/stats")
    public Result<ArticleStatsDTO> articleStats(@PathVariable("id") Long articleId,
                                                @RequestParam(defaultValue = "7d") String range) {
        return Result.success(dashboardAppService.getArticleStats(requiredUserId(), articleId, range));
    }

    /**
     * 获取当前登录用户 ID，未登录时抛出未授权异常。
     *
     * @return 当前用户 ID
     */
    private Long requiredUserId() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }
}
