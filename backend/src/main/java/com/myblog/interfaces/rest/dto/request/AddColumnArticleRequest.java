package com.myblog.interfaces.rest.dto.request;

/**
 * 添加专栏文章请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AddColumnArticleRequest {

    private Long articleId;
    private Integer sortOrder;

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Integer getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
