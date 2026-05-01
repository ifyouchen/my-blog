package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Message;
import com.myblog.domain.repository.MessageRepository;
import com.myblog.infrastructure.repository.persistence.converter.MessagePersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.MessageDO;
import com.myblog.infrastructure.repository.persistence.mapper.MessageMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 消息 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisMessageRepository implements MessageRepository {

    private final MessageMapper messageMapper;

    public MyBatisMessageRepository(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public Optional<Message> findById(Long id) {
        MessageDO messageDO = messageMapper.selectById(id);
        return Optional.ofNullable(MessagePersistenceConverter.toDomain(messageDO));
    }

    @Override
    public List<Message> findByConversation(Long conversationId, Long currentUserId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<MessageDO> messageDOList = messageMapper.selectByConversation(conversationId, offset, pageSize);
        List<Message> messages = new ArrayList<>(messageDOList.size());
        for (MessageDO messageDO : messageDOList) {
            messages.add(MessagePersistenceConverter.toDomain(messageDO));
        }
        return messages;
    }

    @Override
    public long countByConversation(Long conversationId, Long currentUserId) {
        return messageMapper.countByConversation(conversationId);
    }

    @Override
    public long countUnreadByConversation(Long conversationId, Long receiverId) {
        return messageMapper.countUnreadByConversation(conversationId, receiverId);
    }

    @Override
    public long countTotalUnread(Long userId) {
        return messageMapper.countTotalUnread(userId);
    }

    @Override
    public Message save(Message message) {
        MessageDO messageDO = MessagePersistenceConverter.toData(message);
        messageMapper.insert(messageDO);
        return message;
    }

    @Override
    public int markAllRead(Long conversationId, Long receiverId) {
        return messageMapper.markAllRead(conversationId, receiverId);
    }

    @Override
    public Long nextId() {
        return messageMapper.selectNextId();
    }
}
