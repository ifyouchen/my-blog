package com.myblog.interfaces.rest.controller;

import com.myblog.application.service.ArticleLikeAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 文章点赞 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api")
public class ArticleLikeController {

    private final ArticleLikeAppService articleLikeAppService;

    public ArticleLikeController(ArticleLikeAppService articleLikeAppService) {
        this.articleLikeAppService = articleLikeAppService;
    }

    /**
     * 点赞文章。
     *
     * @param articleId 文章 ID
     * @return 点赞结果
     */
    @PostMapping("/articles/{articleId}/like")
    public Result<Map<String, Object>> likeArticle(@PathVariable Long articleId) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        articleLikeAppService.likeArticle(articleId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", true);
        return Result.success(result);
    }

    /**
     * 取消点赞文章。
     *
     * @param articleId 文章 ID
     * @return 取消点赞结果
     */
    @DeleteMapping("/articles/{articleId}/like")
    public Result<Map<String, Object>> unlikeArticle(@PathVariable Long articleId) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        articleLikeAppService.unlikeArticle(articleId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", false);
        return Result.success(result);
    }

    /**
     * 查询文章点赞状态。
     *
     * @param articleId 文章 ID
     * @return 点赞状态
     */
    @GetMapping("/articles/{articleId}/like/status")
    public Result<Map<String, Object>> getLikeStatus(@PathVariable Long articleId) {
        Long userId = AuthContext.getCurrentUserId();
        boolean hasLiked = false;
        if (userId != null) {
            hasLiked = articleLikeAppService.hasLiked(articleId, userId);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("liked", hasLiked);
        return Result.success(result);
    }
}
