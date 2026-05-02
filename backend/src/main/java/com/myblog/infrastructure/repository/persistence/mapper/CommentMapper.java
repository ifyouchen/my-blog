package com.myblog.infrastructure.repository.persistence.mapper;

import com.myblog.infrastructure.repository.persistence.entity.CommentDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 评论 MyBatis Mapper。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface CommentMapper {

    CommentDO selectById(@Param("id") Long id);

    CommentDO selectByIdForAdmin(@Param("id") Long id);

    List<CommentDO> selectAll();

    long countAll();

    long countCreatedOn(@Param("date") LocalDate date);

    long countCreatedSince(@Param("date") LocalDate date);

    List<CommentDO> selectAdminPage(@Param("articleId") Long articleId,
                                    @Param("status") String status,
                                    @Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    long countAdminPage(@Param("articleId") Long articleId,
                        @Param("status") String status,
                        @Param("keyword") String keyword);

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

    /**
     * 批量统计多个根评论的回复数。
     */
    List<Map<String, Object>> countRepliesByRootIds(@Param("rootCommentIds") List<Long> rootCommentIds);

    /**
     * 批量软删除一级评论及其全部回复。
     */
    int deleteThreadByRootCommentId(@Param("rootCommentId") Long rootCommentId);

    Long selectNextId();

    int insert(CommentDO commentDO);
    int insertOrUpdate(CommentDO commentDO);


    int update(CommentDO commentDO);

    int clearPinnedByArticleId(@Param("articleId") Long articleId);

    int deleteById(@Param("id") Long id);

    /** 原子递增评论点赞数（避免并发丢失更新）。 */
    int incrementLikeCount(@Param("commentId") Long commentId);

    /** 原子递减评论点赞数（防止降为负数）。 */
    int decrementLikeCount(@Param("commentId") Long commentId);
}
