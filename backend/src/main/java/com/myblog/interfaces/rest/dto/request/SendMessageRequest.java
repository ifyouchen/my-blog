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

    /**
     * 获取消息内容.
     *
     * @return 消息内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容.
     *
     * @param content 消息内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取消息类型.
     *
     * @return 消息类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置消息类型.
     *
     * @param type 消息类型
     */
    public void setType(String type) {
        this.type = type;
    }
}
