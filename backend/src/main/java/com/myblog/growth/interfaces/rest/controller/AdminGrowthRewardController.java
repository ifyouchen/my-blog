package com.myblog.growth.interfaces.rest.controller;

import com.myblog.application.dto.AnnualCreatorCandidateDTO;
import com.myblog.growth.application.service.GrowthRewardBackfillAppService;
import com.myblog.application.service.RecommendationApplicationAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端成长奖励修复与候选资格接口.
 *
 * @author Codex
 * @since 2026-05-17
 */
@RestController
@RequestMapping("/api/admin/growth")
public class AdminGrowthRewardController {

    private final GrowthRewardBackfillAppService growthRewardBackfillAppService;
    private final RecommendationApplicationAppService recommendationApplicationAppService;

    public AdminGrowthRewardController(GrowthRewardBackfillAppService growthRewardBackfillAppService,
                                       RecommendationApplicationAppService recommendationApplicationAppService) {
        this.growthRewardBackfillAppService = growthRewardBackfillAppService;
        this.recommendationApplicationAppService = recommendationApplicationAppService;
    }

    /**
     * 查询年度创作者候选列表.
     *
     * @param userKeyword 用户名或邮箱（可选）
     * @return 候选列表
     */
    @GetMapping("/annual-creator-candidates")
    public Result<List<AnnualCreatorCandidateDTO>> getAnnualCreatorCandidates(
            @RequestParam(required = false) String userKeyword) {
        requireAdmin();
        return Result.success(recommendationApplicationAppService.listAnnualCreatorCandidates(userKeyword));
    }

    /**
     * 执行成长奖励补偿.
     *
     * @param request 请求体
     * @return 补偿结果
     */
    @PostMapping("/rewards/backfill")
    public Result<Map<String, Object>> backfill(@RequestBody(required = false) Map<String, Object> request) {
        requireAdmin();
        String mode = request == null || request.get("mode") == null ? "ALL" : String.valueOf(request.get("mode"));
        if ("USER".equalsIgnoreCase(mode)) {
            Object userIdObj = request.get("userId");
            if (userIdObj == null) {
                throw new ApplicationException(ErrorCode.PARAM_ERROR, "userId 不能为空");
            }
            Long userId = Long.valueOf(String.valueOf(userIdObj));
            return Result.success(growthRewardBackfillAppService.backfillUser(userId));
        }
        return Result.success(growthRewardBackfillAppService.backfillAll());
    }

    private void requireAdmin() {
        if (!UserRole.ADMIN.name().equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作，需要 ADMIN 角色");
        }
    }
}
