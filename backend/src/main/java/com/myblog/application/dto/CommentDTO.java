package com.myblog.application.dto;

import java.util.List;

/**
 * 评论数据传输对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CommentDTO {

    private Long id;
    private Long articleId;
    private Long userId;
    private Long rootCommentId;
    private Long parentId;
    private String content;
    private String status;
    private String createdAt;
    private String editedAt;
    private Integer editCount;
    private Integer replyCount;
    private Integer likeCount;
    private Boolean liked;
    private Boolean pinned;
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canPin;
    private Boolean author;
    private UserDTO user;
    private UserDTO replyToUser;
    private List<CommentDTO> replyPreview;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRootCommentId() {
        return rootCommentId;
    }

    public void setRootCommentId(Long rootCommentId) {
        this.rootCommentId = rootCommentId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(String editedAt) {
        this.editedAt = editedAt;
    }

    public Integer getEditCount() {
        return editCount;
    }

    public void setEditCount(Integer editCount) {
        this.editCount = editCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Boolean getLiked() {
        return liked;
    }

    public void setLiked(Boolean liked) {
        this.liked = liked;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public Boolean getCanEdit() {
        return canEdit;
    }

    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }

    public Boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }

    public Boolean getCanPin() {
        return canPin;
    }

    public void setCanPin(Boolean canPin) {
        this.canPin = canPin;
    }

    public Boolean getAuthor() {
        return author;
    }

    public void setAuthor(Boolean author) {
        this.author = author;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public UserDTO getReplyToUser() {
        return replyToUser;
    }

    public void setReplyToUser(UserDTO replyToUser) {
        this.replyToUser = replyToUser;
    }

    public List<CommentDTO> getReplyPreview() {
        return replyPreview;
    }

    public void setReplyPreview(List<CommentDTO> replyPreview) {
        this.replyPreview = replyPreview;
    }
}
