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

    MessageDO selectById(@Param("id") Long id);

    List<MessageDO> selectByConversation(@Param("conversationId") Long conversationId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    long countByConversation(@Param("conversationId") Long conversationId);

    long countUnreadByConversation(@Param("conversationId") Long conversationId,
                                   @Param("receiverId") Long receiverId);

    long countTotalUnread(@Param("userId") Long userId);

    int insert(MessageDO messageDO);

    int markAllRead(@Param("conversationId") Long conversationId,
                    @Param("receiverId") Long receiverId);

    Long selectNextId();
}
