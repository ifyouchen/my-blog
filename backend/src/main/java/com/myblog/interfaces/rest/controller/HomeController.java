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

    /**
     * 获取首页引导数据，包括推荐专栏、热门标签、作者排行、精选文章等。
     *
     * @return 首页引导数据
     */
    @GetMapping("/bootstrap")
    public Result<HomeBootstrapResponse> getBootstrap() {
        return Result.success(toResponse(homeBootstrapAppService.getBootstrap()));
    }

    /**
     * 将首页引导 DTO 转换为响应对象。
     *
     * @param dto 首页引导 DTO
     * @return 首页引导响应
     */
    private HomeBootstrapResponse toResponse(HomeBootstrapDTO dto) {
        HomeBootstrapResponse response = new HomeBootstrapResponse();
        response.setStats(dto.getStats());
        response.setCategories(dto.getCategories());
        response.setCategoryGroups(dto.getCategoryGroups());
        response.setRecommendedColumns(toColumnResponses(dto.getRecommendedColumns()));
        response.setAuthorRankings(toAuthorRankingResponses(dto.getAuthorRankings()));
        response.setFeaturedArticles(toArticleResponses(dto.getFeaturedArticles()));
        response.setWeeklyArticles(toArticleResponses(dto.getWeeklyArticles()));
        response.setHotTopics(dto.getHotTopics());
        response.setTodayFocus(dto.getTodayFocus() == null ? null : restDtoMapper.toPublicResponse(dto.getTodayFocus()));
        response.setLearningTopics(dto.getLearningTopics());
        response.setHotTags(dto.getHotTags());
        response.setHotKeywords(dto.getHotKeywords());
        return response;
    }

    /**
     * 将专栏 DTO 列表转换为响应对象列表。
     *
     * @param items 专栏 DTO 列表
     * @return 专栏响应列表
     */
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

    /**
     * 将作者排行 DTO 列表转换为响应对象列表。
     *
     * @param items 作者排行 DTO 列表
     * @return 作者排行响应列表
     */
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

    /**
     * 将文章 DTO 列表转换为响应对象列表。
     *
     * @param items 文章 DTO 列表
     * @return 文章响应列表
     */
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
