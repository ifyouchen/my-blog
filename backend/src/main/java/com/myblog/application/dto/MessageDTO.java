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
    /** 被回复消息 ID */
    private Long parentId;
    /** 被回复消息快照 */
    private ReplyMessageDTO repliedMessage;
    /** 消息内容 */
    private String content;
    /** 消息类型 */
    private String type;
    /** 是否已读 */
    private Boolean read;
    /** 是否已撤回 */
    private Boolean recalled;
    /** 撤回时间 */
    private String recalledAt;
    /** 创建时间 */
    private String createdAt;

    /**
     * 被回复消息快照。
     */
    public static class ReplyMessageDTO {
        /** 发送者名称 */
        private String senderName;
        /** 消息内容 */
        private String content;
        /** 消息类型 */
        private String type;

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public ReplyMessageDTO getRepliedMessage() {
        return repliedMessage;
    }

    public void setRepliedMessage(ReplyMessageDTO repliedMessage) {
        this.repliedMessage = repliedMessage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Boolean getRecalled() {
        return recalled;
    }

    public void setRecalled(Boolean recalled) {
        this.recalled = recalled;
    }

    public String getRecalledAt() {
        return recalledAt;
    }

    public void setRecalledAt(String recalledAt) {
        this.recalledAt = recalledAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
