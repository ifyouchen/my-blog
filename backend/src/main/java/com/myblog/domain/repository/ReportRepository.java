package com.myblog.domain.repository;

import com.myblog.domain.model.aggregate.Report;
import com.myblog.domain.model.valueobject.ReportId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.enums.ReportReasonType;
import com.myblog.shared.enums.ReportStatus;
import com.myblog.shared.enums.ReportTargetType;

import java.util.List;
import java.util.Optional;

/**
 * 举报仓储接口。
 *
 * @author Codex
 * @since 1.0.0
 */
public interface ReportRepository {

    Optional<Report> findById(ReportId reportId);

    List<Report> findAdminPage(ReportStatus status, ReportTargetType targetType, ReportReasonType reasonType,
                               int page, int pageSize);

    long countAdminPage(ReportStatus status, ReportTargetType targetType, ReportReasonType reasonType);

    boolean existsPendingByReporterAndTarget(UserId reporterUserId, ReportTargetType targetType, Long targetId);

    Report save(Report report);

    Long nextId();
}
