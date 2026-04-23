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
