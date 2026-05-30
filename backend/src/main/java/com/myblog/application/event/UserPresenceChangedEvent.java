package com.myblog.application.event;

/**
 * 用户在线状态变更事件。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UserPresenceChangedEvent {

    private final Long userId;
    private final boolean online;
    private final String lastSeenAt;

    public UserPresenceChangedEvent(Long userId, boolean online, String lastSeenAt) {
        this.userId = userId;
        this.online = online;
        this.lastSeenAt = lastSeenAt;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isOnline() {
        return online;
    }

    public String getLastSeenAt() {
        return lastSeenAt;
    }
}
