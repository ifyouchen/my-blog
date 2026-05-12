package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 作者文章统计 DO。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AuthorArticleStatsDO {

    /**
     * 作者用户 ID
     */
    private Long authorId;

    /**
     * 文章总数量
     */
    private Integer articleCount;

    /**
     * 累计阅读量
     */
    private Long totalViews;

    /**
     * 累计点赞量
     */
    private Long totalLikes;

    /**
     * 获取作者用户 ID。
     *
     * @return 作者用户 ID
     */
    public Long getAuthorId() {
        return authorId;
    }

    /**
     * 设置作者用户 ID。
     *
     * @param authorId 作者用户 ID
     */
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    /**
     * 获取文章总数量。
     *
     * @return 文章数量
     */
    public Integer getArticleCount() {
        return articleCount;
    }

    /**
     * 设置文章总数量。
     *
     * @param articleCount 文章数量
     */
    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }

    /**
     * 获取累计阅读量。
     *
     * @return 累计阅读量
     */
    public Long getTotalViews() {
        return totalViews;
    }

    /**
     * 设置累计阅读量。
     *
     * @param totalViews 累计阅读量
     */
    public void setTotalViews(Long totalViews) {
        this.totalViews = totalViews;
    }

    /**
     * 获取累计点赞量。
     *
     * @return 累计点赞量
     */
    public Long getTotalLikes() {
        return totalLikes;
    }

    /**
     * 设置累计点赞量。
     *
     * @param totalLikes 累计点赞量
     */
    public void setTotalLikes(Long totalLikes) {
        this.totalLikes = totalLikes;
    }
}
