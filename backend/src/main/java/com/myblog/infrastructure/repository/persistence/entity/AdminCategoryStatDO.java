package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 管理后台分类文章统计 DO。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdminCategoryStatDO {

    /**
     * 分类名称
     */
    private String category;

    /**
     * 该分类下的文章数量
     */
    private long articleCount;

    /**
     * 获取分类名称。
     *
     * @return 分类名称
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置分类名称。
     *
     * @param category 分类名称
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 获取该分类下的文章数量。
     *
     * @return 文章数量
     */
    public long getArticleCount() {
        return articleCount;
    }

    /**
     * 设置该分类下的文章数量。
     *
     * @param articleCount 文章数量
     */
    public void setArticleCount(long articleCount) {
        this.articleCount = articleCount;
    }
}

