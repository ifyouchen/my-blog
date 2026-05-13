package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.NotBlank;

/**
 * 处理举报请求.
 * <p>
 * 管理员处理用户举报时的请求参数.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ResolveReportRequest {

    /** 处理动作（如 IGNORE、WARN、DELETE 等）. */
    @NotBlank(message = "处理动作不能为空")
    private String action;

    /** 处理备注说明. */
    private String handleNote;

    /**
     * 获取处理动作.
     *
     * @return 处理动作
     */
    public String getAction() {
        return action;
    }

    /**
     * 设置处理动作.
     *
     * @param action 处理动作
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 获取处理备注.
     *
     * @return 处理备注
     */
    public String getHandleNote() {
        return handleNote;
    }

    /**
     * 设置处理备注.
     *
     * @param handleNote 处理备注
     */
    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }
}
