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

    /**
     * 私有构造函数，防止实例化。
     */
    private MessagePersistenceConverter() {
    }

    /**
     * 将消息数据对象转换为领域对象。
     *
     * @param messageDO 消息数据对象
     * @return 消息领域对象，若 messageDO 为 null 则返回 null
     */
    public static Message toDomain(MessageDO messageDO) {
        if (messageDO == null) {
            return null;
        }
        return Message.restore(
            messageDO.getId(),
            messageDO.getConversationId(),
            messageDO.getSenderId(),
            messageDO.getParentId(),
            messageDO.getContent(),
            messageDO.getType(),
            messageDO.getReadAt(),
            messageDO.getSenderDeletedAt(),
            messageDO.getReceiverDeletedAt(),
            messageDO.getRecalledAt(),
            messageDO.getCreatedAt(),
            messageDO.getUpdatedAt(),
            messageDO.getDeletedAt(),
            messageDO.getVersion()
        );
    }

    /**
     * 将消息领域对象转换为数据对象。
     *
     * @param message 消息领域对象
     * @return 消息数据对象
     */
    public static MessageDO toData(Message message) {
        MessageDO messageDO = new MessageDO();
        messageDO.setId(message.getId().getValue());
        messageDO.setConversationId(message.getConversationId());
        messageDO.setSenderId(message.getSenderId());
        messageDO.setParentId(message.getParentId());
        messageDO.setContent(message.getContent());
        messageDO.setType(message.getType());
        messageDO.setReadAt(message.getReadAt());
        messageDO.setSenderDeletedAt(message.getSenderDeletedAt());
        messageDO.setReceiverDeletedAt(message.getReceiverDeletedAt());
        messageDO.setRecalledAt(message.getRecalledAt());
        messageDO.setCreatedAt(message.getCreatedAt());
        messageDO.setUpdatedAt(message.getUpdatedAt());
        messageDO.setDeletedAt(message.getDeletedAt());
        messageDO.setVersion(message.getVersion());
        return messageDO;
    }
}
