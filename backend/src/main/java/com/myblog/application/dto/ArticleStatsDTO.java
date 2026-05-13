package com.myblog.application.dto;

import java.util.List;

/**
 * 单篇文章详细统计数据（含趋势数据）。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleStatsDTO {

    /** 文章 ID */
    private Long articleId;
    /** 文章标题 */
    private String title;
    /** 文章状态 */
    private String status;
    /** 阅读数 */
    private int viewCount;
    /** 点赞数 */
    private int likeCount;
    /** 收藏数 */
    private int favoriteCount;
    /** 评论数 */
    private int commentCount;
    /** 发布时间 */
    private String publishedAt;
    /** 更新时间 */
    private String updatedAt;
    /** 趋势数据列表（按日期升序）。 */
    private List<ArticleStatsTrendPointDTO> trends;

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public Long getArticleId() { return articleId; }

    /**
     * 设置文章 ID。
     *
     * @param articleId 文章 ID
     */
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    /**
     * 获取文章标题。
     *
     * @return 文章标题
     */
    public String getTitle() { return title; }

    /**
     * 设置文章标题。
     *
     * @param title 文章标题
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * 获取文章状态。
     *
     * @return 文章状态
     */
    public String getStatus() { return status; }

    /**
     * 设置文章状态。
     *
     * @param status 文章状态
     */
    public void setStatus(String status) { this.status = status; }

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

    /**
     * 获取发布时间。
     *
     * @return 发布时间
     */
    public String getPublishedAt() { return publishedAt; }

    /**
     * 设置发布时间。
     *
     * @param publishedAt 发布时间
     */
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public String getUpdatedAt() { return updatedAt; }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    /**
     * 获取趋势数据列表。
     *
     * @return 趋势数据列表
     */
    public List<ArticleStatsTrendPointDTO> getTrends() { return trends; }

    /**
     * 设置趋势数据列表。
     *
     * @param trends 趋势数据列表
     */
    public void setTrends(List<ArticleStatsTrendPointDTO> trends) { this.trends = trends; }
}
