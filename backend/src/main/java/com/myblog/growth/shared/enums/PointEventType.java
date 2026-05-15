package com.myblog.growth.shared.enums;

/**
 * 积分行为类型枚举.
 * <p>
 * 对应 {@code point_rule_config.event_type} 字段，
 * 以及 {@code user_point_journal.event_type} 字段。
 * </p>
 */
public enum PointEventType {

    /** 每日签到奖励积分. */
    SIGN_IN,

    /** 邀请新用户注册奖励. */
    INVITE,

    /** 充值获得积分（1元=1积分）. */
    RECHARGE,

    /** 解锁文章消耗积分（负流水）. */
    UNLOCK,

    /** 管理员调整积分（正负均有）. */
    ADMIN_ADJUST,

    /** 发布文章奖励少量积分（每日上限）. */
    PUBLISH,

    /** 作者文章解锁分账收益. */
    REVENUE_SHARE
}

