package com.myblog.application.dto;

import java.util.Map;

/**
 * 通知数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class NotificationDTO {

    /** 通知 ID */
    private Long id;
    /** 通知类型 */
    private String type;
    /** 是否已读 */
    private Boolean read;
    /** 创建时间 */
    private String createdAt;
    /** 关联文章 ID */
    private Long articleId;
    /** 关联评论 ID */
    private Long commentId;
    /** 触发通知的行为人 */
    private UserDTO actor;
    /** 扩展数据 */
    private Map<String, Object> payload;
    /** 跳转链接 */
    private String targetUrl;

    /**
     * 获取通知 ID。
     *
     * @return 通知 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置通知 ID。
     *
     * @param id 通知 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取通知类型。
     *
     * @return 通知类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置通知类型。
     *
     * @param type 通知类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取是否已读。
     *
     * @return 是否已读
     */
    public Boolean getRead() {
        return read;
    }

    /**
     * 设置是否已读。
     *
     * @param read 是否已读
     */
    public void setRead(Boolean read) {
        this.read = read;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取关联文章 ID。
     *
     * @return 关联文章 ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置关联文章 ID。
     *
     * @param articleId 关联文章 ID
     */
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    /**
     * 获取关联评论 ID。
     *
     * @return 关联评论 ID
     */
    public Long getCommentId() {
        return commentId;
    }

    /**
     * 设置关联评论 ID。
     *
     * @param commentId 关联评论 ID
     */
    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    /**
     * 获取触发通知的行为人。
     *
     * @return 行为人信息
     */
    public UserDTO getActor() {
        return actor;
    }

    /**
     * 设置触发通知的行为人。
     *
     * @param actor 行为人信息
     */
    public void setActor(UserDTO actor) {
        this.actor = actor;
    }

    /**
     * 获取扩展数据。
     *
     * @return 扩展数据
     */
    public Map<String, Object> getPayload() {
        return payload;
    }

    /**
     * 设置扩展数据。
     *
     * @param payload 扩展数据
     */
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    /**
     * 获取跳转链接。
     *
     * @return 跳转链接
     */
    public String getTargetUrl() {
        return targetUrl;
    }

    /**
     * 设置跳转链接。
     *
     * @param targetUrl 跳转链接
     */
    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}