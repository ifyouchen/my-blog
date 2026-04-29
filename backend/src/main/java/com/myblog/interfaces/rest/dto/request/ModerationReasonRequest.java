package com.myblog.interfaces.rest.dto.request;

/**
 * 治理原因请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ModerationReasonRequest {

    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
