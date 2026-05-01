package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Message;

import java.util.List;
import java.util.Optional;

/**
 * 消息仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface MessageRepository {

    /**
     * 根据消息 ID 查询消息。
     */
    Optional<Message> findById(Long id);

    /**
     * 查询会话的消息列表（分页，按时间倒序）。
     */
    List<Message> findByConversation(Long conversationId, Long currentUserId, int page, int pageSize);

    /**
     * 统计会话的消息数量。
     */
    long countByConversation(Long conversationId, Long currentUserId);

    /**
     * 统计会话中的未读消息数量。
     */
    long countUnreadByConversation(Long conversationId, Long receiverId);

    /**
     * 统计用户所有会话的未读消息总数。
     */
    long countTotalUnread(Long userId);

    /**
     * 保存消息。
     */
    Message save(Message message);

    /**
     * 标记会话中所有消息为已读。
     */
    int markAllRead(Long conversationId, Long receiverId);

    /**
     * 生成下一个消息 ID。
     */
    Long nextId();
}
