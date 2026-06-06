package com.myblog.domain.model.readmodel;

import java.time.LocalDate;

/**
 * 创作者趋势数据点。
 *
 * @author Codex
 * @since 1.0.0
 */
public class DashboardTrendPoint {

    private LocalDate statDate;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;

    /**
     * 获取统计日期。
     *
     * @return 统计日期
     */
    public LocalDate getStatDate() {
        return statDate;
    }

    /**
     * 设置统计日期。
     *
     * @param statDate 统计日期
     */
    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    /**
     * 获取阅读量。
     *
     * @return 阅读量
     */
    public Integer getViewCount() {
        return viewCount;
    }

    /**
     * 设置阅读量。
     *
     * @param viewCount 阅读量
     */
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    /**
     * 获取点赞量。
     *
     * @return 点赞量
     */
    public Integer getLikeCount() {
        return likeCount;
    }

    /**
     * 设置点赞量。
     *
     * @param likeCount 点赞量
     */
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    /**
     * 获取收藏量。
     *
     * @return 收藏量
     */
    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    /**
     * 设置收藏量。
     *
     * @param favoriteCount 收藏量
     */
    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    /**
     * 获取评论量。
     *
     * @return 评论量
     */
    public Integer getCommentCount() {
        return commentCount;
    }

    /**
     * 设置评论量。
     *
     * @param commentCount 评论量
     */
    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }
}
