package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.service.ColumnAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.dto.response.ColumnResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 专栏 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/columns")
public class ColumnController {

    private final ColumnAppService columnAppService;
    private final RestDtoMapper restDtoMapper;

    public ColumnController(ColumnAppService columnAppService, RestDtoMapper restDtoMapper) {
        this.columnAppService = columnAppService;
        this.restDtoMapper = restDtoMapper;
    }

    @GetMapping
    public Result<PageResult<ColumnResponse>> pageColumns(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "9") int pageSize) {
        PageResult<ColumnDTO> pageResult = columnAppService.pageColumns(page, pageSize, AuthContext.getCurrentUserId());
        List<ColumnResponse> items = new ArrayList<ColumnResponse>(pageResult.getItems().size());
        for (ColumnDTO item : pageResult.getItems()) {
            items.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(new PageResult<ColumnResponse>(
            items,
            pageResult.getPage(),
            pageResult.getPageSize(),
            pageResult.getTotal()
        ));
    }

    @GetMapping("/recommended")
    public Result<List<ColumnResponse>> listRecommendedColumns(@RequestParam(defaultValue = "3") int limit) {
        List<ColumnResponse> items = new ArrayList<ColumnResponse>();
        for (ColumnDTO item : columnAppService.listRecommendedColumns(limit, AuthContext.getCurrentUserId())) {
            items.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(items);
    }

    @GetMapping("/{id}")
    public Result<ColumnResponse> getColumnDetail(@PathVariable Long id) {
        return Result.success(restDtoMapper.toPublicResponse(
            columnAppService.getColumnDetail(id, AuthContext.getCurrentUserId())
        ));
    }

    @GetMapping("/{id}/articles/{articleId}/neighbors")
    public Result<java.util.Map<String, ArticleResponse>> getColumnArticleNeighbors(
            @PathVariable Long id,
            @PathVariable Long articleId) {
        java.util.Map<String, com.myblog.application.dto.ArticleDTO> neighbors =
            columnAppService.getColumnArticleNeighbors(id, articleId);
        java.util.Map<String, ArticleResponse> resp = new java.util.LinkedHashMap<>();
        resp.put("prev", neighbors.get("prev") == null ? null : restDtoMapper.toPublicResponse(neighbors.get("prev")));
        resp.put("next", neighbors.get("next") == null ? null : restDtoMapper.toPublicResponse(neighbors.get("next")));
        return Result.success(resp);
    }

    @GetMapping("/{id}/articles")
    public Result<PageResult<ArticleResponse>> pageColumnArticles(@PathVariable Long id,
                                                                  @RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<ArticleDTO> pageResult = columnAppService.pageColumnArticles(id, page, pageSize);
        List<ArticleResponse> items = new ArrayList<ArticleResponse>(pageResult.getItems().size());
        for (ArticleDTO item : pageResult.getItems()) {
            items.add(restDtoMapper.toPublicResponse(item));
        }
        return Result.success(new PageResult<ArticleResponse>(
            items,
            pageResult.getPage(),
            pageResult.getPageSize(),
            pageResult.getTotal()
        ));
    }

    @PostMapping("/{id}/subscribe")
    public Result<Void> subscribeColumn(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        columnAppService.subscribeColumn(id, userId);
        return Result.success();
    }

    @DeleteMapping("/{id}/subscribe")
    public Result<Void> unsubscribeColumn(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        columnAppService.unsubscribeColumn(id, userId);
        return Result.success();
    }
}
