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
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
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
    use_count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
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
    parent_id BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED-已发布 DELETED-已删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    KEY idx_blog_comment_article (article_id),
    KEY idx_blog_comment_user (user_id),
    KEY idx_blog_comment_parent (parent_id),
    CONSTRAINT fk_blog_comment_article FOREIGN KEY (article_id) REFERENCES blog_article (id),
    CONSTRAINT fk_blog_comment_user FOREIGN KEY (user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客评论表';

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

CREATE TABLE IF NOT EXISTS blog_admin_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    admin_user_id BIGINT UNSIGNED NOT NULL COMMENT '管理员用户ID',
    operation VARCHAR(100) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) NOT NULL COMMENT '目标类型',
    target_id BIGINT UNSIGNED DEFAULT NULL COMMENT '目标ID',
    detail VARCHAR(1000) DEFAULT NULL COMMENT '操作详情',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间',
    version INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (id),
    KEY idx_blog_admin_log_admin_created (admin_user_id, created_at),
    CONSTRAINT fk_blog_admin_log_admin FOREIGN KEY (admin_user_id) REFERENCES blog_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客管理员操作日志表';
