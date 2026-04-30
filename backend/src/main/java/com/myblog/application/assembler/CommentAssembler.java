package com.myblog.application.assembler;

import com.myblog.application.dto.CommentDTO;
import com.myblog.application.dto.UserDTO;
import com.myblog.domain.model.aggregate.Comment;
import com.myblog.domain.model.aggregate.User;

import java.time.format.DateTimeFormatter;

/**
 * 评论应用组装器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class CommentAssembler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private CommentAssembler() {
    }

    /**
     * 将评论聚合根转换为评论 DTO。
     *
     * @param comment 评论聚合根
     * @param user 评论用户
     * @return 评论 DTO
     */
    public static CommentDTO toDTO(Comment comment, User user) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId().getValue());
        dto.setArticleId(comment.getArticleId().getValue());
        dto.setUserId(comment.getUserId().getValue());
        dto.setRootCommentId(comment.getRootCommentId());
        dto.setParentId(comment.getParentId());
        dto.setContent(comment.getContent());
        dto.setStatus(comment.getStatus().name());
        dto.setLikeCount(comment.getLikeCount());
        dto.setEditCount(comment.getEditCount());
        dto.setPinned(comment.isPinned());
        if (comment.getCreatedAt() != null) {
            dto.setCreatedAt(FORMATTER.format(comment.getCreatedAt()));
        }
        if (comment.getEditedAt() != null) {
            dto.setEditedAt(FORMATTER.format(comment.getEditedAt()));
        }
        if (user != null) {
            dto.setUser(UserAssembler.toDTO(user));
        }
        return dto;
    }
}
