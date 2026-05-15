package com.myblog.growth.interfaces.rest.controller;

import com.myblog.growth.application.service.ArticleUnlockAppService;
import com.myblog.growth.interfaces.rest.dto.response.UnlockStatusResponse;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 文章积分解锁接口.
 *
 * <pre>
 * POST /api/articles/{id}/unlock        — 解锁文章（需要登录）
 * GET  /api/articles/{id}/unlock-status — 查询解锁状态（需要登录）
 * </pre>
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleUnlockController {

    private final ArticleUnlockAppService articleUnlockAppService;

    /**
     * 构造注入.
     *
     * @param articleUnlockAppService 文章解锁应用服务
     */
    public ArticleUnlockController(ArticleUnlockAppService articleUnlockAppService) {
        this.articleUnlockAppService = articleUnlockAppService;
    }

    /**
     * 解锁文章.
     *
     * @param id 文章 ID
     * @return 解锁结果（articleId、unlocked、pointsCost、balanceAfter、orderNo）
     */
    @PostMapping("/{id}/unlock")
    public Result<Map<String, Object>> unlock(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        ArticleUnlockAppService.UnlockResult result = articleUnlockAppService.unlock(userId, id);

        Map<String, Object> data = new HashMap<>();
        data.put("articleId", result.getArticleId());
        data.put("unlocked", result.isUnlocked());
        data.put("pointsCost", result.getPointsCost());
        data.put("balanceAfter", result.getBalanceAfter());
        if (result.getOrderNo() != null) {
            data.put("orderNo", result.getOrderNo());
        }
        if (result.getMessage() != null) {
            data.put("message", result.getMessage());
        }
        return Result.success(data);
    }

    /**
     * 查询解锁状态.
     *
     * @param id 文章 ID
     * @return 解锁状态（是否需要解锁、积分价格、是否已解锁、当前余额）
     */
    @GetMapping("/{id}/unlock-status")
    public Result<UnlockStatusResponse> getUnlockStatus(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        ArticleUnlockAppService.UnlockStatusResult result =
                articleUnlockAppService.getUnlockStatus(userId, id);

        UnlockStatusResponse resp = new UnlockStatusResponse();
        resp.setArticleId(result.getArticleId());
        resp.setNeedUnlock(result.isNeedUnlock());
        resp.setUnlockPointPrice(result.getUnlockPointPrice());
        resp.setUnlocked(result.isUnlocked());
        resp.setCurrentBalance(result.getCurrentBalance());
        return Result.success(resp);
    }
}

