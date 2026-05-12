package com.myblog.application.command;

/**
 * 处理举报命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ResolveReportCommand {

    /**
     * 举报 ID
     */
    private Long reportId;

    /**
     * 处理人用户 ID
     */
    private Long handlerUserId;

    /**
     * 处理动作（RESOLVE 确认属实 / REJECT 驳回）
     */
    private String action;

    /**
     * 处理备注
     */
    private String handleNote;

    /**
     * 获取举报 ID。
     *
     * @return 举报 ID
     */
    public Long getReportId() {
        return reportId;
    }

    /**
     * 设置举报 ID。
     *
     * @param reportId 举报 ID
     */
    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    /**
     * 获取处理人用户 ID。
     *
     * @return 处理人用户 ID
     */
    public Long getHandlerUserId() {
        return handlerUserId;
    }

    /**
     * 设置处理人用户 ID。
     *
     * @param handlerUserId 处理人用户 ID
     */
    public void setHandlerUserId(Long handlerUserId) {
        this.handlerUserId = handlerUserId;
    }

    /**
     * 获取处理动作。
     *
     * @return 处理动作
     */
    public String getAction() {
        return action;
    }

    /**
     * 设置处理动作。
     *
     * @param action 处理动作
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 获取处理备注。
     *
     * @return 处理备注
     */
    public String getHandleNote() {
        return handleNote;
    }

    /**
     * 设置处理备注。
     *
     * @param handleNote 处理备注
     */
    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }
}
