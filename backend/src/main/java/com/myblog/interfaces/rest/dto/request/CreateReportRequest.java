package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建举报请求.
 * <p>
 * 用户举报内容（文章、评论等）时的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CreateReportRequest {

    /** 举报目标类型（如 ARTICLE、COMMENT）. */
    @NotBlank(message = "举报目标类型不能为空")
    private String targetType;

    /** 举报目标的ID. */
    @NotNull(message = "举报目标不能为空")
    private Long targetId;

    /** 举报原因类型（如 SPAM、INAPPROPRIATE 等）. */
    @NotBlank(message = "举报原因不能为空")
    private String reasonType;

    /** 举报详细描述. */
    private String reasonDetail;

    /**
     * 获取举报目标类型.
     *
     * @return 目标类型
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 设置举报目标类型.
     *
     * @param targetType 目标类型
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /**
     * 获取举报目标ID.
     *
     * @return 目标ID
     */
    public Long getTargetId() {
        return targetId;
    }

    /**
     * 设置举报目标ID.
     *
     * @param targetId 目标ID
     */
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    /**
     * 获取举报原因类型.
     *
     * @return 原因类型
     */
    public String getReasonType() {
        return reasonType;
    }

    /**
     * 设置举报原因类型.
     *
     * @param reasonType 原因类型
     */
    public void setReasonType(String reasonType) {
        this.reasonType = reasonType;
    }

    /**
     * 获取举报详细描述.
     *
     * @return 详细描述
     */
    public String getReasonDetail() {
        return reasonDetail;
    }

    /**
     * 设置举报详细描述.
     *
     * @param reasonDetail 详细描述
     */
    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }
}
