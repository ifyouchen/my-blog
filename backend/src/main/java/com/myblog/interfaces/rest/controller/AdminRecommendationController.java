package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.RecommendationApplicationDTO;
import com.myblog.application.service.RecommendationApplicationAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.enums.UserRole;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端首页推荐申请接口.
 *
 * @author Codex
 * @since 2026-05-17
 */
@RestController
@RequestMapping("/api/admin/recommendation-applications")
public class AdminRecommendationController {

    private final RecommendationApplicationAppService recommendationApplicationAppService;

    public AdminRecommendationController(RecommendationApplicationAppService recommendationApplicationAppService) {
        this.recommendationApplicationAppService = recommendationApplicationAppService;
    }

    /**
     * 分页查询推荐申请.
     *
     * @param status 状态
     * @param page   页码
     * @param size   每页数量
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<RecommendationApplicationDTO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin();
        return Result.success(recommendationApplicationAppService.pageForAdmin(status, page, size));
    }

    /**
     * 审批通过推荐申请.
     *
     * @param id      申请ID
     * @param request 请求体
     * @return 审批结果
     */
    @PostMapping("/{id}/approve")
    public Result<RecommendationApplicationDTO> approve(@PathVariable Long id,
                                                        @RequestBody(required = false) Map<String, String> request) {
        requireAdmin();
        String reviewNote = request == null ? null : request.get("reviewNote");
        return Result.success(recommendationApplicationAppService.approve(
            id,
            AuthContext.getRequiredUserId(),
            reviewNote
        ));
    }

    /**
     * 审批拒绝推荐申请.
     *
     * @param id      申请ID
     * @param request 请求体
     * @return 审批结果
     */
    @PostMapping("/{id}/reject")
    public Result<RecommendationApplicationDTO> reject(@PathVariable Long id,
                                                       @RequestBody(required = false) Map<String, String> request) {
        requireAdmin();
        String reviewNote = request == null ? null : request.get("reviewNote");
        return Result.success(recommendationApplicationAppService.reject(
            id,
            AuthContext.getRequiredUserId(),
            reviewNote
        ));
    }

    private void requireAdmin() {
        if (!UserRole.ADMIN.name().equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "无权操作，需要 ADMIN 角色");
        }
    }
}
