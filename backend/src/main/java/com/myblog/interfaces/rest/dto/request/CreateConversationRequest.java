package com.myblog.interfaces.rest.dto.request;

/**
 * 创建会话请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CreateConversationRequest {

    private Long participantId;

    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }
}
