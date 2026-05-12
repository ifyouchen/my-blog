package com.myblog.application.command;

import com.myblog.shared.enums.NotificationType;

/**
 * 创建通知命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CreateNotificationCommand {

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
    private NotificationType type;

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
     * 获取通知接收人用户 ID。
     *
     * @return 通知接收人用户 ID
     */
    public Long getReceiverUserId() {
        return receiverUserId;
    }

    /**
     * 设置通知接收人用户 ID。
     *
     * @param receiverUserId 通知接收人用户 ID
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
    public NotificationType getType() {
        return type;
    }

    /**
     * 设置通知类型。
     *
     * @param type 通知类型
     */
    public void setType(NotificationType type) {
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
}