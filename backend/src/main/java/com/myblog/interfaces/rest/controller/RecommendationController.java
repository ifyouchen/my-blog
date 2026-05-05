package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.application.service.RecommendationAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.Result;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.dto.response.ColumnResponse;
import com.myblog.interfaces.rest.dto.response.UserResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 推荐 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationAppService recommendationAppService;
    private final RestDtoMapper restDtoMapper;

    public RecommendationController(RecommendationAppService recommendationAppService,
                                    RestDtoMapper restDtoMapper) {
        this.recommendationAppService = recommendationAppService;
        this.restDtoMapper = restDtoMapper;
    }

    @GetMapping("/authors")
    public Result<List<UserResponse>> listRecommendedAuthors(@RequestParam(defaultValue = "5") int limit) {
        List<UserDTO> items = recommendationAppService.listRecommendedAuthors(limit, AuthContext.getCurrentUserId());
        List<UserResponse> responses = new ArrayList<>(items.size());
        for (UserDTO item : items) {
            responses.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(responses);
    }

    @GetMapping("/columns")
    public Result<List<ColumnResponse>> listRecommendedColumns(@RequestParam(defaultValue = "5") int limit) {
        List<ColumnDTO> items = recommendationAppService.listRecommendedColumns(limit, AuthContext.getCurrentUserId());
        List<ColumnResponse> responses = new ArrayList<>(items.size());
        for (ColumnDTO item : items) {
            responses.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(responses);
    }

    @GetMapping("/articles")
    public Result<List<ArticleResponse>> listFeaturedArticles(@RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int pageSize) {
        List<ArticleDTO> items = recommendationAppService.listFeaturedArticles(page, pageSize);
        List<ArticleResponse> responses = new ArrayList<>(items.size());
        for (ArticleDTO item : items) {
            responses.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(responses);
    }
}
