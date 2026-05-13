package com.myblog.application.dto;

/**
 * 单篇文章趋势统计数据点。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleStatsTrendPointDTO {

    /** 日期 */
    private String date;
    /** 阅读数 */
    private int viewCount;
    /** 点赞数 */
    private int likeCount;
    /** 收藏数 */
    private int favoriteCount;
    /** 评论数 */
    private int commentCount;

    /**
     * 获取日期。
     *
     * @return 日期
     */
    public String getDate() { return date; }

    /**
     * 设置日期。
     *
     * @param date 日期
     */
    public void setDate(String date) { this.date = date; }

    /**
     * 获取阅读数。
     *
     * @return 阅读数
     */
    public int getViewCount() { return viewCount; }

    /**
     * 设置阅读数。
     *
     * @param viewCount 阅读数
     */
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    /**
     * 获取点赞数。
     *
     * @return 点赞数
     */
    public int getLikeCount() { return likeCount; }

    /**
     * 设置点赞数。
     *
     * @param likeCount 点赞数
     */
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    /**
     * 获取收藏数。
     *
     * @return 收藏数
     */
    public int getFavoriteCount() { return favoriteCount; }

    /**
     * 设置收藏数。
     *
     * @param favoriteCount 收藏数
     */
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

    /**
     * 获取评论数。
     *
     * @return 评论数
     */
    public int getCommentCount() { return commentCount; }

    /**
     * 设置评论数。
     *
     * @param commentCount 评论数
     */
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
}
