-- 扩展 feature_weight 列类型：tinyint unsigned (max 255) → int unsigned (max ~42亿)
-- 以支持 0-1000000 的精选权重范围
ALTER TABLE blog_article
    MODIFY COLUMN `feature_weight` int unsigned NOT NULL DEFAULT 0
    COMMENT '精选权重 0-1000000，值越大在精选列表和推荐中排越靠前';

