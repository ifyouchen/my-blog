package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 文章数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleDO {

    /**
     * 文章 ID
     */
    private Long id;

    /**
     * 作者用户 ID
     */
    private Long authorId;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章正文内容
     */
    private String content;

    /**
     * 封面图片 URL
     */
    private String coverUrl;

    /**
     * 文章分类
     */
    private String category;

    /**
     * 下架原因
     */
    private String offlineReason;

    /**
     * 敏感词警告标记
     */
    private Boolean warnFlag;

    /**
     * 是否精选
     */
    private Boolean featured;

    /**
     * 精选时间
     */
    private LocalDateTime featuredAt;

    /**
     * 精选权重
     */
    private Integer featureWeight;

    /**
     * URL 别名（slug）
     */
    private String slug;

    /**
     * SEO 标题
     */
    private String seoTitle;

    /**
     * SEO 描述
     */
    private String seoDescription;

    /**
     * 文章状态
     */
    private String status;

    /**
     * 定时发布时间
     */
    private LocalDateTime scheduledPublishAt;

    /**
     * 阅读数
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 收藏数
     */
    private Integer favoriteCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间，为 null 表示未删除
     */
    private LocalDateTime deletedAt;

    /**
     * 乐观锁版本号
     */
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


    /**
     * 获取敏感词警告标记。
     *
     * @return 敏感词警告标记
     */
    public Boolean getWarnFlag() {
        return warnFlag;
    }

    /**
     * 设置敏感词警告标记。
     *
     * @param warnFlag 敏感词警告标记
     */
    public void setWarnFlag(Boolean warnFlag) {
        this.warnFlag = warnFlag;
    }

    /**
     * 获取是否精选。
     *
     * @return 是否精选
     */
    public Boolean getFeatured() {
        return featured;
    }

    /**
     * 设置是否精选。
     *
     * @param featured 是否精选
     */
    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    /**
     * 获取精选时间。
     *
     * @return 精选时间
     */
    public LocalDateTime getFeaturedAt() {
        return featuredAt;
    }

    /**
     * 设置精选时间。
     *
     * @param featuredAt 精选时间
     */
    public void setFeaturedAt(LocalDateTime featuredAt) {
        this.featuredAt = featuredAt;
    }

    /**
     * 获取精选权重。
     *
     * @return 精选权重
     */
    public Integer getFeatureWeight() {
        return featureWeight;
    }

    /**
     * 设置精选权重。
     *
     * @param featureWeight 精选权重
     */
    public void setFeatureWeight(Integer featureWeight) {
        this.featureWeight = featureWeight;
    }

    /**
     * 获取 URL 别名（slug）。
     *
     * @return URL 别名
     */
    public String getSlug() {
        return slug;
    }

    /**
     * 设置 URL 别名（slug）。
     *
     * @param slug URL 别名
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * 获取 SEO 标题。
     *
     * @return SEO 标题
     */
    public String getSeoTitle() {
        return seoTitle;
    }

    /**
     * 设置 SEO 标题。
     *
     * @param seoTitle SEO 标题
     */
    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    /**
     * 获取 SEO 描述。
     *
     * @return SEO 描述
     */
    public String getSeoDescription() {
        return seoDescription;
    }

    /**
     * 设置 SEO 描述。
     *
     * @param seoDescription SEO 描述
     */
    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
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
     * 获取定时发布时间。
     *
     * @return 定时发布时间
     */
    public LocalDateTime getScheduledPublishAt() {
        return scheduledPublishAt;
    }

    /**
     * 设置定时发布时间。
     *
     * @param scheduledPublishAt 定时发布时间
     */
    public void setScheduledPublishAt(LocalDateTime scheduledPublishAt) {
        this.scheduledPublishAt = scheduledPublishAt;
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
