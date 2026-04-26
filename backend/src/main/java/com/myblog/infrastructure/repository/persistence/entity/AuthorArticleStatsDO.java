package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 作者文章统计 DO。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AuthorArticleStatsDO {

    private Long authorId;
    private Integer articleCount;
    private Long totalViews;
    private Long totalLikes;

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Integer getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }

    public Long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(Long totalViews) {
        this.totalViews = totalViews;
    }

    public Long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(Long totalLikes) {
        this.totalLikes = totalLikes;
    }
}
