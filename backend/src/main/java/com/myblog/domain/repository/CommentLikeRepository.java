package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.CommentLike;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 评论点赞仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface CommentLikeRepository {

    Optional<CommentLike> findByCommentAndUser(CommentId commentId, UserId userId);

    Optional<CommentLike> findAnyByCommentAndUser(CommentId commentId, UserId userId);

    boolean exists(CommentId commentId, UserId userId);

    List<CommentLike> findByCommentIdsAndUser(List<Long> commentIds, UserId userId);

    CommentLike save(CommentLike commentLike);

    Long nextId();
}
