package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 文章标签行数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleTagRowDO {

    private Long articleId;
    private String tagName;

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
