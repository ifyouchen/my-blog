package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.MessageId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 消息聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class Message {

    private MessageId id;
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

    private Message() {
    }

    /**
     * 创建新消息。
     */
    public static Message create(Long id, Long conversationId, Long senderId, String content, String type) {
        Message message = new Message();
        message.id = new MessageId(id);
        message.conversationId = conversationId;
        message.senderId = senderId;
        message.content = content;
        message.type = type != null ? type : "TEXT";
        message.createdAt = LocalDateTime.now();
        message.updatedAt = message.createdAt;
        message.deletedAt = null;
        message.version = 0L;
        return message;
    }

    /**
     * 从持久化数据恢复消息。
     */
    public static Message restore(Long id, Long conversationId, Long senderId,
                                  String content, String type, LocalDateTime readAt,
                                  LocalDateTime senderDeletedAt, LocalDateTime receiverDeletedAt,
                                  LocalDateTime createdAt, LocalDateTime updatedAt,
                                  LocalDateTime deletedAt, Long version) {
        Message message = new Message();
        message.id = new MessageId(id);
        message.conversationId = conversationId;
        message.senderId = senderId;
        message.content = content;
        message.type = type;
        message.readAt = readAt;
        message.senderDeletedAt = senderDeletedAt;
        message.receiverDeletedAt = receiverDeletedAt;
        message.createdAt = createdAt;
        message.updatedAt = updatedAt;
        message.deletedAt = deletedAt;
        message.version = version;
        return message;
    }

    /**
     * 标记已读。
     */
    public void markRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
            this.updatedAt = this.readAt;
        }
    }

    public boolean isRead() {
        return this.readAt != null;
    }

    /**
     * 发送方删除消息。
     */
    public void deleteBySender() {
        this.senderDeletedAt = LocalDateTime.now();
        this.updatedAt = this.senderDeletedAt;
    }

    /**
     * 接收方删除消息。
     */
    public void deleteByReceiver() {
        this.receiverDeletedAt = LocalDateTime.now();
        this.updatedAt = this.receiverDeletedAt;
    }

    public MessageId getId() {
        return id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getSenderDeletedAt() {
        return senderDeletedAt;
    }

    public LocalDateTime getReceiverDeletedAt() {
        return receiverDeletedAt;
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
