package com.myblog.infrastructure.repository.persistence.repository;

import com.myblog.domain.model.aggregate.Report;
import com.myblog.domain.model.valueobject.ReportId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.domain.repository.ReportRepository;
import com.myblog.infrastructure.repository.persistence.converter.ReportPersistenceConverter;
import com.myblog.infrastructure.repository.persistence.entity.ReportDO;
import com.myblog.infrastructure.repository.persistence.mapper.ReportMapper;
import com.myblog.shared.enums.ReportReasonType;
import com.myblog.shared.enums.ReportStatus;
import com.myblog.shared.enums.ReportTargetType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 举报 MyBatis 仓储实现。
 *
 * @author Codex
 * @since 1.0.0
 */
@Repository
@Profile("!memory")
public class MyBatisReportRepository implements ReportRepository {

    private final ReportMapper reportMapper;

    public MyBatisReportRepository(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    @Override
    public Optional<Report> findById(ReportId reportId) {
        return Optional.ofNullable(ReportPersistenceConverter.toDomain(reportMapper.selectById(reportId.getValue())));
    }

    @Override
    public List<Report> findAdminPage(ReportStatus status, ReportTargetType targetType, ReportReasonType reasonType,
                                      int page, int pageSize) {
        int currentPage = Math.max(page, 1);
        int currentPageSize = Math.max(pageSize, 1);
        int offset = (currentPage - 1) * currentPageSize;
        List<ReportDO> reportDOList = reportMapper.selectAdminPage(
            status == null ? null : status.name(),
            targetType == null ? null : targetType.name(),
            reasonType == null ? null : reasonType.name(),
            offset,
            currentPageSize
        );
        List<Report> reports = new ArrayList<Report>(reportDOList.size());
        for (ReportDO reportDO : reportDOList) {
            reports.add(ReportPersistenceConverter.toDomain(reportDO));
        }
        return reports;
    }

    @Override
    public long countAdminPage(ReportStatus status, ReportTargetType targetType, ReportReasonType reasonType) {
        return reportMapper.countAdminPage(
            status == null ? null : status.name(),
            targetType == null ? null : targetType.name(),
            reasonType == null ? null : reasonType.name()
        );
    }

    @Override
    public boolean existsPendingByReporterAndTarget(UserId reporterUserId, ReportTargetType targetType, Long targetId) {
        return reportMapper.countPendingByReporterAndTarget(
            reporterUserId.getValue(),
            targetType.name(),
            targetId
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Report save(Report report) {
        reportMapper.insertOrUpdate(ReportPersistenceConverter.toData(report));
        return report;
    }

    @Override
    public Long nextId() {
        return reportMapper.selectNextId();
    }
}
