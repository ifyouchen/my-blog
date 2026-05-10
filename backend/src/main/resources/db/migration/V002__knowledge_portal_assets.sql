-- DevNotes 技术知识门户资产化迁移脚本。
-- 可重复执行：通过 information_schema 检查列/索引/表是否已存在。

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing $$
CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_sql_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = table_name_value
        AND COLUMN_NAME = column_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN ', column_sql_value);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS add_index_if_missing $$
CREATE PROCEDURE add_index_if_missing(
    IN table_name_value VARCHAR(64),
    IN index_name_value VARCHAR(64),
    IN index_sql_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = table_name_value
        AND INDEX_NAME = index_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD ', index_sql_value);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_column_if_missing('blog_topic', 'intro', '`intro` text NULL COMMENT ''专题导读''');
CALL add_column_if_missing('blog_topic', 'difficulty',
    '`difficulty` varchar(20) NOT NULL DEFAULT ''INTERMEDIATE'' COMMENT ''难度：BEGINNER/INTERMEDIATE/ADVANCED''');
CALL add_column_if_missing('blog_topic', 'estimated_minutes',
    '`estimated_minutes` int NOT NULL DEFAULT 0 COMMENT ''预计阅读分钟数''');
CALL add_column_if_missing('blog_topic', 'source_type',
    '`source_type` varchar(20) NOT NULL DEFAULT ''ORIGINAL'' COMMENT ''来源类型：ORIGINAL/CURATED/IMPORTED''');
CALL add_column_if_missing('blog_topic', 'source_note',
    '`source_note` varchar(500) NULL DEFAULT '''' COMMENT ''来源说明''');
CALL add_column_if_missing('blog_topic', 'recommended',
    '`recommended` tinyint(1) NOT NULL DEFAULT 1 COMMENT ''是否推荐：1-推荐 0-不推荐''');
CALL add_column_if_missing('blog_topic', 'recommend_weight',
    '`recommend_weight` int NOT NULL DEFAULT 0 COMMENT ''推荐权重，值越大越靠前''');

CALL add_column_if_missing('blog_column', 'intro', '`intro` text NULL COMMENT ''专栏导读''');
CALL add_column_if_missing('blog_column', 'difficulty',
    '`difficulty` varchar(20) NOT NULL DEFAULT ''INTERMEDIATE'' COMMENT ''难度：BEGINNER/INTERMEDIATE/ADVANCED''');
CALL add_column_if_missing('blog_column', 'estimated_minutes',
    '`estimated_minutes` int NOT NULL DEFAULT 0 COMMENT ''预计阅读分钟数''');
CALL add_column_if_missing('blog_column', 'source_type',
    '`source_type` varchar(20) NOT NULL DEFAULT ''ORIGINAL'' COMMENT ''来源类型：ORIGINAL/CURATED/IMPORTED''');
CALL add_column_if_missing('blog_column', 'source_note',
    '`source_note` varchar(500) NULL DEFAULT '''' COMMENT ''来源说明''');

CALL add_column_if_missing('blog_topic_article', 'section_title',
    '`section_title` varchar(120) NULL DEFAULT ''推荐阅读'' COMMENT ''章节名称''');
CALL add_column_if_missing('blog_topic_article', 'step_order',
    '`step_order` int NOT NULL DEFAULT 0 COMMENT ''学习路径步骤序号''');
CALL add_column_if_missing('blog_topic_article', 'required',
    '`required` tinyint(1) NOT NULL DEFAULT 1 COMMENT ''是否必读''');
CALL add_column_if_missing('blog_topic_article', 'editor_note',
    '`editor_note` varchar(500) NULL DEFAULT '''' COMMENT ''编者备注''');

CALL add_column_if_missing('blog_column_article', 'section_title',
    '`section_title` varchar(120) NULL DEFAULT ''推荐阅读'' COMMENT ''章节名称''');
CALL add_column_if_missing('blog_column_article', 'step_order',
    '`step_order` int NOT NULL DEFAULT 0 COMMENT ''学习路径步骤序号''');
CALL add_column_if_missing('blog_column_article', 'required',
    '`required` tinyint(1) NOT NULL DEFAULT 1 COMMENT ''是否必读''');
CALL add_column_if_missing('blog_column_article', 'editor_note',
    '`editor_note` varchar(500) NULL DEFAULT '''' COMMENT ''编者备注''');

CREATE TABLE IF NOT EXISTS `blog_learning_progress` (
    `id`                    bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`               bigint unsigned NOT NULL COMMENT '用户ID',
    `asset_type`            varchar(20)     NOT NULL COMMENT '学习资产类型：TOPIC/COLUMN',
    `asset_id`              bigint unsigned NOT NULL COMMENT '学习资产ID',
    `completed_article_ids` text            NULL COMMENT '已读文章ID列表，逗号分隔',
    `completed_count`       int             NOT NULL DEFAULT 0 COMMENT '已读文章数',
    `progress_percent`      int             NOT NULL DEFAULT 0 COMMENT '学习进度百分比',
    `last_article_id`       bigint unsigned NULL COMMENT '最后操作文章ID',
    `last_read_at`          datetime        NULL DEFAULT NULL COMMENT '最后阅读时间',
    `created_at`            datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`            datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`               int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_learning_progress_user_asset` (`user_id`, `asset_type`, `asset_id`) USING BTREE,
    INDEX `idx_learning_progress_asset` (`asset_type`, `asset_id`, `deleted_at`) USING BTREE,
    INDEX `idx_learning_progress_user_updated` (`user_id`, `updated_at` DESC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户学习进度表' ROW_FORMAT = Dynamic;

CALL add_index_if_missing('blog_topic', 'idx_topic_recommended',
    'INDEX `idx_topic_recommended` (`recommended`, `deleted_at`, `recommend_weight` DESC, `sort_order` ASC) USING BTREE');
CALL add_index_if_missing('blog_topic', 'ft_topic_knowledge',
    'FULLTEXT INDEX `ft_topic_knowledge` (`title`, `summary`, `intro`) WITH PARSER ngram');
CALL add_index_if_missing('blog_column', 'ft_column_knowledge',
    'FULLTEXT INDEX `ft_column_knowledge` (`title`, `summary`, `intro`) WITH PARSER ngram');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
