package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Message;
import com.myblog.domain.repository.MessageRepository;
import com.myblog.infrastructure.repository.persistence.converter.MessagePersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.MessageDO;
import com.myblog.infrastructure.repository.persistence.mapper.MessageMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    private final IdGenerator idGenerator;

    /**
     * 创建消息 MyBatis 仓储。
     *
     * @param messageMapper 消息 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisMessageRepository(MessageMapper messageMapper, IdGenerator idGenerator) {
        this.messageMapper = messageMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据 ID 查询消息。
     *
     * @param id 消息 ID
     * @return 消息 Optional
     */
    @Override
    public Optional<Message> findById(Long id) {
        MessageDO messageDO = messageMapper.selectById(id);
        return Optional.ofNullable(MessagePersistenceConverter.toDomain(messageDO));
    }

    /**
     * 分页查询会话消息列表。
     *
     * @param conversationId 会话 ID
     * @param currentUserId  当前用户 ID
     * @param page           页码
     * @param pageSize       每页大小
     * @return 消息列表
     */
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

    /**
     * 统计会话消息数量。
     *
     * @param conversationId 会话 ID
     * @param currentUserId  当前用户 ID
     * @return 消息数量
     */
    @Override
    public long countByConversation(Long conversationId, Long currentUserId) {
        return messageMapper.countByConversation(conversationId);
    }

    /**
     * 统计会话中指定接收者的未读消息数量。
     *
     * @param conversationId 会话 ID
     * @param receiverId     接收者用户 ID
     * @return 未读消息数量
     */
    @Override
    public long countUnreadByConversation(Long conversationId, Long receiverId) {
        return messageMapper.countUnreadByConversation(conversationId, receiverId);
    }

    /**
     * 统计用户全部未读消息数量。
     *
     * @param userId 用户 ID
     * @return 未读消息数量
     */
    @Override
    public long countTotalUnread(Long userId) {
        return messageMapper.countTotalUnread(userId);
    }

    /**
     * 保存消息。
     *
     * @param message 消息聚合根
     * @return 保存后的消息
     */
    @Override
    public Message save(Message message) {
        MessageDO messageDO = MessagePersistenceConverter.toData(message);
        messageMapper.insert(messageDO);
        return message;
    }

    /**
     * 将会话中指定接收者的全部消息标记为已读。
     *
     * @param conversationId 会话 ID
     * @param receiverId     接收者用户 ID
     * @return 影响行数
     */
    @Override
    public int markAllRead(Long conversationId, Long receiverId) {
        return messageMapper.markAllRead(conversationId, receiverId);
    }

    /**
     * 撤回消息。
     *
     * @param id         消息 ID
     * @param senderId   发送者用户 ID
     * @param recalledAt 撤回时间
     * @return 影响行数
     */
    @Override
    public int recallMessage(Long id, Long senderId, LocalDateTime recalledAt) {
        return messageMapper.recallMessage(id, senderId, recalledAt);
    }

    /**
     * 生成下一个消息 ID。
     *
     * @return 消息 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_message");
    }
}
