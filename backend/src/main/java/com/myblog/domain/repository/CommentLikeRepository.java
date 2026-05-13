package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.CommentLike;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 评论点赞仓储接口。
 *
 * @author my-blog
 * @since 1.0.0
 */
public interface CommentLikeRepository {

    /**
     * 查询当前有效的评论点赞记录。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 点赞记录 Optional
     */
    Optional<CommentLike> findByCommentAndUser(CommentId commentId, UserId userId);

    /**
     * 查询评论点赞记录（包含已取消/逻辑删除的记录）。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 点赞记录 Optional
     */
    Optional<CommentLike> findAnyByCommentAndUser(CommentId commentId, UserId userId);

    /**
     * 判断用户是否已对评论点赞（仅有效记录）。
     *
     * @param commentId 评论 ID
     * @param userId    用户 ID
     * @return 是否已点赞
     */
    boolean exists(CommentId commentId, UserId userId);

    /**
     * 批量查询用户对多条评论的点赞记录。
     *
     * @param commentIds 评论 ID 列表
     * @param userId     用户 ID
     * @return 点赞记录列表
     */
    List<CommentLike> findByCommentIdsAndUser(List<Long> commentIds, UserId userId);

    /**
     * 保存评论点赞记录（新增或更新）。
     *
     * @param commentLike 评论点赞聚合根
     * @return 保存后的评论点赞记录
     */
    CommentLike save(CommentLike commentLike);

    /**
     * 生成下一个评论点赞记录 ID。
     *
     * @return 点赞记录 ID
     */
    Long nextId();
}
