package com.myblog.shared.enums;

/**
 * 举报处理动作。
 *
 * @author Codex
 * @since 1.0.0
 */
public enum ReportResolveAction {

    /**
     * 驳回举报（内容无问题）
     */
    REJECT,

    /**
     * 下架被举报的文章
     */
    OFFLINE_ARTICLE,

    /**
     * 删除被举报的评论
     */
    DELETE_COMMENT,

    /**
     * 禁用被举报的用户
     */
    DISABLE_USER
}
