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

    @GetMapping("/{id}")
    public Result<TopicResponse> getTopicDetail(@PathVariable Long id) {
        return Result.success(toResponse(topicAppService.getTopicDetail(id)));
    }

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

    private TopicResponse toResponse(TopicDTO dto) {
        TopicResponse response = new TopicResponse();
        response.setId(dto.getId());
        response.setTitle(dto.getTitle());
        response.setSummary(dto.getSummary());
        response.setCoverUrl(dto.getCoverUrl());
        response.setArticleCount(dto.getArticleCount());
        return response;
    }
}
