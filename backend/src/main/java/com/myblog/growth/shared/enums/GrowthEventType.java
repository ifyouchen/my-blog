package com.myblog.growth.shared.enums;

/**
 * 成长行为事件类型.
 */
public enum GrowthEventType {

    /** 点赞文章. */
    LIKE,

    /** 阅读文章. */
    READ,

    /** 发表评论. */
    COMMENT,

    /** 收藏文章. */
    FAVORITE,

    /** 分享文章. */
    SHARE,

    /** 关注用户. */
    FOLLOW,

    /** 发布文章（作者行为）. */
    PUBLISH,

    /** 升级（记录用）. */
    LEVEL_UP;
}

