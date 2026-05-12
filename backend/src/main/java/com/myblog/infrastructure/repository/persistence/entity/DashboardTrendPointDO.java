package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDate;

/**
 * 创作者趋势聚合行。
 *
 * @author Codex
 * @since 1.0.0
 */
public class DashboardTrendPointDO {

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 当日阅读量
     */
    private Integer viewCount;

    /**
     * 当日点赞数
     */
    private Integer likeCount;

    /**
     * 当日收藏数
     */
    private Integer favoriteCount;

    /**
     * 当日评论数
     */
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
     * 获取当日阅读量。
     *
     * @return 阅读量
     */
    public Integer getViewCount() {
        return viewCount;
    }

    /**
     * 设置当日阅读量。
     *
     * @param viewCount 阅读量
     */
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    /**
     * 获取当日点赞数。
     *
     * @return 点赞数
     */
    public Integer getLikeCount() {
        return likeCount;
    }

    /**
     * 设置当日点赞数。
     *
     * @param likeCount 点赞数
     */
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    /**
     * 获取当日收藏数。
     *
     * @return 收藏数
     */
    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    /**
     * 设置当日收藏数。
     *
     * @param favoriteCount 收藏数
     */
    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    /**
     * 获取当日评论数。
     *
     * @return 评论数
     */
    public Integer getCommentCount() {
        return commentCount;
    }

    /**
     * 设置当日评论数。
     *
     * @param commentCount 评论数
     */
    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }
}
