package com.myblog.application.dto;

import java.time.LocalDateTime;

/**
 * 专题应用数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class TopicDTO {

    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private int articleCount;
    private String intro;
    private String difficulty;
    private Integer estimatedMinutes;
    private String sourceType;
    private String sourceNote;
    private boolean recommended;
    private Integer recommendWeight;
    private LearningProgressDTO progress;
    private java.util.List<LearningPathArticleDTO> outline;
    private ArticleDTO nextArticle;
    private String status;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public int getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceNote() {
        return sourceNote;
    }

    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public Integer getRecommendWeight() {
        return recommendWeight;
    }

    public void setRecommendWeight(Integer recommendWeight) {
        this.recommendWeight = recommendWeight;
    }

    public LearningProgressDTO getProgress() {
        return progress;
    }

    public void setProgress(LearningProgressDTO progress) {
        this.progress = progress;
    }

    public java.util.List<LearningPathArticleDTO> getOutline() {
        return outline;
    }

    public void setOutline(java.util.List<LearningPathArticleDTO> outline) {
        this.outline = outline;
    }

    public ArticleDTO getNextArticle() {
        return nextArticle;
    }

    public void setNextArticle(ArticleDTO nextArticle) {
        this.nextArticle = nextArticle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
