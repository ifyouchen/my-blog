SET NAMES utf8mb4;
USE `my_blog`;

CREATE TABLE IF NOT EXISTS `recharge_order` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `pay_order_no`  varchar(128)    NOT NULL COMMENT '支付单号（幂等键）',
    `user_id`       bigint unsigned NOT NULL COMMENT '用户ID',
    `amount_fen`    bigint          NOT NULL COMMENT '充值金额（分）',
    `points_granted` bigint         NOT NULL DEFAULT 0 COMMENT '发放积分数',
    `status`        varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED',
    `notify_count`  int             NOT NULL DEFAULT 0 COMMENT '回调次数（监控用）',
    `sign`          varchar(512)    NULL DEFAULT NULL COMMENT '支付平台签名',
    `raw_payload`   text            NULL COMMENT '原始回调报文',
    `created_at`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`       int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_pay_order_no` (`pay_order_no`) USING BTREE,
    INDEX `idx_user_created` (`user_id`, `created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值订单表';

