package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDate;

/**
 * 创作者趋势聚合行。
 *
 * @author Codex
 * @since 1.0.0
 */
public class DashboardTrendPointDO {

    private LocalDate statDate;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }
}
