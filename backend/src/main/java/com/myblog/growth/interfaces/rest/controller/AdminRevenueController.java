package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.RevenueShareAppService;
import com.myblog.growth.infrastructure.repository.persistence.entity.RevenueShareJournalDO;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分账收益管理接口.
 *
 * <pre>
 * GET /api/revenue/my            — 查询我的分账流水（作者视角）
 * GET /api/admin/revenue/{uid}   — 查询指定作者的分账流水（管理员视角）
 * </pre>
 */
@RestController
public class AdminRevenueController {

    private final RevenueShareAppService revenueShareAppService;

    /**
     * 构造注入.
     *
     * @param revenueShareAppService 分账应用服务
     */
    public AdminRevenueController(RevenueShareAppService revenueShareAppService) {
        this.revenueShareAppService = revenueShareAppService;
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
        long total = revenueShareAppService.countAuthorJournals(userId);
        List<RevenueShareJournalDO> journals =
                revenueShareAppService.getAuthorJournals(userId, safePage, safeSize);

        List<RevenueShareAppService.RevenueShareVO> voList = journals.stream()
                .map(RevenueShareAppService.RevenueShareVO::new)
                .collect(Collectors.toList());

        return Result.success(new PageResult<>(voList, safePage, safeSize, total));
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

        long total = revenueShareAppService.countAuthorJournals(uid);
        List<RevenueShareJournalDO> journals =
                revenueShareAppService.getAuthorJournals(uid, safePage, safeSize);

        List<RevenueShareAppService.RevenueShareVO> voList = journals.stream()
                .map(RevenueShareAppService.RevenueShareVO::new)
                .collect(Collectors.toList());

        return Result.success(new PageResult<>(voList, safePage, safeSize, total));
    }
}

