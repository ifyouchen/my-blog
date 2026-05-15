SET NAMES utf8mb4;
USE `my_blog`;

CREATE TABLE IF NOT EXISTS `user_point_account` (
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      bigint unsigned NOT NULL COMMENT '用户ID',
    `balance`      bigint          NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    `total_earned` bigint          NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    `total_spent`  bigint          NOT NULL DEFAULT 0 COMMENT '累计消耗积分',
    `created_at`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`   datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`      int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分账户';

CREATE TABLE IF NOT EXISTS `user_point_journal` (
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      bigint unsigned NOT NULL COMMENT '用户ID',
    `delta`        bigint          NOT NULL COMMENT '变化量（正=入账，负=扣减）',
    `balance_after` bigint         NOT NULL DEFAULT 0 COMMENT '变更后余额快照',
    `source_type`  varchar(50)     NOT NULL COMMENT '来源类型',
    `biz_no`       varchar(128)    NOT NULL COMMENT '业务单号（幂等键）',
    `remark`       varchar(500)    NULL DEFAULT NULL COMMENT '备注',
    `operator`     varchar(100)    NULL DEFAULT NULL COMMENT '操作人',
    `created_at`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at`   datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`      int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_biz_no` (`biz_no`) USING BTREE,
    INDEX `idx_user_created` (`user_id`, `created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分流水（不可变）';

CREATE TABLE IF NOT EXISTS `point_rule_config` (
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_type`  varchar(50)     NOT NULL COMMENT '来源类型',
    `point_amount` bigint          NOT NULL DEFAULT 0 COMMENT '单次积分量',
    `daily_limit`  int             NOT NULL DEFAULT 0 COMMENT '单日上限次数（0=不限）',
    `enabled`      tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `operator`     varchar(100)    NULL DEFAULT NULL COMMENT '最后操作人',
    `reason`       varchar(500)    NULL DEFAULT NULL COMMENT '变更原因',
    `created_at`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`   datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`      int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_source_type` (`source_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分规则配置';

CREATE TABLE IF NOT EXISTS `admin_point_adjust_log` (
    `id`                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `target_user_id`    bigint unsigned NOT NULL COMMENT '被调分用户ID',
    `delta`             bigint          NOT NULL COMMENT '调整量',
    `reason`            varchar(500)    NOT NULL COMMENT '调整原因',
    `biz_no`            varchar(128)    NOT NULL COMMENT '幂等单号',
    `operator_user_id`  bigint unsigned NOT NULL COMMENT '管理员用户ID',
    `operator_username` varchar(100)    NOT NULL COMMENT '管理员用户名',
    `created_at`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at`        datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`           int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_biz_no` (`biz_no`) USING BTREE,
    INDEX `idx_target_created` (`target_user_id`, `created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员积分调整日志';

-- 初始化积分规则
INSERT IGNORE INTO `point_rule_config` (`source_type`, `point_amount`, `daily_limit`, `enabled`, `operator`, `reason`) VALUES
('SIGN_IN',  10, 1, 1, 'system', '初始化'),
('INVITE',   50, 0, 1, 'system', '初始化'),
('RECHARGE',  1, 0, 1, 'system', '初始化 - 1元=1积分'),
('PUBLISH',   5, 3, 1, 'system', '初始化 - 作者发布文章，每日最多3篇计积分'),
('UNLOCK_SHARE_RATIO', 50, 0, 1, 'system', '初始化 - 平台分账比例50%');

