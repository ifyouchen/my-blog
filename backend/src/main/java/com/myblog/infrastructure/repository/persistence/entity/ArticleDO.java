package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 文章数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleDO {

    private Long id;
    private Long authorId;
    private String title;
    private String summary;
    private String content;
    private String coverUrl;
    private String category;
    private String offlineReason;
    private Boolean featured;
    private LocalDateTime featuredAt;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

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
     * 获取标题。
     *
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置标题。
     *
     * @param title 标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取摘要。
     *
     * @return 摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置摘要。
     *
     * @param summary 摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取正文。
     *
     * @return 正文
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置正文。
     *
     * @param content 正文
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
     * 获取分类。
     *
     * @return 分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置分类。
     *
     * @param category 分类
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 获取下架原因。
     *
     * @return 下架原因
     */
    public String getOfflineReason() {
        return offlineReason;
    }

    /**
     * 设置下架原因。
     *
     * @param offlineReason 下架原因
     */
    public void setOfflineReason(String offlineReason) {
        this.offlineReason = offlineReason;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public LocalDateTime getFeaturedAt() {
        return featuredAt;
    }

    public void setFeaturedAt(LocalDateTime featuredAt) {
        this.featuredAt = featuredAt;
    }

    /**
     * 获取状态。
     *
     * @return 状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态。
     *
     * @param status 状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取阅读数。
     *
     * @return 阅读数
     */
    public Integer getViewCount() {
        return viewCount;
    }

    /**
     * 设置阅读数。
     *
     * @param viewCount 阅读数
     */
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    /**
     * 获取点赞数。
     *
     * @return 点赞数
     */
    public Integer getLikeCount() {
        return likeCount;
    }

    /**
     * 设置点赞数。
     *
     * @param likeCount 点赞数
     */
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    /**
     * 获取收藏数。
     *
     * @return 收藏数
     */
    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    /**
     * 设置收藏数。
     *
     * @param favoriteCount 收藏数
     */
    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    /**
     * 获取评论数。
     *
     * @return 评论数
     */
    public Integer getCommentCount() {
        return commentCount;
    }

    /**
     * 设置评论数。
     *
     * @param commentCount 评论数
     */
    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    /**
     * 获取发布时间。
     *
     * @return 发布时间
     */
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    /**
     * 设置发布时间。
     *
     * @param publishedAt 发布时间
     */
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取删除时间。
     *
     * @return 删除时间
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置删除时间。
     *
     * @param deletedAt 删除时间
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置版本号。
     *
     * @param version 版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
