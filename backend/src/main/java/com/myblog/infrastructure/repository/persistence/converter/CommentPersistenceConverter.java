package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.Comment.CommentStatus;
import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.infrastructure.repository.persistence.entity.CommentDO;

/**
 * 评论持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class CommentPersistenceConverter {

    /**
     * 私有构造函数，防止实例化。
     */
    private CommentPersistenceConverter() {
    }

    /**
     * 将评论数据对象转换为领域对象。
     *
     * @param commentDO 评论数据对象
     * @return 评论领域对象
     */
    public static Comment toDomain(CommentDO commentDO) {
        if (commentDO == null) {
            return null;
        }
        return Comment.restore(
            commentDO.getId(),
            new ArticleId(commentDO.getArticleId()),
            new UserId(commentDO.getUserId()),
            commentDO.getRootCommentId(),
            commentDO.getParentId(),
            commentDO.getContent(),
            CommentStatus.valueOf(commentDO.getStatus()),
            commentDO.getLikeCount(),
            commentDO.getEditedAt(),
            commentDO.getEditCount(),
            commentDO.getPinned(),
            commentDO.getPinnedAt(),
            commentDO.getCreatedAt(),
            commentDO.getUpdatedAt()
        );
    }

    /**
     * 将评论领域对象转换为数据对象。
     *
     * @param comment 评论领域对象
     * @return 评论数据对象
     */
    public static CommentDO toData(Comment comment) {
        CommentDO commentDO = new CommentDO();
        commentDO.setId(comment.getId().getValue());
        commentDO.setArticleId(comment.getArticleId().getValue());
        commentDO.setUserId(comment.getUserId().getValue());
        commentDO.setRootCommentId(comment.getRootCommentId());
        commentDO.setParentId(comment.getParentId());
        commentDO.setContent(comment.getContent());
        commentDO.setStatus(comment.getStatus().name());
        commentDO.setLikeCount(comment.getLikeCount());
        commentDO.setEditedAt(comment.getEditedAt());
        commentDO.setEditCount(comment.getEditCount());
        commentDO.setPinned(comment.isPinned());
        commentDO.setPinnedAt(comment.getPinnedAt());
        commentDO.setCreatedAt(comment.getCreatedAt());
        commentDO.setUpdatedAt(comment.getUpdatedAt());
        return commentDO;
    }
}
