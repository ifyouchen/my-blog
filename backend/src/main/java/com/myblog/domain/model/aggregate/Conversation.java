package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ConversationId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 会话聚合根。
 * 两个用户之间只有一个会话，参与者按 ID 大小排序。
 *
 * @author Codex
 * @since 1.0.0
 */
public class Conversation {

    private ConversationId id;
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

    private Conversation() {
    }

    /**
     * 创建新会话。
     *
     * @param id             会话ID
     * @param participantAId 参与者A（较小ID）
     * @param participantBId 参与者B（较大ID）
     * @return 会话聚合根
     */
    public static Conversation create(Long id, Long participantAId, Long participantBId) {
        Conversation conversation = new Conversation();
        conversation.id = new ConversationId(id);
        conversation.participantAId = participantAId;
        conversation.participantBId = participantBId;
        conversation.createdAt = LocalDateTime.now();
        conversation.updatedAt = conversation.createdAt;
        conversation.deletedAt = null;
        conversation.version = 0L;
        return conversation;
    }

    /**
     * 从持久化数据恢复会话。
     */
    public static Conversation restore(Long id, Long participantAId, Long participantBId,
                                       String lastMessage, LocalDateTime lastMessageAt,
                                       LocalDateTime aDeletedAt, LocalDateTime bDeletedAt,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt, Long version) {
        Conversation conversation = new Conversation();
        conversation.id = new ConversationId(id);
        conversation.participantAId = participantAId;
        conversation.participantBId = participantBId;
        conversation.lastMessage = lastMessage;
        conversation.lastMessageAt = lastMessageAt;
        conversation.aDeletedAt = aDeletedAt;
        conversation.bDeletedAt = bDeletedAt;
        conversation.createdAt = createdAt;
        conversation.updatedAt = updatedAt;
        conversation.deletedAt = deletedAt;
        conversation.version = version;
        return conversation;
    }

    /**
     * 更新最后一条消息。
     */
    public void updateLastMessage(String content) {
        this.lastMessage = content.length() > 500 ? content.substring(0, 500) : content;
        this.lastMessageAt = LocalDateTime.now();
        this.updatedAt = this.lastMessageAt;
    }

    /**
     * 指定用户删除会话。
     */
    public void deleteByUser(Long userId) {
        if (userId.equals(participantAId)) {
            this.aDeletedAt = LocalDateTime.now();
        } else if (userId.equals(participantBId)) {
            this.bDeletedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断指定用户是否已删除会话。
     */
    public boolean isDeletedByUser(Long userId) {
        if (userId.equals(participantAId)) {
            return aDeletedAt != null;
        } else if (userId.equals(participantBId)) {
            return bDeletedAt != null;
        }
        return false;
    }

    /**
     * 获取对方用户 ID。
     */
    public Long getOtherParticipantId(Long userId) {
        if (userId.equals(participantAId)) {
            return participantBId;
        }
        return participantAId;
    }

    public ConversationId getId() {
        return id;
    }

    public Long getParticipantAId() {
        return participantAId;
    }

    public Long getParticipantBId() {
        return participantBId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public LocalDateTime getADeletedAt() {
        return aDeletedAt;
    }

    public LocalDateTime getBDeletedAt() {
        return bDeletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Long getVersion() {
        return version;
    }
}
