package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Message;
import com.myblog.infrastructure.repository.persistence.entity.MessageDO;

/**
 * 消息持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class MessagePersistenceConverter {

    private MessagePersistenceConverter() {
    }

    public static Message toDomain(MessageDO messageDO) {
        if (messageDO == null) {
            return null;
        }
        return Message.restore(
            messageDO.getId(),
            messageDO.getConversationId(),
            messageDO.getSenderId(),
            messageDO.getContent(),
            messageDO.getType(),
            messageDO.getReadAt(),
            messageDO.getSenderDeletedAt(),
            messageDO.getReceiverDeletedAt(),
            messageDO.getCreatedAt(),
            messageDO.getUpdatedAt(),
            messageDO.getDeletedAt(),
            messageDO.getVersion()
        );
    }

    public static MessageDO toData(Message message) {
        MessageDO messageDO = new MessageDO();
        messageDO.setId(message.getId().getValue());
        messageDO.setConversationId(message.getConversationId());
        messageDO.setSenderId(message.getSenderId());
        messageDO.setContent(message.getContent());
        messageDO.setType(message.getType());
        messageDO.setReadAt(message.getReadAt());
        messageDO.setSenderDeletedAt(message.getSenderDeletedAt());
        messageDO.setReceiverDeletedAt(message.getReceiverDeletedAt());
        messageDO.setCreatedAt(message.getCreatedAt());
        messageDO.setUpdatedAt(message.getUpdatedAt());
        messageDO.setDeletedAt(message.getDeletedAt());
        messageDO.setVersion(message.getVersion());
        return messageDO;
    }
}
