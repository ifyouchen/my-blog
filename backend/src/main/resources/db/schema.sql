CREATE TABLE IF NOT EXISTS blog_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(120) NOT NULL COMMENT '邮箱',
    password_hash VARCHAR(120) NOT NULL COMMENT '密码哈希',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    bio VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER-普通用户 ADMIN-管理员',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常 DISABLED-禁用 DELETED-删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_user_username (username),
    UNIQUE KEY uk_blog_user_email (email),
    KEY idx_blog_user_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客用户表';

CREATE TABLE IF NOT EXISTS blog_article (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    author_id BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
    title VARCHAR(120) NOT NULL COMMENT '文章标题',
    summary VARCHAR(300) NOT NULL DEFAULT '' COMMENT '文章摘要',
    content MEDIUMTEXT NOT NULL COMMENT '文章正文',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面地址',
    category VARCHAR(50) NOT NULL COMMENT '文章分类',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿 PUBLISHED-已发布 OFFLINE-已下架 DELETED-已删除',
    view_count INT NOT NULL DEFAULT 0 COMMENT '阅读数',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数',
    published_at DATETIME DEFAULT NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    KEY idx_blog_article_author (author_id),
    KEY idx_blog_article_status_created (status, created_at),
    KEY idx_blog_article_category_created (category, created_at),
    CONSTRAINT fk_blog_article_author FOREIGN KEY (author_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章表';

CREATE TABLE IF NOT EXISTS blog_article_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    article_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_article_tag_article_name (article_id, tag_name),
    KEY idx_blog_article_tag_name (tag_name),
    CONSTRAINT fk_blog_article_tag_article FOREIGN KEY (article_id) REFERENCES blog_article (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章标签表';

CREATE TABLE IF NOT EXISTS blog_category (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '分类说明',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常 DISABLED-禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_category_name (name),
    KEY idx_blog_category_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客分类表';

CREATE TABLE IF NOT EXISTS blog_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '标签说明',
    use_count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL-正常 DISABLED-禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_tag_name (name),
    KEY idx_blog_tag_status_count (status, use_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客标签表';

CREATE TABLE IF NOT EXISTS blog_comment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    article_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '评论用户ID',
    root_comment_id BIGINT UNSIGNED DEFAULT NULL COMMENT '根评论ID',
    parent_id BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED-已发布 DELETED-已删除',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
    pinned_at DATETIME DEFAULT NULL COMMENT '置顶时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    KEY idx_blog_comment_article (article_id),
    KEY idx_blog_comment_user (user_id),
    KEY idx_blog_comment_root (root_comment_id),
    KEY idx_blog_comment_parent (parent_id),
    CONSTRAINT fk_blog_comment_article FOREIGN KEY (article_id) REFERENCES blog_article (id),
    CONSTRAINT fk_blog_comment_user FOREIGN KEY (user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客评论表';

CREATE TABLE IF NOT EXISTS blog_comment_like (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    comment_id BIGINT UNSIGNED NOT NULL COMMENT '评论ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_comment_like_comment_user (comment_id, user_id),
    KEY idx_blog_comment_like_user (user_id),
    CONSTRAINT fk_blog_comment_like_comment FOREIGN KEY (comment_id) REFERENCES blog_comment (id),
    CONSTRAINT fk_blog_comment_like_user FOREIGN KEY (user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客评论点赞表';

SET @blog_comment_add_root_comment_id = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_comment ADD COLUMN root_comment_id BIGINT UNSIGNED DEFAULT NULL COMMENT ''根评论ID'' AFTER user_id',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_comment'
      AND column_name = 'root_comment_id'
);
PREPARE stmt FROM @blog_comment_add_root_comment_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_comment_add_like_count = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_comment ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT ''点赞数'' AFTER status',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_comment'
      AND column_name = 'like_count'
);
PREPARE stmt FROM @blog_comment_add_like_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_comment_add_pinned = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_comment ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否置顶'' AFTER like_count',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_comment'
      AND column_name = 'pinned'
);
PREPARE stmt FROM @blog_comment_add_pinned;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_comment_add_pinned_at = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_comment ADD COLUMN pinned_at DATETIME DEFAULT NULL COMMENT ''置顶时间'' AFTER pinned',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_comment'
      AND column_name = 'pinned_at'
);
PREPARE stmt FROM @blog_comment_add_pinned_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_comment_add_root_index = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_blog_comment_root ON blog_comment (root_comment_id)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_comment'
      AND index_name = 'idx_blog_comment_root'
);
PREPARE stmt FROM @blog_comment_add_root_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

WITH RECURSIVE blog_comment_tree AS (
    SELECT id, id AS root_id
    FROM blog_comment
    WHERE deleted_at IS NULL
      AND (parent_id IS NULL OR parent_id = 0)
    UNION ALL
    SELECT child.id, tree.root_id
    FROM blog_comment child
    INNER JOIN blog_comment_tree tree ON child.parent_id = tree.id
    WHERE child.deleted_at IS NULL
)
UPDATE blog_comment target
INNER JOIN blog_comment_tree source ON target.id = source.id
SET target.root_comment_id = source.root_id
WHERE target.root_comment_id IS NULL
   OR target.root_comment_id <> source.root_id;

CREATE TABLE IF NOT EXISTS blog_article_like (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    article_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_article_like_article_user (article_id, user_id),
    KEY idx_blog_article_like_user (user_id),
    CONSTRAINT fk_blog_article_like_article FOREIGN KEY (article_id) REFERENCES blog_article (id),
    CONSTRAINT fk_blog_article_like_user FOREIGN KEY (user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章点赞表';

CREATE TABLE IF NOT EXISTS blog_article_favorite (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    article_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_article_favorite_article_user (article_id, user_id),
    KEY idx_blog_article_favorite_user (user_id),
    CONSTRAINT fk_blog_article_favorite_article FOREIGN KEY (article_id) REFERENCES blog_article (id),
    CONSTRAINT fk_blog_article_favorite_user FOREIGN KEY (user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章收藏表';

CREATE TABLE IF NOT EXISTS blog_article_view (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    article_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    KEY idx_blog_article_view_article_created (article_id, created_at),
    KEY idx_blog_article_view_user (user_id),
    CONSTRAINT fk_blog_article_view_article FOREIGN KEY (article_id) REFERENCES blog_article (id),
    CONSTRAINT fk_blog_article_view_user FOREIGN KEY (user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客阅读记录表';

CREATE TABLE IF NOT EXISTS blog_user_follow (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    follower_user_id BIGINT UNSIGNED NOT NULL COMMENT '关注者用户ID',
    following_user_id BIGINT UNSIGNED NOT NULL COMMENT '被关注用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_user_follow_pair (follower_user_id, following_user_id),
    KEY idx_blog_user_follow_following (following_user_id),
    CONSTRAINT fk_blog_user_follow_follower FOREIGN KEY (follower_user_id) REFERENCES blog_user (id),
    CONSTRAINT fk_blog_user_follow_following FOREIGN KEY (following_user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客用户关注表';

CREATE TABLE IF NOT EXISTS blog_column (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    author_id BIGINT UNSIGNED NOT NULL COMMENT '专栏作者ID',
    title VARCHAR(120) NOT NULL COMMENT '专栏标题',
    summary VARCHAR(500) DEFAULT '' COMMENT '专栏简介',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '专栏封面',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED-已发布 OFFLINE-已下架 DELETED-已删除',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    subscriber_count INT NOT NULL DEFAULT 0 COMMENT '订阅数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    KEY idx_blog_column_author (author_id),
    KEY idx_blog_column_status_sort (status, sort_order),
    CONSTRAINT fk_blog_column_author FOREIGN KEY (author_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客专栏表';

CREATE TABLE IF NOT EXISTS blog_column_article (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    column_id BIGINT UNSIGNED NOT NULL COMMENT '专栏ID',
    article_id BIGINT UNSIGNED NOT NULL COMMENT '文章ID',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_column_article_pair (column_id, article_id),
    KEY idx_blog_column_article_article (article_id),
    CONSTRAINT fk_blog_column_article_column FOREIGN KEY (column_id) REFERENCES blog_column (id),
    CONSTRAINT fk_blog_column_article_article FOREIGN KEY (article_id) REFERENCES blog_article (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客专栏文章关联表';

CREATE TABLE IF NOT EXISTS blog_column_subscription (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    column_id BIGINT UNSIGNED NOT NULL COMMENT '专栏ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_blog_column_subscription_pair (column_id, user_id),
    KEY idx_blog_column_subscription_user (user_id),
    CONSTRAINT fk_blog_column_subscription_column FOREIGN KEY (column_id) REFERENCES blog_column (id),
    CONSTRAINT fk_blog_column_subscription_user FOREIGN KEY (user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客专栏订阅表';

CREATE TABLE IF NOT EXISTS blog_admin_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    admin_user_id BIGINT UNSIGNED NOT NULL COMMENT '管理员用户ID',
    admin_username VARCHAR(100) DEFAULT NULL COMMENT '管理员用户名',
    operation VARCHAR(100) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) NOT NULL COMMENT '目标类型',
    target_id BIGINT UNSIGNED DEFAULT NULL COMMENT '目标ID',
    detail VARCHAR(1000) DEFAULT NULL COMMENT '操作详情',
    request_method VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
    request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    user_agent VARCHAR(255) DEFAULT NULL COMMENT '用户代理',
    result_status VARCHAR(32) DEFAULT NULL COMMENT '结果状态',
    before_snapshot TEXT COMMENT '变更前快照',
    after_snapshot TEXT COMMENT '变更后快照',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    KEY idx_blog_admin_log_admin_created (admin_user_id, created_at),
    CONSTRAINT fk_blog_admin_log_admin FOREIGN KEY (admin_user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客管理员操作日志表';

SET @blog_admin_log_add_admin_username = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_admin_log ADD COLUMN admin_username VARCHAR(100) DEFAULT NULL COMMENT ''管理员用户名'' AFTER admin_user_id',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_admin_log'
      AND column_name = 'admin_username'
);
PREPARE stmt FROM @blog_admin_log_add_admin_username;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_admin_log_add_request_method = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_admin_log ADD COLUMN request_method VARCHAR(16) DEFAULT NULL COMMENT ''请求方法'' AFTER detail',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_admin_log'
      AND column_name = 'request_method'
);
PREPARE stmt FROM @blog_admin_log_add_request_method;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_admin_log_add_request_uri = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_admin_log ADD COLUMN request_uri VARCHAR(255) DEFAULT NULL COMMENT ''请求路径'' AFTER request_method',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_admin_log'
      AND column_name = 'request_uri'
);
PREPARE stmt FROM @blog_admin_log_add_request_uri;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_admin_log_add_user_agent = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_admin_log ADD COLUMN user_agent VARCHAR(255) DEFAULT NULL COMMENT ''用户代理'' AFTER ip_address',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_admin_log'
      AND column_name = 'user_agent'
);
PREPARE stmt FROM @blog_admin_log_add_user_agent;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_admin_log_add_result_status = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_admin_log ADD COLUMN result_status VARCHAR(32) DEFAULT NULL COMMENT ''结果状态'' AFTER user_agent',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_admin_log'
      AND column_name = 'result_status'
);
PREPARE stmt FROM @blog_admin_log_add_result_status;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_admin_log_add_before_snapshot = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_admin_log ADD COLUMN before_snapshot TEXT COMMENT ''变更前快照'' AFTER result_status',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_admin_log'
      AND column_name = 'before_snapshot'
);
PREPARE stmt FROM @blog_admin_log_add_before_snapshot;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_admin_log_add_after_snapshot = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_admin_log ADD COLUMN after_snapshot TEXT COMMENT ''变更后快照'' AFTER before_snapshot',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_admin_log'
      AND column_name = 'after_snapshot'
);
PREPARE stmt FROM @blog_admin_log_add_after_snapshot;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_category_add_description = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_category ADD COLUMN description VARCHAR(255) DEFAULT NULL COMMENT ''分类说明'' AFTER name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_category'
      AND column_name = 'description'
);
PREPARE stmt FROM @blog_category_add_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_category_add_enabled = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_category ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''是否启用：1-启用 0-禁用'' AFTER sort_order',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_category'
      AND column_name = 'enabled'
);
PREPARE stmt FROM @blog_category_add_enabled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_tag_add_description = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_tag ADD COLUMN description VARCHAR(255) DEFAULT NULL COMMENT ''标签说明'' AFTER name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_tag'
      AND column_name = 'description'
);
PREPARE stmt FROM @blog_tag_add_description;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @blog_tag_add_enabled = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE blog_tag ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''是否启用：1-启用 0-禁用'' AFTER use_count',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'blog_tag'
      AND column_name = 'enabled'
);
PREPARE stmt FROM @blog_tag_add_enabled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE blog_category
SET enabled = CASE
    WHEN status = 'DISABLED' THEN 0
    ELSE 1
END
WHERE deleted_at IS NULL;

UPDATE blog_tag
SET enabled = CASE
    WHEN status = 'DISABLED' THEN 0
    ELSE 1
END
WHERE deleted_at IS NULL;
