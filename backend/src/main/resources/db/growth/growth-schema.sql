SET NAMES utf8mb4;
USE `my_blog`;

-- ======================================================
-- Growth 模块 DDL & 初始化数据
-- 包含：经验/等级、积分账户、签到、邀请、充值、文章解锁
-- ======================================================


-- ========================
-- 1. 经验 & 等级
-- ========================

-- 用户成长账户
CREATE TABLE IF NOT EXISTS `user_growth_account` (
    `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       bigint unsigned  NOT NULL COMMENT '用户ID，关联 blog_user.id',
    `current_exp`   int unsigned     NOT NULL DEFAULT 0 COMMENT '当前总经验值',
    `current_level` int unsigned     NOT NULL DEFAULT 1 COMMENT '当前等级',
    `created_at`    datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    datetime         NULL DEFAULT NULL COMMENT '软删除时间',
    `version`       int              NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成长账户';

-- 用户经验流水（不可变）
CREATE TABLE IF NOT EXISTS `user_exp_journal` (
    `id`             bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        bigint unsigned  NOT NULL COMMENT '用户ID',
    `delta`          int              NOT NULL COMMENT '经验变化量（正数=增加）',
    `balance_after`  int unsigned     NOT NULL DEFAULT 0 COMMENT '变更后余额快照',
    `event_type`     varchar(50)      NOT NULL COMMENT '行为类型：LIKE/READ/COMMENT/FAVORITE/SHARE/FOLLOW',
    `source_id`      bigint unsigned  NULL COMMENT '来源ID（文章ID/评论ID 等）',
    `remark`         varchar(500)     NULL DEFAULT NULL COMMENT '备注',
    `idempotent_key` varchar(128)     NOT NULL COMMENT '幂等键',
    `created_at`     datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at`     datetime         NULL DEFAULT NULL COMMENT '软删除时间',
    `version`        int              NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_idempotent_key` (`idempotent_key`) USING BTREE,
    INDEX `idx_user_created` (`user_id`, `created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户经验流水（不可变）';

-- 每日奖励计数器
CREATE TABLE IF NOT EXISTS `daily_reward_counter` (
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     bigint unsigned  NOT NULL COMMENT '用户ID',
    `reward_type` varchar(50)      NOT NULL COMMENT '奖励类型',
    `stat_date`   date             NOT NULL COMMENT '统计日期',
    `count`       int unsigned     NOT NULL DEFAULT 0 COMMENT '当日累计次数',
    `exp_granted` int unsigned     NOT NULL DEFAULT 0 COMMENT '当日已发经验',
    `created_at`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime         NULL DEFAULT NULL COMMENT '软删除时间',
    `version`     int              NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_type_date` (`user_id`, `reward_type`, `stat_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日奖励计数器';

-- 经验规则配置
CREATE TABLE IF NOT EXISTS `growth_rule_config` (
    `id`                   bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_type`           varchar(50)      NOT NULL COMMENT '行为类型',
    `role`                 varchar(20)      NOT NULL DEFAULT 'ACTOR' COMMENT '作用角色：ACTOR/AUTHOR',
    `exp_amount`           int unsigned     NOT NULL DEFAULT 0 COMMENT '单次经验量',
    `daily_limit`          int unsigned     NOT NULL DEFAULT 0 COMMENT '单日上限次数（0=不限）',
    `daily_limit_strategy` varchar(20)      NOT NULL DEFAULT 'SKIP' COMMENT '超限策略：SKIP/PARTIAL',
    `enabled`              tinyint(1)       NOT NULL DEFAULT 1 COMMENT '是否启用',
    `effective_at`         datetime         NULL DEFAULT NULL COMMENT '生效时间（NULL=立即生效）',
    `operator`             varchar(100)     NULL DEFAULT NULL COMMENT '最后操作人',
    `reason`               varchar(500)     NULL DEFAULT NULL COMMENT '变更原因',
    `created_at`           datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`           datetime         NULL DEFAULT NULL COMMENT '软删除时间',
    `version`              int              NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_event_role_enabled` (`event_type`, `role`, `enabled`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='经验规则配置';

-- 等级阈值配置
CREATE TABLE IF NOT EXISTS `level_threshold_config` (
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `level`       int unsigned     NOT NULL COMMENT '等级值（1,2,3...）',
    `min_exp`     int unsigned     NOT NULL COMMENT '达到该等级所需最低累计经验',
    `level_name`  varchar(50)      NOT NULL DEFAULT '' COMMENT '等级名称',
    `description` varchar(255)     NULL DEFAULT NULL COMMENT '等级描述',
    `enabled`     tinyint(1)       NOT NULL DEFAULT 1 COMMENT '是否启用',
    `operator`    varchar(100)     NULL DEFAULT NULL COMMENT '最后操作人',
    `created_at`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime         NULL DEFAULT NULL COMMENT '软删除时间',
    `version`     int              NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_level` (`level`) USING BTREE,
    INDEX `idx_level_enabled` (`level`, `enabled`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='等级阈值配置';

-- 事件幂等消费记录
CREATE TABLE IF NOT EXISTS `event_consume_record` (
    `id`            bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_id`      varchar(128)     NOT NULL COMMENT '事件唯一ID',
    `consumer_name` varchar(100)     NOT NULL COMMENT '消费者名称',
    `status`        varchar(20)      NOT NULL DEFAULT 'PROCESSING' COMMENT '状态：PROCESSING/SUCCESS/FAILED',
    `error_msg`     varchar(1000)    NULL DEFAULT NULL COMMENT '失败原因',
    `created_at`    datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    datetime         NULL DEFAULT NULL COMMENT '软删除时间',
    `version`       int              NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_event_consumer` (`event_id`, `consumer_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件幂等消费记录';


-- ========================
-- 2. 积分账户
-- ========================

-- 用户积分账户
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

-- 用户积分流水（不可变）
CREATE TABLE IF NOT EXISTS `user_point_journal` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       bigint unsigned NOT NULL COMMENT '用户ID',
    `delta`         bigint          NOT NULL COMMENT '变化量（正=入账，负=扣减）',
    `balance_after` bigint          NOT NULL DEFAULT 0 COMMENT '变更后余额快照',
    `source_type`   varchar(50)     NOT NULL COMMENT '来源类型',
    `biz_no`        varchar(128)    NOT NULL COMMENT '业务单号（幂等键）',
    `remark`        varchar(500)    NULL DEFAULT NULL COMMENT '备注',
    `operator`      varchar(100)    NULL DEFAULT NULL COMMENT '操作人',
    `created_at`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at`    datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`       int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_biz_no` (`biz_no`) USING BTREE,
    INDEX `idx_user_created` (`user_id`, `created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分流水（不可变）';

-- 积分规则配置
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

-- 管理员积分调整日志
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


-- ========================
-- 3. 签到
-- ========================

-- 用户签到记录
CREATE TABLE IF NOT EXISTS `sign_in_record` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`          bigint unsigned NOT NULL COMMENT '用户ID',
    `sign_date`        date            NOT NULL COMMENT '签到日期',
    `consecutive_days` int unsigned    NOT NULL DEFAULT 1 COMMENT '连续签到天数',
    `points_granted`   int             NOT NULL DEFAULT 0 COMMENT '本次发放积分',
    `created_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at`       datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`          int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_sign_date` (`user_id`, `sign_date`) USING BTREE,
    INDEX `idx_user_date_desc` (`user_id`, `sign_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户签到记录';

-- 用户签到统计表
CREATE TABLE IF NOT EXISTS `user_sign_in_stats` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         bigint unsigned NOT NULL COMMENT '用户ID',
    `total_sign_days` int unsigned    NOT NULL DEFAULT 0 COMMENT '累计签到总天数',
    `current_streak`  int unsigned    NOT NULL DEFAULT 0 COMMENT '当前连续签到天数',
    `longest_streak`  int unsigned    NOT NULL DEFAULT 0 COMMENT '历史最长连续签到天数',
    `last_sign_date`  date            NULL DEFAULT NULL COMMENT '最后签到日期',
    `created_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`         int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户签到统计表';

-- 连续签到奖励配置表
CREATE TABLE IF NOT EXISTS `sign_in_consecutive_reward_config` (
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `min_days`     int unsigned    NOT NULL COMMENT '最小连续天数',
    `max_days`     int unsigned    NULL DEFAULT NULL COMMENT '最大连续天数（NULL=无上限）',
    `bonus_points` int             NOT NULL DEFAULT 0 COMMENT '额外奖励积分',
    `reward_tier`  varchar(32)     NOT NULL COMMENT '档位标识：NORMAL/TRIPLE/WEEK/BIWEEK/MONTH',
    `reward_desc`  varchar(128)    NULL DEFAULT NULL COMMENT '奖励描述',
    `enabled`      tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`   datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`      int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_days_range` (`min_days`, `max_days`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连续签到奖励配置表';

-- 累计签到里程碑奖励配置表
CREATE TABLE IF NOT EXISTS `sign_in_cumulative_reward_config` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `milestone_days` int unsigned    NOT NULL COMMENT '累计签到天数里程碑',
    `reward_points`  int             NOT NULL DEFAULT 0 COMMENT '奖励积分',
    `reward_title`   varchar(64)     NULL DEFAULT NULL COMMENT '解锁称号',
    `badge_code`     varchar(64)     NULL DEFAULT NULL COMMENT '徽章标识',
    `description`    varchar(255)    NULL DEFAULT NULL COMMENT '里程碑说明',
    `enabled`        tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`     datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`        int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_milestone` (`milestone_days`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='累计签到里程碑奖励配置表';

-- 等级奖励配置表
CREATE TABLE IF NOT EXISTS `level_reward_config` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `level`         int unsigned    NOT NULL COMMENT '等级',
    `reward_points` int             NOT NULL DEFAULT 0 COMMENT '升级奖励积分',
    `reward_title`  varchar(64)     NULL DEFAULT NULL COMMENT '解锁称号',
    `description`   varchar(255)    NULL DEFAULT NULL COMMENT '奖励说明',
    `enabled`       tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`       int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_level` (`level`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='等级奖励配置表';

-- 等级权益配置表
CREATE TABLE IF NOT EXISTS `level_privilege_config` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `level`          int unsigned    NOT NULL COMMENT '等级',
    `privilege_code` varchar(64)     NOT NULL COMMENT '权益编码',
    `privilege_name` varchar(64)     NOT NULL COMMENT '权益名称',
    `description`    varchar(255)    NULL DEFAULT NULL COMMENT '权益说明',
    `enabled`        tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`     datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`        int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_level_privilege` (`level`, `privilege_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='等级权益配置表';

-- 用户权益授予记录表
CREATE TABLE IF NOT EXISTS `user_privilege_entitlement` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        bigint unsigned NOT NULL COMMENT '用户ID',
    `privilege_code` varchar(64)     NOT NULL COMMENT '权益编码',
    `source_level`   int unsigned    NOT NULL COMMENT '权益来源等级',
    `status`         varchar(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE',
    `granted_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发放时间',
    `created_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`     datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`        int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_privilege` (`user_id`, `privilege_code`) USING BTREE,
    INDEX `idx_privilege_code` (`privilege_code`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户权益授予记录表';

-- 用户奖励领取记录表
CREATE TABLE IF NOT EXISTS `user_reward_grant_log` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        bigint unsigned NOT NULL COMMENT '用户ID',
    `reward_type`    varchar(32)     NOT NULL COMMENT '奖励类型：LEVEL_UP/CONSECUTIVE_SIGN/CUMULATIVE_SIGN',
    `reward_id`      bigint unsigned NOT NULL COMMENT '关联配置ID',
    `points_granted` int             NOT NULL DEFAULT 0 COMMENT '发放积分数',
    `granted_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发放时间',
    `remark`         varchar(255)    NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_reward` (`user_id`, `reward_type`, `reward_id`) USING BTREE,
    INDEX `idx_user_type` (`user_id`, `reward_type`) USING BTREE,
    INDEX `idx_user_granted` (`user_id`, `granted_at`) USING BTREE,
    INDEX `idx_reward_type_granted` (`reward_type`, `granted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户奖励领取记录表';

-- 徽章定义表
CREATE TABLE IF NOT EXISTS `badge_definition` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(64)     NOT NULL COMMENT '徽章编码',
    `type`        varchar(32)     NOT NULL COMMENT '徽章类型：LEVEL/SIGN/ANNUAL',
    `name`        varchar(64)     NOT NULL COMMENT '徽章名称',
    `description` varchar(255)    NULL DEFAULT NULL COMMENT '徽章描述',
    `icon_key`    varchar(64)     NOT NULL DEFAULT '' COMMENT '前端图形键',
    `tone`        varchar(32)     NOT NULL DEFAULT 'slate' COMMENT '视觉色调',
    `rarity`      varchar(32)     NOT NULL DEFAULT 'COMMON' COMMENT '稀有度',
    `sort_order`  int             NOT NULL DEFAULT 0 COMMENT '排序值',
    `enabled`     tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`     int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_code` (`code`) USING BTREE,
    INDEX `idx_type_sort` (`type`, `sort_order`, `deleted_at`) USING BTREE,
    INDEX `idx_enabled_sort` (`enabled`, `sort_order`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='徽章定义表';

-- 用户已拥有徽章表
CREATE TABLE IF NOT EXISTS `user_badge` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     bigint unsigned NOT NULL COMMENT '用户ID',
    `badge_code`  varchar(64)     NOT NULL COMMENT '徽章编码',
    `source_type` varchar(32)     NOT NULL COMMENT '来源类型：LEVEL/SIGN_IN/PRIVILEGE/BACKFILL',
    `source_id`   bigint unsigned NULL DEFAULT NULL COMMENT '来源ID',
    `granted_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授予时间',
    `created_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`     int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_badge` (`user_id`, `badge_code`) USING BTREE,
    INDEX `idx_badge_code` (`badge_code`, `deleted_at`) USING BTREE,
    INDEX `idx_user_granted` (`user_id`, `granted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户已拥有徽章表';

-- 用户徽章佩戴设置表
CREATE TABLE IF NOT EXISTS `user_badge_setting` (
    `id`                  bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`             bigint unsigned NOT NULL COMMENT '用户ID',
    `equipped_badge_code` varchar(64)     NULL DEFAULT NULL COMMENT '当前佩戴徽章编码',
    `created_at`          datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`          datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`             int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user` (`user_id`) USING BTREE,
    INDEX `idx_equipped_badge` (`equipped_badge_code`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户徽章佩戴设置表';


-- ========================
-- 4. 邀请
-- ========================

-- 邀请关系表
CREATE TABLE IF NOT EXISTS `invite_relation` (
    `id`                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `inviter_user_id`   bigint unsigned NOT NULL COMMENT '邀请人用户ID',
    `invitee_user_id`   bigint unsigned NOT NULL COMMENT '被邀请人用户ID',
    `reward_status`     varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT '奖励状态：PENDING/GRANTED/SKIPPED',
    `reward_granted_at` datetime        NULL DEFAULT NULL COMMENT '奖励发放时间',
    `trigger_biz_no`    varchar(128)    NULL DEFAULT NULL COMMENT '触发奖励的业务单号',
    `skip_reason`       varchar(500)    NULL DEFAULT NULL COMMENT '跳过原因',
    `created_at`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`        datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`           int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_invitee` (`invitee_user_id`) USING BTREE,
    INDEX `idx_inviter_status` (`inviter_user_id`, `reward_status`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请关系表';

-- v3 migration: 追加邀请码字段
ALTER TABLE `invite_relation`
    ADD COLUMN `invite_code` varchar(20) NULL DEFAULT NULL COMMENT '使用的邀请码' AFTER `invitee_user_id`;

-- ========================
-- 5. 充值
-- ========================

-- 充值订单表
CREATE TABLE IF NOT EXISTS `recharge_order` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `pay_order_no`   varchar(128)    NOT NULL COMMENT '支付单号（幂等键）',
    `user_id`        bigint unsigned NOT NULL COMMENT '用户ID',
    `amount_fen`     bigint          NOT NULL COMMENT '充值金额（分）',
    `points_granted` bigint          NOT NULL DEFAULT 0 COMMENT '发放积分数',
    `status`         varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED',
    `notify_count`   int             NOT NULL DEFAULT 0 COMMENT '回调次数（监控用）',
    `sign`           varchar(512)    NULL DEFAULT NULL COMMENT '支付平台签名',
    `raw_payload`    text            NULL COMMENT '原始回调报文',
    `created_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`     datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`        int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_pay_order_no` (`pay_order_no`) USING BTREE,
    INDEX `idx_user_created` (`user_id`, `created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单表';


-- ========================
-- 6. 文章解锁
-- ========================

-- 文章解锁订单表
CREATE TABLE IF NOT EXISTS `article_unlock_order` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`    varchar(128)    NOT NULL COMMENT '解锁订单号（幂等键）',
    `user_id`     bigint unsigned NOT NULL COMMENT '解锁用户ID',
    `article_id`  bigint unsigned NOT NULL COMMENT '文章ID',
    `points_cost` bigint          NOT NULL COMMENT '扣除积分数',
    `status`      varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED',
    `fail_reason` varchar(500)    NULL DEFAULT NULL COMMENT '失败原因',
    `created_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`     int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_order_no` (`order_no`) USING BTREE,
    UNIQUE INDEX `uk_user_article` (`user_id`, `article_id`) USING BTREE,
    INDEX `idx_article_status` (`article_id`, `status`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章解锁订单';

-- 文章解锁关系表（解锁成功后的凭证）
CREATE TABLE IF NOT EXISTS `article_unlock_relation` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`    bigint unsigned NOT NULL COMMENT '解锁用户ID',
    `article_id` bigint unsigned NOT NULL COMMENT '文章ID',
    `order_no`   varchar(128)    NOT NULL COMMENT '关联订单号',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at` datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`    int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_article` (`user_id`, `article_id`) USING BTREE,
    INDEX `idx_article_id` (`article_id`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章解锁关系（解锁凭证）';

-- 分账流水表
CREATE TABLE IF NOT EXISTS `revenue_share_journal` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`        varchar(128)    NOT NULL COMMENT '关联解锁订单号',
    `article_id`      bigint unsigned NOT NULL COMMENT '文章ID',
    `author_id`       bigint unsigned NOT NULL COMMENT '作者用户ID',
    `total_points`    bigint          NOT NULL COMMENT '订单总积分',
    `platform_points` bigint          NOT NULL COMMENT '平台分成积分',
    `author_points`   bigint          NOT NULL COMMENT '作者分成积分',
    `share_ratio`     varchar(20)     NOT NULL DEFAULT '50:50' COMMENT '分成比例（平台:作者）',
    `settlement_status` varchar(20)    NOT NULL DEFAULT 'PENDING' COMMENT '结算状态：PENDING/SETTLED/FAILED',
    `point_journal_biz_no` varchar(128) NULL DEFAULT NULL COMMENT '作者积分入账流水业务单号',
    `settled_at`      datetime        NULL DEFAULT NULL COMMENT '结算完成时间',
    `retry_count`     int             NOT NULL DEFAULT 0 COMMENT '结算重试次数',
    `last_error`      varchar(500)    NULL DEFAULT NULL COMMENT '最近一次结算失败原因',
    `created_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at`      datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`         int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_order_no` (`order_no`) USING BTREE,
    INDEX `idx_author_created` (`author_id`, `created_at`) USING BTREE,
    INDEX `idx_settlement_status` (`settlement_status`, `retry_count`, `created_at`) USING BTREE,
    INDEX `idx_status_created` (`settlement_status`, `created_at`) USING BTREE,
    INDEX `idx_author_status_created` (`author_id`, `settlement_status`, `created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分账流水';

-- 文章表新增字段（ALTER，不重建表）
ALTER TABLE `blog_article`
    ADD COLUMN `need_unlock`        tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否需要解锁：1-需要 0-免费' AFTER `seo_description`,
    ADD COLUMN `unlock_point_price` int        NOT NULL DEFAULT 0 COMMENT '解锁所需积分数' AFTER `need_unlock`;

-- 首页推荐申请表
CREATE TABLE IF NOT EXISTS `article_recommendation_application` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id`  bigint unsigned NOT NULL COMMENT '文章ID',
    `author_id`   bigint unsigned NOT NULL COMMENT '作者用户ID',
    `status`      varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
    `applied_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `reviewed_at` datetime        NULL DEFAULT NULL COMMENT '审核时间',
    `reviewed_by` bigint unsigned NULL DEFAULT NULL COMMENT '审核人用户ID',
    `review_note` varchar(255)    NULL DEFAULT NULL COMMENT '审核备注',
    `created_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`     int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_article_status` (`article_id`, `status`, `deleted_at`) USING BTREE,
    INDEX `idx_author_status` (`author_id`, `status`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页推荐申请表';


-- ========================
-- 7. 初始化数据
-- ========================

-- 初始化等级配置
INSERT IGNORE INTO `level_threshold_config` (`level`, `min_exp`, `level_name`, `description`, `enabled`) VALUES
(1,     0, '新手', '刚入门的博客读者',       1),
(2,   100, '学徒', '已积累一定阅读经验',     1),
(3,   300, '进阶', '活跃参与社区互动',       1),
(4,   800, '达人', '持续输出高质量内容',     1),
(5,  1800, '专家', '社区认可的内容创作者',   1),
(6,  3500, '大师作者', '领域深度贡献者',     1),
(7,  7000, '资深专家', '平台资深内容贡献者', 1),
(8, 13000, '领域导师', '持续产出优质内容',   1),
(9, 22000, '技术领袖', '具备社区影响力',     1),
(10, 35000, '荣耀创作者', '长期杰出贡献者',  1);

-- 初始化徽章定义
INSERT IGNORE INTO `badge_definition`
    (`code`, `type`, `name`, `description`, `icon_key`, `tone`, `rarity`, `sort_order`, `enabled`)
VALUES
('LEVEL_1', 'LEVEL', '新手', '达到 Lv.1', 'level-1', 'mint', 'COMMON', 101, 1),
('LEVEL_2', 'LEVEL', '学徒', '达到 Lv.2', 'level-2', 'sky', 'COMMON', 102, 1),
('LEVEL_3', 'LEVEL', '进阶', '达到 Lv.3', 'level-3', 'indigo', 'COMMON', 103, 1),
('LEVEL_4', 'LEVEL', '达人', '达到 Lv.4', 'level-4', 'amber', 'RARE', 104, 1),
('LEVEL_5', 'LEVEL', '专家', '达到 Lv.5', 'level-5', 'rose', 'RARE', 105, 1),
('LEVEL_6', 'LEVEL', '大师作者', '达到 Lv.6', 'level-6', 'violet', 'EPIC', 106, 1),
('LEVEL_7', 'LEVEL', '资深专家', '达到 Lv.7', 'level-7', 'teal', 'EPIC', 107, 1),
('LEVEL_8', 'LEVEL', '领域导师', '达到 Lv.8', 'level-8', 'gold', 'LEGENDARY', 108, 1),
('LEVEL_9', 'LEVEL', '技术领袖', '达到 Lv.9', 'level-9', 'crimson', 'LEGENDARY', 109, 1),
('LEVEL_10', 'LEVEL', '荣耀创作者', '达到 Lv.10', 'level-10', 'aurora', 'LEGENDARY', 110, 1),
('SIGN_7', 'SIGN', '一周坚持者', '累计签到 7 天', 'sign-7', 'mint', 'COMMON', 201, 1),
('SIGN_30', 'SIGN', '月度达人', '累计签到 30 天', 'sign-30', 'sky', 'RARE', 202, 1),
('SIGN_100', 'SIGN', '百日挑战者', '累计签到 100 天', 'sign-100', 'teal', 'EPIC', 203, 1),
('SIGN_200', 'SIGN', '半年坚持者', '累计签到 200 天', 'sign-200', 'amber', 'EPIC', 204, 1),
('SIGN_365', 'SIGN', '年度创作者', '累计签到 365 天', 'sign-365', 'gold', 'LEGENDARY', 205, 1),
('ANNUAL_CREATOR_CANDIDATE', 'ANNUAL', '年度创作者候选', '获得年度创作者候选资格', 'annual-candidate', 'crimson', 'LEGENDARY', 301, 1);

-- 初始化经验规则
INSERT INTO `growth_rule_config`
    (`event_type`, `role`, `exp_amount`, `daily_limit`, `daily_limit_strategy`, `enabled`, `operator`, `reason`)
SELECT seed.event_type, seed.role, seed.exp_amount, seed.daily_limit,
       seed.daily_limit_strategy, seed.enabled, seed.operator, seed.reason
FROM (
    SELECT 'LIKE' AS event_type, 'ACTOR' AS role, 1 AS exp_amount, 20 AS daily_limit,
           'SKIP' AS daily_limit_strategy, 1 AS enabled, 'system' AS operator, '初始化' AS reason
    UNION ALL SELECT 'LIKE', 'AUTHOR', 3, 30, 'SKIP', 1, 'system', '初始化'
    UNION ALL SELECT 'READ', 'ACTOR', 1, 20, 'SKIP', 1, 'system', '初始化'
    UNION ALL SELECT 'COMMENT', 'ACTOR', 3, 10, 'SKIP', 1, 'system', '初始化'
    UNION ALL SELECT 'COMMENT', 'AUTHOR', 5, 20, 'SKIP', 1, 'system', '初始化'
    UNION ALL SELECT 'FAVORITE', 'ACTOR', 3, 10, 'SKIP', 1, 'system', '初始化'
    UNION ALL SELECT 'FAVORITE', 'AUTHOR', 8, 20, 'SKIP', 1, 'system', '初始化'
    UNION ALL SELECT 'SHARE', 'ACTOR', 3, 5, 'SKIP', 1, 'system', '初始化'
    UNION ALL SELECT 'FOLLOW', 'ACTOR', 5, 5, 'SKIP', 1, 'system', '初始化'
    UNION ALL SELECT 'FOLLOW', 'AUTHOR', 10, 10, 'SKIP', 1, 'system', '初始化'
    UNION ALL SELECT 'PUBLISH', 'ACTOR', 30, 3, 'SKIP', 1, 'system',
                     '初始化 - 作者发布文章每日最多3篇计经验'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `growth_rule_config` existing
    WHERE existing.event_type = seed.event_type
      AND existing.role = seed.role
);

-- 初始化积分规则
INSERT IGNORE INTO `point_rule_config` (`source_type`, `point_amount`, `daily_limit`, `enabled`, `operator`, `reason`) VALUES
('SIGN_IN',            10, 1, 1, 'system', '初始化'),
('INVITE',             50, 0, 1, 'system', '初始化'),
('RECHARGE',            1, 0, 1, 'system', '初始化 - 1元=1积分'),
('PUBLISH',             5, 3, 1, 'system', '初始化 - 作者发布文章，每日最多3篇计积分'),
('UNLOCK_SHARE_RATIO', 50, 0, 1, 'system', '初始化 - 平台分账比例50%');

-- 初始化连续签到奖励配置
UPDATE `sign_in_consecutive_reward_config` duplicate_config
JOIN (
    SELECT current_config.id
    FROM `sign_in_consecutive_reward_config` current_config
    JOIN (
        SELECT `min_days`, `max_days`, MIN(`id`) AS keep_id
        FROM `sign_in_consecutive_reward_config`
        WHERE `deleted_at` IS NULL
        GROUP BY `min_days`, `max_days`
        HAVING COUNT(*) > 1
    ) kept_config
      ON current_config.`min_days` = kept_config.`min_days`
     AND current_config.`max_days` <=> kept_config.`max_days`
     AND current_config.`id` <> kept_config.keep_id
    WHERE current_config.`deleted_at` IS NULL
) duplicates
  ON duplicate_config.`id` = duplicates.`id`
SET duplicate_config.`deleted_at` = NOW(),
    duplicate_config.`updated_at` = NOW(),
    duplicate_config.`version` = duplicate_config.`version` + 1
WHERE duplicate_config.`deleted_at` IS NULL;

INSERT INTO `sign_in_consecutive_reward_config`
    (`min_days`, `max_days`, `bonus_points`, `reward_tier`, `reward_desc`)
SELECT seed.min_days, seed.max_days, seed.bonus_points, seed.reward_tier, seed.reward_desc
FROM (
    SELECT 1 AS min_days, 2 AS max_days, 0 AS bonus_points, 'NORMAL' AS reward_tier, '继续加油' AS reward_desc
    UNION ALL SELECT 3, 4, 5, 'TRIPLE', '连续 3 天，额外 +5'
    UNION ALL SELECT 5, 6, 8, 'TRIPLE', '连续 5 天，额外 +8'
    UNION ALL SELECT 7, 13, 10, 'WEEK', '连续 7 天，额外 +10'
    UNION ALL SELECT 14, 29, 20, 'BIWEEK', '连续 14 天，额外 +20'
    UNION ALL SELECT 30, NULL, 50, 'MONTH', '连续 30 天，额外 +50'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `sign_in_consecutive_reward_config` existing
    WHERE existing.deleted_at IS NULL
      AND existing.min_days = seed.min_days
      AND (
          existing.max_days = seed.max_days
          OR (existing.max_days IS NULL AND seed.max_days IS NULL)
      )
);

-- 初始化累计签到里程碑配置
INSERT IGNORE INTO `sign_in_cumulative_reward_config`
    (`milestone_days`, `reward_points`, `reward_title`, `badge_code`, `description`)
VALUES
(7, 20, '一周坚持者', 'SIGN_7', '累计签到 7 天'),
(30, 100, '月度达人', 'SIGN_30', '累计签到 30 天'),
(100, 500, '百日挑战者', 'SIGN_100', '累计签到 100 天'),
(200, 1000, '半年坚持者', 'SIGN_200', '累计签到 200 天'),
(365, 2000, '年度创作者', 'SIGN_365', '累计签到 365 天');

UPDATE `sign_in_cumulative_reward_config`
SET `badge_code` = CASE `milestone_days`
    WHEN 7 THEN 'SIGN_7'
    WHEN 30 THEN 'SIGN_30'
    WHEN 100 THEN 'SIGN_100'
    WHEN 200 THEN 'SIGN_200'
    WHEN 365 THEN 'SIGN_365'
    ELSE `badge_code`
END
WHERE `deleted_at` IS NULL
  AND (`badge_code` IS NULL OR `badge_code` = '')
  AND `milestone_days` IN (7, 30, 100, 200, 365);

-- 初始化等级奖励配置
INSERT IGNORE INTO `level_reward_config` (`level`, `reward_points`, `reward_title`, `description`) VALUES
(1, 0, '新手', '注册即得'),
(2, 50, '见习作者', '升级奖励 50 积分'),
(3, 100, '活跃创作者', '升级奖励 100 积分'),
(4, 200, '优质作者', '解锁付费文章发布权限'),
(5, 500, '专家作者', '升级奖励 500 积分'),
(6, 1000, '大师作者', '解锁专属徽章'),
(7, 2000, '宗师作者', '平台首页推荐资格'),
(8, 5000, '传奇作者', '年度创作者评选资格');

-- 初始化等级权益配置
INSERT IGNORE INTO `level_privilege_config`
    (`level`, `privilege_code`, `privilege_name`, `description`, `enabled`)
VALUES
    (4, 'PAID_ARTICLE_PUBLISH', '付费文章发布权限', '达到 Lv.4 后可开启文章积分解锁', 1),
    (6, 'EXCLUSIVE_BADGE', '专属徽章', '达到 Lv.6 后显示专属徽章', 1),
    (7, 'HOMEPAGE_RECOMMEND_ELIGIBLE', '首页推荐申请资格', '达到 Lv.7 后可申请首页推荐', 1),
    (8, 'ANNUAL_CREATOR_ELIGIBLE', '年度创作者候选资格', '达到 Lv.8 后进入年度创作者候选池', 1);

