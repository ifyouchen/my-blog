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
     *
     * @param id 会话 ID
     * @return 会话 Optional
     */
    Optional<Conversation> findById(Long id);

    /**
     * 根据参与者查询会话（忽略删除）。
     *
     * @param participantAId 参与者 A 用户 ID
     * @param participantBId 参与者 B 用户 ID
     * @return 会话 Optional
     */
    Optional<Conversation> findByParticipants(Long participantAId, Long participantBId);

    /**
     * 查询用户的会话列表（分页，按最后消息时间倒序）。
     *
     * @param userId   用户 ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 会话列表
     */
    List<Conversation> findByUser(Long userId, int page, int pageSize);

    /**
     * 统计用户的会话数量。
     *
     * @param userId 用户 ID
     * @return 会话数量
     */
    long countByUser(Long userId);

    /**
     * 保存会话。
     *
     * @param conversation 会话聚合根
     * @return 保存后的会话
     */
    Conversation save(Conversation conversation);

    /**
     * 更新会话的最后消息。
     *
     * @param conversationId 会话 ID
     * @param lastMessage    最后消息摘要
     * @param lastMessageAt  最后消息时间
     */
    void updateLastMessage(Long conversationId, String lastMessage, LocalDateTime lastMessageAt);

    /**
     * 标记会话为已删除。
     *
     * @param conversationId 会话 ID
     * @param userId         执行删除的用户 ID
     */
    void deleteByUser(Long conversationId, Long userId);

    /**
     * 生成下一个会话 ID。
     *
     * @return 会话 ID
     */
    Long nextId();
}
