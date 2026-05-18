package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.RevenueShareAppService;
import com.myblog.growth.application.service.RevenueShareSettlementAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分账收益管理接口.
 *
 * <pre>
 * GET  /api/revenue/my                         — 查询我的分账流水（作者视角）
 * GET  /api/admin/revenue/{uid}                — 查询指定作者的分账流水（管理员视角）
 * GET  /api/admin/revenue-shares               — 管理端分页查询分账流水
 * POST /api/admin/revenue-shares/{orderNo}/retry — 管理员手动重试分账结算
 * </pre>
 */
@RestController
public class AdminRevenueController {

    private final RevenueShareAppService revenueShareAppService;
    private final RevenueShareSettlementAppService settlementAppService;

    /**
     * 构造注入.
     *
     * @param revenueShareAppService 分账应用服务
     * @param settlementAppService   分账结算服务
     */
    public AdminRevenueController(RevenueShareAppService revenueShareAppService,
                                  RevenueShareSettlementAppService settlementAppService) {
        this.revenueShareAppService = revenueShareAppService;
        this.settlementAppService = settlementAppService;
    }

    /**
     * 查询我的分账流水（作者视角，需要登录）.
     *
     * @param page 页码（从 1 开始，默认 1）
     * @param size 每页条数（默认 20，最大 50）
     * @return 分账流水分页结果
     */
    @GetMapping("/api/revenue/my")
    public Result<PageResult<RevenueShareAppService.RevenueShareVO>> getMyRevenue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        Long userId = AuthContext.getRequiredUserId();
        return Result.success(revenueShareAppService.pageAuthorRevenue(userId, safePage, safeSize));
    }

    /**
     * 管理员查询指定作者的分账流水（需要 ADMIN 角色）.
     *
     * @param uid  作者用户 ID
     * @param page 页码（从 1 开始，默认 1）
     * @param size 每页条数（默认 20，最大 50）
     * @return 分账流水分页结果
     */
    @GetMapping("/api/admin/revenue/{uid}")
    public Result<PageResult<RevenueShareAppService.RevenueShareVO>> getAuthorRevenue(
            @PathVariable Long uid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        requireAdmin();
        return Result.success(revenueShareAppService.pageAuthorRevenue(uid, safePage, safeSize));
    }

    /**
     * 管理端分页查询分账流水.
     *
     * @param authorId         作者用户 ID（可选）
     * @param authorKeyword    作者用户名或邮箱（可选）
     * @param settlementStatus 结算状态（可选：PENDING/SETTLED/FAILED）
     * @param page             页码（从 1 开始，默认 1）
     * @param size             每页条数（默认 20，最大 50）
     * @return 分账流水分页结果
     */
    @GetMapping("/api/admin/revenue-shares")
    public Result<PageResult<RevenueShareAppService.RevenueShareVO>> pageRevenueShares(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String authorKeyword,
            @RequestParam(required = false) String settlementStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);

        requireAdmin();
        return Result.success(revenueShareAppService.pageAdminRevenue(
                authorId, normalizeKeyword(authorKeyword), normalizeStatus(settlementStatus), safePage, safeSize));
    }

    /**
     * 管理员手动重试分账结算.
     *
     * @param orderNo 解锁订单号
     * @return 结算结果
     */
    @PostMapping("/api/admin/revenue-shares/{orderNo}/retry")
    public Result<RevenueShareSettlementAppService.SettlementResult> retryRevenueShare(
            @PathVariable String orderNo) {
        requireAdmin();
        return Result.success(settlementAppService.settle(orderNo));
    }

    private void requireAdmin() {
        AuthContext.getRequiredUserId();
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
    }

    private String normalizeStatus(String settlementStatus) {
        if (settlementStatus == null || settlementStatus.trim().isEmpty()) {
            return null;
        }
        return settlementStatus.trim().toUpperCase();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim();
    }
}

