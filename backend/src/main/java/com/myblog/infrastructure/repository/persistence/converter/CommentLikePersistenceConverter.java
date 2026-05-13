package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.CommentLike;
import com.myblog.domain.model.valueobject.CommentId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.infrastructure.repository.persistence.entity.CommentLikeDO;

/**
 * 评论点赞持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class CommentLikePersistenceConverter {

    private CommentLikePersistenceConverter() {
    }

    /**
     * 将评论点赞数据对象转换为领域对象。
     *
     * @param commentLikeDO 评论点赞数据对象
     * @return 评论点赞领域对象，若 commentLikeDO 为 null 则返回 null
     */
    public static CommentLike toDomain(CommentLikeDO commentLikeDO) {
        if (commentLikeDO == null) {
            return null;
        }
        return CommentLike.restore(
            commentLikeDO.getId(),
            new CommentId(commentLikeDO.getCommentId()),
            new UserId(commentLikeDO.getUserId()),
            commentLikeDO.getCreatedAt(),
            commentLikeDO.getUpdatedAt(),
            commentLikeDO.getDeletedAt(),
            commentLikeDO.getVersion()
        );
    }

    /**
     * 将评论点赞领域对象转换为数据对象。
     *
     * @param commentLike 评论点赞领域对象
     * @return 评论点赞数据对象
     */
    public static CommentLikeDO toData(CommentLike commentLike) {
        CommentLikeDO commentLikeDO = new CommentLikeDO();
        commentLikeDO.setId(commentLike.getId().getValue());
        commentLikeDO.setCommentId(commentLike.getCommentId().getValue());
        commentLikeDO.setUserId(commentLike.getUserId().getValue());
        commentLikeDO.setCreatedAt(commentLike.getCreatedAt());
        commentLikeDO.setUpdatedAt(commentLike.getUpdatedAt());
        commentLikeDO.setDeletedAt(commentLike.getDeletedAt());
        commentLikeDO.setVersion(commentLike.getVersion());
        return commentLikeDO;
    }
}
