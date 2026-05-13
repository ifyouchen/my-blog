package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Conversation;
import com.myblog.infrastructure.repository.persistence.entity.ConversationDO;

/**
 * 会话持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ConversationPersistenceConverter {

    /**
     * 私有构造函数，防止实例化。
     */
    private ConversationPersistenceConverter() {
    }

    /**
     * 将会话数据对象转换为领域对象。
     *
     * @param conversationDO 会话数据对象
     * @return 会话领域对象，若 conversationDO 为 null 则返回 null
     */
    public static Conversation toDomain(ConversationDO conversationDO) {
        if (conversationDO == null) {
            return null;
        }
        return Conversation.restore(
            conversationDO.getId(),
            conversationDO.getParticipantAId(),
            conversationDO.getParticipantBId(),
            conversationDO.getLastMessage(),
            conversationDO.getLastMessageAt(),
            conversationDO.getADeletedAt(),
            conversationDO.getBDeletedAt(),
            conversationDO.getCreatedAt(),
            conversationDO.getUpdatedAt(),
            conversationDO.getDeletedAt(),
            conversationDO.getVersion()
        );
    }

    /**
     * 将会话领域对象转换为数据对象。
     *
     * @param conversation 会话领域对象
     * @return 会话数据对象
     */
    public static ConversationDO toData(Conversation conversation) {
        ConversationDO conversationDO = new ConversationDO();
        conversationDO.setId(conversation.getId().getValue());
        conversationDO.setParticipantAId(conversation.getParticipantAId());
        conversationDO.setParticipantBId(conversation.getParticipantBId());
        conversationDO.setLastMessage(conversation.getLastMessage());
        conversationDO.setLastMessageAt(conversation.getLastMessageAt());
        conversationDO.setADeletedAt(conversation.getADeletedAt());
        conversationDO.setBDeletedAt(conversation.getBDeletedAt());
        conversationDO.setCreatedAt(conversation.getCreatedAt());
        conversationDO.setUpdatedAt(conversation.getUpdatedAt());
        conversationDO.setDeletedAt(conversation.getDeletedAt());
        conversationDO.setVersion(conversation.getVersion());
        return conversationDO;
    }
}
