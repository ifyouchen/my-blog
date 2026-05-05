package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.AuthorRankingDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.HomeBootstrapDTO;
import com.myblog.application.service.HomeBootstrapAppService;
import com.myblog.application.service.HomeStatsAppService;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.dto.response.AuthorRankingResponse;
import com.myblog.interfaces.rest.dto.response.ColumnResponse;
import com.myblog.interfaces.rest.dto.response.HomeBootstrapResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeStatsAppService homeStatsAppService;
    private final HomeBootstrapAppService homeBootstrapAppService;
    private final RestDtoMapper restDtoMapper;

    /**
     * 创建首页控制器。
     *
     * @param homeStatsAppService 首页统计应用服务
     */
    public HomeController(HomeStatsAppService homeStatsAppService,
                          HomeBootstrapAppService homeBootstrapAppService,
                          RestDtoMapper restDtoMapper) {
        this.homeStatsAppService = homeStatsAppService;
        this.homeBootstrapAppService = homeBootstrapAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 获取首页统计数据。
     *
     * @return 首页统计数据
     */
    @GetMapping("/stats")
    public Result<HomeStatsAppService.HomeStats> getStats() {
        return Result.success(homeStatsAppService.getStats());
    }

    @GetMapping("/bootstrap")
    public Result<HomeBootstrapResponse> getBootstrap() {
        return Result.success(toResponse(homeBootstrapAppService.getBootstrap()));
    }

    private HomeBootstrapResponse toResponse(HomeBootstrapDTO dto) {
        HomeBootstrapResponse response = new HomeBootstrapResponse();
        response.setStats(dto.getStats());
        response.setCategories(dto.getCategories());
        response.setRecommendedColumns(toColumnResponses(dto.getRecommendedColumns()));
        response.setAuthorRankings(toAuthorRankingResponses(dto.getAuthorRankings()));
        response.setFeaturedArticles(toArticleResponses(dto.getFeaturedArticles()));
        response.setHotTopics(dto.getHotTopics());
        return response;
    }

    private List<ColumnResponse> toColumnResponses(List<ColumnDTO> items) {
        List<ColumnResponse> responses = new ArrayList<ColumnResponse>();
        if (items == null) {
            return responses;
        }
        for (ColumnDTO item : items) {
            responses.add(restDtoMapper.toPublicResponse(item));
        }
        return responses;
    }

    private List<AuthorRankingResponse> toAuthorRankingResponses(List<AuthorRankingDTO> items) {
        List<AuthorRankingResponse> responses = new ArrayList<AuthorRankingResponse>();
        if (items == null) {
            return responses;
        }
        for (AuthorRankingDTO item : items) {
            responses.add(restDtoMapper.toPublicResponse(item));
        }
        return responses;
    }

    private List<ArticleResponse> toArticleResponses(List<ArticleDTO> items) {
        List<ArticleResponse> responses = new ArrayList<ArticleResponse>();
        if (items == null) {
            return responses;
        }
        for (ArticleDTO item : items) {
            responses.add(restDtoMapper.toPublicResponse(item));
        }
        return responses;
    }
}
