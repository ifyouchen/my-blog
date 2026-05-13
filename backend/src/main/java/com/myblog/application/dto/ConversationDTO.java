package com.myblog.application.dto;

/**
 * 会话数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ConversationDTO {

    /** 会话 ID */
    private Long id;
    /** 对话参与者 */
    private UserDTO participant;
    /** 最后一条消息内容 */
    private String lastMessage;
    /** 最后一条消息时间 */
    private String lastMessageAt;
    /** 未读消息数 */
    private long unreadCount;

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
     * 获取对话参与者。
     *
     * @return 对话参与者
     */
    public UserDTO getParticipant() {
        return participant;
    }

    /**
     * 设置对话参与者。
     *
     * @param participant 对话参与者
     */
    public void setParticipant(UserDTO participant) {
        this.participant = participant;
    }

    /**
     * 获取最后一条消息内容。
     *
     * @return 最后一条消息内容
     */
    public String getLastMessage() {
        return lastMessage;
    }

    /**
     * 设置最后一条消息内容。
     *
     * @param lastMessage 最后一条消息内容
     */
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    /**
     * 获取最后一条消息时间。
     *
     * @return 最后一条消息时间
     */
    public String getLastMessageAt() {
        return lastMessageAt;
    }

    /**
     * 设置最后一条消息时间。
     *
     * @param lastMessageAt 最后一条消息时间
     */
    public void setLastMessageAt(String lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    /**
     * 获取未读消息数。
     *
     * @return 未读消息数
     */
    public long getUnreadCount() {
        return unreadCount;
    }

    /**
     * 设置未读消息数。
     *
     * @param unreadCount 未读消息数
     */
    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
