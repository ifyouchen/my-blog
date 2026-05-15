SET NAMES utf8mb4;
USE `my_blog`;

-- 文章解锁订单表
CREATE TABLE IF NOT EXISTS `article_unlock_order` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`       varchar(128)    NOT NULL COMMENT '解锁订单号（幂等键）',
    `user_id`        bigint unsigned NOT NULL COMMENT '解锁用户ID',
    `article_id`     bigint unsigned NOT NULL COMMENT '文章ID',
    `points_cost`    bigint          NOT NULL COMMENT '扣除积分数',
    `status`         varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED',
    `fail_reason`    varchar(500)    NULL DEFAULT NULL COMMENT '失败原因',
    `created_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`     datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`        int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
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
    `created_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_at`      datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`         int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_order_no` (`order_no`) USING BTREE,
    INDEX `idx_author_created` (`author_id`, `created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分账流水';

-- 文章表新增字段（ALTER，不重建表）
ALTER TABLE `blog_article`
    ADD COLUMN IF NOT EXISTS `need_unlock`        tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否需要解锁：1-需要 0-免费' AFTER `seo_description`,
    ADD COLUMN IF NOT EXISTS `unlock_point_price` int        NOT NULL DEFAULT 0 COMMENT '解锁所需积分数' AFTER `need_unlock`;

