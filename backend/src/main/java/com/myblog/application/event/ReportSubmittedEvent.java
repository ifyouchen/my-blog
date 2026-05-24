package com.myblog.application.event;

import java.time.LocalDateTime;

/**
 * 举报提交事件.
 * <p>
 * 当用户提交举报时触发，用于通知管理员审核.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ReportSubmittedEvent {

    /** 举报记录ID. */
    private final Long reportId;

    /** 举报人用户ID. */
    private final Long reporterUserId;

    /** 举报目标类型. */
    private final String targetType;

    /** 举报目标ID. */
    private final Long targetId;

    /** 事件发生时间. */
    private final LocalDateTime occurredOn;

    /**
     * 构造举报提交事件.
     *
     * @param reportId       举报记录ID
     * @param reporterUserId 举报人用户ID
     * @param targetType     举报目标类型
     * @param targetId       举报目标ID
     */
    public ReportSubmittedEvent(Long reportId, Long reporterUserId, String targetType, Long targetId) {
        this.reportId = reportId;
        this.reporterUserId = reporterUserId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getReportId() {
        return reportId;
    }

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
