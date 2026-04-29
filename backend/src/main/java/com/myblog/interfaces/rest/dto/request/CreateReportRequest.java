package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建举报请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CreateReportRequest {

    @NotBlank(message = "举报目标类型不能为空")
    private String targetType;

    @NotNull(message = "举报目标不能为空")
    private Long targetId;

    @NotBlank(message = "举报原因不能为空")
    private String reasonType;

    private String reasonDetail;

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getReasonType() {
        return reasonType;
    }

    public void setReasonType(String reasonType) {
        this.reasonType = reasonType;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }
}
