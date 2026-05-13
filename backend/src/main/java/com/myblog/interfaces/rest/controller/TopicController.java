package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.TopicDTO;
import com.myblog.application.service.TopicAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.dto.response.TopicResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 专题 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicAppService topicAppService;
    private final RestDtoMapper restDtoMapper;

    public TopicController(TopicAppService topicAppService, RestDtoMapper restDtoMapper) {
        this.topicAppService = topicAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 分页查询专题列表。
     *
     * @param page     页码
     * @param pageSize 每页数量
     * @return 专题分页结果
     */
    @GetMapping
    public Result<PageResult<TopicResponse>> pageTopics(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<TopicDTO> pageResult = topicAppService.pageTopics(page, pageSize);
        List<TopicResponse> items = new ArrayList<>(pageResult.getItems().size());
        for (TopicDTO item : pageResult.getItems()) {
            items.add(toResponse(item));
        }
        return Result.success(new PageResult<>(
            items,
            pageResult.getPage(),
            pageResult.getPageSize(),
            pageResult.getTotal()
        ));
    }

    /**
     * 获取专题详情。
     *
     * @param id 专题 ID
     * @return 专题详情
     */
    @GetMapping("/{id}")
    public Result<TopicResponse> getTopicDetail(@PathVariable Long id) {
        return Result.success(toResponse(topicAppService.getTopicDetail(id, AuthContext.getCurrentUserId())));
    }

    /**
     * 获取专题内文章的上一篇和下一篇。
     *
     * @param id        专题 ID
     * @param articleId 当前文章 ID
     * @return 包含 prev 和 next 的文章对
     */
    @GetMapping("/{id}/articles/{articleId}/neighbors")
    public Result<java.util.Map<String, ArticleResponse>> getTopicArticleNeighbors(
            @PathVariable Long id,
            @PathVariable Long articleId) {
        java.util.Map<String, com.myblog.application.dto.ArticleDTO> neighbors =
            topicAppService.getTopicArticleNeighbors(id, articleId);
        java.util.Map<String, ArticleResponse> resp = new java.util.LinkedHashMap<>();
        resp.put("prev", neighbors.get("prev") == null ? null : restDtoMapper.toPublicResponse(neighbors.get("prev")));
        resp.put("next", neighbors.get("next") == null ? null : restDtoMapper.toPublicResponse(neighbors.get("next")));
        return Result.success(resp);
    }

    /**
     * 分页查询专题内文章列表。
     *
     * @param id       专题 ID
     * @param page     页码
     * @param pageSize 每页数量
     * @return 文章分页结果
     */
    @GetMapping("/{id}/articles")
    public Result<PageResult<ArticleResponse>> pageTopicArticles(@PathVariable Long id,
                                                                  @RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<ArticleDTO> pageResult = topicAppService.pageTopicArticles(id, page, pageSize);
        List<ArticleResponse> items = new ArrayList<>(pageResult.getItems().size());
        for (ArticleDTO item : pageResult.getItems()) {
            items.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(new PageResult<>(
            items,
            pageResult.getPage(),
            pageResult.getPageSize(),
            pageResult.getTotal()
        ));
    }

    /**
     * 将专题 DTO 转换为响应对象。
     *
     * @param dto 专题 DTO
     * @return 专题响应
     */
    private TopicResponse toResponse(TopicDTO dto) {
        TopicResponse response = new TopicResponse();
        response.setId(dto.getId());
        response.setTitle(dto.getTitle());
        response.setSummary(dto.getSummary());
        response.setCoverUrl(dto.getCoverUrl());
        response.setArticleCount(dto.getArticleCount());
        response.setIntro(dto.getIntro());
        response.setDifficulty(dto.getDifficulty());
        response.setEstimatedMinutes(dto.getEstimatedMinutes());
        response.setSourceType(dto.getSourceType());
        response.setSourceNote(dto.getSourceNote());
        response.setRecommended(dto.isRecommended());
        response.setRecommendWeight(dto.getRecommendWeight());
        response.setProgress(dto.getProgress());
        response.setOutline(dto.getOutline());
        response.setNextArticle(dto.getNextArticle() == null ? null : restDtoMapper.toPublicResponse(dto.getNextArticle()));
        return response;
    }
}
