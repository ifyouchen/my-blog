package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.NotBlank;

/**
 * 处理举报请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ResolveReportRequest {

    @NotBlank(message = "处理动作不能为空")
    private String action;

    private String handleNote;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }
}
