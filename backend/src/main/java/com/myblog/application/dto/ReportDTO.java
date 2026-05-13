package com.myblog.application.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 举报数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ReportDTO {

    /** 举报 ID */
    private Long id;
    /** 举报人用户 ID */
    private Long reporterUserId;
    /** 举报人用户名 */
    private String reporterUsername;
    /** 举报人昵称 */
    private String reporterNickname;
    /** 举报目标类型 */
    private String targetType;
    /** 举报目标 ID */
    private Long targetId;
    /** 举报原因类型 */
    private String reasonType;
    /** 举报原因详情 */
    private String reasonDetail;
    /** 举报状态 */
    private String status;
    /** 处理人用户 ID */
    private Long handlerUserId;
    /** 处理人用户名 */
    private String handlerUsername;
    /** 处理人昵称 */
    private String handlerNickname;
    /** 处理备注 */
    private String handleNote;
    /** 处理时间 */
    private LocalDateTime handledAt;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
    /** 举报目标摘要信息 */
    private Map<String, Object> targetSummary;

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
     * 获取举报人用户名。
     *
     * @return 举报人用户名
     */
    public String getReporterUsername() {
        return reporterUsername;
    }

    /**
     * 设置举报人用户名。
     *
     * @param reporterUsername 举报人用户名
     */
    public void setReporterUsername(String reporterUsername) {
        this.reporterUsername = reporterUsername;
    }

    /**
     * 获取举报人昵称。
     *
     * @return 举报人昵称
     */
    public String getReporterNickname() {
        return reporterNickname;
    }

    /**
     * 设置举报人昵称。
     *
     * @param reporterNickname 举报人昵称
     */
    public void setReporterNickname(String reporterNickname) {
        this.reporterNickname = reporterNickname;
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
     * 获取举报原因详情。
     *
     * @return 举报原因详情
     */
    public String getReasonDetail() {
        return reasonDetail;
    }

    /**
     * 设置举报原因详情。
     *
     * @param reasonDetail 举报原因详情
     */
    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }

    /**
     * 获取举报状态。
     *
     * @return 举报状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置举报状态。
     *
     * @param status 举报状态
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
     * 获取处理人用户名。
     *
     * @return 处理人用户名
     */
    public String getHandlerUsername() {
        return handlerUsername;
    }

    /**
     * 设置处理人用户名。
     *
     * @param handlerUsername 处理人用户名
     */
    public void setHandlerUsername(String handlerUsername) {
        this.handlerUsername = handlerUsername;
    }

    /**
     * 获取处理人昵称。
     *
     * @return 处理人昵称
     */
    public String getHandlerNickname() {
        return handlerNickname;
    }

    /**
     * 设置处理人昵称。
     *
     * @param handlerNickname 处理人昵称
     */
    public void setHandlerNickname(String handlerNickname) {
        this.handlerNickname = handlerNickname;
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
     * 获取处理时间。
     *
     * @return 处理时间
     */
    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    /**
     * 设置处理时间。
     *
     * @param handledAt 处理时间
     */
    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取举报目标摘要信息。
     *
     * @return 举报目标摘要信息
     */
    public Map<String, Object> getTargetSummary() {
        return targetSummary;
    }

    /**
     * 设置举报目标摘要信息。
     *
     * @param targetSummary 举报目标摘要信息
     */
    public void setTargetSummary(Map<String, Object> targetSummary) {
        this.targetSummary = targetSummary;
    }
}
