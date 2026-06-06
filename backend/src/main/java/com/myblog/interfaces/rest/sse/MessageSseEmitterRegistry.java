package com.myblog.interfaces.rest.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 私信 SSE 连接注册表。
 *
 * @author Codex
 * @since 1.0.0
 */
@Component
public class MessageSseEmitterRegistry {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters =
        new ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>();

    /**
     * 注册用户 SSE 连接。
     *
     * @param userId 用户 ID
     * @param emitter SSE 连接
     */
    public void add(Long userId, SseEmitter emitter) {
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<SseEmitter>()).add(emitter);
    }

    /**
     * 移除用户 SSE 连接。
     *
     * @param userId 用户 ID
     * @param emitter SSE 连接
     * @return true 表示该用户本实例已无连接
     */
    public boolean remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return true;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId, userEmitters);
            return true;
        }
        return false;
    }

    /**
     * 判断用户在本实例是否还有 SSE 连接。
     *
     * @param userId 用户 ID
     * @return true 表示存在连接
     */
    public boolean hasEmitter(Long userId) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters != null && !userEmitters.isEmpty();
    }

    /**
     * 向用户全部本地 SSE 连接发送事件。
     *
     * @param userId 用户 ID
     * @param eventName 事件名称
     * @param data 事件数据
     */
    public void emit(Long userId, String eventName, String data) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        List<SseEmitter> toRemove = new ArrayList<SseEmitter>();
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                toRemove.add(emitter);
            }
        }
        userEmitters.removeAll(toRemove);
    }
}
