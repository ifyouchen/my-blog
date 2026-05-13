package com.myblog.interfaces.rest.dto.request;

/**
 * 添加专栏文章请求.
 * <p>
 * 用于将已有文章添加到专栏中.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class AddColumnArticleRequest {

    /** 要添加的文章ID. */
    private Long articleId;

    /** 在专栏中的排序顺序，默认为0. */
    private Integer sortOrder;

    /**
     * 获取文章ID.
     *
     * @return 文章ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置文章ID.
     *
     * @param articleId 文章ID
     */
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    /**
     * 获取排序顺序.
     *
     * @return 排序顺序，默认为0
     */
    public Integer getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    /**
     * 设置排序顺序.
     *
     * @param sortOrder 排序顺序
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
