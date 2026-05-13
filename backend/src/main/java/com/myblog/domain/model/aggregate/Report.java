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
 * @author my-blog
 * @since 1.0.0
 */
public class Report {

    /**
     * 举报 ID
     */
    private ReportId id;

    /**
     * 举报人用户 ID
     */
    private UserId reporterUserId;

    /**
     * 被举报目标类型（ARTICLE / COMMENT）
     */
    private ReportTargetType targetType;

    /**
     * 被举报目标 ID
     */
    private Long targetId;

    /**
     * 举报原因类型
     */
    private ReportReasonType reasonType;

    /**
     * 举报原因补充说明
     */
    private String reasonDetail;

    /**
     * 举报处理状态
     */
    private ReportStatus status;

    /**
     * 处理人用户 ID
     */
    private UserId handlerUserId;

    /**
     * 处理备注
     */
    private String handleNote;

    /**
     * 举报处理完成时间
     */
    private LocalDateTime handledAt;

    /**
     * 举报创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 举报最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    private Report() {
    }

    /**
     * 创建举报聚合根。
     *
     * @param id             举报 ID
     * @param reporterUserId 举报人用户 ID
     * @param targetType     被举报目标类型
     * @param targetId       被举报目标 ID
     * @param reasonType     举报原因类型
     * @param reasonDetail   举报原因补充说明
     * @return 举报聚合根
     */
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

    /**
     * 从持久化数据还原举报聚合根。
     *
     * @param id             举报 ID
     * @param reporterUserId 举报人用户 ID
     * @param targetType     被举报目标类型
     * @param targetId       被举报目标 ID
     * @param reasonType     举报原因类型
     * @param reasonDetail   举报原因补充说明
     * @param status         处理状态
     * @param handlerUserId  处理人用户 ID
     * @param handleNote     处理备注
     * @param handledAt      处理完成时间
     * @param createdAt      创建时间
     * @param updatedAt      更新时间
     * @param version        乐观锁版本号
     * @return 举报聚合根
     */
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

    /**
     * 将举报标记为已处理（确认属实）。
     *
     * @param handlerUserId 处理人用户 ID
     * @param handleNote    处理备注
     */
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

    /**
     * 将举报标记为已驳回（不属实）。
     *
     * @param handlerUserId 处理人用户 ID
     * @param handleNote    处理备注
     */
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

    /**
     * 校验举报状态必须为待处理，否则抛出异常。
     */
    private void ensurePending() {
        if (!ReportStatus.PENDING.equals(this.status)) {
            throw new DomainException(ErrorCode.CONFLICT, "举报已处理");
        }
    }

    /**
     * 规范化补充说明文本，截断超过 1000 字符的部分。
     *
     * @param value 原始文本
     * @return 规范化后的文本
     */
    private static String normalizeDetail(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > 1000 ? trimmed.substring(0, 1000) : trimmed;
    }

    /**
     * 获取举报 ID。
     *
     * @return 举报 ID
     */
    public ReportId getId() {
        return id;
    }

    /**
     * 获取举报人用户 ID。
     *
     * @return 举报人用户 ID
     */
    public UserId getReporterUserId() {
        return reporterUserId;
    }

    /**
     * 获取被举报目标类型。
     *
     * @return 被举报目标类型
     */
    public ReportTargetType getTargetType() {
        return targetType;
    }

    /**
     * 获取被举报目标 ID。
     *
     * @return 被举报目标 ID
     */
    public Long getTargetId() {
        return targetId;
    }

    /**
     * 获取举报原因类型。
     *
     * @return 举报原因类型
     */
    public ReportReasonType getReasonType() {
        return reasonType;
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
     * 获取举报处理状态。
     *
     * @return 举报处理状态
     */
    public ReportStatus getStatus() {
        return status;
    }

    /**
     * 获取处理人用户 ID。
     *
     * @return 处理人用户 ID
     */
    public UserId getHandlerUserId() {
        return handlerUserId;
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
     * 获取举报处理完成时间。
     *
     * @return 处理完成时间
     */
    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    /**
     * 获取举报创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取举报最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}
