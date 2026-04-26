package com.myblog.application.dto;

import java.util.Map;

/**
 * 通知数据传输对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class NotificationDTO {

    private Long id;
    private String type;
    private Boolean read;
    private String createdAt;
    private Long articleId;
    private Long commentId;
    private UserDTO actor;
    private Map<String, Object> payload;
    private String targetUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public UserDTO getActor() {
        return actor;
    }

    public void setActor(UserDTO actor) {
        this.actor = actor;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}