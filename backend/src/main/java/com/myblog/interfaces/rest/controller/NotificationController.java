package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.NotificationDTO;
import com.myblog.application.service.NotificationAppService;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 通知 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    /** userId -> list of active emitters */
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> USER_EMITTERS =
        new ConcurrentHashMap<>();

    private final NotificationAppService notificationAppService;

    public NotificationController(NotificationAppService notificationAppService) {
        this.notificationAppService = notificationAppService;
    }

    /**
     * SSE 通知流：客户端保持长连接，服务端主动推送未读计数变化。
     * 每次有新通知写入后，可调用 {@link #pushUnreadCount(Long, long)} 推送。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录"));
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(180_000L); // 3分钟超时，客户端重连

        USER_EMITTERS.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            CopyOnWriteArrayList<SseEmitter> list = USER_EMITTERS.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    USER_EMITTERS.remove(userId, list);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // 立即推送当前未读数（连接建立时同步一次）
        try {
            long unread = notificationAppService.countUnread(userId);
            emitter.send(SseEmitter.event()
                .name("unread")
                .data("{\"count\":" + unread + "}"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 向指定用户的所有 SSE 连接推送未读计数（供内部调用）。
     *
     * @param userId      用户 ID
     * @param unreadCount 最新未读数
     */
    public static void pushUnreadCount(Long userId, long unreadCount) {
        CopyOnWriteArrayList<SseEmitter> emitters = USER_EMITTERS.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        String data = "{\"count\":" + unreadCount + "}";
        List<SseEmitter> toRemove = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("unread").data(data));
            } catch (IOException e) {
                toRemove.add(emitter);
            }
        }
        emitters.removeAll(toRemove);
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