package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.ConversationDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ConversationMapper {

    /**
     * 根据 ID 查询会话。
     *
     * @param id 会话 ID
     * @return 会话数据对象
     */
    ConversationDO selectById(@Param("id") Long id);

    /**
     * 根据两个参与者查询会话。
     *
     * @param participantAId 参与者 A 用户 ID
     * @param participantBId 参与者 B 用户 ID
     * @return 会话数据对象
     */
    ConversationDO selectByParticipants(@Param("participantAId") Long participantAId,
                                        @Param("participantBId") Long participantBId);

    /**
     * 分页查询用户参与的会话列表。
     *
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 会话列表
     */
    List<ConversationDO> selectByUser(@Param("userId") Long userId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /**
     * 统计用户参与的会话数量。
     *
     * @param userId 用户 ID
     * @return 会话数量
     */
    long countByUser(@Param("userId") Long userId);

    /**
     * 插入会话。
     *
     * @param conversationDO 会话数据对象
     * @return 影响行数
     */
    int insert(ConversationDO conversationDO);

    /**
     * 更新会话最后一条消息信息。
     *
     * @param id            会话 ID
     * @param lastMessage   最后一条消息内容
     * @param lastMessageAt 最后一条消息时间
     * @return 影响行数
     */
    int updateLastMessage(@Param("id") Long id,
                          @Param("lastMessage") String lastMessage,
                          @Param("lastMessageAt") LocalDateTime lastMessageAt);

    /**
     * 标记用户已删除会话（软删除，更新对应的 deleted_at 字段）。
     *
     * @param id     会话 ID
     * @param column 要更新的字段名（a_deleted_at 或 b_deleted_at）
     * @return 影响行数
     */
    int markDeletedByUser(@Param("id") Long id,
                          @Param("column") String column);

    /**
     * 查询下一个会话 ID。
     *
     * @return 下一个会话 ID
     */
    Long selectNextId();
}
