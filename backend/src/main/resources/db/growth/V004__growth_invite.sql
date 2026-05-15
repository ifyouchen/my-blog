SET NAMES utf8mb4;
USE `my_blog`;

CREATE TABLE IF NOT EXISTS `invite_relation` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `inviter_user_id` bigint unsigned NOT NULL COMMENT '邀请人用户ID',
    `invitee_user_id` bigint unsigned NOT NULL COMMENT '被邀请人用户ID',
    `reward_status`   varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT '奖励状态：PENDING/GRANTED/SKIPPED',
    `reward_granted_at` datetime      NULL DEFAULT NULL COMMENT '奖励发放时间',
    `trigger_biz_no`  varchar(128)    NULL DEFAULT NULL COMMENT '触发奖励的业务单号',
    `skip_reason`     varchar(500)    NULL DEFAULT NULL COMMENT '跳过原因',
    `created_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      datetime        NULL DEFAULT NULL COMMENT '软删除时间',
    `version`         int             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_invitee` (`invitee_user_id`) USING BTREE,
    INDEX `idx_inviter_status` (`inviter_user_id`, `reward_status`, `deleted_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请关系表';

