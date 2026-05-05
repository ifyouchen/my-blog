package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.service.ArticleFavoriteAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 文章收藏 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api")
public class ArticleFavoriteController {

    private final ArticleFavoriteAppService articleFavoriteAppService;
    private final RestDtoMapper restDtoMapper;

    public ArticleFavoriteController(ArticleFavoriteAppService articleFavoriteAppService,
                                     RestDtoMapper restDtoMapper) {
        this.articleFavoriteAppService = articleFavoriteAppService;
        this.restDtoMapper = restDtoMapper;
    }

    @PostMapping("/articles/{articleId}/favorite")
    public Result<Map<String, Object>> favoriteArticle(@PathVariable Long articleId) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        articleFavoriteAppService.favoriteArticle(articleId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", true);
        return Result.success(result);
    }

    @DeleteMapping("/articles/{articleId}/favorite")
    public Result<Map<String, Object>> unfavoriteArticle(@PathVariable Long articleId) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        articleFavoriteAppService.unfavoriteArticle(articleId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", false);
        return Result.success(result);
    }

    @GetMapping("/articles/{articleId}/favorite/status")
    public Result<Map<String, Object>> getFavoriteStatus(@PathVariable Long articleId) {
        Long userId = AuthContext.getCurrentUserId();
        boolean hasFavorited = false;
        if (userId != null) {
            hasFavorited = articleFavoriteAppService.hasFavorited(articleId, userId);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", hasFavorited);
        return Result.success(result);
    }

    @GetMapping("/users/me/favorites")
    public Result<PageResult<ArticleResponse>> getMyFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        PageResult<ArticleDTO> favorites = articleFavoriteAppService.getUserFavorites(userId, page, pageSize, keyword);
        java.util.List<ArticleResponse> items = new java.util.ArrayList<ArticleResponse>();
        for (ArticleDTO favorite : favorites.getItems()) {
            items.add(restDtoMapper.toResponse(favorite));
        }
        return Result.success(new PageResult<ArticleResponse>(
            items,
            favorites.getPage(),
            favorites.getPageSize(),
            favorites.getTotal()
        ));
    }
}
