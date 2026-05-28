package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.MessageId;

import java.time.LocalDateTime;

/**
 * 消息聚合根。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class Message {

    /**
     * 消息 ID
     */
    private MessageId id;

    /**
     * 所属会话 ID
     */
    private Long conversationId;

    /**
     * 消息发送人用户 ID
     */
    private Long senderId;

    /**
     * 被回复消息 ID
     */
    private Long parentId;

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
     * 撤回时间，为 null 表示未撤回
     */
    private LocalDateTime recalledAt;

    /**
     * 消息创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 消息最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 消息软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Long version;

    private Message() {
    }

    /**
     * 创建新消息。
     *
     * @param id             消息 ID
     * @param conversationId 所属会话 ID
     * @param senderId       发送人用户 ID
     * @param content        消息内容
     * @param type           消息类型
     * @param parentId       被回复消息 ID，可为 null
     * @return 消息聚合根
     */
    public static Message create(Long id, Long conversationId, Long senderId, String content, String type, Long parentId) {
        Message message = new Message();
        message.id = new MessageId(id);
        message.conversationId = conversationId;
        message.senderId = senderId;
        message.parentId = parentId;
        message.content = content;
        message.type = type != null ? type : "TEXT";
        message.createdAt = LocalDateTime.now();
        message.updatedAt = message.createdAt;
        message.deletedAt = null;
        message.version = 0L;
        return message;
    }

    /**
     * 从持久化数据恢复消息聚合根。
     *
     * @param id                 消息 ID
     * @param conversationId     所属会话 ID
     * @param senderId           发送人用户 ID
     * @param parentId           被回复消息 ID
     * @param content            消息内容
     * @param type               消息类型
     * @param readAt             已读时间
     * @param senderDeletedAt    发送方删除时间
     * @param receiverDeletedAt  接收方删除时间
     * @param recalledAt         撤回时间
     * @param createdAt          创建时间
     * @param updatedAt          更新时间
     * @param deletedAt          删除时间
     * @param version            乐观锁版本号
     * @return 消息聚合根
     */
    public static Message restore(Long id, Long conversationId, Long senderId, Long parentId,
                                  String content, String type, LocalDateTime readAt,
                                  LocalDateTime senderDeletedAt, LocalDateTime receiverDeletedAt,
                                  LocalDateTime recalledAt,
                                  LocalDateTime createdAt, LocalDateTime updatedAt,
                                  LocalDateTime deletedAt, Long version) {
        Message message = new Message();
        message.id = new MessageId(id);
        message.conversationId = conversationId;
        message.senderId = senderId;
        message.parentId = parentId;
        message.content = content;
        message.type = type;
        message.readAt = readAt;
        message.senderDeletedAt = senderDeletedAt;
        message.receiverDeletedAt = receiverDeletedAt;
        message.recalledAt = recalledAt;
        message.createdAt = createdAt;
        message.updatedAt = updatedAt;
        message.deletedAt = deletedAt;
        message.version = version;
        return message;
    }

    /**
     * 撤回消息。
     *
     * @param now 撤回时间
     */
    public void recall(LocalDateTime now) {
        this.recalledAt = now;
        this.updatedAt = now;
    }

    /**
     * 判断消息是否可以被撤回（10分钟内）。
     *
     * @param now 当前时间
     * @return 可撤回返回 true
     */
    public boolean canRecall(LocalDateTime now) {
        return recalledAt == null && createdAt != null
            && createdAt.plusMinutes(10).isAfter(now);
    }

    /**
     * 将消息标记为已读，幂等操作（已读则跳过）。
     */
    public void markRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
            this.updatedAt = this.readAt;
        }
    }

    /**
     * 判断消息是否已读。
     *
     * @return 已读返回 true，否则返回 false
     */
    public boolean isRead() {
        return this.readAt != null;
    }

    /**
     * 发送方删除消息（仅对发送方不可见，接收方仍可见）。
     */
    public void deleteBySender() {
        this.senderDeletedAt = LocalDateTime.now();
        this.updatedAt = this.senderDeletedAt;
    }

    /**
     * 接收方删除消息（仅对接收方不可见，发送方仍可见）。
     */
    public void deleteByReceiver() {
        this.receiverDeletedAt = LocalDateTime.now();
        this.updatedAt = this.receiverDeletedAt;
    }

    /**
     * 获取消息 ID。
     *
     * @return 消息 ID
     */
    public MessageId getId() {
        return id;
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
     * 获取消息发送人用户 ID。
     *
     * @return 发送人用户 ID
     */
    public Long getSenderId() {
        return senderId;
    }

    /**
     * 获取被回复消息 ID。
     *
     * @return 被回复消息 ID，可能为 null
     */
    public Long getParentId() {
        return parentId;
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
     * 获取消息类型。
     *
     * @return 消息类型
     */
    public String getType() {
        return type;
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
     * 获取撤回时间。
     *
     * @return 撤回时间，未撤回则为 null
     */
    public LocalDateTime getRecalledAt() {
        return recalledAt;
    }

    /**
     * 判断消息是否已撤回。
     *
     * @return 已撤回返回 true
     */
    public boolean isRecalled() {
        return this.recalledAt != null;
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
     * 获取接收方删除消息的时间。
     *
     * @return 接收方删除时间，未删除则为 null
     */
    public LocalDateTime getReceiverDeletedAt() {
        return receiverDeletedAt;
    }

    /**
     * 获取消息创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取消息最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取消息软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Long getVersion() {
        return version;
    }
}
