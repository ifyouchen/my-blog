package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.CommentId;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评论仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface CommentRepository {

    /**
     * 根据评论 ID 查询评论。
     *
     * @param commentId 评论 ID
     * @return 评论 Optional
     */
    Optional<Comment> findById(CommentId commentId);

    /**
     * 根据评论 ID 查询评论，包含待审核和已删除状态。
     *
     * @param commentId 评论 ID
     * @return 评论 Optional
     */
    Optional<Comment> findByIdForAdmin(CommentId commentId);

    /**
     * 根据文章 ID 查询评论列表。
     *
     * @param articleId 文章 ID
     * @return 评论列表
     */
    List<Comment> findByArticleId(ArticleId articleId);

    /**
     * 分页查询后台评论列表。
     *
     * @param articleId 文章 ID
     * @param keyword 关键字
     * @param page 页码
     * @param pageSize 每页大小
     * @return 评论列表
     */
    List<Comment> findAdminPage(Long articleId, String status, String keyword, int page, int pageSize);

    /**
     * 统计后台评论数量。
     *
     * @param articleId 文章 ID
     * @param keyword 关键字
     * @return 评论数量
     */
    long countAdminPage(Long articleId, String status, String keyword);

    /**
     * 分页查询文章一级评论。
     *
     * @param articleId 文章 ID
     * @param sort 排序方式
     * @param page 页码
     * @param pageSize 每页大小
     * @return 一级评论列表
     */
    List<Comment> findRootComments(ArticleId articleId, String sort, int page, int pageSize);

    /**
     * 统计文章一级评论数量。
     *
     * @param articleId 文章 ID
     * @return 一级评论数量
     */
    long countRootComments(ArticleId articleId);

    /**
     * 分页查询楼中楼回复。
     *
     * @param rootCommentId 根评论 ID
     * @param page 页码
     * @param pageSize 每页大小
     * @return 回复列表
     */
    List<Comment> findReplies(CommentId rootCommentId, int page, int pageSize);

    /**
     * 统计楼中楼回复数量。
     *
     * @param rootCommentId 根评论 ID
     * @return 回复数量
     */
    long countReplies(CommentId rootCommentId);

    /**
     * 查询多个根评论的回复预览。
     *
     * @param rootCommentIds 根评论 ID 列表
     * @param limitPerRoot 每个根评论的预览条数
     * @return 回复预览列表
     */
    List<Comment> findReplyPreviewByRootIds(List<Long> rootCommentIds, int limitPerRoot);

    /**
     * 根据评论 ID 批量查询评论。
     *
     * @param commentIds 评论 ID 列表
     * @return 评论列表
     */
    List<Comment> findByIds(List<Long> commentIds);

    /**
     * 查询一级评论整串楼中楼。
     *
     * @param rootCommentId 根评论 ID
     * @return 整串评论列表
     */
    List<Comment> findThreadByRootCommentId(CommentId rootCommentId);

    /**
     * 批量统计多个根评论的回复数量。
     *
     * @param rootCommentIds 根评论 ID 列表
     * @return rootCommentId -> replyCount 映射
     */
    Map<Long, Integer> countRepliesBatch(List<Long> rootCommentIds);

    /**
     * 批量软删除一级评论及其所有回复。
     *
     * @param rootCommentId 根评论 ID
     * @return 实际删除的评论数量
     */
    int deleteThreadByRootCommentId(CommentId rootCommentId);

    /**
     * 保存评论。
     *
     * @param comment 评论聚合根
     * @return 保存后的评论
     */
    Comment save(Comment comment);

    /**
     * 清空文章下的置顶评论。
     *
     * @param articleId 文章 ID
     */
    void clearPinnedByArticleId(ArticleId articleId);

    /**
     * 查询所有评论。
     *
     * @return 评论列表
     */
    List<Comment> findAll();

    /**
     * 统计未删除评论数量。
     *
     * @return 评论数量
     */
    long countAll();

    /**
     * 统计指定日期创建的评论数量。
     *
     * @param date 日期
     * @return 评论数量
     */
    long countCreatedOn(LocalDate date);

    /**
     * 统计指定日期之后创建的评论数量。
     *
     * @param date 起始日期
     * @return 评论数量
     */
    long countCreatedSince(LocalDate date);

    /**
     * 生成下一个评论 ID。
     *
     * @return 评论 ID
     */
    Long nextId();

    /**
     * 原子递增评论点赞数。
     *
     * @param commentId 评论 ID
     */
    void incrementLikeCount(Long commentId);

    /**
     * 原子递减评论点赞数。
     *
     * @param commentId 评论 ID
     */
    void decrementLikeCount(Long commentId);
}
