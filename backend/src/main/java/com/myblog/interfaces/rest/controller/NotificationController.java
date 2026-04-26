package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.NotificationDTO;
import com.myblog.application.service.NotificationAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationAppService notificationAppService;

    public NotificationController(NotificationAppService notificationAppService) {
        this.notificationAppService = notificationAppService;
    }

    @GetMapping
    public Result<PageResult<NotificationDTO>> pageNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "all") String filter) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        if (!"all".equals(filter) && !"unread".equals(filter) && !"read".equals(filter)) {
            filter = "all";
        }

        PageResult<NotificationDTO> result = notificationAppService.pageNotifications(userId, page, pageSize, filter);
        return Result.success(result);
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            Map<String, Long> result = new HashMap<>();
            result.put("count", 0L);
            return Result.success(result);
        }

        long count = notificationAppService.countUnread(userId);
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    @GetMapping("/recent")
    public Result<List<NotificationDTO>> listRecentNotifications(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "unread") String filter) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        if (limit <= 0 || limit > 20) {
            limit = 5;
        }

        if (!"all".equals(filter) && !"unread".equals(filter)) {
            filter = "unread";
        }

        List<NotificationDTO> notifications = notificationAppService.listRecent(userId, filter, limit);
        return Result.success(notifications);
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        notificationAppService.markRead(userId, id);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = AuthContext.getRequiredUserId();
        if (userId == null) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        notificationAppService.markAllRead(userId);
        return Result.success();
    }
}