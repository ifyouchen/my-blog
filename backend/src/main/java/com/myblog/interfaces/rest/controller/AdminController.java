package com.myblog.interfaces.rest.controller;

import com.myblog.application.service.AdminAppService;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAppService adminAppService;
    private final AdminLogAppService adminLogAppService;

    public AdminController(AdminAppService adminAppService, AdminLogAppService adminLogAppService) {
        this.adminAppService = adminAppService;
        this.adminLogAppService = adminLogAppService;
    }

    private void ensureAdmin() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "仅管理员可访问后台");
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        ensureAdmin();
        return Result.success(adminAppService.getStats());
    }

    @GetMapping("/users")
    public Result<PageResult<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        ensureAdmin();
        return Result.success(adminAppService.getUsers(page, pageSize, status));
    }

    @GetMapping("/articles")
    public Result<PageResult<Map<String, Object>>> getArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        ensureAdmin();
        return Result.success(adminAppService.getArticles(page, pageSize, status, keyword));
    }

    @GetMapping("/comments")
    public Result<PageResult<Map<String, Object>>> getComments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long articleId) {
        ensureAdmin();
        return Result.success(adminAppService.getComments(page, pageSize, articleId));
    }

    /**
     * 分页查询管理员操作日志。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @return 日志分页结果
     */
    @GetMapping("/logs")
    public Result<PageResult<Map<String, Object>>> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        ensureAdmin();
        return Result.success(adminLogAppService.getLogs(page, pageSize));
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
}
