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
    private int wordCount;
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
    private String featuredAt;
    private int featureWeight;
    private String slug;
    private String seoTitle;
    private String seoDescription;
    private boolean needUnlock;
    private int unlockPointPrice;
    /** 内容是否被锁定（付费文章且用户无权查看全文时=true）. */
    private boolean contentLocked;
    /** 解锁原因/状态标识：AUTHOR_SELF / ADMIN_BYPASS / UNLOCKED / CONTENT_LOCKED / null. */
    private String unlockReason;
    private String scheduledPublishAt;
    private String offlineReason;
    private boolean warnFlag;
    private Long recommendationApplicationId;
    private String recommendationApplicationStatus;
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
     * 获取正文字数。
     *
     * @return 正文字数
     */
    public int getWordCount() {
        return wordCount;
    }

    /**
     * 设置正文字数。
     *
     * @param wordCount 正文字数
     */
    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
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

    /**
     * 获取当前用户是否已点赞。
     *
     * @return 是否已点赞
     */
    public boolean isLiked() {
        return liked;
    }

    /**
     * 设置当前用户是否已点赞。
     *
     * @param liked 是否已点赞
     */
    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    /**
     * 获取当前用户是否已收藏。
     *
     * @return 是否已收藏
     */
    public boolean isFavorited() {
        return favorited;
    }

    /**
     * 设置当前用户是否已收藏。
     *
     * @param favorited 是否已收藏
     */
    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    /**
     * 获取是否为精选文章。
     *
     * @return 是否精选
     */
    public boolean isFeatured() {
        return featured;
    }

    /**
     * 设置是否为精选文章。
     *
     * @param featured 是否精选
     */
    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    /**
     * 获取精选时间。
     *
     * @return 精选时间
     */
    public String getFeaturedAt() {
        return featuredAt;
    }

    /**
     * 设置精选时间。
     *
     * @param featuredAt 精选时间
     */
    public void setFeaturedAt(String featuredAt) {
        this.featuredAt = featuredAt;
    }

    /**
     * 获取精选权重。
     *
     * @return 精选权重
     */
    public int getFeatureWeight() {
        return featureWeight;
    }

    /**
     * 设置精选权重。
     *
     * @param featureWeight 精选权重
     */
    public void setFeatureWeight(int featureWeight) {
        this.featureWeight = featureWeight;
    }

    /**
     * 获取 URL 别名。
     *
     * @return URL 别名
     */
    public String getSlug() {
        return slug;
    }

    /**
     * 设置 URL 别名。
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
     * 获取是否需要积分解锁。
     *
     * @return 是否需要积分解锁
     */
    public boolean isNeedUnlock() {
        return needUnlock;
    }

    /**
     * 设置是否需要积分解锁。
     *
     * @param needUnlock 是否需要积分解锁
     */
    public void setNeedUnlock(boolean needUnlock) {
        this.needUnlock = needUnlock;
    }

    /**
     * 获取解锁所需积分。
     *
     * @return 解锁积分
     */
    public int getUnlockPointPrice() {
        return unlockPointPrice;
    }

    /**
     * 设置解锁所需积分。
     *
     * @param unlockPointPrice 解锁积分
     */
    public void setUnlockPointPrice(int unlockPointPrice) {
        this.unlockPointPrice = unlockPointPrice;
    }

    public boolean isContentLocked() { return contentLocked; }
    public void setContentLocked(boolean contentLocked) { this.contentLocked = contentLocked; }

    public String getUnlockReason() { return unlockReason; }
    public void setUnlockReason(String unlockReason) { this.unlockReason = unlockReason; }

    /**
     * 获取定时发布时间。
     *
     * @return 定时发布时间
     */
    public String getScheduledPublishAt() {
        return scheduledPublishAt;
    }

    /**
     * 设置定时发布时间。
     *
     * @param scheduledPublishAt 定时发布时间
     */
    public void setScheduledPublishAt(String scheduledPublishAt) {
        this.scheduledPublishAt = scheduledPublishAt;
    }

    /**
     * 获取下线原因。
     *
     * @return 下线原因
     */
    public String getOfflineReason() {
        return offlineReason;
    }

    /**
     * 设置下线原因。
     *
     * @param offlineReason 下线原因
     */
    public void setOfflineReason(String offlineReason) {
        this.offlineReason = offlineReason;
    }

    /**
     * 获取是否有警告标记。
     *
     * @return 是否有警告标记
     */
    public boolean isWarnFlag() {
        return warnFlag;
    }

    /**
     * 设置是否有警告标记。
     *
     * @param warnFlag 是否有警告标记
     */
    public void setWarnFlag(boolean warnFlag) {
        this.warnFlag = warnFlag;
    }

    /**
     * 获取推荐申请ID。
     *
     * @return 推荐申请ID
     */
    public Long getRecommendationApplicationId() {
        return recommendationApplicationId;
    }

    /**
     * 设置推荐申请ID。
     *
     * @param recommendationApplicationId 推荐申请ID
     */
    public void setRecommendationApplicationId(Long recommendationApplicationId) {
        this.recommendationApplicationId = recommendationApplicationId;
    }

    /**
     * 获取推荐申请状态。
     *
     * @return 推荐申请状态
     */
    public String getRecommendationApplicationStatus() {
        return recommendationApplicationStatus;
    }

    /**
     * 设置推荐申请状态。
     *
     * @param recommendationApplicationStatus 推荐申请状态
     */
    public void setRecommendationApplicationStatus(String recommendationApplicationStatus) {
        this.recommendationApplicationStatus = recommendationApplicationStatus;
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
