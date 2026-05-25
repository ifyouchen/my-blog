package com.myblog.application.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 邮件发送任务。
 *
 * @author Codex
 * @since 1.0.0
 */
public class EmailTask {

    private final EmailTaskType type;
    private final String email;
    private final String code;
    private final String username;
    private final String resetLink;
    private final int retryCount;

    @JsonCreator
    private EmailTask(@JsonProperty("type") EmailTaskType type,
                      @JsonProperty("email") String email,
                      @JsonProperty("code") String code,
                      @JsonProperty("username") String username,
                      @JsonProperty("resetLink") String resetLink,
                      @JsonProperty("retryCount") int retryCount) {
        this.type = type;
        this.email = email;
        this.code = code;
        this.username = username;
        this.resetLink = resetLink;
        this.retryCount = retryCount;
    }

    public static EmailTask registerCode(String email, String code) {
        return new EmailTask(EmailTaskType.REGISTER_CODE, email, code, null, null, 0);
    }

    public static EmailTask passwordReset(String email, String username, String resetLink) {
        return new EmailTask(EmailTaskType.PASSWORD_RESET, email, null, username, resetLink, 0);
    }

    public EmailTask nextRetry() {
        return new EmailTask(type, email, code, username, resetLink, retryCount + 1);
    }

    public EmailTaskType getType() {
        return type;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }

    public String getUsername() {
        return username;
    }

    public String getResetLink() {
        return resetLink;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
