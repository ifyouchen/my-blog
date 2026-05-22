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
    private final IdGenerator idGenerator;

    /**
     * 创建会话 MyBatis 仓储。
     *
     * @param conversationMapper 会话 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisConversationRepository(ConversationMapper conversationMapper, IdGenerator idGenerator) {
        this.conversationMapper = conversationMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据 ID 查询会话。
     *
     * @param id 会话 ID
     * @return 会话 Optional
     */
    @Override
    public Optional<Conversation> findById(Long id) {
        ConversationDO conversationDO = conversationMapper.selectById(id);
        return Optional.ofNullable(ConversationPersistenceConverter.toDomain(conversationDO));
    }

    /**
     * 根据两个参与者查询会话。
     *
     * @param participantAId 参与者 A 用户 ID
     * @param participantBId 参与者 B 用户 ID
     * @return 会话 Optional
     */
    @Override
    public Optional<Conversation> findByParticipants(Long participantAId, Long participantBId) {
        ConversationDO conversationDO = conversationMapper.selectByParticipants(participantAId, participantBId);
        return Optional.ofNullable(ConversationPersistenceConverter.toDomain(conversationDO));
    }

    /**
     * 分页查询用户参与的会话列表。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 会话列表
     */
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

    /**
     * 统计用户参与的会话数量。
     *
     * @param userId 用户 ID
     * @return 会话数量
     */
    @Override
    public long countByUser(Long userId) {
        return conversationMapper.countByUser(userId);
    }

    /**
     * 保存会话。
     *
     * @param conversation 会话聚合根
     * @return 保存后的会话
     */
    @Override
    public Conversation save(Conversation conversation) {
        ConversationDO conversationDO = ConversationPersistenceConverter.toData(conversation);
        conversationMapper.insert(conversationDO);
        return conversation;
    }

    /**
     * 更新会话最后一条消息信息。
     *
     * @param conversationId 会话 ID
     * @param lastMessage    最后一条消息内容
     * @param lastMessageAt  最后一条消息时间
     */
    @Override
    public void updateLastMessage(Long conversationId, String lastMessage, LocalDateTime lastMessageAt) {
        conversationMapper.updateLastMessage(conversationId, lastMessage, lastMessageAt);
    }

    /**
     * 标记用户已删除会话（软删除）。
     *
     * @param conversationId 会话 ID
     * @param userId         当前用户 ID
     */
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

    /**
     * 生成下一个会话 ID。
     *
     * @return 会话 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_conversation");
    }
}
