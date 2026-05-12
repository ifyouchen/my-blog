package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 专栏数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ColumnDO {

    /**
     * 专栏 ID
     */
    private Long id;

    /**
     * 作者用户 ID
     */
    private Long authorId;

    /**
     * 专栏标题
     */
    private String title;

    /**
     * 专栏摘要
     */
    private String summary;

    /**
     * 封面图片 URL
     */
    private String coverUrl;

    /**
     * 专栏状态（如：DRAFT、PUBLISHED）
     */
    private String status;

    /**
     * 排序权重，值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 订阅者数量
     */
    private Integer subscriberCount;

    /**
     * 专栏包含的文章数量
     */
    private Integer articleCount;

    /**
     * 专栏详细介绍
     */
    private String intro;

    /**
     * 难度级别（如：BEGINNER、INTERMEDIATE、ADVANCED）
     */
    private String difficulty;

    /**
     * 预计完成所需时间（分钟）
     */
    private Integer estimatedMinutes;

    /**
     * 内容来源类型（如：ORIGINAL、IMPORTED）
     */
    private String sourceType;

    /**
     * 内容来源备注
     */
    private String sourceNote;

    /**
     * 是否推荐
     */
    private Boolean recommended;

    /**
     * 推荐权重，值越大优先级越高
     */
    private Integer recommendWeight;

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
     * 获取专栏 ID。
     *
     * @return 专栏 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置专栏 ID。
     *
     * @param id 专栏 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取作者用户 ID。
     *
     * @return 作者用户 ID
     */
    public Long getAuthorId() {
        return authorId;
    }

    /**
     * 设置作者用户 ID。
     *
     * @param authorId 作者用户 ID
     */
    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    /**
     * 获取专栏标题。
     *
     * @return 专栏标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置专栏标题。
     *
     * @param title 专栏标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取专栏摘要。
     *
     * @return 专栏摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置专栏摘要。
     *
     * @param summary 专栏摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取封面图片 URL。
     *
     * @return 封面图片 URL
     */
    public String getCoverUrl() {
        return coverUrl;
    }

    /**
     * 设置封面图片 URL。
     *
     * @param coverUrl 封面图片 URL
     */
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    /**
     * 获取专栏状态。
     *
     * @return 专栏状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置专栏状态。
     *
     * @param status 专栏状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取排序权重。
     *
     * @return 排序权重
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 设置排序权重。
     *
     * @param sortOrder 排序权重
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 获取订阅者数量。
     *
     * @return 订阅者数量
     */
    public Integer getSubscriberCount() {
        return subscriberCount;
    }

    /**
     * 设置订阅者数量。
     *
     * @param subscriberCount 订阅者数量
     */
    public void setSubscriberCount(Integer subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    /**
     * 获取专栏包含的文章数量。
     *
     * @return 文章数量
     */
    public Integer getArticleCount() {
        return articleCount;
    }

    /**
     * 设置专栏包含的文章数量。
     *
     * @param articleCount 文章数量
     */
    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
    }

    /**
     * 获取专栏详细介绍。
     *
     * @return 专栏介绍
     */
    public String getIntro() {
        return intro;
    }

    /**
     * 设置专栏详细介绍。
     *
     * @param intro 专栏介绍
     */
    public void setIntro(String intro) {
        this.intro = intro;
    }

    /**
     * 获取难度级别。
     *
     * @return 难度级别
     */
    public String getDifficulty() {
        return difficulty;
    }

    /**
     * 设置难度级别。
     *
     * @param difficulty 难度级别
     */
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * 获取预计完成所需时间（分钟）。
     *
     * @return 预计分钟数
     */
    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    /**
     * 设置预计完成所需时间（分钟）。
     *
     * @param estimatedMinutes 预计分钟数
     */
    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    /**
     * 获取内容来源类型。
     *
     * @return 来源类型
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * 设置内容来源类型。
     *
     * @param sourceType 来源类型
     */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 获取内容来源备注。
     *
     * @return 来源备注
     */
    public String getSourceNote() {
        return sourceNote;
    }

    /**
     * 设置内容来源备注。
     *
     * @param sourceNote 来源备注
     */
    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }

    /**
     * 获取是否推荐。
     *
     * @return true 表示推荐，false 表示不推荐
     */
    public Boolean getRecommended() {
        return recommended;
    }

    /**
     * 设置是否推荐。
     *
     * @param recommended 是否推荐
     */
    public void setRecommended(Boolean recommended) {
        this.recommended = recommended;
    }

    /**
     * 获取推荐权重。
     *
     * @return 推荐权重
     */
    public Integer getRecommendWeight() {
        return recommendWeight;
    }

    /**
     * 设置推荐权重。
     *
     * @param recommendWeight 推荐权重
     */
    public void setRecommendWeight(Integer recommendWeight) {
        this.recommendWeight = recommendWeight;
    }

    /**
     * 获取记录创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置记录创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取记录最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置记录最后更新时间。
     *
     * @param updatedAt 最后更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 获取软删除时间。
     *
     * @return 删除时间，未删除则为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置软删除时间。
     *
     * @param deletedAt 删除时间
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * 设置乐观锁版本号。
     *
     * @param version 版本号
     */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
