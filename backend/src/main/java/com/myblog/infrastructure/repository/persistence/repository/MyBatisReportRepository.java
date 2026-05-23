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
public class MyBatisReportRepository implements ReportRepository {

    private final ReportMapper reportMapper;
    private final IdGenerator idGenerator;

    /**
     * 创建举报 MyBatis 仓储。
     *
     * @param reportMapper 举报 Mapper
     * @param idGenerator 全局 ID 生成器
     */
    public MyBatisReportRepository(ReportMapper reportMapper, IdGenerator idGenerator) {
        this.reportMapper = reportMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * 根据举报 ID 查询举报记录。
     *
     * @param reportId 举报 ID
     * @return 举报 Optional
     */
    @Override
    public Optional<Report> findById(ReportId reportId) {
        return Optional.ofNullable(ReportPersistenceConverter.toDomain(reportMapper.selectById(reportId.getValue())));
    }

    /**
     * 后台管理分页查询举报列表。
     *
     * @param status     举报状态
     * @param targetType 举报目标类型
     * @param reasonType 举报原因类型
     * @param page       页码
     * @param pageSize   每页大小
     * @return 举报列表
     */
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

    /**
     * 统计后台管理举报数量。
     *
     * @param status     举报状态
     * @param targetType 举报目标类型
     * @param reasonType 举报原因类型
     * @return 举报数量
     */
    @Override
    public long countAdminPage(ReportStatus status, ReportTargetType targetType, ReportReasonType reasonType) {
        return reportMapper.countAdminPage(
            status == null ? null : status.name(),
            targetType == null ? null : targetType.name(),
            reasonType == null ? null : reasonType.name()
        );
    }

    /**
     * 判断举报人是否已对同一目标提交待处理举报。
     *
     * @param reporterUserId 举报人用户 ID
     * @param targetType     举报目标类型
     * @param targetId       举报目标 ID
     * @return 是否已存在待处理举报
     */
    @Override
    public boolean existsPendingByReporterAndTarget(UserId reporterUserId, ReportTargetType targetType, Long targetId) {
        return reportMapper.countPendingByReporterAndTarget(
            reporterUserId.getValue(),
            targetType.name(),
            targetId
        ) > 0;
    }

    /**
     * 保存举报记录。
     *
     * @param report 举报聚合根
     * @return 保存后的举报
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Report save(Report report) {
        reportMapper.insertOrUpdate(ReportPersistenceConverter.toData(report));
        return report;
    }

    /**
     * 生成下一个举报 ID。
     *
     * @return 举报 ID
     */
    @Override
    public Long nextId() {
        return idGenerator.nextId("blog_report");
    }
}
