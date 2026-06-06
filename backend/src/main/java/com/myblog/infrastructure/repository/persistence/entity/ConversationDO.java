package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 会话数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ConversationDO {

    /** 会话 ID。 */
    private Long id;
    /** 参与者 A 的用户 ID。 */
    private Long participantAId;
    /** 参与者 B 的用户 ID。 */
    private Long participantBId;
    /** 最后一条消息的预览内容。 */
    private String lastMessage;
    /** 最后一条消息的发送时间。 */
    private LocalDateTime lastMessageAt;
    /** 参与者 A 删除会话的时间，为 null 表示未删除。 */
    private LocalDateTime aDeletedAt;
    /** 参与者 B 删除会话的时间，为 null 表示未删除。 */
    private LocalDateTime bDeletedAt;
    /** 参与者 A 是否置顶会话。 */
    private Boolean aPinned;
    /** 参与者 A 置顶时间。 */
    private LocalDateTime aPinnedAt;
    /** 参与者 A 是否开启消息免打扰。 */
    private Boolean aMuted;
    /** 参与者 B 是否置顶会话。 */
    private Boolean bPinned;
    /** 参与者 B 置顶时间。 */
    private LocalDateTime bPinnedAt;
    /** 参与者 B 是否开启消息免打扰。 */
    private Boolean bMuted;
    /** 记录创建时间。 */
    private LocalDateTime createdAt;
    /** 记录最后更新时间。 */
    private LocalDateTime updatedAt;
    /** 软删除时间，为 null 表示未删除。 */
    private LocalDateTime deletedAt;
    /** 乐观锁版本号。 */
    private Long version;

    /**
     * 获取会话 ID。
     *
     * @return 会话 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置会话 ID。
     *
     * @param id 会话 ID
     */
    public void setId(Long id) {
        this.id = id;
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
     * 设置参与者 A 的用户 ID。
     *
     * @param participantAId 参与者 A 用户 ID
     */
    public void setParticipantAId(Long participantAId) {
        this.participantAId = participantAId;
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
     * 设置参与者 B 的用户 ID。
     *
     * @param participantBId 参与者 B 用户 ID
     */
    public void setParticipantBId(Long participantBId) {
        this.participantBId = participantBId;
    }

    /**
     * 获取最后一条消息的预览内容。
     *
     * @return 最后一条消息预览
     */
    public String getLastMessage() {
        return lastMessage;
    }

    /**
     * 设置最后一条消息的预览内容。
     *
     * @param lastMessage 最后一条消息预览
     */
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    /**
     * 获取最后一条消息的发送时间。
     *
     * @return 最后消息发送时间
     */
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    /**
     * 设置最后一条消息的发送时间。
     *
     * @param lastMessageAt 最后消息发送时间
     */
    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
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
     * 设置参与者 A 删除会话的时间。
     *
     * @param aDeletedAt 参与者 A 删除时间
     */
    public void setADeletedAt(LocalDateTime aDeletedAt) {
        this.aDeletedAt = aDeletedAt;
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
     * 设置参与者 B 删除会话的时间。
     *
     * @param bDeletedAt 参与者 B 删除时间
     */
    public void setBDeletedAt(LocalDateTime bDeletedAt) {
        this.bDeletedAt = bDeletedAt;
    }

    /**
     * 获取参与者 A 是否置顶会话。
     *
     * @return 已置顶返回 true，否则返回 false
     */
    public Boolean getAPinned() {
        return aPinned;
    }

    /**
     * 设置参与者 A 是否置顶会话。
     *
     * @param aPinned 是否置顶
     */
    public void setAPinned(Boolean aPinned) {
        this.aPinned = aPinned;
    }

    /**
     * 获取参与者 A 置顶时间。
     *
     * @return 置顶时间
     */
    public LocalDateTime getAPinnedAt() {
        return aPinnedAt;
    }

    /**
     * 设置参与者 A 置顶时间。
     *
     * @param aPinnedAt 置顶时间
     */
    public void setAPinnedAt(LocalDateTime aPinnedAt) {
        this.aPinnedAt = aPinnedAt;
    }

    /**
     * 获取参与者 A 是否开启消息免打扰。
     *
     * @return 已免打扰返回 true，否则返回 false
     */
    public Boolean getAMuted() {
        return aMuted;
    }

    /**
     * 设置参与者 A 是否开启消息免打扰。
     *
     * @param aMuted 是否免打扰
     */
    public void setAMuted(Boolean aMuted) {
        this.aMuted = aMuted;
    }

    /**
     * 获取参与者 B 是否置顶会话。
     *
     * @return 已置顶返回 true，否则返回 false
     */
    public Boolean getBPinned() {
        return bPinned;
    }

    /**
     * 设置参与者 B 是否置顶会话。
     *
     * @param bPinned 是否置顶
     */
    public void setBPinned(Boolean bPinned) {
        this.bPinned = bPinned;
    }

    /**
     * 获取参与者 B 置顶时间。
     *
     * @return 置顶时间
     */
    public LocalDateTime getBPinnedAt() {
        return bPinnedAt;
    }

    /**
     * 设置参与者 B 置顶时间。
     *
     * @param bPinnedAt 置顶时间
     */
    public void setBPinnedAt(LocalDateTime bPinnedAt) {
        this.bPinnedAt = bPinnedAt;
    }

    /**
     * 获取参与者 B 是否开启消息免打扰。
     *
     * @return 已免打扰返回 true，否则返回 false
     */
    public Boolean getBMuted() {
        return bMuted;
    }

    /**
     * 设置参与者 B 是否开启消息免打扰。
     *
     * @param bMuted 是否免打扰
     */
    public void setBMuted(Boolean bMuted) {
        this.bMuted = bMuted;
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
