package com.myblog.shared.enums;

/**
 * 文章状态。
 *
 * @author Codex
 * @since 1.0.0
 */
public enum ArticleStatus {

    /**
     * 草稿
     */
    DRAFT,

    /**
     * 定时发布（已设定发布时间，待发布）
     */
    SCHEDULED,

    /**
     * 已发布
     */
    PUBLISHED,

    /**
     * 已下架
     */
    OFFLINE,

    /**
     * 已删除
     */
    DELETED
}
