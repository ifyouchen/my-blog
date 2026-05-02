package com.myblog.application.dto;

import java.util.List;

/**
 * 文章应用数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleDTO {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverUrl;
    private String category;
    private List<String> tags;
    private String status;
    private int viewCount;
    private int likeCount;
    private int favoriteCount;
    private int commentCount;
    private String publishedAt;
    private String updatedAt;
    private String favoritedAt;
    private boolean liked;
    private boolean favorited;
    private boolean featured;
    private String slug;
    private String seoTitle;
    private String seoDescription;
    private String scheduledPublishAt;
    private String offlineReason;
    private boolean warnFlag;
    private UserDTO author;

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置文章 ID。
     *
     * @param id 文章 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取文章标题。
     *
     * @return 文章标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置文章标题。
     *
     * @param title 文章标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取文章摘要。
     *
     * @return 文章摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置文章摘要。
     *
     * @param summary 文章摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取文章正文。
     *
     * @return 文章正文
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置文章正文。
     *
     * @param content 文章正文
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取封面地址。
     *
     * @return 封面地址
     */
    public String getCoverUrl() {
        return coverUrl;
    }

    /**
     * 设置封面地址。
     *
     * @param coverUrl 封面地址
     */
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    /**
     * 获取文章分类。
     *
     * @return 文章分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置文章分类。
     *
     * @param category 文章分类
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 获取文章标签。
     *
     * @return 文章标签
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * 设置文章标签。
     *
     * @param tags 文章标签
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * 获取文章状态。
     *
     * @return 文章状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置文章状态。
     *
     * @param status 文章状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取阅读数。
     *
     * @return 阅读数
     */
    public int getViewCount() {
        return viewCount;
    }

    /**
     * 设置阅读数。
     *
     * @param viewCount 阅读数
     */
    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    /**
     * 获取点赞数。
     *
     * @return 点赞数
     */
    public int getLikeCount() {
        return likeCount;
    }

    /**
     * 设置点赞数。
     *
     * @param likeCount 点赞数
     */
    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    /**
     * 获取收藏数。
     *
     * @return 收藏数
     */
    public int getFavoriteCount() {
        return favoriteCount;
    }

    /**
     * 设置收藏数。
     *
     * @param favoriteCount 收藏数
     */
    public void setFavoriteCount(int favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    /**
     * 获取评论数。
     *
     * @return 评论数
     */
    public int getCommentCount() {
        return commentCount;
    }

    /**
     * 设置评论数。
     *
     * @param commentCount 评论数
     */
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    /**
     * 获取发布时间。
     *
     * @return 发布时间
     */
    public String getPublishedAt() {
        return publishedAt;
    }

    /**
     * 设置发布时间。
     *
     * @param publishedAt 发布时间
     */
    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取收藏时间。
     *
     * @return 收藏时间
     */
    public String getFavoritedAt() {
        return favoritedAt;
    }

    /**
     * 设置收藏时间。
     *
     * @param favoritedAt 收藏时间
     */
    public void setFavoritedAt(String favoritedAt) {
        this.favoritedAt = favoritedAt;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    public String getScheduledPublishAt() {
        return scheduledPublishAt;
    }

    public void setScheduledPublishAt(String scheduledPublishAt) {
        this.scheduledPublishAt = scheduledPublishAt;
    }

    public String getOfflineReason() {
        return offlineReason;
    }

    public void setOfflineReason(String offlineReason) {
        this.offlineReason = offlineReason;
    }

    public boolean isWarnFlag() {
        return warnFlag;
    }

    public void setWarnFlag(boolean warnFlag) {
        this.warnFlag = warnFlag;
    }

    /**
     * 获取作者信息。
     *
     * @return 作者信息
     */
    public UserDTO getAuthor() {
        return author;
    }

    /**
     * 设置作者信息。
     *
     * @param author 作者信息
     */
    public void setAuthor(UserDTO author) {
        this.author = author;
    }
}
