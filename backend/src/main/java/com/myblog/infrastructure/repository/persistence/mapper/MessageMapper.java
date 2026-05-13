package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.MessageDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface MessageMapper {

    /**
     * 根据 ID 查询消息。
     *
     * @param id 消息 ID
     * @return 消息数据对象
     */
    MessageDO selectById(@Param("id") Long id);

    /**
     * 分页查询会话消息列表。
     *
     * @param conversationId 会话 ID
     * @param offset         偏移量
     * @param limit          限制数量
     * @return 消息列表
     */
    List<MessageDO> selectByConversation(@Param("conversationId") Long conversationId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /**
     * 统计会话消息数量。
     *
     * @param conversationId 会话 ID
     * @return 消息数量
     */
    long countByConversation(@Param("conversationId") Long conversationId);

    /**
     * 统计会话中指定接收者的未读消息数量。
     *
     * @param conversationId 会话 ID
     * @param receiverId     接收者用户 ID
     * @return 未读消息数量
     */
    long countUnreadByConversation(@Param("conversationId") Long conversationId,
                                   @Param("receiverId") Long receiverId);

    /**
     * 统计用户全部未读消息数量。
     *
     * @param userId 用户 ID
     * @return 未读消息数量
     */
    long countTotalUnread(@Param("userId") Long userId);

    /**
     * 插入消息。
     *
     * @param messageDO 消息数据对象
     * @return 影响行数
     */
    int insert(MessageDO messageDO);

    /**
     * 将会话中指定接收者的全部消息标记为已读。
     *
     * @param conversationId 会话 ID
     * @param receiverId     接收者用户 ID
     * @return 影响行数
     */
    int markAllRead(@Param("conversationId") Long conversationId,
                    @Param("receiverId") Long receiverId);

    /**
     * 查询下一个消息 ID。
     *
     * @return 下一个消息 ID
     */
    Long selectNextId();
}
