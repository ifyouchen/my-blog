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

    ConversationDO selectById(@Param("id") Long id);

    ConversationDO selectByParticipants(@Param("participantAId") Long participantAId,
                                        @Param("participantBId") Long participantBId);

    List<ConversationDO> selectByUser(@Param("userId") Long userId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    long countByUser(@Param("userId") Long userId);

    int insert(ConversationDO conversationDO);

    int updateLastMessage(@Param("id") Long id,
                          @Param("lastMessage") String lastMessage,
                          @Param("lastMessageAt") LocalDateTime lastMessageAt);

    int markDeletedByUser(@Param("id") Long id,
                          @Param("column") String column);

    Long selectNextId();
}
