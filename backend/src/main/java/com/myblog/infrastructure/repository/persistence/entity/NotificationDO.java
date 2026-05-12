package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 通知数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class NotificationDO {

    /**
     * 通知 ID
     */
    private Long id;

    /**
     * 通知接收人用户 ID
     */
    private Long receiverUserId;

    /**
     * 触发通知的行为人用户 ID
     */
    private Long actorUserId;

    /**
     * 通知类型
     */
    private String type;

    /**
     * 关联文章 ID（可为 null）
     */
    private Long articleId;

    /**
     * 关联评论 ID（可为 null）
     */
    private Long commentId;

    /**
     * 扩展数据 JSON（可为 null）
     */
    private String payloadJson;

    /**
     * 通知已读时间，为 null 表示未读
     */
    private LocalDateTime readAt;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Long version;

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
     * 获取通知接收人用户 ID。
     *
     * @return 接收人用户 ID
     */
    public Long getReceiverUserId() {
        return receiverUserId;
    }

    /**
     * 设置通知接收人用户 ID。
     *
     * @param receiverUserId 接收人用户 ID
     */
    public void setReceiverUserId(Long receiverUserId) {
        this.receiverUserId = receiverUserId;
    }

    /**
     * 获取触发通知的行为人用户 ID。
     *
     * @return 行为人用户 ID
     */
    public Long getActorUserId() {
        return actorUserId;
    }

    /**
     * 设置触发通知的行为人用户 ID。
     *
     * @param actorUserId 行为人用户 ID
     */
    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
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
     * 获取扩展数据 JSON。
     *
     * @return 扩展数据 JSON
     */
    public String getPayloadJson() {
        return payloadJson;
    }

    /**
     * 设置扩展数据 JSON。
     *
     * @param payloadJson 扩展数据 JSON
     */
    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    /**
     * 获取通知已读时间。
     *
     * @return 已读时间，未读则为 null
     */
    public LocalDateTime getReadAt() {
        return readAt;
    }

    /**
     * 设置通知已读时间。
     *
     * @param readAt 已读时间
     */
    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    /**
     * 获取记录创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置记录创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取记录最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置记录最后更新时间。
     *
     * @param updatedAt 最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置软删除时间。
     *
     * @param deletedAt 删除时间
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Long getVersion() {
        return version;
    }

    /**
     * 设置乐观锁版本号。
     *
     * @param version 版本号
     */
    public void setVersion(Long version) {
        this.version = version;
    }
}