package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 文章标签行数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleTagRowDO {

    /**
     * 文章 ID
     */
    private Long articleId;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置文章 ID。
     *
     * @param articleId 文章 ID
     */
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    /**
     * 获取标签名称。
     *
     * @return 标签名称
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * 设置标签名称。
     *
     * @param tagName 标签名称
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
