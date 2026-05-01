package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Conversation;
import com.myblog.domain.repository.ConversationRepository;
import com.myblog.infrastructure.repository.persistence.converter.ConversationPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.ConversationDO;
import com.myblog.infrastructure.repository.persistence.mapper.ConversationMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 会话 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
public class MyBatisConversationRepository implements ConversationRepository {

    private final ConversationMapper conversationMapper;

    public MyBatisConversationRepository(ConversationMapper conversationMapper) {
        this.conversationMapper = conversationMapper;
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        ConversationDO conversationDO = conversationMapper.selectById(id);
        return Optional.ofNullable(ConversationPersistenceConverter.toDomain(conversationDO));
    }

    @Override
    public Optional<Conversation> findByParticipants(Long participantAId, Long participantBId) {
        ConversationDO conversationDO = conversationMapper.selectByParticipants(participantAId, participantBId);
        return Optional.ofNullable(ConversationPersistenceConverter.toDomain(conversationDO));
    }

    @Override
    public List<Conversation> findByUser(Long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<ConversationDO> conversationDOList = conversationMapper.selectByUser(userId, offset, pageSize);
        List<Conversation> conversations = new ArrayList<>(conversationDOList.size());
        for (ConversationDO conversationDO : conversationDOList) {
            conversations.add(ConversationPersistenceConverter.toDomain(conversationDO));
        }
        return conversations;
    }

    @Override
    public long countByUser(Long userId) {
        return conversationMapper.countByUser(userId);
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationDO conversationDO = ConversationPersistenceConverter.toData(conversation);
        conversationMapper.insert(conversationDO);
        return conversation;
    }

    @Override
    public void updateLastMessage(Long conversationId, String lastMessage, LocalDateTime lastMessageAt) {
        conversationMapper.updateLastMessage(conversationId, lastMessage, lastMessageAt);
    }

    @Override
    public void deleteByUser(Long conversationId, Long userId) {
        // 根据 userId 决定更新 a_deleted_at 还是 b_deleted_at
        // 需要先查出会话来确定
        ConversationDO conversationDO = conversationMapper.selectById(conversationId);
        if (conversationDO == null) {
            return;
        }
        String column;
        if (userId.equals(conversationDO.getParticipantAId())) {
            column = "a_deleted_at";
        } else {
            column = "b_deleted_at";
        }
        conversationMapper.markDeletedByUser(conversationId, column);
    }

    @Override
    public Long nextId() {
        return conversationMapper.selectNextId();
    }
}
