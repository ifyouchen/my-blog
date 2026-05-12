package com.myblog.shared.enums;

/**
 * 举报处理状态。
 *
 * @author Codex
 * @since 1.0.0
 */
public enum ReportStatus {

    /**
     * 待处理
     */
    PENDING,

    /**
     * 已处理（确认属实）
     */
    RESOLVED,

    /**
     * 已驳回（不属实）
     */
    REJECTED
}
