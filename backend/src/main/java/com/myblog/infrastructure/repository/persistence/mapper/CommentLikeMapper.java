package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.CommentLikeDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论点赞 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface CommentLikeMapper {

    CommentLikeDO selectByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    CommentLikeDO selectAnyByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    int countByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    List<CommentLikeDO> selectByCommentIdsAndUser(@Param("commentIds") List<Long> commentIds, @Param("userId") Long userId);

    int countById(@Param("id") Long id);

    Long selectNextId();

    int insert(CommentLikeDO commentLikeDO);
    int insertOrUpdate(CommentLikeDO commentLikeDO);


    int update(CommentLikeDO commentLikeDO);
}
