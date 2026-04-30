package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 管理后台分类文章统计 DO。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdminCategoryStatDO {

    private String category;
    private long articleCount;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(long articleCount) {
        this.articleCount = articleCount;
    }
}

