-- 回填现有专栏的 article_count 冗余字段
-- 从 blog_column_article 中统计每个专栏已发布、未删除的文章数
UPDATE blog_column c
SET c.article_count = (
    SELECT COUNT(1)
    FROM blog_column_article ca
    INNER JOIN blog_article a ON a.id = ca.article_id
    WHERE ca.column_id = c.id
    AND ca.deleted_at IS NULL
    AND a.deleted_at IS NULL
    AND a.status = 'PUBLISHED'
)
WHERE c.deleted_at IS NULL;
