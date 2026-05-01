package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ConversationRepository {

    /**
     * 根据会话 ID 查询会话。
     */
    Optional<Conversation> findById(Long id);

    /**
     * 根据参与者查询会话（忽略删除）。
     */
    Optional<Conversation> findByParticipants(Long participantAId, Long participantBId);

    /**
     * 查询用户的会话列表（分页，按最后消息时间倒序）。
     */
    List<Conversation> findByUser(Long userId, int page, int pageSize);

    /**
     * 统计用户的会话数量。
     */
    long countByUser(Long userId);

    /**
     * 保存会话。
     */
    Conversation save(Conversation conversation);

    /**
     * 更新会话的最后消息。
     */
    void updateLastMessage(Long conversationId, String lastMessage, LocalDateTime lastMessageAt);

    /**
     * 标记会话为已删除。
     */
    void deleteByUser(Long conversationId, Long userId);

    /**
     * 生成下一个会话 ID。
     */
    Long nextId();
}
