package com.myblog.infrastructure.repository.persistence.converter;

import com.myblog.domain.model.aggregate.Report;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.infrastructure.repository.persistence.entity.ReportDO;
import com.myblog.shared.enums.ReportReasonType;
import com.myblog.shared.enums.ReportStatus;
import com.myblog.shared.enums.ReportTargetType;

/**
 * 举报持久化转换器。
 *
 * @author Codex
 * @since 1.0.0
 */
public final class ReportPersistenceConverter {

    private ReportPersistenceConverter() {
    }

    public static Report toDomain(ReportDO reportDO) {
        if (reportDO == null) {
            return null;
        }
        return Report.restore(
            reportDO.getId(),
            new UserId(reportDO.getReporterUserId()),
            ReportTargetType.valueOf(reportDO.getTargetType()),
            reportDO.getTargetId(),
            ReportReasonType.valueOf(reportDO.getReasonType()),
            reportDO.getReasonDetail(),
            ReportStatus.valueOf(reportDO.getStatus()),
            reportDO.getHandlerUserId() == null ? null : new UserId(reportDO.getHandlerUserId()),
            reportDO.getHandleNote(),
            reportDO.getHandledAt(),
            reportDO.getCreatedAt(),
            reportDO.getUpdatedAt(),
            reportDO.getVersion()
        );
    }

    public static ReportDO toData(Report report) {
        ReportDO reportDO = new ReportDO();
        reportDO.setId(report.getId().getValue());
        reportDO.setReporterUserId(report.getReporterUserId().getValue());
        reportDO.setTargetType(report.getTargetType().name());
        reportDO.setTargetId(report.getTargetId());
        reportDO.setReasonType(report.getReasonType().name());
        reportDO.setReasonDetail(report.getReasonDetail());
        reportDO.setStatus(report.getStatus().name());
        reportDO.setHandlerUserId(report.getHandlerUserId() == null ? null : report.getHandlerUserId().getValue());
        reportDO.setHandleNote(report.getHandleNote());
        reportDO.setHandledAt(report.getHandledAt());
        reportDO.setCreatedAt(report.getCreatedAt());
        reportDO.setUpdatedAt(report.getUpdatedAt());
        reportDO.setVersion(report.getVersion());
        return reportDO;
    }
}
