package com.myblog.application.dto;

/**
 * 创作者工作台趋势点。
 *
 * @author Codex
 * @since 1.0.0
 */
public class DashboardTrendPointDTO {

    private String date;
    private int viewCount;
    private int likeCount;
    private int favoriteCount;
    private int commentCount;
    private int interactionCount;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(int favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public int getInteractionCount() {
        return interactionCount;
    }

    public void setInteractionCount(int interactionCount) {
        this.interactionCount = interactionCount;
    }
}
