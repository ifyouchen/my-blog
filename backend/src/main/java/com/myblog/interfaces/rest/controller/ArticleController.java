package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.ArticleRecommendationSectionDTO;
import com.myblog.application.dto.ArticleRecommendationsDTO;
import com.myblog.application.dto.ArticlePublishValidationDTO;
import com.myblog.application.dto.RecommendationApplicationDTO;
import com.myblog.application.dto.ArticleVersionDTO;
import com.myblog.application.query.ArticlePageQuery;
import com.myblog.application.service.ArticleAppService;
import com.myblog.application.service.RecommendationAppService;
import com.myblog.application.service.RecommendationApplicationAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.CreateArticleRequest;
import com.myblog.interfaces.rest.dto.request.UpdateArticleStatusRequest;
import com.myblog.interfaces.rest.dto.request.UpdateArticleUnlockRuleRequest;
import com.myblog.interfaces.rest.dto.response.ArticleRecommendationSectionResponse;
import com.myblog.interfaces.rest.dto.response.ArticleRecommendationsResponse;
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
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

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
    private final RecommendationAppService recommendationAppService;
    private final RecommendationApplicationAppService recommendationApplicationAppService;
    private final RestDtoMapper restDtoMapper;

    /**
     * 创建文章接口。
     *
     * @param articleAppService 文章应用服务
     * @param restDtoMapper REST DTO 转换器
     */
    public ArticleController(ArticleAppService articleAppService,
                             RecommendationAppService recommendationAppService,
                             RecommendationApplicationAppService recommendationApplicationAppService,
                             RestDtoMapper restDtoMapper) {
        this.articleAppService = articleAppService;
        this.recommendationAppService = recommendationAppService;
        this.recommendationApplicationAppService = recommendationApplicationAppService;
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
     * @param authorKeyword 作者关键字
     * @param dateFrom 起始日期
     * @param dateTo 结束日期
     * @param followingOnly 仅看已关注作者
     * @return 文章分页响应
     */
    @GetMapping
    public Result<PageResult<ArticleResponse>> pageArticles(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int pageSize,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String category,
                                                            @RequestParam(required = false) String tag,
                                                            @RequestParam(defaultValue = "latest") String sort,
                                                            @RequestParam(required = false) String authorKeyword,
                                                            @RequestParam(required = false) String dateFrom,
                                                            @RequestParam(required = false) String dateTo,
                                                            @RequestParam(defaultValue = "false")
                                                            boolean followingOnly) {
        ArticlePageQuery query = new ArticlePageQuery(page, pageSize, keyword, category, tag, sort);
        query.setAuthorKeyword(authorKeyword);
        query.setDateFrom(dateFrom);
        query.setDateTo(dateTo);
        query.setFollowingOnly(followingOnly);
        query.setCurrentUserId(AuthContext.getCurrentUserId());
        PageResult<ArticleDTO> pageResult = articleAppService.pagePublishedArticles(query);
        List<ArticleResponse> items = new ArrayList<ArticleResponse>();
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

    /**
     * 获取文章详情。
     *
     * @param id 文章 ID
     * @return 文章详情
     */
    @GetMapping("/{id}")
    public Result<ArticleResponse> getArticle(@PathVariable Long id) {
        return Result.success(restDtoMapper.toPublicResponse(
            articleAppService.getArticleDetail(id, AuthContext.getCurrentUserId(), AuthContext.getRole())
        ));
    }

    /**
     * 获取相关文章（同分类推荐，排除自身）。
     *
     * @param id 文章 ID
     * @param limit 返回数量（默认 5，最多 20）
     * @return 相关文章列表
     */
    @GetMapping("/{id}/related")
    public Result<List<ArticleResponse>> getRelatedArticles(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "5") int limit) {
        List<ArticleDTO> dtos = articleAppService.getRelatedArticles(id, limit);
        List<ArticleResponse> items = new ArrayList<ArticleResponse>();
        for (ArticleDTO dto : dtos) {
            items.add(restDtoMapper.toPublicResponse(dto));
        }
        return Result.success(items);
    }

    /**
     * 获取文章推荐分组。
     *
     * @param id 文章 ID
     * @param limit 推荐总数上限
     * @return 推荐分组
     */
    @GetMapping("/{id}/recommendations")
    public Result<ArticleRecommendationsResponse> getArticleRecommendations(@PathVariable Long id,
                                                                            @RequestParam(defaultValue = "12")
                                                                            int limit) {
        return Result.success(toRecommendationsResponse(
            recommendationAppService.getArticleRecommendations(id, limit)
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
     * 发布前校验文章内容。
     *
     * @param request 当前编辑内容
     * @return 校验结果
     */
    @PostMapping("/validate")
    public Result<ArticlePublishValidationDTO> validateArticle(@RequestBody CreateArticleRequest request) {
        return Result.success(articleAppService.validateDraftForPublish(restDtoMapper.toCommand(request, null)));
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
     * 更新文章状态。
     *
     * @param id 文章 ID
     * @param request 状态更新请求
     * @return 更新后的文章详情
     */
    @PutMapping("/{id}/status")
    public Result<ArticleResponse> updateArticleStatus(@PathVariable Long id,
                                                       @RequestBody UpdateArticleStatusRequest request) {
        return Result.success(restDtoMapper.toResponse(
            articleAppService.updateArticleStatus(
                id,
                request.getStatus(),
                AuthContext.getRequiredUserId(),
                AuthContext.getRole()
            )
        ));
    }

    /**
     * 更新文章阅读权限与积分解锁规则。
     *
     * @param id 文章 ID
     * @param request 解锁规则更新请求
     * @return 更新后的文章详情
     */
    @PutMapping("/{id}/unlock-rule")
    public Result<ArticleResponse> updateArticleUnlockRule(@PathVariable Long id,
                                                           @RequestBody @Valid
                                                           UpdateArticleUnlockRuleRequest request) {
        boolean needUnlock = Boolean.TRUE.equals(request.getNeedUnlock());
        int unlockPointPrice = request.getUnlockPointPrice() == null ? 0 : request.getUnlockPointPrice();
        return Result.success(restDtoMapper.toResponse(
            articleAppService.updateArticleUnlockRule(
                id,
                needUnlock,
                unlockPointPrice,
                AuthContext.getRequiredUserId(),
                AuthContext.getRole()
            )
        ));
    }

    /**
     * 提交首页推荐申请。
     *
     * @param id 文章 ID
     * @return 推荐申请结果
     */
    @PostMapping("/{id}/homepage-recommendation-apply")
    public Result<RecommendationApplicationDTO> applyHomepageRecommendation(@PathVariable Long id) {
        return Result.success(recommendationApplicationAppService.apply(id, AuthContext.getRequiredUserId()));
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

    // ── 版本历史 ──────────────────────────────────────────────────────────

    /**
     * 获取文章版本列表。
     *
     * @param id 文章 ID
     * @return 版本列表（不含正文）
     */
    @GetMapping("/{id}/versions")
    public Result<List<ArticleVersionDTO>> listVersions(@PathVariable Long id) {
        return Result.success(
            articleAppService.listVersions(id, AuthContext.getRequiredUserId(), AuthContext.getRole())
        );
    }

    /**
     * 获取指定版本详情（含正文）。
     *
     * @param id        文章 ID
     * @param versionNo 版本号
     * @return 版本详情
     */
    @GetMapping("/{id}/versions/{versionNo}")
    public Result<ArticleVersionDTO> getVersion(@PathVariable Long id,
                                                @PathVariable Integer versionNo) {
        return Result.success(
            articleAppService.getVersion(id, versionNo, AuthContext.getRequiredUserId(), AuthContext.getRole())
        );
    }

    /**
     * 恢复指定版本为新草稿。
     *
     * @param id        文章 ID
     * @param versionNo 版本号
     * @return 恢复后的草稿文章
     */
    @PostMapping("/{id}/versions/{versionNo}/restore")
    public Result<ArticleResponse> restoreVersion(@PathVariable Long id,
                                                  @PathVariable Integer versionNo) {
        return Result.success(restDtoMapper.toResponse(
            articleAppService.restoreVersion(id, versionNo, AuthContext.getRequiredUserId(), AuthContext.getRole())
        ));
    }

    /**
     * 导出当前用户的全部文章为 ZIP（每篇一个 Markdown 文件）。
     */
    @GetMapping("/me/export")
    public void exportMyArticles(HttpServletResponse response) throws IOException {
        Long userId = AuthContext.getRequiredUserId();
        String filename = "my-articles-"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        articleAppService.exportMyArticlesZip(userId, response.getOutputStream());
        response.getOutputStream().flush();
    }

    /**
     * 获取文章上下篇。
     *
     * @param id 当前文章 ID
     * @return prev/next ArticleResponse
     */
    @GetMapping("/{id}/neighbors")
    public Result<java.util.Map<String, ArticleResponse>> getNeighbors(@PathVariable Long id) {
        java.util.Map<String, com.myblog.application.dto.ArticleDTO> neighbors =
            articleAppService.getArticleNeighbors(id);
        java.util.Map<String, ArticleResponse> resp = new java.util.LinkedHashMap<>();
        resp.put("prev", neighbors.get("prev") == null
            ? null
            : restDtoMapper.toPublicResponse(neighbors.get("prev")));
        resp.put("next", neighbors.get("next") == null
            ? null
            : restDtoMapper.toPublicResponse(neighbors.get("next")));
        return Result.success(resp);
    }

    /**
     * 将文章推荐 DTO 转换为响应对象。
     *
     * @param dto 文章推荐 DTO
     * @return 文章推荐响应
     */
    private ArticleRecommendationsResponse toRecommendationsResponse(ArticleRecommendationsDTO dto) {
        ArticleRecommendationsResponse response = new ArticleRecommendationsResponse();
        List<ArticleRecommendationSectionResponse> sections =
            new ArrayList<ArticleRecommendationSectionResponse>();
        for (ArticleRecommendationSectionDTO section : dto.getSections()) {
            ArticleRecommendationSectionResponse sectionResponse = new ArticleRecommendationSectionResponse();
            sectionResponse.setKey(section.getKey());
            sectionResponse.setTitle(section.getTitle());
            List<ArticleResponse> items = new ArrayList<ArticleResponse>();
            for (ArticleDTO item : section.getItems()) {
                items.add(restDtoMapper.toPublicResponse(item));
            }
            sectionResponse.setItems(items);
            sections.add(sectionResponse);
        }
        response.setSections(sections);
        return response;
    }
}
