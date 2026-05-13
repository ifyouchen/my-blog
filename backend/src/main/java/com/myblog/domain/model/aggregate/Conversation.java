package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ConversationId;

import java.time.LocalDateTime;

/**
 * 会话聚合根。
 * 两个用户之间只有一个会话，参与者按 ID 大小排序。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class Conversation {

    /**
     * 会话 ID
     */
    private ConversationId id;

    /**
     * 参与者 A 的用户 ID（ID 较小的一方）
     */
    private Long participantAId;

    /**
     * 参与者 B 的用户 ID（ID 较大的一方）
     */
    private Long participantBId;

    /**
     * 最后一条消息的内容摘要（最多 500 字符）
     */
    private String lastMessage;

    /**
     * 最后一条消息的发送时间
     */
    private LocalDateTime lastMessageAt;

    /**
     * 参与者 A 删除会话的时间，为 null 表示未删除
     */
    private LocalDateTime aDeletedAt;

    /**
     * 参与者 B 删除会话的时间，为 null 表示未删除
     */
    private LocalDateTime bDeletedAt;

    /**
     * 会话创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 会话最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 会话软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Long version;

    private Conversation() {
    }

    /**
     * 创建新会话。
     *
     * @param id             会话 ID
     * @param participantAId 参与者 A（ID 较小）
     * @param participantBId 参与者 B（ID 较大）
     * @return 会话聚合根
     */
    public static Conversation create(Long id, Long participantAId, Long participantBId) {
        Conversation conversation = new Conversation();
        conversation.id = new ConversationId(id);
        conversation.participantAId = participantAId;
        conversation.participantBId = participantBId;
        conversation.createdAt = LocalDateTime.now();
        conversation.updatedAt = conversation.createdAt;
        conversation.deletedAt = null;
        conversation.version = 0L;
        return conversation;
    }

    /**
     * 从持久化数据恢复会话聚合根。
     *
     * @param id             会话 ID
     * @param participantAId 参与者 A 用户 ID
     * @param participantBId 参与者 B 用户 ID
     * @param lastMessage    最后一条消息摘要
     * @param lastMessageAt  最后一条消息时间
     * @param aDeletedAt     参与者 A 删除时间
     * @param bDeletedAt     参与者 B 删除时间
     * @param createdAt      创建时间
     * @param updatedAt      更新时间
     * @param deletedAt      删除时间
     * @param version        乐观锁版本号
     * @return 会话聚合根
     */
    public static Conversation restore(Long id, Long participantAId, Long participantBId,
                                       String lastMessage, LocalDateTime lastMessageAt,
                                       LocalDateTime aDeletedAt, LocalDateTime bDeletedAt,
                                       LocalDateTime createdAt, LocalDateTime updatedAt,
                                       LocalDateTime deletedAt, Long version) {
        Conversation conversation = new Conversation();
        conversation.id = new ConversationId(id);
        conversation.participantAId = participantAId;
        conversation.participantBId = participantBId;
        conversation.lastMessage = lastMessage;
        conversation.lastMessageAt = lastMessageAt;
        conversation.aDeletedAt = aDeletedAt;
        conversation.bDeletedAt = bDeletedAt;
        conversation.createdAt = createdAt;
        conversation.updatedAt = updatedAt;
        conversation.deletedAt = deletedAt;
        conversation.version = version;
        return conversation;
    }

    /**
     * 更新最后一条消息摘要，超过 500 字符时自动截断。
     *
     * @param content 消息内容
     */
    public void updateLastMessage(String content) {
        this.lastMessage = content.length() > 500 ? content.substring(0, 500) : content;
        this.lastMessageAt = LocalDateTime.now();
        this.updatedAt = this.lastMessageAt;
    }

    /**
     * 指定用户删除会话（仅对该用户不可见）。
     *
     * @param userId 执行删除操作的用户 ID
     */
    public void deleteByUser(Long userId) {
        if (userId.equals(participantAId)) {
            this.aDeletedAt = LocalDateTime.now();
        } else if (userId.equals(participantBId)) {
            this.bDeletedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断指定用户是否已删除该会话。
     *
     * @param userId 用户 ID
     * @return 已删除返回 true，否则返回 false
     */
    public boolean isDeletedByUser(Long userId) {
        if (userId.equals(participantAId)) {
            return aDeletedAt != null;
        } else if (userId.equals(participantBId)) {
            return bDeletedAt != null;
        }
        return false;
    }

    /**
     * 获取对方参与者的用户 ID。
     *
     * @param userId 当前用户 ID
     * @return 对方用户 ID
     */
    public Long getOtherParticipantId(Long userId) {
        if (userId.equals(participantAId)) {
            return participantBId;
        }
        return participantAId;
    }

    /**
     * 获取会话 ID。
     *
     * @return 会话 ID
     */
    public ConversationId getId() {
        return id;
    }

    /**
     * 获取参与者 A 的用户 ID。
     *
     * @return 参与者 A 用户 ID
     */
    public Long getParticipantAId() {
        return participantAId;
    }

    /**
     * 获取参与者 B 的用户 ID。
     *
     * @return 参与者 B 用户 ID
     */
    public Long getParticipantBId() {
        return participantBId;
    }

    /**
     * 获取最后一条消息的内容摘要。
     *
     * @return 最后一条消息内容摘要
     */
    public String getLastMessage() {
        return lastMessage;
    }

    /**
     * 获取最后一条消息的发送时间。
     *
     * @return 最后一条消息发送时间
     */
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    /**
     * 获取参与者 A 删除会话的时间。
     *
     * @return 参与者 A 删除时间，未删除则为 null
     */
    public LocalDateTime getADeletedAt() {
        return aDeletedAt;
    }

    /**
     * 获取参与者 B 删除会话的时间。
     *
     * @return 参与者 B 删除时间，未删除则为 null
     */
    public LocalDateTime getBDeletedAt() {
        return bDeletedAt;
    }

    /**
     * 获取会话创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取会话最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取会话软删除时间。
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
