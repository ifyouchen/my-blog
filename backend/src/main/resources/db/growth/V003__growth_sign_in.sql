SET NAMES utf8mb4;
USE `my_blog`;

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

