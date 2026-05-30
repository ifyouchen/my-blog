package com.myblog.interfaces.rest.controller;

import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.application.dto.AnnouncementDTO;
import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.CategoryGroupDTO;
import com.myblog.application.dto.ColumnDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.dto.TopicDTO;
import com.myblog.application.service.AdminAppService;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.application.service.AnnouncementAppService;
import com.myblog.application.service.CategoryAppService;
import com.myblog.application.service.CategoryGroupAppService;
import com.myblog.application.service.ColumnAppService;
import com.myblog.application.service.SensitiveWordAppService;
import com.myblog.application.service.TagAppService;
import com.myblog.application.service.TopicAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import com.myblog.interfaces.rest.dto.request.AddColumnArticleRequest;
import com.myblog.interfaces.rest.dto.request.AdminBatchArticleRequest;
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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台控制器。
 * <p>
 * 提供管理员后台的核心 API，包括用户、文章、评论、分类、标签、
 * 专栏、专题、公告、敏感词等的管理操作，以及数据导出功能。
 * 所有接口均需要管理员权限，操作均记录管理员日志。
 * </p>
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAppService adminAppService;
    private final AdminLogAppService adminLogAppService;
    private final CategoryAppService categoryAppService;
    private final CategoryGroupAppService categoryGroupAppService;
    private final TagAppService tagAppService;
    private final ColumnAppService columnAppService;
    private final TopicAppService topicAppService;
    private final AnnouncementAppService announcementAppService;
    private final SensitiveWordAppService sensitiveWordAppService;

    public AdminController(AdminAppService adminAppService, AdminLogAppService adminLogAppService,
                           CategoryAppService categoryAppService, TagAppService tagAppService,
                           CategoryGroupAppService categoryGroupAppService,
                           ColumnAppService columnAppService, TopicAppService topicAppService,
                           AnnouncementAppService announcementAppService,
                           SensitiveWordAppService sensitiveWordAppService) {
        this.adminAppService = adminAppService;
        this.adminLogAppService = adminLogAppService;
        this.categoryAppService = categoryAppService;
        this.categoryGroupAppService = categoryGroupAppService;
        this.tagAppService = tagAppService;
        this.columnAppService = columnAppService;
        this.topicAppService = topicAppService;
        this.announcementAppService = announcementAppService;
        this.sensitiveWordAppService = sensitiveWordAppService;
    }

    /**
     * 校验管理员权限，未登录或非管理员时抛出异常。
     */
    private void ensureAdmin() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "仅管理员可访问后台");
        }
    }

    /**
     * 查询后台概览统计。
     *
     * @return 概览统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        ensureAdmin();
        return Result.success(adminAppService.getStats());
    }

    /**
     * 分页查询后台用户。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 状态筛选
     * @param keyword 关键字
     * @param role 角色筛选
     * @return 用户分页结果
     */
    @GetMapping("/users")
    public Result<PageResult<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        ensureAdmin();
        return Result.success(adminAppService.getUsers(page, pageSize, status, keyword, role));
    }

    /**
     * 分页查询后台文章。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 状态筛选
     * @param keyword 关键字
     * @param category 分类筛选
     * @return 文章分页结果
     */
    @GetMapping("/articles")
    public Result<PageResult<Map<String, Object>>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        ensureAdmin();
        return Result.success(adminAppService.getArticles(page, pageSize, status, keyword, category));
    }

    /**
     * 分页查询后台评论。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param articleId 文章 ID
     * @param status 评论状态
     * @param keyword 关键字
     * @return 评论分页结果
     */
    @GetMapping("/comments")
    public Result<PageResult<Map<String, Object>>> getComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        ensureAdmin();
        return Result.success(adminAppService.getComments(page, pageSize, articleId, status, keyword));
    }

    /**
     * 分页查询后台分类列表。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param enabled 启用状态
     * @return 分类分页结果
     */
    @GetMapping("/categories")
    public Result<PageResult<CategoryDTO>> getCategories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String keyword) {
        ensureAdmin();
        return Result.success(categoryAppService.getCategoryPage(page, pageSize, enabled, groupId, keyword));
    }

    /**
     * 分页查询后台标签列表。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param enabled 启用状态
     * @param keyword 关键字
     * @return 标签分页结果
     */
    @GetMapping("/tags")
    public Result<PageResult<TagDTO>> getTags(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String keyword) {
        ensureAdmin();
        return Result.success(tagAppService.getTagPage(page, pageSize, enabled, keyword));
    }

    /**
     * 创建分类（管理员后台）。
     *
     * @param request            创建参数
     * @param httpServletRequest HTTP 请求
     * @return 分类信息
     */
    @PostMapping("/categories")
    public Result<CategoryDTO> createCategory(@RequestBody Map<String, Object> request,
                                              @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Long groupId = parseLong(request.get("groupId"), null);
        Integer sortOrder = parseInteger(request.get("sortOrder"), 0);
        CategoryDTO categoryDTO = categoryAppService.createCategory(name, groupId, description, sortOrder);
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
     * 更新分类（管理员后台）。
     *
     * @param id                 分类 ID
     * @param request            更新参数
     * @param httpServletRequest HTTP 请求
     * @return 分类信息
     */
    @PutMapping("/categories/{id}")
    public Result<CategoryDTO> updateCategory(@PathVariable Long id,
                                              @RequestBody Map<String, Object> request,
                                              @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        CategoryDTO beforeCategory = categoryAppService.getCategory(id);
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Long groupId = parseLong(request.get("groupId"), null);
        Integer sortOrder = parseInteger(request.get("sortOrder"), 0);
        Boolean enabled = parseBoolean(request.get("enabled"), true);
        CategoryDTO categoryDTO = categoryAppService.updateCategory(id, name, groupId, description, sortOrder, enabled);
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
     * 删除分类（管理员后台）。
     *
     * @param id                 分类 ID
     * @param httpServletRequest HTTP 请求
     * @return 删除结果
     */
    @DeleteMapping("/categories/{id}")
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
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("deleted", true);
        return Result.success(result);
    }

    @GetMapping("/category-groups")
    public Result<List<CategoryGroupDTO>> getCategoryGroups(@RequestParam(required = false) Boolean enabled) {
        ensureAdmin();
        return Result.success(categoryGroupAppService.listGroups(enabled));
    }

    @PostMapping("/category-groups")
    public Result<CategoryGroupDTO> createCategoryGroup(@RequestBody Map<String, Object> request,
                                                        @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        CategoryGroupDTO groupDTO = categoryGroupAppService.createGroup(
            (String) request.get("name"),
            (String) request.get("description"),
            parseInteger(request.get("sortOrder"), 0)
        );
        adminLogAppService.recordOperation(buildLogCommand(
            "CREATE_CATEGORY_GROUP",
            "CATEGORY_GROUP",
            groupDTO.getId(),
            "创建分类组 " + groupDTO.getName(),
            null,
            toCategoryGroupSnapshot(groupDTO),
            httpServletRequest
        ));
        return Result.success(groupDTO);
    }

    @PutMapping("/category-groups/{id}")
    public Result<CategoryGroupDTO> updateCategoryGroup(@PathVariable Long id,
                                                        @RequestBody Map<String, Object> request,
                                                        @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        CategoryGroupDTO beforeGroup = categoryGroupAppService.getGroup(id);
        CategoryGroupDTO groupDTO = categoryGroupAppService.updateGroup(
            id,
            (String) request.get("name"),
            (String) request.get("description"),
            parseInteger(request.get("sortOrder"), 0),
            parseBoolean(request.get("enabled"), true)
        );
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_CATEGORY_GROUP",
            "CATEGORY_GROUP",
            groupDTO.getId(),
            "更新分类组 " + groupDTO.getName() + "，enabled=" + groupDTO.getEnabled(),
            toCategoryGroupSnapshot(beforeGroup),
            toCategoryGroupSnapshot(groupDTO),
            httpServletRequest
        ));
        return Result.success(groupDTO);
    }

    @DeleteMapping("/category-groups/{id}")
    public Result<Map<String, Object>> deleteCategoryGroup(@PathVariable Long id,
                                                           @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        CategoryGroupDTO beforeGroup = categoryGroupAppService.getGroup(id);
        categoryGroupAppService.deleteGroup(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_CATEGORY_GROUP",
            "CATEGORY_GROUP",
            id,
            "删除分类组 " + beforeGroup.getName(),
            toCategoryGroupSnapshot(beforeGroup),
            null,
            httpServletRequest
        ));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("deleted", true);
        return Result.success(result);
    }

    @GetMapping("/tag-groups")
    public Result<List<Map<String, Object>>> getTagGroups() {
        ensureAdmin();
        return Result.success(tagAppService.getTagGroups());
    }

    @PutMapping("/tag-groups")
    public Result<Map<String, Object>> renameTagGroup(@RequestBody Map<String, String> request) {
        ensureAdmin();
        String oldName = request.get("oldName");
        String newName = request.get("newName");
        tagAppService.renameGroup(oldName, newName);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("renamed", true);
        return Result.success(result);
    }

    @DeleteMapping("/tag-groups")
    public Result<Map<String, Object>> deleteTagGroup(@RequestBody Map<String, String> request) {
        ensureAdmin();
        String name = request.get("name");
        tagAppService.deleteGroup(name);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("deleted", true);
        return Result.success(result);
    }

    /**
     * 创建标签（管理员后台）。
     *
     * @param request            创建参数
     * @param httpServletRequest HTTP 请求
     * @return 标签信息
     */
    @PostMapping("/tags")
    public Result<TagDTO> createTag(@RequestBody Map<String, Object> request,
                                    @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        TagDTO tagDTO = tagAppService.createTag(name, description);
        adminLogAppService.recordOperation(buildLogCommand(
            "CREATE_TAG",
            "TAG",
            tagDTO.getId(),
            "创建标签 " + tagDTO.getName(),
            null,
            toTagSnapshot(tagDTO),
            httpServletRequest
        ));
        return Result.success(tagDTO);
    }

    /**
     * 更新标签（管理员后台）。
     *
     * @param id                 标签 ID
     * @param request            更新参数
     * @param httpServletRequest HTTP 请求
     * @return 标签信息
     */
    @PutMapping("/tags/{id}")
    public Result<TagDTO> updateTag(@PathVariable Long id,
                                    @RequestBody Map<String, Object> request,
                                    @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        TagDTO beforeTag = tagAppService.getTag(id);
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Boolean enabled = parseBoolean(request.get("enabled"), true);
        TagDTO tagDTO = tagAppService.updateTag(id, name, description, enabled);
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_TAG",
            "TAG",
            tagDTO.getId(),
            "更新标签 " + tagDTO.getName() + "，enabled=" + tagDTO.getEnabled(),
            toTagSnapshot(beforeTag),
            toTagSnapshot(tagDTO),
            httpServletRequest
        ));
        return Result.success(tagDTO);
    }

    /**
     * 删除标签（管理员后台）。
     *
     * @param id                 标签 ID
     * @param httpServletRequest HTTP 请求
     * @return 删除结果
     */
    @DeleteMapping("/tags/{id}")
    public Result<Map<String, Object>> deleteTag(@PathVariable Long id,
                                                 @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        TagDTO beforeTag = tagAppService.getTag(id);
        tagAppService.deleteTag(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_TAG",
            "TAG",
            id,
            "删除标签 " + beforeTag.getName(),
            toTagSnapshot(beforeTag),
            null,
            httpServletRequest
        ));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("deleted", true);
        return Result.success(result);
    }

    /**
     * 分页查询管理员操作日志。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param actionType 操作类型
     * @param resultStatus 结果状态
     * @param dateFrom 开始日期
     * @param dateTo 结束日期
     * @return 日志分页结果
     */
    @GetMapping("/logs")
    public Result<PageResult<Map<String, Object>>> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String resultStatus,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        ensureAdmin();
        return Result.success(adminLogAppService.getLogs(page, pageSize, actionType, resultStatus, dateFrom, dateTo));
    }

    /**
     * 更新用户状态。
     *
     * @param id 用户 ID
     * @param status 用户状态
     * @param request HTTP 请求
     * @return 更新结果
     */
    @PutMapping("/users/{id}/status")
    public Result<Map<String, Object>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.updateUserStatus(id, status));
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_USER_STATUS",
            "USER",
            id,
            "更新用户状态为 " + status,
            buildUserStatusBeforeSnapshot(result.getData()),
            buildUserStatusAfterSnapshot(result.getData()),
            request
        ));
        return result;
    }

    /**
     * 更新用户角色。
     *
     * @param id 用户 ID
     * @param role 用户角色
     * @param request HTTP 请求
     * @return 更新结果
     */
    @PutMapping("/users/{id}/role")
    public Result<Map<String, Object>> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(
            adminAppService.updateUserRole(id, role, AuthContext.getRequiredUserId()));
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_USER_ROLE",
            "USER",
            id,
            "更新用户角色为 " + role,
            buildUserRoleBeforeSnapshot(result.getData()),
            buildUserRoleAfterSnapshot(result.getData()),
            request
        ));
        return result;
    }

    /**
     * 推荐用户。
     *
     * @param id 用户 ID
     * @param request HTTP 请求
     * @return 推荐结果
     */
    @PostMapping("/users/{id}/recommend")
    public Result<Map<String, Object>> recommendUser(@PathVariable Long id,
                                                     @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.featureUser(id));
        adminLogAppService.recordOperation(buildLogCommand(
            "RECOMMEND_USER", "USER", id,
            "推荐用户 " + id, null, result.getData(), request));
        return result;
    }

    /**
     * 取消推荐用户。
     *
     * @param id 用户 ID
     * @param request HTTP 请求
     * @return 取消推荐结果
     */
    @PostMapping("/users/{id}/unrecommend")
    public Result<Map<String, Object>> unrecommendUser(@PathVariable Long id,
                                                       @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.unfeatureUser(id));
        adminLogAppService.recordOperation(buildLogCommand(
            "UNRECOMMEND_USER", "USER", id,
            "取消推荐用户 " + id, null, result.getData(), request));
        return result;
    }

    /**
     * 更新文章状态。
     *
     * @param id 文章 ID
     * @param status 文章状态
     * @param request HTTP 请求
     * @return 更新结果
     */
    @PutMapping("/articles/{id}/status")
    public Result<Map<String, Object>> updateArticleStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.updateArticleStatus(id, status));
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_ARTICLE_STATUS",
            "ARTICLE",
            id,
            "更新文章状态为 " + status,
            buildArticleStatusBeforeSnapshot(result.getData()),
            buildArticleStatusAfterSnapshot(result.getData()),
            request
        ));
        return result;
    }

    /**
     * 批量更新文章状态。
     *
     * @param requestPayload 批量请求
     * @param request HTTP 请求
     * @return 更新结果
     */
    @PostMapping("/articles/batch/status")
    public Result<Map<String, Object>> batchUpdateArticleStatus(
            @RequestBody AdminBatchArticleRequest requestPayload,
            @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(
            adminAppService.batchUpdateArticleStatus(requestPayload.getIds(), requestPayload.getStatus())
        );
        adminLogAppService.recordOperation(buildLogCommand(
            "BATCH_UPDATE_ARTICLE_STATUS",
            "ARTICLE",
            null,
            "批量更新文章状态为 " + requestPayload.getStatus() + "，数量 " + result.getData().get("processedCount"),
            null,
            result.getData(),
            request
        ));
        return result;
    }

    /**
     * 删除文章（管理员后台）。
     *
     * @param id      文章 ID
     * @param request HTTP 请求
     * @return 删除结果
     */
    @DeleteMapping("/articles/{id}")
    public Result<Map<String, Object>> deleteArticle(@PathVariable Long id,
                                                     @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.deleteArticle(id));
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_ARTICLE",
            "ARTICLE",
            id,
            "删除文章 " + result.getData().get("title"),
            buildArticleStatusBeforeSnapshot(result.getData()),
            buildArticleStatusAfterSnapshot(result.getData()),
            request
        ));
        return result;
    }

    /**
     * 批量删除文章。
     *
     * @param requestPayload 批量请求
     * @param request HTTP 请求
     * @return 删除结果
     */
    @PostMapping("/articles/batch/delete")
    public Result<Map<String, Object>> batchDeleteArticles(@RequestBody AdminBatchArticleRequest requestPayload,
                                                           @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.batchDeleteArticles(requestPayload.getIds()));
        adminLogAppService.recordOperation(buildLogCommand(
            "BATCH_DELETE_ARTICLE",
            "ARTICLE",
            null,
            "批量删除文章，数量 " + result.getData().get("processedCount"),
            null,
            result.getData(),
            request
        ));
        return result;
    }

    /**
     * 删除评论。
     *
     * @param id 评论 ID
     * @param request HTTP 请求
     * @return 删除结果
     */
    @DeleteMapping("/comments/{id}")
    public Result<Map<String, Object>> deleteComment(@PathVariable Long id, @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.deleteComment(id));
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_COMMENT",
            "COMMENT",
            id,
            "删除评论 " + id,
            result.getData(),
            null,
            request
        ));
        return result;
    }

    /**
     * 审核通过评论。
     *
     * @param id 评论 ID
     * @param request HTTP 请求
     * @return 审核结果
     */
    @PutMapping("/comments/{id}/approve")
    public Result<Map<String, Object>> approveComment(@PathVariable Long id, @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.approveComment(id));
        adminLogAppService.recordOperation(buildLogCommand(
            "APPROVE_COMMENT",
            "COMMENT",
            id,
            "审核通过评论 " + id,
            result.getData(),
            null,
            request
        ));
        return result;
    }

    /**
     * 拒绝评论。
     *
     * @param id 评论 ID
     * @param request HTTP 请求
     * @return 操作结果
     */
    @PutMapping("/comments/{id}/reject")
    public Result<Map<String, Object>> rejectComment(@PathVariable Long id, @Nullable HttpServletRequest request) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.rejectComment(id));
        adminLogAppService.recordOperation(buildLogCommand(
            "REJECT_COMMENT",
            "COMMENT",
            id,
            "拒绝评论 " + id,
            result.getData(),
            null,
            request
        ));
        return result;
    }

    // ==================== 专栏管理 ====================

    /**
     * 分页查询专栏列表（管理后台）。
     */
    @GetMapping("/columns")
    public Result<PageResult<ColumnDTO>> getColumns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        ensureAdmin();
        return Result.success(columnAppService.adminPageColumns(keyword, status, page, pageSize));
    }

    /**
     * 创建专栏。
     */
    @PostMapping("/columns")
    public Result<ColumnDTO> createColumn(@RequestBody Map<String, Object> request,
                                          @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        Long authorId = parseLong(request.get("authorId"), null);
        String title = (String) request.get("title");
        String summary = (String) request.get("summary");
        String coverUrl = (String) request.get("coverUrl");
        Integer sortOrder = parseInteger(request.get("sortOrder"), 0);
        ColumnDTO dto = columnAppService.adminCreateColumn(authorId, title, summary, coverUrl, sortOrder);
        adminLogAppService.recordOperation(buildLogCommand(
            "CREATE_COLUMN", "COLUMN", dto.getId(),
            "创建专栏 " + dto.getTitle(), null, toColumnSnapshot(dto), httpServletRequest));
        return Result.success(dto);
    }

    /**
     * 更新专栏。
     */
    @PutMapping("/columns/{id}")
    public Result<ColumnDTO> updateColumn(@PathVariable Long id,
                                          @RequestBody Map<String, Object> request,
                                          @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        ColumnDTO before = columnAppService.adminPageColumns(null, null, 1, Integer.MAX_VALUE)
            .getItems().stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
        String title = (String) request.get("title");
        String summary = (String) request.get("summary");
        String coverUrl = (String) request.get("coverUrl");
        Integer sortOrder = parseInteger(request.get("sortOrder"), null);
        String status = (String) request.get("status");
        ColumnDTO dto = columnAppService.adminUpdateColumn(id, title, summary, coverUrl, sortOrder, status);
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_COLUMN", "COLUMN", id,
            "更新专栏 " + dto.getTitle(), toColumnSnapshot(before), toColumnSnapshot(dto), httpServletRequest));
        return Result.success(dto);
    }

    /**
     * 删除专栏（软删除）。
     */
    @DeleteMapping("/columns/{id}")
    public Result<Map<String, Object>> deleteColumn(@PathVariable Long id,
                                                    @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        columnAppService.adminDeleteColumn(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_COLUMN", "COLUMN", id, "删除专栏 " + id, null, null, httpServletRequest));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("deleted", true);
        return Result.success(result);
    }

    /**
     * 向专栏添加文章。
     */
    @PostMapping("/columns/{id}/articles")
    public Result<Map<String, Object>> addColumnArticle(@PathVariable Long id,
                                                         @RequestBody AddColumnArticleRequest request,
                                                         @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        columnAppService.addColumnArticle(id, request.getArticleId(), request.getSortOrder());
        adminLogAppService.recordOperation(buildLogCommand(
            "ADD_COLUMN_ARTICLE", "COLUMN_ARTICLE", id,
            "向专栏 " + id + " 添加文章 " + request.getArticleId(), null, null, httpServletRequest));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("bound", true);
        return Result.success(result);
    }

    /**
     * 从专栏移除文章。
     */
    @DeleteMapping("/columns/{columnId}/articles/{articleId}")
    public Result<Map<String, Object>> removeColumnArticle(@PathVariable Long columnId,
                                                           @PathVariable Long articleId,
                                                           @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        columnAppService.removeColumnArticle(columnId, articleId);
        adminLogAppService.recordOperation(buildLogCommand(
            "REMOVE_COLUMN_ARTICLE", "COLUMN_ARTICLE", columnId,
            "从专栏 " + columnId + " 移除文章 " + articleId, null, null, httpServletRequest));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("removed", true);
        return Result.success(result);
    }

    // ==================== 专题管理 ====================

    /**
     * 分页查询专题列表（管理后台）。
     */
    @GetMapping("/topics")
    public Result<PageResult<TopicDTO>> getTopics(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        ensureAdmin();
        return Result.success(topicAppService.adminPageTopics(keyword, page, pageSize));
    }

    /**
     * 创建专题。
     */
    @PostMapping("/topics")
    public Result<TopicDTO> createTopic(@RequestBody Map<String, Object> request,
                                        @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        String title = (String) request.get("title");
        String summary = (String) request.get("summary");
        String coverUrl = (String) request.get("coverUrl");
        Integer sortOrder = parseInteger(request.get("sortOrder"), 0);
        String difficulty = (String) request.get("difficulty");
        TopicDTO dto = topicAppService.adminCreateTopic(title, summary, coverUrl, sortOrder, difficulty);
        adminLogAppService.recordOperation(buildLogCommand(
            "CREATE_TOPIC", "TOPIC", dto.getId(),
            "创建专题 " + dto.getTitle(), null, toTopicSnapshot(dto), httpServletRequest));
        return Result.success(dto);
    }

    /**
     * 更新专题。
     */
    @PutMapping("/topics/{id}")
    public Result<TopicDTO> updateTopic(@PathVariable Long id,
                                         @RequestBody Map<String, Object> request,
                                         @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        String title = (String) request.get("title");
        String summary = (String) request.get("summary");
        String coverUrl = (String) request.get("coverUrl");
        Integer sortOrder = parseInteger(request.get("sortOrder"), null);
        String status = (String) request.get("status");
        String difficulty = (String) request.get("difficulty");
        TopicDTO dto = topicAppService.adminUpdateTopic(id, title, summary, coverUrl, sortOrder, status, difficulty);
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_TOPIC", "TOPIC", id,
            "更新专题 " + dto.getTitle(), null, toTopicSnapshot(dto), httpServletRequest));
        return Result.success(dto);
    }

    /**
     * 删除专题（软删除）。
     */
    @DeleteMapping("/topics/{id}")
    public Result<Map<String, Object>> deleteTopic(@PathVariable Long id,
                                                    @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        topicAppService.adminDeleteTopic(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_TOPIC", "TOPIC", id, "删除专题 " + id, null, null, httpServletRequest));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("deleted", true);
        return Result.success(result);
    }

    /**
     * 专题绑定文章。
     */
    @PostMapping("/topics/{id}/articles")
    public Result<Map<String, Object>> bindTopicArticle(@PathVariable Long id,
                                                         @RequestBody Map<String, Object> request,
                                                         @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        Long articleId = parseLong(request.get("articleId"), null);
        Integer sortOrder = parseInteger(request.get("sortOrder"), 0);
        topicAppService.adminBindArticle(id, articleId, sortOrder);
        adminLogAppService.recordOperation(buildLogCommand(
            "BIND_TOPIC_ARTICLE", "TOPIC_ARTICLE", id,
            "专题 " + id + " 绑定文章 " + articleId, null, null, httpServletRequest));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("bound", true);
        return Result.success(result);
    }

    /**
     * 专题解绑文章。
     */
    @DeleteMapping("/topics/{topicId}/articles/{articleId}")
    public Result<Map<String, Object>> unbindTopicArticle(@PathVariable Long topicId,
                                                           @PathVariable Long articleId,
                                                           @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        topicAppService.adminUnbindArticle(topicId, articleId);
        adminLogAppService.recordOperation(buildLogCommand(
            "UNBIND_TOPIC_ARTICLE", "TOPIC_ARTICLE", topicId,
            "专题 " + topicId + " 解绑文章 " + articleId, null, null, httpServletRequest));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("removed", true);
        return Result.success(result);
    }

    // ==================== 文章精选 ====================

    /**
     * 设为精选。
     */
    @PostMapping("/articles/{id}/feature")
    public Result<Map<String, Object>> featureArticle(@PathVariable Long id,
                                                      @RequestBody(required = false) Map<String, Object> body,
                                                      @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        Integer weight = null;
        if (body != null && body.get("weight") instanceof Number) {
            weight = ((Number) body.get("weight")).intValue();
        }
        Result<Map<String, Object>> result = Result.success(adminAppService.featureArticle(id, weight));
        adminLogAppService.recordOperation(buildLogCommand(
            "FEATURE_ARTICLE", "ARTICLE", id,
            "精选文章 " + id + " weight=" + weight, null, result.getData(), httpServletRequest));
        return result;
    }

    /**
     * 取消精选。
     */
    @PostMapping("/articles/{id}/unfeature")
    public Result<Map<String, Object>> unfeatureArticle(@PathVariable Long id,
                                                        @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        Result<Map<String, Object>> result = Result.success(adminAppService.unfeatureArticle(id));
        adminLogAppService.recordOperation(buildLogCommand(
            "UNFEATURE_ARTICLE", "ARTICLE", id,
            "取消精选文章 " + id, null, result.getData(), httpServletRequest));
        return result;
    }

    // ==================== 公告管理 ====================

    /**
     * 分页查询公告列表（管理后台）。
     */
    @GetMapping("/announcements")
    public Result<PageResult<AnnouncementDTO>> getAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        ensureAdmin();
        return Result.success(announcementAppService.pageAll(page, pageSize));
    }

    /**
     * 创建公告。
     */
    @PostMapping("/announcements")
    public Result<AnnouncementDTO> createAnnouncement(@RequestBody Map<String, Object> request,
                                                      @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        AnnouncementDTO dto = announcementAppService.create(request);
        adminLogAppService.recordOperation(buildLogCommand(
            "CREATE_ANNOUNCEMENT", "ANNOUNCEMENT", dto.getId(),
            "创建公告 " + dto.getTitle(), null, toAnnouncementSnapshot(dto), httpServletRequest));
        return Result.success(dto);
    }

    /**
     * 更新公告。
     */
    @PutMapping("/announcements/{id}")
    public Result<AnnouncementDTO> updateAnnouncement(@PathVariable Long id,
                                                      @RequestBody Map<String, Object> request,
                                                      @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        AnnouncementDTO dto = announcementAppService.update(id, request);
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_ANNOUNCEMENT", "ANNOUNCEMENT", id,
            "更新公告 " + dto.getTitle(), null, toAnnouncementSnapshot(dto), httpServletRequest));
        return Result.success(dto);
    }

    /**
     * 删除公告（软删除）。
     */
    @DeleteMapping("/announcements/{id}")
    public Result<Map<String, Object>> deleteAnnouncement(@PathVariable Long id,
                                                          @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        announcementAppService.delete(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_ANNOUNCEMENT", "ANNOUNCEMENT", id,
            "删除公告 " + id, null, null, httpServletRequest));
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("deleted", true);
        return Result.success(result);
    }

    /**
     * 发布公告。
     */
    @PostMapping("/announcements/{id}/publish")
    public Result<AnnouncementDTO> publishAnnouncement(@PathVariable Long id,
                                                       @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        AnnouncementDTO dto = announcementAppService.publish(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "PUBLISH_ANNOUNCEMENT", "ANNOUNCEMENT", id,
            "发布公告 " + dto.getTitle(), null, toAnnouncementSnapshot(dto), httpServletRequest));
        return Result.success(dto);
    }

    /**
     * 撤回公告。
     */
    @PostMapping("/announcements/{id}/unpublish")
    public Result<AnnouncementDTO> unpublishAnnouncement(@PathVariable Long id,
                                                         @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        AnnouncementDTO dto = announcementAppService.unpublish(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "UNPUBLISH_ANNOUNCEMENT", "ANNOUNCEMENT", id,
            "撤回公告 " + dto.getTitle(), null, toAnnouncementSnapshot(dto), httpServletRequest));
        return Result.success(dto);
    }
    // ==================== 敏感词管理 ====================

    /**
     * 分页查询敏感词列表。
     */
    @GetMapping("/sensitive-words")
    public Result<PageResult<Map<String, Object>>> getSensitiveWords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        ensureAdmin();
        return Result.success(sensitiveWordAppService.pageList(keyword, category, page, pageSize));
    }

    /**
     * 创建敏感词。
     */
    @PostMapping("/sensitive-words")
    public Result<Map<String, Object>> createSensitiveWord(@RequestBody Map<String, Object> request,
                                                           @Nullable HttpServletRequest httpReq) {
        ensureAdmin();
        String word = (String) request.get("word");
        String category = (String) request.get("category");
        String level = (String) request.get("level");
        Map<String, Object> result = sensitiveWordAppService.create(word, category, level);
        adminLogAppService.recordOperation(buildLogCommand(
            "CREATE_SENSITIVE_WORD", "SENSITIVE_WORD", (Long) result.get("id"),
            "创建敏感词 " + word, null, result, httpReq));
        return Result.success(result);
    }

    /**
     * 更新敏感词。
     */
    @PutMapping("/sensitive-words/{id}")
    public Result<Map<String, Object>> updateSensitiveWord(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> request,
                                                           @Nullable HttpServletRequest httpReq) {
        ensureAdmin();
        String word = (String) request.get("word");
        String category = (String) request.get("category");
        String level = (String) request.get("level");
        Map<String, Object> result = sensitiveWordAppService.update(id, word, category, level);
        adminLogAppService.recordOperation(buildLogCommand(
            "UPDATE_SENSITIVE_WORD", "SENSITIVE_WORD", id,
            "更新敏感词 " + id, null, result, httpReq));
        return Result.success(result);
    }

    /**
     * 删除敏感词（软删除）。
     */
    @DeleteMapping("/sensitive-words/{id}")
    public Result<Map<String, Object>> deleteSensitiveWord(@PathVariable Long id,
                                                           @Nullable HttpServletRequest httpReq) {
        ensureAdmin();
        sensitiveWordAppService.delete(id);
        adminLogAppService.recordOperation(buildLogCommand(
            "DELETE_SENSITIVE_WORD", "SENSITIVE_WORD", id,
            "删除敏感词 " + id, null, null, httpReq));
        Map<String, Object> r = new HashMap<String, Object>();
        r.put("deleted", true);
        return Result.success(r);
    }

    /**
     * 构建公告快照。
     *
     * @param dto 公告 DTO
     * @return 快照数据
     */
    private Map<String, Object> toAnnouncementSnapshot(AnnouncementDTO dto) {
        if (dto == null) {
            return null;
        }
        Map<String, Object> s = new LinkedHashMap<String, Object>();
        s.put("id", dto.getId());
        s.put("title", dto.getTitle());
        s.put("published", dto.getPublished());
        s.put("target", dto.getTarget());
        return s;
    }

    /**
     * 构建专栏快照。
     *
     * @param dto 专栏 DTO
     * @return 快照数据
     */
    private Map<String, Object> toColumnSnapshot(ColumnDTO dto) {
        if (dto == null) {
            return null;
        }
        Map<String, Object> s = new LinkedHashMap<String, Object>();
        s.put("id", dto.getId());
        s.put("title", dto.getTitle());
        s.put("status", dto.getStatus());
        s.put("sortOrder", dto.getSortOrder());
        return s;
    }

    /**
     * 构建专题快照。
     *
     * @param dto 专题 DTO
     * @return 快照数据
     */
    private Map<String, Object> toTopicSnapshot(TopicDTO dto) {
        if (dto == null) {
            return null;
        }
        Map<String, Object> s = new LinkedHashMap<String, Object>();
        s.put("id", dto.getId());
        s.put("title", dto.getTitle());
        s.put("status", dto.getStatus());
        s.put("sortOrder", dto.getSortOrder());
        return s;
    }

    /**
     * 将请求参数转为 Long，转换失败时返回默认値。
     *
     * @param value    原始参数值
     * @param fallback 默认値
     * @return Long 类型结果
     */
    private Long parseLong(Object value, Long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Long.parseLong(((String) value).trim());
        }
        return fallback;
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
     * 构建用户状态变更前快照。
     *
     * @param result 业务结果
     * @return 变更前快照
     */
    private Map<String, Object> buildUserStatusBeforeSnapshot(Map<String, Object> result) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", result.get("id"));
        snapshot.put("username", result.get("username"));
        snapshot.put("status", result.get("previousStatus"));
        return snapshot;
    }

    /**
     * 构建用户状态变更后快照。
     *
     * @param result 业务结果
     * @return 变更后快照
     */
    private Map<String, Object> buildUserStatusAfterSnapshot(Map<String, Object> result) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", result.get("id"));
        snapshot.put("username", result.get("username"));
        snapshot.put("status", result.get("status"));
        return snapshot;
    }

    /**
     * 构建用户角色变更前快照。
     *
     * @param result 业务结果
     * @return 变更前快照
     */
    private Map<String, Object> buildUserRoleBeforeSnapshot(Map<String, Object> result) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", result.get("id"));
        snapshot.put("username", result.get("username"));
        snapshot.put("role", result.get("previousRole"));
        return snapshot;
    }

    /**
     * 构建用户角色变更后快照。
     *
     * @param result 业务结果
     * @return 变更后快照
     */
    private Map<String, Object> buildUserRoleAfterSnapshot(Map<String, Object> result) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", result.get("id"));
        snapshot.put("username", result.get("username"));
        snapshot.put("role", result.get("role"));
        return snapshot;
    }

    /**
     * 构建文章状态变更前快照。
     *
     * @param result 业务结果
     * @return 变更前快照
     */
    private Map<String, Object> buildArticleStatusBeforeSnapshot(Map<String, Object> result) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", result.get("id"));
        snapshot.put("title", result.get("title"));
        snapshot.put("status", result.get("previousStatus"));
        return snapshot;
    }

    /**
     * 构建文章状态变更后快照。
     *
     * @param result 业务结果
     * @return 变更后快照
     */
    private Map<String, Object> buildArticleStatusAfterSnapshot(Map<String, Object> result) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", result.get("id"));
        snapshot.put("title", result.get("title"));
        snapshot.put("status", result.get("status"));
        return snapshot;
    }

    /**
     * 构建分类快照。
     *
     * @param categoryDTO 分类 DTO
     * @return 快照数据
     */
    private Map<String, Object> toCategorySnapshot(CategoryDTO categoryDTO) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", categoryDTO.getId());
        snapshot.put("name", categoryDTO.getName());
        snapshot.put("groupId", categoryDTO.getGroupId());
        snapshot.put("groupName", categoryDTO.getGroupName());
        snapshot.put("description", categoryDTO.getDescription());
        snapshot.put("sortOrder", categoryDTO.getSortOrder());
        snapshot.put("enabled", categoryDTO.getEnabled());
        return snapshot;
    }

    /**
     * 构建分类组快照。
     *
     * @param groupDTO 分类组 DTO
     * @return 快照数据
     */
    private Map<String, Object> toCategoryGroupSnapshot(CategoryGroupDTO groupDTO) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", groupDTO.getId());
        snapshot.put("name", groupDTO.getName());
        snapshot.put("description", groupDTO.getDescription());
        snapshot.put("sortOrder", groupDTO.getSortOrder());
        snapshot.put("enabled", groupDTO.getEnabled());
        snapshot.put("categoryCount", groupDTO.getCategoryCount());
        return snapshot;
    }

    /**
     * 构建标签快照。
     *
     * @param tagDTO 标签 DTO
     * @return 快照数据
     */
    private Map<String, Object> toTagSnapshot(TagDTO tagDTO) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", tagDTO.getId());
        snapshot.put("name", tagDTO.getName());
        snapshot.put("description", tagDTO.getDescription());
        snapshot.put("groupName", tagDTO.getGroupName());
        snapshot.put("enabled", tagDTO.getEnabled());
        return snapshot;
    }

    /**
     * 将请求参数转为 Integer，转换失败时返回默认値。
     *
     * @param value    原始参数值
     * @param fallback 默认値
     * @return Integer 类型结果
     */
    private Integer parseInteger(Object value, Integer fallback) {
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Integer.valueOf(Integer.parseInt(((String) value).trim()));
        }
        return fallback;
    }

    /**
     * 将请求参数转为 Boolean，转换失败时返回默认値。
     *
     * @param value    原始参数值
     * @param fallback 默认値
     * @return Boolean 类型结果
     */
    private Boolean parseBoolean(Object value, Boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return Boolean.valueOf(((String) value).trim());
        }
        return fallback;
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

    // ==================== 数据导出 ====================

    /**
     * 导出文章列表 CSV。
     */
    @GetMapping("/export/articles")
    public void exportArticles(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            HttpServletResponse response) throws IOException {
        ensureAdmin();
        String filename = "articles-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        byte[] data = adminAppService.exportArticlesCsv(status, keyword, category);
        // Write BOM for Excel compatibility
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    /**
     * 导出用户列表 CSV。
     */
    @GetMapping("/export/users")
    public void exportUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            HttpServletResponse response) throws IOException {
        ensureAdmin();
        String filename = "users-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        byte[] data = adminAppService.exportUsersCsv(status, keyword, role);
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }
}
