package com.myblog.interfaces.rest.dto.request;

/**
 * 发送消息请求.
 * <p>
 * 在私信会话中发送消息的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class SendMessageRequest {

    /** 消息内容. */
    private String content;

    /** 消息类型（如 TEXT、IMAGE 等）. */
    private String type;

    /** 被回复消息 ID. */
    private Long parentId;

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
