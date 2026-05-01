package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 会话数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ConversationDO {

    private Long id;
    private Long participantAId;
    private Long participantBId;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime aDeletedAt;
    private LocalDateTime bDeletedAt;
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

    public Long getParticipantAId() {
        return participantAId;
    }

    public void setParticipantAId(Long participantAId) {
        this.participantAId = participantAId;
    }

    public Long getParticipantBId() {
        return participantBId;
    }

    public void setParticipantBId(Long participantBId) {
        this.participantBId = participantBId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public LocalDateTime getADeletedAt() {
        return aDeletedAt;
    }

    public void setADeletedAt(LocalDateTime aDeletedAt) {
        this.aDeletedAt = aDeletedAt;
    }

    public LocalDateTime getBDeletedAt() {
        return bDeletedAt;
    }

    public void setBDeletedAt(LocalDateTime bDeletedAt) {
        this.bDeletedAt = bDeletedAt;
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
