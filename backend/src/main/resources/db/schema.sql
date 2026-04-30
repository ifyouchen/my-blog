SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS `my_blog` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `my_blog`;

-- ----------------------------
-- blog_user
-- ----------------------------
DROP TABLE IF EXISTS `blog_user`;
CREATE TABLE `blog_user` (
    `id`               bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`         varchar(50)      NOT NULL COMMENT '用户名',
    `email`            varchar(120)     NOT NULL COMMENT '邮箱',
    `password_hash`    varchar(120)     NOT NULL COMMENT '密码哈希',
    `nickname`         varchar(50)      NOT NULL COMMENT '昵称',
    `avatar_url`       varchar(500)     NULL DEFAULT NULL COMMENT '头像地址',
    `bio`                     varchar(500)     NULL DEFAULT NULL COMMENT '个人简介',
    `website`                 varchar(500)     NULL DEFAULT NULL COMMENT '个人网站',
    `github`                  varchar(200)     NULL DEFAULT NULL COMMENT 'GitHub 用户名',
    `twitter`                 varchar(200)     NULL DEFAULT NULL COMMENT 'Twitter/X 用户名',
    `location`                varchar(200)     NULL DEFAULT NULL COMMENT '所在地',
    `disable_reason`          varchar(500)     NULL DEFAULT NULL COMMENT '禁用原因',
    `password_reset_token`    varchar(128)     NULL DEFAULT NULL COMMENT '密码重置 Token',
    `password_reset_expire`   datetime         NULL DEFAULT NULL COMMENT '密码重置 Token 过期时间',
    `last_login_at`           datetime         NULL DEFAULT NULL COMMENT '最近登录时间',
    `last_login_ip`           varchar(64)      NULL DEFAULT NULL COMMENT '最近登录 IP',
    `last_username_changed_at` datetime        NULL DEFAULT NULL COMMENT '最近修改用户名时间',
    `recommended`             tinyint(1)       NOT NULL DEFAULT 0 COMMENT '是否推荐：1-推荐 0-不推荐',
    `recommend_weight` int              NOT NULL DEFAULT 0 COMMENT '推荐权重，值越大越靠前',
    `role`             varchar(20)      NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户 ADMIN-管理员',
    `status`           varchar(20)      NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常 DISABLED-禁用 DELETED-删除',
    `created_at`       datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`       datetime         NULL DEFAULT NULL COMMENT '删除时间',
    `version`          int              NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_user_username` (`username`) USING BTREE,
    UNIQUE INDEX `uk_blog_user_email` (`email`) USING BTREE,
    INDEX `idx_blog_user_status_created` (`status`, `created_at`) USING BTREE,
    INDEX `idx_user_recommended` (`recommended`, `deleted_at`, `recommend_weight` DESC, `created_at` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1004
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_category
-- ----------------------------
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(50)     NOT NULL COMMENT '分类名称',
    `description` varchar(255)    NULL DEFAULT NULL COMMENT '分类说明',
    `sort_order`  int             NOT NULL DEFAULT 0 COMMENT '排序值',
    `enabled`     tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    `status`      varchar(20)     NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常 DISABLED-禁用',
    `created_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`     int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_category_name` (`name`) USING BTREE,
    INDEX `idx_blog_category_status_sort` (`status`, `sort_order`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_tag
-- ----------------------------
DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(50)     NOT NULL COMMENT '标签名称',
    `description` varchar(255)    NULL DEFAULT NULL COMMENT '标签说明',
    `use_count`   int             NOT NULL DEFAULT 0 COMMENT '使用次数',
    `enabled`     tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    `status`      varchar(20)     NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常 DISABLED-禁用',
    `created_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`     int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_tag_name` (`name`) USING BTREE,
    INDEX `idx_blog_tag_status_count` (`status`, `use_count`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_article
-- ----------------------------
DROP TABLE IF EXISTS `blog_article`;
CREATE TABLE `blog_article` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `author_id`       bigint unsigned NOT NULL COMMENT '作者ID',
    `title`           varchar(120)    NOT NULL COMMENT '文章标题',
    `summary`         varchar(300)    NOT NULL DEFAULT '' COMMENT '文章摘要',
    `content`         mediumtext      NOT NULL COMMENT '文章正文',
    `cover_url`       varchar(500)    NULL DEFAULT NULL COMMENT '封面地址',
    `category`        varchar(50)     NOT NULL COMMENT '文章分类',
    `offline_reason`  varchar(500)    NULL DEFAULT NULL COMMENT '下架原因',
    `featured`        tinyint(1)      NOT NULL DEFAULT 0 COMMENT '是否精选：1-精选 0-非精选',
    `featured_at`     datetime        NULL DEFAULT NULL COMMENT '精选时间',
    `slug`            varchar(255)    NULL DEFAULT NULL COMMENT 'URL 友好标识，唯一索引',
    `seo_title`       varchar(255)    NULL DEFAULT NULL COMMENT 'SEO 标题',
    `seo_description` varchar(500)    NULL DEFAULT NULL COMMENT 'SEO 描述',
    `warn_flag`       tinyint(1)      NOT NULL DEFAULT 0 COMMENT '敏感词警告标记：0-正常 1-含警告词待审核',
    `status`          varchar(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿 PUBLISHED-已发布 OFFLINE-已下架 DELETED-已删除',
    `view_count`      int             NOT NULL DEFAULT 0 COMMENT '阅读数',
    `like_count`      int             NOT NULL DEFAULT 0 COMMENT '点赞数',
    `favorite_count`  int             NOT NULL DEFAULT 0 COMMENT '收藏数',
    `comment_count`   int             NOT NULL DEFAULT 0 COMMENT '评论数',
    `published_at`    datetime        NULL DEFAULT NULL COMMENT '发布时间',
    `created_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`         int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_article_slug` (`slug`) USING BTREE,
    INDEX `idx_article_feed_latest` (`status`, `deleted_at`, `published_at` DESC, `id` DESC) USING BTREE,
    INDEX `idx_article_category_latest` (`status`, `deleted_at`, `category`, `published_at` DESC, `id` DESC) USING BTREE,
    INDEX `idx_article_author_status_updated_v2` (`author_id`, `deleted_at`, `status`, `updated_at` DESC, `id` DESC) USING BTREE,
    INDEX `idx_article_hot_v2` (`status`, `deleted_at`, `view_count` DESC, `published_at` DESC, `id` DESC) USING BTREE,
    INDEX `idx_article_featured_v2` (`status`, `deleted_at`, `like_count` DESC, `published_at` DESC, `id` DESC) USING BTREE,
    INDEX `idx_article_featured` (`featured`, `status`, `deleted_at`, `featured_at` DESC, `id` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9004
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客文章表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_article_version
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_version`;
CREATE TABLE `blog_article_version` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id` bigint unsigned NOT NULL COMMENT '文章ID',
    `version_no` int             NOT NULL COMMENT '版本号，从 1 开始递增',
    `title`      varchar(500)    NOT NULL COMMENT '文章标题快照',
    `content`    longtext        NOT NULL COMMENT '文章正文快照',
    `summary`    varchar(500)    NULL DEFAULT NULL COMMENT '文章摘要快照',
    `saved_by`   bigint unsigned NOT NULL COMMENT '保存操作的用户ID',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_article_version` (`article_id`, `version_no` DESC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '文章版本历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_article_favorite
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_favorite`;
CREATE TABLE `blog_article_favorite` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id` bigint unsigned NOT NULL COMMENT '文章ID',
    `user_id`    bigint unsigned NOT NULL COMMENT '用户ID',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`    int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_article_favorite_article_user` (`article_id`, `user_id`) USING BTREE,
    INDEX `idx_favorite_user_page_v2` (`user_id`, `deleted_at`, `created_at` DESC, `article_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客文章收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_article_like
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_like`;
CREATE TABLE `blog_article_like` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id` bigint unsigned NOT NULL COMMENT '文章ID',
    `user_id`    bigint unsigned NOT NULL COMMENT '用户ID',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`    int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_article_like_article_user` (`article_id`, `user_id`) USING BTREE,
    INDEX `idx_like_user_article_v2` (`user_id`, `deleted_at`, `article_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客文章点赞表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_article_tag
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_tag`;
CREATE TABLE `blog_article_tag` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id` bigint unsigned NOT NULL COMMENT '文章ID',
    `tag_name`   varchar(50)     NOT NULL COMMENT '标签名称',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`    int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_article_tag_article_name` (`article_id`, `tag_name`) USING BTREE,
    INDEX `idx_article_tag_lookup_v2` (`tag_name`, `deleted_at`, `article_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客文章标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_article_view
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_view`;
CREATE TABLE `blog_article_view` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id` bigint unsigned NOT NULL COMMENT '文章ID',
    `user_id`    bigint unsigned NULL COMMENT '用户ID',
    `ip_address` varchar(64)     NULL DEFAULT NULL COMMENT 'IP地址',
    `user_agent` varchar(500)    NULL DEFAULT NULL COMMENT '浏览器信息',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`    int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_blog_article_view_article_created` (`article_id`, `created_at`) USING BTREE,
    INDEX `idx_blog_article_view_user` (`user_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客阅读记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_column
-- ----------------------------
DROP TABLE IF EXISTS `blog_column`;
CREATE TABLE `blog_column` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `author_id`        bigint unsigned NOT NULL COMMENT '专栏作者ID',
    `title`            varchar(120)    NOT NULL COMMENT '专栏标题',
    `summary`          varchar(500)    NULL DEFAULT '' COMMENT '专栏简介',
    `cover_url`        varchar(500)    NULL DEFAULT NULL COMMENT '专栏封面',
    `status`           varchar(20)     NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED-已发布 OFFLINE-已下架 DELETED-已删除',
    `sort_order`       int             NOT NULL DEFAULT 0 COMMENT '排序值',
    `subscriber_count` int             NOT NULL DEFAULT 0 COMMENT '订阅数',
    `article_count`    int             NOT NULL DEFAULT 0 COMMENT '已发布文章数（冗余字段，避免子查询）',
    `recommended`      tinyint(1)      NOT NULL DEFAULT 0 COMMENT '是否推荐：1-推荐 0-不推荐',
    `recommend_weight` int             NOT NULL DEFAULT 0 COMMENT '推荐权重，值越大越靠前',
    `created_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`       datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`          int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_blog_column_author` (`author_id`) USING BTREE,
    INDEX `idx_column_published_sort_v2` (`status`, `deleted_at`, `sort_order` ASC, `subscriber_count` DESC, `id` DESC) USING BTREE,
    INDEX `idx_column_recommended` (`recommended`, `deleted_at`, `recommend_weight` DESC, `subscriber_count` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2004
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客专栏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_column_article
-- ----------------------------
DROP TABLE IF EXISTS `blog_column_article`;
CREATE TABLE `blog_column_article` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `column_id`  bigint unsigned NOT NULL COMMENT '专栏ID',
    `article_id` bigint unsigned NOT NULL COMMENT '文章ID',
    `sort_order` int             NOT NULL DEFAULT 0 COMMENT '排序值',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`    int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_column_article_pair` (`column_id`, `article_id`) USING BTREE,
    INDEX `idx_blog_column_article_article` (`article_id`) USING BTREE,
    INDEX `idx_column_article_page_v2` (`column_id`, `deleted_at`, `sort_order` ASC, `id` ASC, `article_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客专栏文章关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_column_subscription
-- ----------------------------
DROP TABLE IF EXISTS `blog_column_subscription`;
CREATE TABLE `blog_column_subscription` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `column_id`  bigint unsigned NOT NULL COMMENT '专栏ID',
    `user_id`    bigint unsigned NOT NULL COMMENT '用户ID',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`    int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_column_subscription_pair` (`column_id`, `user_id`) USING BTREE,
    INDEX `idx_blog_column_subscription_user` (`user_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客专栏订阅表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_comment
-- ----------------------------
DROP TABLE IF EXISTS `blog_comment`;
CREATE TABLE `blog_comment` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id`      bigint unsigned NOT NULL COMMENT '文章ID',
    `user_id`         bigint unsigned NOT NULL COMMENT '评论用户ID',
    `root_comment_id` bigint unsigned NULL COMMENT '根评论ID',
    `parent_id`       bigint unsigned NULL COMMENT '父评论ID',
    `content`         varchar(1000)   NOT NULL COMMENT '评论内容',
    `status`          varchar(20)     NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED-已发布 DELETED-已删除',
    `like_count`      int             NOT NULL DEFAULT 0 COMMENT '点赞数',
    `edited_at`       datetime        NULL DEFAULT NULL COMMENT '最后编辑时间',
    `edit_count`      tinyint         NOT NULL DEFAULT 0 COMMENT '编辑次数',
    `pinned`          tinyint(1)      NOT NULL DEFAULT 0 COMMENT '是否置顶',
    `pinned_at`       datetime        NULL DEFAULT NULL COMMENT '置顶时间',
    `created_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`         int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_blog_comment_user` (`user_id`) USING BTREE,
    INDEX `idx_comment_root_page_v2` (`article_id`, `parent_id`, `status`, `deleted_at`, `pinned` DESC, `pinned_at` DESC, `like_count` DESC, `created_at` DESC, `id` DESC) USING BTREE,
    INDEX `idx_comment_reply_page_v2` (`root_comment_id`, `parent_id`, `status`, `deleted_at`, `created_at` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_comment_like
-- ----------------------------
DROP TABLE IF EXISTS `blog_comment_like`;
CREATE TABLE `blog_comment_like` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `comment_id` bigint unsigned NOT NULL COMMENT '评论ID',
    `user_id`    bigint unsigned NOT NULL COMMENT '用户ID',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`    int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_comment_like_comment_user` (`comment_id`, `user_id`) USING BTREE,
    INDEX `idx_blog_comment_like_user` (`user_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客评论点赞表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_notification
-- ----------------------------
DROP TABLE IF EXISTS `blog_notification`;
CREATE TABLE `blog_notification` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `receiver_user_id` bigint unsigned NOT NULL COMMENT '接收者用户ID',
    `actor_user_id`    bigint unsigned NOT NULL COMMENT '触发者用户ID',
    `type`             varchar(32)     NOT NULL COMMENT '通知类型：ARTICLE_LIKE, ARTICLE_FAVORITE, ARTICLE_COMMENT, COMMENT_REPLY, COMMENT_LIKE, USER_FOLLOW',
    `article_id`       bigint unsigned NULL COMMENT '文章ID',
    `comment_id`       bigint unsigned NULL COMMENT '评论ID',
    `payload_json`     text            NULL COMMENT '展示快照JSON',
    `read_at`          datetime        NULL DEFAULT NULL COMMENT '已读时间',
    `created_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`       datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`          bigint          NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_blog_notification_actor` (`actor_user_id`) USING BTREE,
    INDEX `idx_notification_receiver_page_v2` (`receiver_user_id`, `deleted_at`, `created_at` DESC, `id` DESC) USING BTREE,
    INDEX `idx_notification_receiver_read_page_v2` (`receiver_user_id`, `deleted_at`, `read_at`, `created_at` DESC, `id` DESC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_user_follow
-- ----------------------------
DROP TABLE IF EXISTS `blog_user_follow`;
CREATE TABLE `blog_user_follow` (
    `id`                bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `follower_user_id`  bigint unsigned NOT NULL COMMENT '关注者用户ID',
    `following_user_id` bigint unsigned NOT NULL COMMENT '被关注用户ID',
    `created_at`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`        datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`           int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_blog_user_follow_pair` (`follower_user_id`, `following_user_id`) USING BTREE,
    INDEX `idx_following_page_v2` (`follower_user_id`, `deleted_at`, `created_at` DESC, `id` DESC, `following_user_id`) USING BTREE,
    INDEX `idx_follower_count_v2` (`following_user_id`, `deleted_at`, `follower_user_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客用户关注表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_user_search_history
-- ----------------------------
DROP TABLE IF EXISTS `blog_user_search_history`;
CREATE TABLE `blog_user_search_history` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`          bigint unsigned NOT NULL COMMENT '用户ID',
    `keyword`          varchar(100)    NOT NULL COMMENT '搜索关键字',
    `search_count`     int             NOT NULL DEFAULT 1 COMMENT '搜索次数',
    `last_searched_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后搜索时间',
    `created_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`       datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`          bigint          NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_search_history_user_keyword` (`user_id`, `keyword`) USING BTREE,
    INDEX `idx_search_history_last_searched` (`last_searched_at`) USING BTREE,
    INDEX `idx_search_history_recent_v2` (`user_id`, `deleted_at`, `last_searched_at` DESC, `id` DESC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户搜索历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_report
-- ----------------------------
DROP TABLE IF EXISTS `blog_report`;
CREATE TABLE `blog_report` (
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reporter_user_id` bigint unsigned NOT NULL COMMENT '举报用户ID',
    `target_type`      varchar(20)     NOT NULL COMMENT '目标类型：ARTICLE COMMENT USER',
    `target_id`        bigint unsigned NOT NULL COMMENT '目标ID',
    `reason_type`      varchar(32)     NOT NULL COMMENT '原因类型：SPAM INFRINGEMENT ABUSE ILLEGAL OTHER',
    `reason_detail`    varchar(1000)   NOT NULL DEFAULT '' COMMENT '原因详情',
    `status`           varchar(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING RESOLVED REJECTED',
    `handler_user_id`  bigint unsigned NULL DEFAULT NULL COMMENT '处理人ID',
    `handle_note`      varchar(1000)   NULL DEFAULT NULL COMMENT '处理备注',
    `handled_at`       datetime        NULL DEFAULT NULL COMMENT '处理时间',
    `created_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`       datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`          int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_blog_report_status_created` (`status`, `created_at` DESC, `id` DESC) USING BTREE,
    INDEX `idx_blog_report_target` (`target_type`, `target_id`) USING BTREE,
    INDEX `idx_blog_report_reporter_created` (`reporter_user_id`, `created_at` DESC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客举报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_article_daily_stats
-- ----------------------------
DROP TABLE IF EXISTS `blog_article_daily_stats`;
CREATE TABLE `blog_article_daily_stats` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id`     bigint unsigned NOT NULL COMMENT '文章ID',
    `stat_date`      date            NOT NULL COMMENT '统计日期',
    `view_count`     int             NOT NULL DEFAULT 0 COMMENT '阅读数',
    `like_count`     int             NOT NULL DEFAULT 0 COMMENT '点赞数',
    `favorite_count` int             NOT NULL DEFAULT 0 COMMENT '收藏数',
    `comment_count`  int             NOT NULL DEFAULT 0 COMMENT '评论数',
    `created_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`     datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`        int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_article_daily_stats_article_date` (`article_id`, `stat_date`) USING BTREE,
    INDEX `idx_article_daily_stats_date` (`stat_date`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '文章日统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_creator_daily_stats
-- ----------------------------
DROP TABLE IF EXISTS `blog_creator_daily_stats`;
CREATE TABLE `blog_creator_daily_stats` (
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        bigint unsigned NOT NULL COMMENT '创作者用户ID',
    `stat_date`      date            NOT NULL COMMENT '统计日期',
    `view_count`     int             NOT NULL DEFAULT 0 COMMENT '阅读数',
    `like_count`     int             NOT NULL DEFAULT 0 COMMENT '点赞数',
    `favorite_count` int             NOT NULL DEFAULT 0 COMMENT '收藏数',
    `comment_count`  int             NOT NULL DEFAULT 0 COMMENT '评论数',
    `follower_count` int             NOT NULL DEFAULT 0 COMMENT '粉丝数',
    `article_count`  int             NOT NULL DEFAULT 0 COMMENT '文章数',
    `created_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`     datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`        int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_creator_daily_stats_user_date` (`user_id`, `stat_date`) USING BTREE,
    INDEX `idx_creator_daily_stats_date` (`stat_date`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '创作者日统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_admin_log
-- ----------------------------
DROP TABLE IF EXISTS `blog_admin_log`;
CREATE TABLE `blog_admin_log` (
    `id`              bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `admin_user_id`   bigint unsigned NOT NULL COMMENT '管理员用户ID',
    `admin_username`  varchar(100)    NULL DEFAULT NULL COMMENT '管理员用户名',
    `operation`       varchar(100)    NOT NULL COMMENT '操作类型',
    `target_type`     varchar(50)     NOT NULL COMMENT '目标类型',
    `target_id`       bigint unsigned NULL COMMENT '目标ID',
    `detail`          varchar(1000)   NULL DEFAULT NULL COMMENT '操作详情',
    `request_method`  varchar(16)     NULL DEFAULT NULL COMMENT '请求方法',
    `request_uri`     varchar(255)    NULL DEFAULT NULL COMMENT '请求路径',
    `ip_address`      varchar(64)     NULL DEFAULT NULL COMMENT 'IP地址',
    `user_agent`      varchar(255)    NULL DEFAULT NULL COMMENT '用户代理',
    `result_status`   varchar(32)     NULL DEFAULT NULL COMMENT '结果状态',
    `before_snapshot` text            NULL COMMENT '变更前快照',
    `after_snapshot`  text            NULL COMMENT '变更后快照',
    `created_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`      datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`         int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_blog_admin_log_admin_created` (`admin_user_id`, `created_at`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客管理员操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_topic
-- ----------------------------
DROP TABLE IF EXISTS `blog_topic`;
CREATE TABLE `blog_topic` (
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`         varchar(120)    NOT NULL COMMENT '专题标题',
    `summary`       varchar(500)    NULL DEFAULT '' COMMENT '专题简介',
    `cover_url`     varchar(500)    NULL DEFAULT NULL COMMENT '专题封面',
    `status`        varchar(20)     NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED-已发布 OFFLINE-已下架',
    `sort_order`    int             NOT NULL DEFAULT 0 COMMENT '排序值',
    `article_count` int             NOT NULL DEFAULT 0 COMMENT '专题内文章数',
    `created_at`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`    datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`       int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_topic_status_sort` (`status`, `deleted_at`, `sort_order` ASC, `id` DESC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客专题表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_topic_article
-- ----------------------------
DROP TABLE IF EXISTS `blog_topic_article`;
CREATE TABLE `blog_topic_article` (
    `id`         bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `topic_id`   bigint unsigned NOT NULL COMMENT '专题ID',
    `article_id` bigint unsigned NOT NULL COMMENT '文章ID',
    `sort_order` int             NOT NULL DEFAULT 0 COMMENT '排序值',
    `created_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`    int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_topic_article_pair` (`topic_id`, `article_id`) USING BTREE,
    INDEX `idx_topic_article_article` (`article_id`) USING BTREE,
    INDEX `idx_topic_article_sort` (`topic_id`, `deleted_at`, `sort_order` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '博客专题文章关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_ad_slot
-- ----------------------------
DROP TABLE IF EXISTS `blog_ad_slot`;
CREATE TABLE `blog_ad_slot` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(64)     NOT NULL COMMENT '广告位编码，唯一，如 home_sidebar',
    `name`        varchar(100)    NOT NULL COMMENT '广告位名称',
    `description` varchar(500)    NULL DEFAULT NULL COMMENT '广告位说明',
    `enabled`     tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    `created_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ad_slot_code` (`code`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '广告位表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_ad_campaign
-- ----------------------------
DROP TABLE IF EXISTS `blog_ad_campaign`;
CREATE TABLE `blog_ad_campaign` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `slot_code`   varchar(64)     NOT NULL COMMENT '关联广告位编码',
    `title`       varchar(200)    NOT NULL COMMENT '广告标题',
    `image_url`   varchar(500)    NULL DEFAULT NULL COMMENT '广告图片地址',
    `target_url`  varchar(1000)   NOT NULL COMMENT '点击跳转链接',
    `label`       varchar(50)     NOT NULL DEFAULT '广告' COMMENT '广告标签，如 广告/推广/赞助',
    `start_at`    datetime        NULL DEFAULT NULL COMMENT '展示开始时间',
    `end_at`      datetime        NULL DEFAULT NULL COMMENT '展示结束时间',
    `enabled`     tinyint(1)      NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    `sort_order`  int             NOT NULL DEFAULT 0 COMMENT '排序值，值越小越靠前',
    `created_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime        NULL DEFAULT NULL COMMENT '删除时间',
    `version`     int             NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_ad_campaign_slot_enabled` (`slot_code`, `enabled`, `deleted_at`, `start_at`, `end_at`, `sort_order`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '广告投放表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_ad_event
-- ----------------------------
DROP TABLE IF EXISTS `blog_ad_event`;
CREATE TABLE `blog_ad_event` (
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `campaign_id` bigint unsigned NOT NULL COMMENT '广告投放ID',
    `event_type`  varchar(20)     NOT NULL COMMENT '事件类型：IMPRESSION-曝光 CLICK-点击',
    `user_id`     bigint unsigned NULL DEFAULT NULL COMMENT '用户ID（登录用户）',
    `ip_address`  varchar(64)     NULL DEFAULT NULL COMMENT 'IP地址',
    `user_agent`  varchar(500)    NULL DEFAULT NULL COMMENT '用户代理',
    `created_at`  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_ad_event_campaign_type` (`campaign_id`, `event_type`, `created_at`) USING BTREE,
    INDEX `idx_ad_event_date` (`created_at`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '广告事件表（曝光/点击记录）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_announcement
-- ----------------------------
DROP TABLE IF EXISTS `blog_announcement`;
CREATE TABLE `blog_announcement` (
    `id`           bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`        varchar(200)     NOT NULL COMMENT '公告标题',
    `content`      text             NOT NULL COMMENT '公告内容（支持 Markdown）',
    `target`       varchar(20)      NOT NULL DEFAULT 'ALL' COMMENT '目标用户：ALL / AUTHOR / ADMIN',
    `published`    tinyint(1)       NOT NULL DEFAULT 0 COMMENT '是否发布：1-已发布 0-草稿',
    `published_at` datetime         NULL DEFAULT NULL COMMENT '发布时间',
    `expires_at`   datetime         NULL DEFAULT NULL COMMENT '过期时间，NULL 表示永不过期',
    `created_at`   datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`   datetime         NULL DEFAULT NULL COMMENT '删除时间',
    `version`      int              NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_announcement_published` (`published`, `published_at` DESC, `deleted_at`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '平台公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- blog_sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `blog_sensitive_word`;
CREATE TABLE `blog_sensitive_word` (
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `word`        varchar(100)     NOT NULL COMMENT '敏感词',
    `category`    varchar(50)      NULL DEFAULT NULL COMMENT '分类（如：政治、色情、广告等）',
    `level`       varchar(20)      NOT NULL DEFAULT 'WARN' COMMENT '等级：WARN-警告 BLOCK-拦截',
    `enabled`     tinyint(1)       NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    `created_at`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime         NULL DEFAULT NULL COMMENT '删除时间',
    `version`     int              NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_sensitive_word` (`word`, `deleted_at`) USING BTREE,
    INDEX `idx_sensitive_word_enabled` (`enabled`, `deleted_at`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '敏感词表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- ----------------------------
-- blog_invite_code
-- ----------------------------
DROP TABLE IF EXISTS `blog_invite_code`;
CREATE TABLE `blog_invite_code` (
    `id`          bigint unsigned  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        varchar(32)      NOT NULL COMMENT '邀请码（唯一）',
    `creator_id`  bigint unsigned  NOT NULL DEFAULT 0 COMMENT '创建者ID（0表示系统生成）',
    `used_by`     bigint unsigned  NULL DEFAULT NULL COMMENT '被使用者ID',
    `used_at`     datetime         NULL DEFAULT NULL COMMENT '使用时间',
    `expired_at`  datetime         NULL DEFAULT NULL COMMENT '过期时间',
    `max_uses`    int              NOT NULL DEFAULT 1 COMMENT '最大使用次数',
    `use_count`   int              NOT NULL DEFAULT 0 COMMENT '已使用次数',
    `created_at`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at`  datetime         NULL DEFAULT NULL COMMENT '删除时间',
    `version`     int              NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_invite_code` (`code`) USING BTREE,
    INDEX `idx_invite_code_creator` (`creator_id`, `deleted_at`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '邀请码表' ROW_FORMAT = Dynamic;

-- Init data
-- ----------------------------
INSERT INTO `blog_ad_slot` (`code`, `name`, `description`, `enabled`) VALUES
('home_sidebar',    '首页右侧栏',   '首页右侧边栏广告位',     1),
('article_sidebar', '文章详情右侧', '文章详情页右侧边栏广告位', 1),
('search_top',      '搜索结果顶部', '搜索结果页顶部广告位',   1);
