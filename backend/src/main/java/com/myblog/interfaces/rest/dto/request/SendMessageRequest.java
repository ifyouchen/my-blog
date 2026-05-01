package com.myblog.interfaces.rest.dto.request;

/**
 * 发送消息请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class SendMessageRequest {

    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
