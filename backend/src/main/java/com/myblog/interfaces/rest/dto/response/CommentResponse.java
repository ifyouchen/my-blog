package com.myblog.interfaces.rest.dto.response;

import java.util.List;

/**
 * 评论响应。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CommentResponse {

    private Long id;
    private Long articleId;
    private Long userId;
    private Long rootCommentId;
    private Long parentId;
    private String content;
    private String createdAt;
    private Integer replyCount;
    private Integer likeCount;
    private Boolean liked;
    private Boolean pinned;
    private Boolean canDelete;
    private Boolean canPin;
    private Boolean author;
    private UserResponse user;
    private UserResponse replyToUser;
    private List<CommentResponse> replyPreview;

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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
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

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public UserResponse getReplyToUser() {
        return replyToUser;
    }

    public void setReplyToUser(UserResponse replyToUser) {
        this.replyToUser = replyToUser;
    }

    public List<CommentResponse> getReplyPreview() {
        return replyPreview;
    }

    public void setReplyPreview(List<CommentResponse> replyPreview) {
        this.replyPreview = replyPreview;
    }
}
