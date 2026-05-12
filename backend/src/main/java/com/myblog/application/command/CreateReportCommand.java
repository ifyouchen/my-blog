package com.myblog.application.command;

/**
 * 创建举报命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CreateReportCommand {

    /**
     * 举报人用户 ID
     */
    private Long reporterUserId;
    /**
     * 举报目标类型（ARTICLE / COMMENT）
     */
    private String targetType;
    /**
     * 举报目标 ID
     */
    private Long targetId;
    /**
     * 举报原因类型
     */
    private String reasonType;
    /**
     * 举报原因补充说明
     */
    private String reasonDetail;

    /**
     * 获取举报人用户 ID。
     *
     * @return 举报人用户 ID
     */
    public Long getReporterUserId() {
        return reporterUserId;
    }

    /**
     * 设置举报人用户 ID。
     *
     * @param reporterUserId 举报人用户 ID
     */
    public void setReporterUserId(Long reporterUserId) {
        this.reporterUserId = reporterUserId;
    }

    /**
     * 获取举报目标类型。
     *
     * @return 举报目标类型
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 设置举报目标类型。
     *
     * @param targetType 举报目标类型
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /**
     * 获取举报目标 ID。
     *
     * @return 举报目标 ID
     */
    public Long getTargetId() {
        return targetId;
    }

    /**
     * 设置举报目标 ID。
     *
     * @param targetId 举报目标 ID
     */
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    /**
     * 获取举报原因类型。
     *
     * @return 举报原因类型
     */
    public String getReasonType() {
        return reasonType;
    }

    /**
     * 设置举报原因类型。
     *
     * @param reasonType 举报原因类型
     */
    public void setReasonType(String reasonType) {
        this.reasonType = reasonType;
    }

    /**
     * 获取举报原因补充说明。
     *
     * @return 举报原因补充说明
     */
    public String getReasonDetail() {
        return reasonDetail;
    }

    /**
     * 设置举报原因补充说明。
     *
     * @param reasonDetail 举报原因补充说明
     */
    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }
}
