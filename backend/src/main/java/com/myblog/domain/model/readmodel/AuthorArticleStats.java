package com.myblog.domain.model.readmodel;

/**
 * 作者文章统计排行行。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AuthorArticleStats {

    private Long authorId;
    private Integer articleCount;
    private Long totalViews;
    private Long totalLikes;

    /**
     * 获取作者 ID。
     *
     * @return 作者 ID
     */
    public Long getAuthorId() {
        return authorId;
    }

    /**
     * 设置作者 ID。
     *
     * @param authorId 作者 ID
     */
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    /**
     * 获取文章数。
     *
     * @return 文章数
     */
    public Integer getArticleCount() {
        return articleCount;
    }

    /**
     * 设置文章数。
     *
     * @param articleCount 文章数
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
