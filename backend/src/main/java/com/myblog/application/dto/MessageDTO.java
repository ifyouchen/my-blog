package com.myblog.application.dto;

/**
 * 消息数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class MessageDTO {

    /** 消息 ID */
    private Long id;
    /** 会话 ID */
    private Long conversationId;
    /** 发送者用户 ID */
    private Long senderId;
    /** 发送者名称 */
    private String senderName;
    /** 发送者头像地址 */
    private String senderAvatar;
    /** 消息内容 */
    private String content;
    /** 消息类型 */
    private String type;
    /** 是否已读 */
    private Boolean read;
    /** 创建时间 */
    private String createdAt;

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
     * 获取会话 ID。
     *
     * @return 会话 ID
     */
    public Long getConversationId() {
        return conversationId;
    }

    /**
     * 设置会话 ID。
     *
     * @param conversationId 会话 ID
     */
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * 获取发送者用户 ID。
     *
     * @return 发送者用户 ID
     */
    public Long getSenderId() {
        return senderId;
    }

    /**
     * 设置发送者用户 ID。
     *
     * @param senderId 发送者用户 ID
     */
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    /**
     * 获取发送者名称。
     *
     * @return 发送者名称
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * 设置发送者名称。
     *
     * @param senderName 发送者名称
     */
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    /**
     * 获取发送者头像地址。
     *
     * @return 发送者头像地址
     */
    public String getSenderAvatar() {
        return senderAvatar;
    }

    /**
     * 设置发送者头像地址。
     *
     * @param senderAvatar 发送者头像地址
     */
    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    /**
     * 获取消息内容。
     *
     * @return 消息内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容。
     *
     * @param content 消息内容
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
}
