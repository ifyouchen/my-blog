package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 消息数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class MessageDO {

    /**
     * 消息 ID
     */
    private Long id;

    /**
     * 所属会话 ID
     */
    private Long conversationId;

    /**
     * 消息发送人用户 ID
     */
    private Long senderId;

    /**
     * 消息正文内容
     */
    private String content;

    /**
     * 消息类型（TEXT / IMAGE 等）
     */
    private String type;

    /**
     * 消息已读时间，为 null 表示未读
     */
    private LocalDateTime readAt;

    /**
     * 发送方删除消息的时间，为 null 表示未删除
     */
    private LocalDateTime senderDeletedAt;

    /**
     * 接收方删除消息的时间，为 null 表示未删除
     */
    private LocalDateTime receiverDeletedAt;

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
     * 获取消息 ID。
     *
     * @return 消息 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置消息 ID。
     *
     * @param id 消息 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取所属会话 ID。
     *
     * @return 会话 ID
     */
    public Long getConversationId() {
        return conversationId;
    }

    /**
     * 设置所属会话 ID。
     *
     * @param conversationId 会话 ID
     */
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * 获取消息发送人用户 ID。
     *
     * @return 发送人用户 ID
     */
    public Long getSenderId() {
        return senderId;
    }

    /**
     * 设置消息发送人用户 ID。
     *
     * @param senderId 发送人用户 ID
     */
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    /**
     * 获取消息正文内容。
     *
     * @return 消息正文内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息正文内容。
     *
     * @param content 消息正文内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取消息类型。
     *
     * @return 消息类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置消息类型。
     *
     * @param type 消息类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取消息已读时间。
     *
     * @return 已读时间，未读则为 null
     */
    public LocalDateTime getReadAt() {
        return readAt;
    }

    /**
     * 设置消息已读时间。
     *
     * @param readAt 已读时间
     */
    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    /**
     * 获取发送方删除消息的时间。
     *
     * @return 发送方删除时间，未删除则为 null
     */
    public LocalDateTime getSenderDeletedAt() {
        return senderDeletedAt;
    }

    /**
     * 设置发送方删除消息的时间。
     *
     * @param senderDeletedAt 发送方删除时间
     */
    public void setSenderDeletedAt(LocalDateTime senderDeletedAt) {
        this.senderDeletedAt = senderDeletedAt;
    }

    /**
     * 获取接收方删除消息的时间。
     *
     * @return 接收方删除时间，未删除则为 null
     */
    public LocalDateTime getReceiverDeletedAt() {
        return receiverDeletedAt;
    }

    /**
     * 设置接收方删除消息的时间。
     *
     * @param receiverDeletedAt 接收方删除时间
     */
    public void setReceiverDeletedAt(LocalDateTime receiverDeletedAt) {
        this.receiverDeletedAt = receiverDeletedAt;
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
