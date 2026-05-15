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
    INDEX `idx_settlement_status` (`settlement_status`, `retry_count`, `created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分账流水';

-- 文章表新增字段（ALTER，不重建表）
ALTER TABLE `blog_article`
    ADD COLUMN `need_unlock`        tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否需要解锁：1-需要 0-免费' AFTER `seo_description`,
    ADD COLUMN `unlock_point_price` int        NOT NULL DEFAULT 0 COMMENT '解锁所需积分数' AFTER `need_unlock`;


-- ========================
-- 7. 初始化数据
-- ========================

-- 初始化等级配置
INSERT IGNORE INTO `level_threshold_config` (`level`, `min_exp`, `level_name`, `description`, `enabled`) VALUES
(1,     0, '新手', '刚入门的博客读者',     1),
(2,   100, '学徒', '已积累一定阅读经验',   1),
(3,   300, '进阶', '活跃参与社区互动',     1),
(4,   700, '达人', '持续输出高质量内容',   1),
(5,  1500, '专家', '社区认可的内容创作者', 1),
(6,  3000, '大师', '领域深度贡献者',       1),
(7,  6000, '宗师', '平台顶尖创作者',       1),
(8, 12000, '传奇', '极少数杰出贡献者',     1);

-- 初始化经验规则
INSERT IGNORE INTO `growth_rule_config`
    (`event_type`, `role`, `exp_amount`, `daily_limit`, `daily_limit_strategy`, `enabled`, `operator`, `reason`) VALUES
('LIKE',     'ACTOR',  2,  20, 'SKIP', 1, 'system', '初始化'),
('LIKE',     'AUTHOR', 5,  50, 'SKIP', 1, 'system', '初始化'),
('READ',     'ACTOR',  1,  30, 'SKIP', 1, 'system', '初始化'),
('COMMENT',  'ACTOR',  5,  10, 'SKIP', 1, 'system', '初始化'),
('COMMENT',  'AUTHOR', 3,  30, 'SKIP', 1, 'system', '初始化'),
('FAVORITE', 'ACTOR',  3,  15, 'SKIP', 1, 'system', '初始化'),
('FAVORITE', 'AUTHOR', 8,  40, 'SKIP', 1, 'system', '初始化'),
('SHARE',    'ACTOR',  4,  10, 'SKIP', 1, 'system', '初始化'),
('FOLLOW',   'ACTOR',  5,   5, 'SKIP', 1, 'system', '初始化'),
('FOLLOW',   'AUTHOR',10,  20, 'SKIP', 1, 'system', '初始化'),
('PUBLISH',  'ACTOR', 20,   3, 'SKIP', 1, 'system', '初始化 - 作者发布文章每日最多3篇计经验');

-- 初始化积分规则
INSERT IGNORE INTO `point_rule_config` (`source_type`, `point_amount`, `daily_limit`, `enabled`, `operator`, `reason`) VALUES
('SIGN_IN',            10, 1, 1, 'system', '初始化'),
('INVITE',             50, 0, 1, 'system', '初始化'),
('RECHARGE',            1, 0, 1, 'system', '初始化 - 1元=1积分'),
('PUBLISH',             5, 3, 1, 'system', '初始化 - 作者发布文章，每日最多3篇计积分'),
('UNLOCK_SHARE_RATIO', 50, 0, 1, 'system', '初始化 - 平台分账比例50%');

