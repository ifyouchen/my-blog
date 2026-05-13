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
 * @author my-blog
 * @since 1.0.0
 */
public interface ReportRepository {

    /**
     * 根据举报 ID 查询举报。
     *
     * @param reportId 举报 ID
     * @return 举报 Optional
     */
    Optional<Report> findById(ReportId reportId);

    /**
     * 分页查询后台举报列表。
     *
     * @param status     举报状态筛选，为 null 时不筛选
     * @param targetType 目标类型筛选，为 null 时不筛选
     * @param reasonType 原因类型筛选，为 null 时不筛选
     * @param page       页码
     * @param pageSize   每页大小
     * @return 举报列表
     */
    List<Report> findAdminPage(ReportStatus status, ReportTargetType targetType, ReportReasonType reasonType,
                               int page, int pageSize);

    /**
     * 统计后台举报数量。
     *
     * @param status     举报状态筛选，为 null 时不筛选
     * @param targetType 目标类型筛选，为 null 时不筛选
     * @param reasonType 原因类型筛选，为 null 时不筛选
     * @return 举报数量
     */
    long countAdminPage(ReportStatus status, ReportTargetType targetType, ReportReasonType reasonType);

    /**
     * 判断指定用户是否已对某目标提交过待处理的举报。
     *
     * @param reporterUserId 举报人用户 ID
     * @param targetType     目标类型
     * @param targetId       目标 ID
     * @return 是否存在待处理的举报
     */
    boolean existsPendingByReporterAndTarget(UserId reporterUserId, ReportTargetType targetType, Long targetId);

    /**
     * 保存举报（新增或更新）。
     *
     * @param report 举报聚合根
     * @return 保存后的举报
     */
    Report save(Report report);

    /**
     * 生成下一个举报 ID。
     *
     * @return 举报 ID
     */
    Long nextId();
}
