package com.myblog.application.dto;

/**
 * 会话数据传输对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ConversationDTO {

    private Long id;
    private UserDTO participant;
    private String lastMessage;
    private String lastMessageAt;
    private long unreadCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserDTO getParticipant() {
        return participant;
    }

    public void setParticipant(UserDTO participant) {
        this.participant = participant;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(String lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
