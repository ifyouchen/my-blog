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

    /**
     * 查询有效的评论点赞记录（未取消）。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 评论点赞数据对象
     */
    CommentLikeDO selectByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 查询评论点赞记录（包含已逻辑删除的记录）。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 评论点赞数据对象
     */
    CommentLikeDO selectAnyByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 统计有效的评论点赞数量。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 评论点赞数量
     */
    int countByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    /**
     * 批量查询用户对多条评论的点赞记录。
     *
     * @param commentIds 评论 ID 列表
     * @param userId     用户 ID
     * @return 评论点赞列表
     */
    List<CommentLikeDO> selectByCommentIdsAndUser(@Param("commentIds") List<Long> commentIds, @Param("userId") Long userId);

    /**
     * 根据主键统计评论点赞记录数量。
     *
     * @param id 点赞记录 ID
     * @return 记录数量
     */
    int countById(@Param("id") Long id);

    /**
     * 查询下一个点赞记录 ID。
     *
     * @return 下一个点赞记录 ID
     */
    Long selectNextId();

    /**
     * 插入评论点赞记录。
     *
     * @param commentLikeDO 评论点赞数据对象
     * @return 影响行数
     */
    int insert(CommentLikeDO commentLikeDO);

    /**
     * 插入或更新评论点赞记录（INSERT ... ON DUPLICATE KEY UPDATE）。
     *
     * @param commentLikeDO 评论点赞数据对象
     * @return 影响行数
     */
    int insertOrUpdate(CommentLikeDO commentLikeDO);

    /**
     * 更新评论点赞记录。
     *
     * @param commentLikeDO 评论点赞数据对象
     * @return 影响行数
     */
    int update(CommentLikeDO commentLikeDO);
}
