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

    /**
     * 根据 ID 查询评论（过滤已删除）。
     *
     * @param id 评论 ID
     * @return 评论数据对象
     */
    CommentDO selectById(@Param("id") Long id);

    /**
     * 根据 ID 查询评论（后台管理，包含已删除）。
     *
     * @param id 评论 ID
     * @return 评论数据对象
     */
    CommentDO selectByIdForAdmin(@Param("id") Long id);

    /**
     * 查询所有评论。
     *
     * @return 评论列表
     */
    List<CommentDO> selectAll();

    /**
     * 统计评论总数。
     *
     * @return 评论总数
     */
    long countAll();

    /**
     * 统计指定日期创建的评论数。
     *
     * @param date 日期
     * @return 评论数
     */
    long countCreatedOn(@Param("date") LocalDate date);

    /**
     * 统计指定日期及以后创建的评论数。
     *
     * @param date 起始日期
     * @return 评论数
     */
    long countCreatedSince(@Param("date") LocalDate date);

    /**
     * 后台管理分页查询评论列表。
     *
     * @param articleId 文章 ID
     * @param status    评论状态
     * @param keyword   关键字
     * @param offset    偏移量
     * @param limit     限制数量
     * @return 评论列表
     */
    List<CommentDO> selectAdminPage(@Param("articleId") Long articleId,
                                    @Param("status") String status,
                                    @Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    /**
     * 统计后台管理评论数量。
     *
     * @param articleId 文章 ID
     * @param status    评论状态
     * @param keyword   关键字
     * @return 评论数量
     */
    long countAdminPage(@Param("articleId") Long articleId,
                        @Param("status") String status,
                        @Param("keyword") String keyword);

    /**
     * 根据文章 ID 查询评论列表。
     *
     * @param articleId 文章 ID
     * @return 评论列表
     */
    List<CommentDO> selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 分页查询一级评论（根评论）。
     *
     * @param articleId 文章 ID
     * @param sort      排序方式
     * @param offset    偏移量
     * @param limit     限制数量
     * @return 根评论列表
     */
    List<CommentDO> selectRootPage(@Param("articleId") Long articleId,
                                   @Param("sort") String sort,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 统计指定文章的根评论数量。
     *
     * @param articleId 文章 ID
     * @return 根评论数量
     */
    long countRootByArticleId(@Param("articleId") Long articleId);

    /**
     * 分页查询指定根评论的回复列表。
     *
     * @param rootCommentId 根评论 ID
     * @param offset        偏移量
     * @param limit         限制数量
     * @return 回复列表
     */
    List<CommentDO> selectRepliesByRootCommentId(@Param("rootCommentId") Long rootCommentId,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    /**
     * 统计指定根评论的回复数量。
     *
     * @param rootCommentId 根评论 ID
     * @return 回复数量
     */
    long countRepliesByRootCommentId(@Param("rootCommentId") Long rootCommentId);

    /**
     * 批量查询多个根评论的回复预览（每个根评论取前 limitPerRoot 条）。
     *
     * @param rootCommentIds 根评论 ID 列表
     * @param limitPerRoot   每个根评论的预览数量
     * @return 回复预览列表
     */
    List<CommentDO> selectReplyPreviewByRootIds(@Param("rootCommentIds") List<Long> rootCommentIds,
                                                @Param("limitPerRoot") int limitPerRoot);

    /**
     * 根据评论 ID 列表批量查询评论。
     *
     * @param commentIds 评论 ID 列表
     * @return 评论列表
     */
    List<CommentDO> selectByIds(@Param("commentIds") List<Long> commentIds);

    /**
     * 查询根评论下的完整评论线（根评论+所有回复）。
     *
     * @param rootCommentId 根评论 ID
     * @return 评论线列表
     */
    List<CommentDO> selectThreadByRootCommentId(@Param("rootCommentId") Long rootCommentId);

    /**
     * 根据 ID 统计评论数量。
     *
     * @param id 评论 ID
     * @return 评论数量
     */
    int countById(@Param("id") Long id);

    /**
     * 批量统计多个根评论的回复数。
     *
     * @param rootCommentIds 根评论 ID 列表
     * @return rootCommentId -> replyCount 映射列表
     */
    List<Map<String, Object>> countRepliesByRootIds(@Param("rootCommentIds") List<Long> rootCommentIds);

    /**
     * 批量软删除一级评论及其全部回复。
     *
     * @param rootCommentId 根评论 ID
     * @return 影响行数
     */
    int deleteThreadByRootCommentId(@Param("rootCommentId") Long rootCommentId);

    /**
     * 查询下一个评论 ID。
     *
     * @return 下一个评论 ID
     */
    Long selectNextId();

    /**
     * 插入评论。
     *
     * @param commentDO 评论数据对象
     * @return 影响行数
     */
    int insert(CommentDO commentDO);

    /**
     * 插入或更新评论（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param commentDO 评论数据对象
     * @return 影响行数
     */
    int insertOrUpdate(CommentDO commentDO);

    /**
     * 更新评论。
     *
     * @param commentDO 评论数据对象
     * @return 影响行数
     */
    int update(CommentDO commentDO);

    /**
     * 清除指定文章下的置顶评论。
     *
     * @param articleId 文章 ID
     * @return 影响行数
     */
    int clearPinnedByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据 ID 删除评论。
     *
     * @param id 评论 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 原子递增评论点赞数（避免并发丢失更新）。
     *
     * @param commentId 评论 ID
     * @return 影响行数
     */
    int incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 原子递减评论点赞数（防止降为负数）。
     *
     * @param commentId 评论 ID
     * @return 影响行数
     */
    int decrementLikeCount(@Param("commentId") Long commentId);
}
