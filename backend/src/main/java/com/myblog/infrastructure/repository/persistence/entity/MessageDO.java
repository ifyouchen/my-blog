package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 消息数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class MessageDO {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private String type;
    private LocalDateTime readAt;
    private LocalDateTime senderDeletedAt;
    private LocalDateTime receiverDeletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getSenderDeletedAt() {
        return senderDeletedAt;
    }

    public void setSenderDeletedAt(LocalDateTime senderDeletedAt) {
        this.senderDeletedAt = senderDeletedAt;
    }

    public LocalDateTime getReceiverDeletedAt() {
        return receiverDeletedAt;
    }

    public void setReceiverDeletedAt(LocalDateTime receiverDeletedAt) {
        this.receiverDeletedAt = receiverDeletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
