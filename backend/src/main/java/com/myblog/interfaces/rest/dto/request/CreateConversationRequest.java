package com.myblog.interfaces.rest.dto.request;

/**
 * 创建会话请求.
 * <p>
 * 用户发起新私信会话时的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CreateConversationRequest {

    /** 会话参与者的用户ID. */
    private Long participantId;

    /**
     * 获取参与者用户ID.
     *
     * @return 参与者用户ID
     */
    public Long getParticipantId() {
        return participantId;
    }

    /**
     * 设置参与者用户ID.
     *
     * @param participantId 参与者用户ID
     */
    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }
}
