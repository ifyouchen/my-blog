SET NAMES utf8mb4;
USE `my_blog`;

-- user_growth_account
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

-- user_exp_journal
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

-- daily_reward_counter
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

-- growth_rule_config
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

-- level_threshold_config
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

-- event_consume_record
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

-- 初始化等级配置
INSERT IGNORE INTO `level_threshold_config` (`level`, `min_exp`, `level_name`, `description`, `enabled`) VALUES
(1,     0,    '新手',   '刚入门的博客读者',       1),
(2,   100,    '学徒',   '已积累一定阅读经验',      1),
(3,   300,    '进阶',   '活跃参与社区互动',        1),
(4,   700,    '达人',   '持续输出高质量内容',      1),
(5,  1500,    '专家',   '社区认可的内容创作者',    1),
(6,  3000,    '大师',   '领域深度贡献者',          1),
(7,  6000,    '宗师',   '平台顶尖创作者',          1),
(8, 12000,    '传奇',   '极少数杰出贡献者',        1);

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

