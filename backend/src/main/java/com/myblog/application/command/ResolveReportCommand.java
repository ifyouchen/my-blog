package com.myblog.application.command;

/**
 * 处理举报命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ResolveReportCommand {

    private Long reportId;
    private Long handlerUserId;
    private String action;
    private String handleNote;

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getHandlerUserId() {
        return handlerUserId;
    }

    public void setHandlerUserId(Long handlerUserId) {
        this.handlerUserId = handlerUserId;
    }

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
