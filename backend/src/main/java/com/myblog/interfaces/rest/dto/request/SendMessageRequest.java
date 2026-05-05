package com.myblog.interfaces.rest.dto.request;

/**
 * 发送消息请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class SendMessageRequest {

    private String content;
    private String type;

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
