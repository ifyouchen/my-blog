package com.myblog.interfaces.rest.dto.request;

/**
 * 内容治理原因请求.
 * <p>
 * 用于管理员对文章进行治理操作时填写处理原因.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ModerationReasonRequest {

    /** 治理原因说明. */
    private String reason;

    /**
     * 获取治理原因.
     *
     * @return 原因说明
     */
    public String getReason() {
        return reason;
    }

    /**
     * 设置治理原因.
     *
     * @param reason 原因说明
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
