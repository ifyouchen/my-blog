-- 为 blog_article 表新增 feature_weight 字段（精选权重，初始类型 tinyint unsigned）
-- 已存在精选的文章默认权重设为 50，非精选保持 0
-- 注意：列类型后续在 V003 中扩展为 int unsigned 以支持更大范围
ALTER TABLE blog_article
ADD COLUMN `feature_weight` tinyint unsigned NOT NULL DEFAULT 0
COMMENT '精选权重，值越大在精选列表和推荐中排越靠前'
AFTER `featured_at`;

-- 回填：已精选文章给予默认权重 50
UPDATE blog_article
SET feature_weight = 50
WHERE featured = 1
  AND deleted_at IS NULL;

