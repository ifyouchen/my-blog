package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Message;

import java.util.List;
import java.util.Optional;

/**
 * 消息仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface MessageRepository {

    /**
     * 根据消息 ID 查询消息。
     *
     * @param id 消息 ID
     * @return 消息 Optional
     */
    Optional<Message> findById(Long id);

    /**
     * 查询会话的消息列表（分页，按时间倒序）。
     *
     * @param conversationId 会话 ID
     * @param currentUserId  当前用户 ID
     * @param page           页码
     * @param pageSize       每页大小
     * @return 消息列表
     */
    List<Message> findByConversation(Long conversationId, Long currentUserId, int page, int pageSize);

    /**
     * 统计会话的消息数量。
     *
     * @param conversationId 会话 ID
     * @param currentUserId  当前用户 ID
     * @return 消息数量
     */
    long countByConversation(Long conversationId, Long currentUserId);

    /**
     * 统计会话中的未读消息数量。
     *
     * @param conversationId 会话 ID
     * @param receiverId     接收人用户 ID
     * @return 未读消息数量
     */
    long countUnreadByConversation(Long conversationId, Long receiverId);

    /**
     * 统计用户所有会话的未读消息总数。
     *
     * @param userId 用户 ID
     * @return 未读消息总数
     */
    long countTotalUnread(Long userId);

    /**
     * 保存消息。
     *
     * @param message 消息聚合根
     * @return 保存后的消息
     */
    Message save(Message message);

    /**
     * 标记会话中所有消息为已读。
     *
     * @param conversationId 会话 ID
     * @param receiverId     接收人用户 ID
     * @return 标记为已读的消息数量
     */
    int markAllRead(Long conversationId, Long receiverId);

    /**
     * 生成下一个消息 ID。
     *
     * @return 消息 ID
     */
    Long nextId();
}
