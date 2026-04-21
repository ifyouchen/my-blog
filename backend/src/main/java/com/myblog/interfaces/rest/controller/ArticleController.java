package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.query.ArticlePageQuery;
import com.myblog.application.service.ArticleAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.CreateArticleRequest;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 文章 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@Validated
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleAppService articleAppService;
    private final RestDtoMapper restDtoMapper;

    /**
     * 创建文章接口。
     *
     * @param articleAppService 文章应用服务
     * @param restDtoMapper REST DTO 转换器
     */
    public ArticleController(ArticleAppService articleAppService, RestDtoMapper restDtoMapper) {
        this.articleAppService = articleAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 分页查询文章。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param sort 排序方式
     * @return 文章分页响应
     */
    @GetMapping
    public Result<PageResult<ArticleResponse>> pageArticles(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int pageSize,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String category,
                                                            @RequestParam(required = false) String tag,
                                                            @RequestParam(defaultValue = "latest") String sort) {
        PageResult<ArticleDTO> pageResult = articleAppService.pagePublishedArticles(
            new ArticlePageQuery(page, pageSize, keyword, category, tag, sort)
        );
        List<ArticleResponse> items = new ArrayList<ArticleResponse>();
        for (ArticleDTO item : pageResult.getItems()) {
            items.add(restDtoMapper.toResponse(item));
        }
        return Result.success(new PageResult<ArticleResponse>(
            items,
            pageResult.getPage(),
            pageResult.getPageSize(),
            pageResult.getTotal()
        ));
    }

    /**
     * 获取文章详情。
     *
     * @param id 文章 ID
     * @return 文章详情
     */
    @GetMapping("/{id}")
    public Result<ArticleResponse> getArticle(@PathVariable Long id) {
        return Result.success(restDtoMapper.toResponse(
            articleAppService.getArticleDetail(id, AuthContext.getRequiredUserId(), AuthContext.getRole())
        ));
    }

    /**
     * 创建文章。
     *
     * @param request 创建文章请求
     * @return 创建后的文章
     */
    @PostMapping
    public Result<ArticleResponse> createArticle(@RequestBody @Valid CreateArticleRequest request) {
        return Result.success(restDtoMapper.toResponse(
            articleAppService.createArticle(restDtoMapper.toCommand(request, AuthContext.getRequiredUserId()))
        ));
    }

    /**
     * 获取编辑态文章。
     *
     * @param id 文章 ID
     * @return 文章详情
     */
    @GetMapping("/{id}/edit")
    public Result<ArticleResponse> getEditableArticle(@PathVariable Long id) {
        return Result.success(restDtoMapper.toResponse(
            articleAppService.getArticleForEdit(id, AuthContext.getRequiredUserId(), AuthContext.getRole())
        ));
    }

    /**
     * 更新文章。
     *
     * @param id 文章 ID
     * @param request 更新请求
     * @return 文章详情
     */
    @PutMapping("/{id}")
    public Result<ArticleResponse> updateArticle(@PathVariable Long id,
                                                 @RequestBody @Valid CreateArticleRequest request) {
        return Result.success(restDtoMapper.toResponse(
            articleAppService.updateArticle(
                id,
                restDtoMapper.toCommand(request, AuthContext.getRequiredUserId()),
                AuthContext.getRequiredUserId(),
                AuthContext.getRole()
            )
        ));
    }

    /**
     * 删除文章。
     *
     * @param id 文章 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleAppService.deleteArticle(id, AuthContext.getRequiredUserId(), AuthContext.getRole());
        return Result.success();
    }
}
