package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.service.ColumnAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.interfaces.rest.dto.response.ColumnResponse;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 创作者专栏管理接口（/api/dashboard/columns）。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/dashboard/columns")
public class DashboardColumnController {

    private final ColumnAppService columnAppService;
    private final RestDtoMapper restDtoMapper;

    public DashboardColumnController(ColumnAppService columnAppService, RestDtoMapper restDtoMapper) {
        this.columnAppService = columnAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 查询当前用户的专栏列表。
     */
    @GetMapping
    public Result<List<ColumnResponse>> listMyColumns() {
        Long userId = requiredUserId();
        List<ColumnDTO> dtos = columnAppService.listMyColumns(userId);
        List<ColumnResponse> items = new ArrayList<>(dtos.size());
        for (ColumnDTO dto : dtos) {
            items.add(restDtoMapper.toResponse(dto));
        }
        return Result.success(items);
    }

    /**
     * 创建专栏。
     *
     * @param body 包含 title、summary、coverUrl 的请求体
     * @return 创建后的专栏
     */
    @PostMapping
    public Result<ColumnResponse> createColumn(@RequestBody Map<String, String> body) {
        Long userId = requiredUserId();
        String title = body.getOrDefault("title", "").trim();
        if (title.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "专栏标题不能为空");
        }
        String summary = body.getOrDefault("summary", "").trim();
        String coverUrl = body.getOrDefault("coverUrl", "").trim();
        ColumnDTO dto = columnAppService.createMyColumn(userId, title, summary, coverUrl);
        return Result.success(restDtoMapper.toResponse(dto));
    }

    /**
     * 更新专栏信息。
     *
     * @param id   专栏 ID
     * @param body 包含 title、summary、coverUrl 的请求体
     * @return 更新后的专栏
     */
    @PutMapping("/{id}")
    public Result<ColumnResponse> updateColumn(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = requiredUserId();
        String title = body.getOrDefault("title", "").trim();
        if (title.isEmpty()) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "专栏标题不能为空");
        }
        String summary = body.getOrDefault("summary", "").trim();
        String coverUrl = body.getOrDefault("coverUrl", "").trim();
        ColumnDTO dto = columnAppService.updateMyColumn(id, userId, title, summary, coverUrl);
        return Result.success(restDtoMapper.toResponse(dto));
    }

    /**
     * 删除专栏（软删除）。
     *
     * @param id 专栏 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteColumn(@PathVariable Long id) {
        columnAppService.deleteMyColumn(id, requiredUserId());
        return Result.success();
    }

    /**
     * 向专栏添加文章。
     *
     * @param id     专栏 ID
     * @param body   包含 articleId 的请求体
     * @return 成功响应
     */
    @PostMapping("/{id}/articles")
    public Result<Void> addArticle(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long articleId = body.get("articleId");
        if (articleId == null) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "articleId 不能为空");
        }
        columnAppService.addMyColumnArticle(id, articleId, requiredUserId());
        return Result.success();
    }

    /**
     * 从专栏移除文章。
     *
     * @param id        专栏 ID
     * @param articleId 文章 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}/articles/{articleId}")
    public Result<Void> removeArticle(@PathVariable Long id, @PathVariable Long articleId) {
        columnAppService.removeMyColumnArticle(id, articleId, requiredUserId());
        return Result.success();
    }

    /**
     * 获取当前登录用户 ID，未登录时抛出未授权异常。
     *
     * @return 当前用户 ID
     */
    private Long requiredUserId() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }
}

