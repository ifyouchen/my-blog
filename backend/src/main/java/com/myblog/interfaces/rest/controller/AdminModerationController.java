package com.myblog.interfaces.rest.controller;

import com.myblog.application.command.RecordAdminLogCommand;
import com.myblog.application.service.AdminAppService;
import com.myblog.application.service.AdminLogAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.interfaces.rest.dto.request.ModerationReasonRequest;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.Result;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台语义化治理接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin")
public class AdminModerationController {

    private final AdminAppService adminAppService;
    private final AdminLogAppService adminLogAppService;

    public AdminModerationController(AdminAppService adminAppService, AdminLogAppService adminLogAppService) {
        this.adminAppService = adminAppService;
        this.adminLogAppService = adminLogAppService;
    }

    @PostMapping("/articles/{id}/offline")
    public Result<Map<String, Object>> offlineArticle(@PathVariable Long id,
                                                      @RequestBody(required = false) ModerationReasonRequest request,
                                                      @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        Map<String, Object> result = adminAppService.offlineArticle(id, request == null ? null : request.getReason());
        adminLogAppService.recordOperation(buildLogCommand(
            "OFFLINE_ARTICLE",
            "ARTICLE",
            id,
            "下架文章 " + result.get("title"),
            buildStatusBeforeSnapshot(result),
            buildStatusAfterSnapshot(result),
            httpServletRequest
        ));
        return Result.success(result);
    }

    @PostMapping("/users/{id}/disable")
    public Result<Map<String, Object>> disableUser(@PathVariable Long id,
                                                   @RequestBody(required = false) ModerationReasonRequest request,
                                                   @Nullable HttpServletRequest httpServletRequest) {
        ensureAdmin();
        Map<String, Object> result = adminAppService.disableUser(id, request == null ? null : request.getReason());
        adminLogAppService.recordOperation(buildLogCommand(
            "DISABLE_USER",
            "USER",
            id,
            "禁用用户 " + result.get("username"),
            buildStatusBeforeSnapshot(result),
            buildStatusAfterSnapshot(result),
            httpServletRequest
        ));
        return Result.success(result);
    }

    private void ensureAdmin() {
        AuthContext.getRequiredUserId();
        if (!"ADMIN".equals(AuthContext.getRole())) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "仅管理员可访问后台");
        }
    }

    private Map<String, Object> buildStatusBeforeSnapshot(Map<String, Object> result) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", result.get("id"));
        snapshot.put("title", result.get("title"));
        snapshot.put("username", result.get("username"));
        snapshot.put("status", result.get("previousStatus"));
        return snapshot;
    }

    private Map<String, Object> buildStatusAfterSnapshot(Map<String, Object> result) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("id", result.get("id"));
        snapshot.put("title", result.get("title"));
        snapshot.put("username", result.get("username"));
        snapshot.put("status", result.get("status"));
        snapshot.put("offlineReason", result.get("offlineReason"));
        snapshot.put("disableReason", result.get("disableReason"));
        return snapshot;
    }

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

    private String resolveIpAddress(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
