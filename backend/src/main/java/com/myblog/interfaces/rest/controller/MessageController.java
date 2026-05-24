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
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 私信 REST 接口。
 *
 * @author Codex
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> MESSAGE_EMITTERS =
        new ConcurrentHashMap<>();
    private static final String MESSAGE_TOPIC = "message:push";
    private static final long KEEPALIVE_INTERVAL_SECONDS = 30;

    private final MessageAppService messageAppService;
    private final ConversationRepository conversationRepository;
    private final RTopic messageTopic;
    private final ScheduledExecutorService keepaliveExecutor;

    public MessageController(MessageAppService messageAppService,
                              ConversationRepository conversationRepository,
                              RedissonClient redissonClient) {
        this.messageAppService = messageAppService;
        this.conversationRepository = conversationRepository;
        this.messageTopic = redissonClient.getTopic(MESSAGE_TOPIC, StringCodec.INSTANCE);
        this.keepaliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-keepalive");
            t.setDaemon(true);
            return t;
        });

        this.messageTopic.addListener(String.class, (channel, msg) -> {
            if (msg == null || msg.isEmpty()) {
                return;
            }
            try {
                // JSON envelope: {"userId":123,"type":"unread","data":"5"}
                // or: {"userId":123,"type":"new-message","data":{...}}
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> envelope = mapper.readValue(msg, Map.class);
                Object userIdObj = envelope.get("userId");
                String type = (String) envelope.get("type");
                if (userIdObj == null || type == null) {
                    return;
                }
                long userId = ((Number) userIdObj).longValue();
                if ("unread".equals(type)) {
                    Object countObj = envelope.get("data");
                    long count = countObj instanceof Number ? ((Number) countObj).longValue() : Long.parseLong(countObj.toString());
                    doPushUnreadCount(userId, count);
                } else if ("new-message".equals(type)) {
                    Object data = envelope.get("data");
                    if (data instanceof Map) {
                        doPushNewMessage(userId, (Map<String, Object>) data);
                    }
                }
            } catch (Exception e) {
                // ignore bad messages
            }
        });
    }

    @PreDestroy
    public void destroy() {
        keepaliveExecutor.shutdownNow();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Long userId = AuthContext.getCurrentUserId();
        if (userId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new ApplicationException(ErrorCode.UNAUTHORIZED, "请先登录"));
            return emitter;
        }

        // No timeout — EventSource auto-reconnects on network errors; indefinite keeps the connection alive
        SseEmitter emitter = new SseEmitter(0L);

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

        // Send periodic keepalive to prevent proxies/gateways from closing idle connections
        keepaliveExecutor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("keepalive"));
            } catch (IOException e) {
                cleanup.run();
            }
        }, KEEPALIVE_INTERVAL_SECONDS, KEEPALIVE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        return emitter;
    }

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
        List<SseEmitter> toRemove = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("new-message").data(data));
            } catch (IOException e) {
                toRemove.add(emitter);
            }
        }
        emitters.removeAll(toRemove);
    }

    public static void pushUnreadCount(Long userId, long unreadCount) {
        CopyOnWriteArrayList<SseEmitter> emitters = MESSAGE_EMITTERS.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        String data = "{\"count\":" + unreadCount + "}";
        List<SseEmitter> toRemove = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("unread").data(data));
            } catch (IOException e) {
                toRemove.add(emitter);
            }
        }
        emitters.removeAll(toRemove);
    }

    private static void doPushUnreadCount(Long userId, long unreadCount) {
        CopyOnWriteArrayList<SseEmitter> emitters = MESSAGE_EMITTERS.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        String data = "{\"count\":" + unreadCount + "}";
        List<SseEmitter> toRemove = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("unread").data(data));
            } catch (IOException e) {
                toRemove.add(emitter);
            }
        }
        emitters.removeAll(toRemove);
    }

    private static void doPushNewMessage(Long userId, Map<String, Object> messageData) {
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
        List<SseEmitter> toRemove = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("new-message").data(data));
            } catch (IOException e) {
                toRemove.add(emitter);
            }
        }
        emitters.removeAll(toRemove);
    }

    @PostMapping("/conversations")
    public Result<ConversationDTO> createConversation(@RequestBody CreateConversationRequest request) {
        if (request.getParticipantId() == null) {
            throw new ApplicationException(ErrorCode.PARAM_ERROR, "参与者ID不能为空");
        }
        ConversationDTO dto = messageAppService.getOrCreateConversation(request.getParticipantId());
        return Result.success(dto);
    }

    @GetMapping("/conversations")
    public Result<PageResult<ConversationDTO>> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<ConversationDTO> result = messageAppService.listConversations(page, pageSize);
        return Result.success(result);
    }

    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(@PathVariable Long id) {
        messageAppService.deleteConversation(id);
        return Result.success(null);
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<PageResult<MessageDTO>> listMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        PageResult<MessageDTO> result = messageAppService.listMessages(id, page, pageSize);
        return Result.success(result);
    }

    private void publishToRedis(Long userId, String type, Object data) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("userId", userId);
            envelope.put("type", type);
            envelope.put("data", data);
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            messageTopic.publish(mapper.writeValueAsString(envelope));
        } catch (Exception e) {
            // Redis pub is best-effort; ignore failures
        }
    }

    @PostMapping("/conversations/{id}/messages")
    public Result<MessageDTO> sendMessage(@PathVariable Long id,
                                           @RequestBody SendMessageRequest request) {
        MessageDTO dto = messageAppService.sendMessage(id, request.getContent(), request.getType());

        Long currentUserId = AuthContext.getRequiredUserId();
        Long receiverId = getReceiverId(dto.getConversationId(), currentUserId);
        if (receiverId != null) {
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", dto.getId());
            messageData.put("conversationId", dto.getConversationId());
            messageData.put("senderId", dto.getSenderId());
            messageData.put("senderName", dto.getSenderName());
            messageData.put("senderAvatar", dto.getSenderAvatar());
            messageData.put("content", dto.getContent());
            messageData.put("type", dto.getType());
            messageData.put("createdAt", dto.getCreatedAt());
            pushNewMessage(receiverId, messageData);
            publishToRedis(receiverId, "new-message", messageData);

            long unread = messageAppService.countUnreadForUser(receiverId);
            pushUnreadCount(receiverId, unread);
            publishToRedis(receiverId, "unread", unread);
        }

        return Result.success(dto);
    }

    @PostMapping("/conversations/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageAppService.markAllRead(id);

        Long userId = AuthContext.getRequiredUserId();
        long unread = messageAppService.countUnread();
        pushUnreadCount(userId, unread);
        publishToRedis(userId, "unread", unread);

        return Result.success(null);
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount() {
        long count = messageAppService.countUnread();
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    private Long getReceiverId(Long conversationId, Long currentUserId) {
        return conversationRepository.findById(conversationId)
            .map(conv -> conv.getOtherParticipantId(currentUserId))
            .orElse(null);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
