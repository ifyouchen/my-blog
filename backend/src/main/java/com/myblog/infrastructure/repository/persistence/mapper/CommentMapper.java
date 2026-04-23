package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.CommentDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface CommentMapper {

    CommentDO selectById(@Param("id") Long id);

    List<CommentDO> selectAll();

    List<CommentDO> selectByArticleId(@Param("articleId") Long articleId);

    List<CommentDO> selectRootPage(@Param("articleId") Long articleId,
                                   @Param("sort") String sort,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    long countRootByArticleId(@Param("articleId") Long articleId);

    List<CommentDO> selectRepliesByRootCommentId(@Param("rootCommentId") Long rootCommentId,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    long countRepliesByRootCommentId(@Param("rootCommentId") Long rootCommentId);

    List<CommentDO> selectReplyPreviewByRootIds(@Param("rootCommentIds") List<Long> rootCommentIds,
                                                @Param("limitPerRoot") int limitPerRoot);

    List<CommentDO> selectByIds(@Param("commentIds") List<Long> commentIds);

    List<CommentDO> selectThreadByRootCommentId(@Param("rootCommentId") Long rootCommentId);

    int countById(@Param("id") Long id);

    Long selectNextId();

    int insert(CommentDO commentDO);

    int update(CommentDO commentDO);

    int clearPinnedByArticleId(@Param("articleId") Long articleId);

    int deleteById(@Param("id") Long id);
}
