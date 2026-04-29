package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ReportId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.enums.ReportReasonType;
import com.myblog.shared.enums.ReportStatus;
import com.myblog.shared.enums.ReportTargetType;
import com.myblog.shared.exception.DomainException;
import com.myblog.shared.exception.ErrorCode;

import java.time.LocalDateTime;

/**
 * 举报聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class Report {

    private ReportId id;
    private UserId reporterUserId;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReasonType reasonType;
    private String reasonDetail;
    private ReportStatus status;
    private UserId handlerUserId;
    private String handleNote;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;

    private Report() {
    }

    public static Report create(Long id, UserId reporterUserId, ReportTargetType targetType, Long targetId,
                                ReportReasonType reasonType, String reasonDetail) {
        if (reporterUserId == null) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "举报人不能为空");
        }
        if (targetType == null) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "举报目标类型不能为空");
        }
        if (targetId == null || targetId.longValue() <= 0L) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "举报目标不能为空");
        }
        if (reasonType == null) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "举报原因不能为空");
        }
        Report report = new Report();
        report.id = new ReportId(id);
        report.reporterUserId = reporterUserId;
        report.targetType = targetType;
        report.targetId = targetId;
        report.reasonType = reasonType;
        report.reasonDetail = normalizeDetail(reasonDetail);
        report.status = ReportStatus.PENDING;
        report.createdAt = LocalDateTime.now();
        report.updatedAt = report.createdAt;
        report.version = 0;
        return report;
    }

    public static Report restore(Long id, UserId reporterUserId, ReportTargetType targetType, Long targetId,
                                 ReportReasonType reasonType, String reasonDetail, ReportStatus status,
                                 UserId handlerUserId, String handleNote, LocalDateTime handledAt,
                                 LocalDateTime createdAt, LocalDateTime updatedAt, Integer version) {
        Report report = new Report();
        report.id = new ReportId(id);
        report.reporterUserId = reporterUserId;
        report.targetType = targetType;
        report.targetId = targetId;
        report.reasonType = reasonType;
        report.reasonDetail = reasonDetail;
        report.status = status;
        report.handlerUserId = handlerUserId;
        report.handleNote = handleNote;
        report.handledAt = handledAt;
        report.createdAt = createdAt;
        report.updatedAt = updatedAt;
        report.version = version == null ? 0 : version;
        return report;
    }

    public void resolve(UserId handlerUserId, String handleNote) {
        ensurePending();
        if (handlerUserId == null) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "处理人不能为空");
        }
        this.status = ReportStatus.RESOLVED;
        this.handlerUserId = handlerUserId;
        this.handleNote = normalizeDetail(handleNote);
        this.handledAt = LocalDateTime.now();
        this.updatedAt = this.handledAt;
    }

    public void reject(UserId handlerUserId, String handleNote) {
        ensurePending();
        if (handlerUserId == null) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "处理人不能为空");
        }
        this.status = ReportStatus.REJECTED;
        this.handlerUserId = handlerUserId;
        this.handleNote = normalizeDetail(handleNote);
        this.handledAt = LocalDateTime.now();
        this.updatedAt = this.handledAt;
    }

    private void ensurePending() {
        if (!ReportStatus.PENDING.equals(this.status)) {
            throw new DomainException(ErrorCode.CONFLICT, "举报已处理");
        }
    }

    private static String normalizeDetail(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > 1000 ? trimmed.substring(0, 1000) : trimmed;
    }

    public ReportId getId() {
        return id;
    }

    public UserId getReporterUserId() {
        return reporterUserId;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public ReportReasonType getReasonType() {
        return reasonType;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public UserId getHandlerUserId() {
        return handlerUserId;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
