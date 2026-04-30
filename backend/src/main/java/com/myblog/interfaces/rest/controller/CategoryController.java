package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ArticleDTO;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.application.query.ArticlePageQuery;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.application.service.ArticleAppService;
import com.myblog.application.service.CategoryAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.response.ArticleResponse;
import com.myblog.interfaces.rest.mapper.RestDtoMapper;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryAppService categoryAppService;
    private final AdminLogAppService adminLogAppService;
    private final ArticleAppService articleAppService;
    private final RestDtoMapper restDtoMapper;

    public CategoryController(CategoryAppService categoryAppService, AdminLogAppService adminLogAppService,
                              ArticleAppService articleAppService, RestDtoMapper restDtoMapper) {
        this.categoryAppService = categoryAppService;
        this.adminLogAppService = adminLogAppService;
        this.articleAppService = articleAppService;
        this.restDtoMapper = restDtoMapper;
    }

    /**
     * 校验管理员权限。
     */
    private void ensureAdmin() {
        if (AuthContext.getRequiredUserId() == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "仅管理员可管理分类");
        }
    }

    @GetMapping
    public Result<List<CategoryDTO>> getCategories(@RequestParam(required = false) Boolean enabled) {
        return Result.success(categoryAppService.getCategories(enabled));
    }

    @GetMapping("/{id}")
    public Result<CategoryDTO> getCategory(@PathVariable Long id) {
        return Result.success(categoryAppService.getCategory(id));
    }

    /**
     * 查询分类下的已发布文章。
     *
     * @param id 分类 ID
     * @param page 页码
     * @param pageSize 每页数量
     * @param sort 排序方式（latest/hot/featured）
     * @return 文章分页结果
     */
    @GetMapping("/{id}/articles")
    public Result<PageResult<ArticleResponse>> getCategoryArticles(@PathVariable Long id,
                                                                   @RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                                   @RequestParam(defaultValue = "latest") String sort) {
        CategoryDTO category = categoryAppService.getCategory(id);
        ArticlePageQuery query = new ArticlePageQuery(page, pageSize, null, category.getName(), null, sort);
        query.setCurrentUserId(AuthContext.getCurrentUserId());
        PageResult<ArticleDTO> pageResult = articleAppService.pagePublishedArticles(query);
        List<ArticleResponse> items = new ArrayList<ArticleResponse>();
        for (ArticleDTO item : pageResult.getItems()) {
            items.add(restDtoMapper.toResponse(item));
        }
        return Result.success(new PageResult<ArticleResponse>(
            items, pageResult.getPage(), pageResult.getPageSize(), pageResult.getTotal()
        ));
    }

    /**
     * 创建分类。
     *
     * @param request 创建参数
     * @param httpServletRequest HTTP 请求
     * @return 分类信息
     */
    @PostMapping
    public Result<CategoryDTO> createCategory(@RequestBody Map<String, Object> request,
                                              @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Integer sortOrder = request.get("sortOrder") != null ? (Integer) request.get("sortOrder") : 0;
        CategoryDTO categoryDTO = categoryAppService.createCategory(name, description, sortOrder);
        adminLogAppService.recordOperation(buildLogCommand(
            "CREATE_CATEGORY",
            "CATEGORY",
            categoryDTO.getId(),
            "创建分类 " + categoryDTO.getName(),
            null,
            toCategorySnapshot(categoryDTO),
            httpServletRequest
        ));
        return Result.success(categoryDTO);
    }

    /**
     * 更新分类。
     *
     * @param id 分类 ID
     * @param request 更新参数
     * @param httpServletRequest HTTP 请求
     * @return 分类信息
     */
    @PutMapping("/{id}")
    public Result<CategoryDTO> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> request,
                                              @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        CategoryDTO beforeCategory = categoryAppService.getCategory(id);
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Integer sortOrder = request.get("sortOrder") != null ? (Integer) request.get("sortOrder") : 0;
        Boolean enabled = request.get("enabled") != null ? (Boolean) request.get("enabled") : true;
        CategoryDTO categoryDTO = categoryAppService.updateCategory(id, name, description, sortOrder, enabled);
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_CATEGORY",
            "CATEGORY",
            categoryDTO.getId(),
            "更新分类 " + categoryDTO.getName() + "，enabled=" + categoryDTO.getEnabled(),
            toCategorySnapshot(beforeCategory),
            toCategorySnapshot(categoryDTO),
            httpServletRequest
        ));
        return Result.success(categoryDTO);
    }

    /**
     * 删除分类。
     *
     * @param id 分类 ID
     * @param httpServletRequest HTTP 请求
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> deleteCategory(@PathVariable Long id,
                                                      @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        CategoryDTO beforeCategory = categoryAppService.getCategory(id);
        categoryAppService.deleteCategory(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_CATEGORY",
            "CATEGORY",
            id,
            "删除分类 " + beforeCategory.getName(),
            toCategorySnapshot(beforeCategory),
            null,
            httpServletRequest
        ));
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", true);
        return Result.success(result);
    }

    /**
     * 解析请求 IP。
     *
     * @param request HTTP 请求
     * @return 请求 IP
     */
    private String resolveIpAddress(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            String[] parts = forwardedFor.split(",");
            return parts[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 构建管理员日志命令。
     *
     * @param operation 操作类型
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @param detail 操作详情
     * @param beforeSnapshot 变更前快照
     * @param afterSnapshot 变更后快照
     * @param request HTTP 请求
     * @return 日志命令
     */
    private RecordAdminLogCommand buildLogCommand(String operation, String targetType, Long targetId,
                                                  String detail, Object beforeSnapshot, Object afterSnapshot,
                                                  @Nullable HttpServletRequest request) {
        RecordAdminLogCommand command = new RecordAdminLogCommand();
        command.setAdminUserId(AuthContext.getRequiredUserId());
        command.setAdminUsername(AuthContext.getUsername());
        command.setOperation(operation);
        command.setTargetType(targetType);
        command.setTargetId(targetId);
        command.setDetail(detail);
        command.setRequestMethod(request == null ? null : request.getMethod());
        command.setRequestUri(request == null ? null : request.getRequestURI());
        command.setIpAddress(resolveIpAddress(request));
        command.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        command.setResultStatus("SUCCESS");
        command.setBeforeSnapshot(beforeSnapshot);
        command.setAfterSnapshot(afterSnapshot);
        return command;
    }

    /**
     * 转换分类快照。
     *
     * @param categoryDTO 分类 DTO
     * @return 快照数据
     */
    private Map<String, Object> toCategorySnapshot(CategoryDTO categoryDTO) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", categoryDTO.getId());
        snapshot.put("name", categoryDTO.getName());
        snapshot.put("description", categoryDTO.getDescription());
        snapshot.put("sortOrder", categoryDTO.getSortOrder());
        snapshot.put("enabled", categoryDTO.getEnabled());
        return snapshot;
    }
}
