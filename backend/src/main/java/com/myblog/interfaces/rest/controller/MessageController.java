package com.myblog.interfaces.rest.controller;

import com.myblog.application.dto.ConversationDTO;
import com.myblog.application.dto.MessageDTO;
import com.myblog.application.service.MessageAppService;
import com.myblog.domain.model.aggregate.Conversation;
import com.myblog.domain.repository.ConversationRepository;
import com.myblog.infrastructure.security.AuthContext;
import com.myblog.shared.exception.ApplicationException;
import com.myblog.shared.exception.ErrorCode;
import com.myblog.shared.result.PageResult;
import com.myblog.shared.result.Result;
import com.myblog.interfaces.rest.dto.request.CreateConversationRequest;
import com.myblog.interfaces.rest.dto.request.SendMessageRequest;
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
 * 私信 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    /** userId -> list of active SSE emitters for message events */
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> MESSAGE_EMITTERS =
        new ConcurrentHashMap<>();

    private final MessageAppService messageAppService;
    private final ConversationRepository conversationRepository;

    public MessageController(MessageAppService messageAppService,
                             ConversationRepository conversationRepository) {
        this.messageAppService = messageAppService;
        this.conversationRepository = conversationRepository;
    }

    /**
     * SSE 消息流：客户端保持长连接，接收新消息和未读计数推送。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录"));
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(180_000L);

        MESSAGE_EMITTERS.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            CopyOnWriteArrayList<SseEmitter> list = MESSAGE_EMITTERS.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    MESSAGE_EMITTERS.remove(userId, list);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // 立即推送当前未读数
        try {
            long unread = messageAppService.countUnread();
            emitter.send(SseEmitter.event()
                .name("unread")
                .data("{\"count\":" + unread + "}"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 向指定用户推送新消息通知。
     */
    public static void pushNewMessage(Long userId, Map<String, Object> messageData) {
        CopyOnWriteArrayList<SseEmitter> emitters = MESSAGE_EMITTERS.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : messageData.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {
                json.append("\"").append(escapeJson((String) val)).append("\"");
            } else {
                json.append(val);
            }
            json.append(",");
        }
        if (json.length() > 1) {
            json.setLength(json.length() - 1);
        }
        json.append("}");

        String data = json.toString();
        List<SseEmitter> toRemove = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("new-message").data(data));
            } catch (IOException e) {
                toRemove.add(emitter);
            }
        }
        emitters.removeAll(toRemove);
    }

    /**
     * 向指定用户推送未读计数。
     */
    public static void pushUnreadCount(Long userId, long unreadCount) {
        CopyOnWriteArrayList<SseEmitter> emitters = MESSAGE_EMITTERS.get(userId);
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

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 获取或创建会话。
     */
    @PostMapping("/conversations")
    public Result<ConversationDTO> createConversation(@RequestBody CreateConversationRequest request) {
        if (request.getParticipantId() == null) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "参与者ID不能为空");
        }
        ConversationDTO dto = messageAppService.getOrCreateConversation(request.getParticipantId());
        return Result.success(dto);
    }

    /**
     * 获取会话列表。
     */
    @GetMapping("/conversations")
    public Result<PageResult<ConversationDTO>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<ConversationDTO> result = messageAppService.listConversations(page, pageSize);
        return Result.success(result);
    }

    /**
     * 删除会话。
     */
    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        messageAppService.deleteConversation(id);
        return Result.success(null);
    }

    /**
     * 获取消息历史。
     */
    @GetMapping("/conversations/{id}/messages")
    public Result<PageResult<MessageDTO>> listMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        PageResult<MessageDTO> result = messageAppService.listMessages(id, page, pageSize);
        return Result.success(result);
    }

    /**
     * 发送消息。
     */
    @PostMapping("/conversations/{id}/messages")
    public Result<MessageDTO> sendMessage(@PathVariable Long id,
                                          @RequestBody SendMessageRequest request) {
        MessageDTO dto = messageAppService.sendMessage(id, request.getContent());

        // SSE 推送
        Long currentUserId = AuthContext.getRequiredUserId();
        Long receiverId = getReceiverId(dto.getConversationId(), currentUserId);
        if (receiverId != null) {
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("conversationId", dto.getConversationId());
            messageData.put("senderId", dto.getSenderId());
            messageData.put("senderName", dto.getSenderName());
            messageData.put("senderAvatar", dto.getSenderAvatar());
            messageData.put("content", dto.getContent());
            messageData.put("createdAt", dto.getCreatedAt());
            pushNewMessage(receiverId, messageData);

            long unread = messageAppService.countUnread();
            pushUnreadCount(receiverId, unread);
        }

        return Result.success(dto);
    }

    /**
     * 标记会话内所有消息为已读。
     */
    @PostMapping("/conversations/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageAppService.markAllRead(id);

        Long userId = AuthContext.getRequiredUserId();
        long unread = messageAppService.countUnread();
        pushUnreadCount(userId, unread);

        return Result.success(null);
    }

    /**
     * 获取未读消息总数。
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount() {
        long count = messageAppService.countUnread();
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    /**
     * 根据当前用户获取会话中对方用户 ID。
     */
    private Long getReceiverId(Long conversationId, Long currentUserId) {
        return conversationRepository.findById(conversationId)
            .map(conv -> conv.getOtherParticipantId(currentUserId))
            .orElse(null);
    }
}
