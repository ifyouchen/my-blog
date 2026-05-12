package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 举报数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ReportDO {

    /**
     * 举报 ID
     */
    private Long id;

    /**
     * 举报人用户 ID
     */
    private Long reporterUserId;

    /**
     * 被举报目标类型（ARTICLE / COMMENT / USER）
     */
    private String targetType;

    /**
     * 被举报目标 ID
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
     * 举报处理状态（PENDING / RESOLVED / REJECTED）
     */
    private String status;

    /**
     * 处理人用户 ID
     */
    private Long handlerUserId;

    /**
     * 处理备注
     */
    private String handleNote;

    /**
     * 举报处理完成时间
     */
    private LocalDateTime handledAt;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 获取举报 ID。
     *
     * @return 举报 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置举报 ID。
     *
     * @param id 举报 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

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
     * 获取被举报目标类型。
     *
     * @return 目标类型
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 设置被举报目标类型。
     *
     * @param targetType 目标类型
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /**
     * 获取被举报目标 ID。
     *
     * @return 目标 ID
     */
    public Long getTargetId() {
        return targetId;
    }

    /**
     * 设置被举报目标 ID。
     *
     * @param targetId 目标 ID
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

    /**
     * 获取举报处理状态。
     *
     * @return 举报处理状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置举报处理状态。
     *
     * @param status 举报处理状态
     */
    public void setStatus(String status) {
        this.status = status;
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

    /**
     * 获取举报处理完成时间。
     *
     * @return 处理完成时间
     */
    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    /**
     * 设置举报处理完成时间。
     *
     * @param handledAt 处理完成时间
     */
    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    /**
     * 获取记录创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置记录创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取记录最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置记录最后更新时间。
     *
     * @param updatedAt 最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置软删除时间。
     *
     * @param deletedAt 删除时间
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置乐观锁版本号。
     *
     * @param version 版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
