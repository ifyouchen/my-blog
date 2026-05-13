package com.myblog.application.dto;

import java.time.LocalDateTime;

/**
 * 专栏应用数据。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ColumnDTO {

    /** 专栏 ID */
    private Long id;
    /** 专栏标题 */
    private String title;
    /** 专栏摘要 */
    private String summary;
    /** 封面图片地址 */
    private String coverUrl;
    /** 订阅人数 */
    private int subscriberCount;
    /** 文章数量 */
    private int articleCount;
    /** 专栏简介 */
    private String intro;
    /** 难度等级 */
    private String difficulty;
    /** 预计学习时长（分钟） */
    private Integer estimatedMinutes;
    /** 来源类型 */
    private String sourceType;
    /** 来源备注 */
    private String sourceNote;
    /** 是否推荐 */
    private boolean recommended;
    /** 推荐权重 */
    private Integer recommendWeight;
    /** 学习进度 */
    private LearningProgressDTO progress;
    /** 学习路径大纲 */
    private java.util.List<LearningPathArticleDTO> outline;
    /** 下一篇待学习文章 */
    private ArticleDTO nextArticle;
    /** 当前用户是否已订阅 */
    private boolean subscribed;
    /** 专栏作者 */
    private UserDTO author;
    /** 专栏状态（PUBLISHED / DRAFT），管理后台使用 */
    private String status;
    /** 排序值，管理后台使用 */
    private Integer sortOrder;
    /** 作者 ID，管理后台使用 */
    private Long authorId;
    /** 创建时间，管理后台使用 */
    private LocalDateTime createdAt;

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
     * 获取封面图片地址。
     *
     * @return 封面图片地址
     */
    public String getCoverUrl() {
        return coverUrl;
    }

    /**
     * 设置封面图片地址。
     *
     * @param coverUrl 封面图片地址
     */
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    /**
     * 获取订阅人数。
     *
     * @return 订阅人数
     */
    public int getSubscriberCount() {
        return subscriberCount;
    }

    /**
     * 设置订阅人数。
     *
     * @param subscriberCount 订阅人数
     */
    public void setSubscriberCount(int subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    /**
     * 获取文章数量。
     *
     * @return 文章数量
     */
    public int getArticleCount() {
        return articleCount;
    }

    /**
     * 设置文章数量。
     *
     * @param articleCount 文章数量
     */
    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
    }

    /**
     * 获取专栏简介。
     *
     * @return 专栏简介
     */
    public String getIntro() {
        return intro;
    }

    /**
     * 设置专栏简介。
     *
     * @param intro 专栏简介
     */
    public void setIntro(String intro) {
        this.intro = intro;
    }

    /**
     * 获取难度等级。
     *
     * @return 难度等级
     */
    public String getDifficulty() {
        return difficulty;
    }

    /**
     * 设置难度等级。
     *
     * @param difficulty 难度等级
     */
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * 获取预计学习时长（分钟）。
     *
     * @return 预计学习时长
     */
    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    /**
     * 设置预计学习时长（分钟）。
     *
     * @param estimatedMinutes 预计学习时长
     */
    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    /**
     * 获取来源类型。
     *
     * @return 来源类型
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * 设置来源类型。
     *
     * @param sourceType 来源类型
     */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 获取来源备注。
     *
     * @return 来源备注
     */
    public String getSourceNote() {
        return sourceNote;
    }

    /**
     * 设置来源备注。
     *
     * @param sourceNote 来源备注
     */
    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }

    /**
     * 获取是否推荐。
     *
     * @return 是否推荐
     */
    public boolean isRecommended() {
        return recommended;
    }

    /**
     * 设置是否推荐。
     *
     * @param recommended 是否推荐
     */
    public void setRecommended(boolean recommended) {
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
     * 获取学习进度。
     *
     * @return 学习进度
     */
    public LearningProgressDTO getProgress() {
        return progress;
    }

    /**
     * 设置学习进度。
     *
     * @param progress 学习进度
     */
    public void setProgress(LearningProgressDTO progress) {
        this.progress = progress;
    }

    /**
     * 获取学习路径大纲。
     *
     * @return 学习路径大纲
     */
    public java.util.List<LearningPathArticleDTO> getOutline() {
        return outline;
    }

    /**
     * 设置学习路径大纲。
     *
     * @param outline 学习路径大纲
     */
    public void setOutline(java.util.List<LearningPathArticleDTO> outline) {
        this.outline = outline;
    }

    /**
     * 获取下一篇待学习文章。
     *
     * @return 下一篇待学习文章
     */
    public ArticleDTO getNextArticle() {
        return nextArticle;
    }

    /**
     * 设置下一篇待学习文章。
     *
     * @param nextArticle 下一篇待学习文章
     */
    public void setNextArticle(ArticleDTO nextArticle) {
        this.nextArticle = nextArticle;
    }

    /**
     * 获取当前用户是否已订阅。
     *
     * @return 是否已订阅
     */
    public boolean isSubscribed() {
        return subscribed;
    }

    /**
     * 设置当前用户是否已订阅。
     *
     * @param subscribed 是否已订阅
     */
    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    /**
     * 获取专栏作者。
     *
     * @return 专栏作者
     */
    public UserDTO getAuthor() {
        return author;
    }

    /**
     * 设置专栏作者。
     *
     * @param author 专栏作者
     */
    public void setAuthor(UserDTO author) {
        this.author = author;
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
     * 获取排序值。
     *
     * @return 排序值
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 设置排序值。
     *
     * @param sortOrder 排序值
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
}
