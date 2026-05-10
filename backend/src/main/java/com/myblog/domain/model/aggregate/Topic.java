package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.TopicId;

import java.time.LocalDateTime;

/**
 * 专题聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class Topic {

    private TopicId id;
    private String title;
    private String summary;
    private String coverUrl;
    private String status;
    private Integer sortOrder;
    private Integer articleCount;
    private String intro;
    private String difficulty;
    private Integer estimatedMinutes;
    private String sourceType;
    private String sourceNote;
    private boolean recommended;
    private Integer recommendWeight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer version;

    private Topic() {
    }

    public static Topic create(Long id, String title, String summary,
                               String coverUrl, Integer sortOrder) {
        Topic topic = new Topic();
        topic.id = new TopicId(id);
        topic.title = title;
        topic.summary = summary;
        topic.coverUrl = coverUrl;
        topic.status = "PUBLISHED";
        topic.sortOrder = sortOrder == null ? 0 : sortOrder;
        topic.articleCount = 0;
        topic.intro = "";
        topic.difficulty = "INTERMEDIATE";
        topic.estimatedMinutes = 0;
        topic.sourceType = "ORIGINAL";
        topic.sourceNote = "";
        topic.recommended = true;
        topic.recommendWeight = 0;
        topic.createdAt = LocalDateTime.now();
        topic.updatedAt = topic.createdAt;
        topic.deletedAt = null;
        topic.version = 0;
        return topic;
    }

    public static Topic restore(Long id, String title, String summary,
                                String coverUrl, String status, Integer sortOrder,
                                Integer articleCount,
                                LocalDateTime createdAt, LocalDateTime updatedAt,
                                LocalDateTime deletedAt, Integer version) {
        return restore(id, title, summary, coverUrl, status, sortOrder, articleCount,
            "", "INTERMEDIATE", 0, "ORIGINAL", "", true, 0,
            createdAt, updatedAt, deletedAt, version);
    }

    public static Topic restore(Long id, String title, String summary,
                                String coverUrl, String status, Integer sortOrder,
                                Integer articleCount, String intro, String difficulty,
                                Integer estimatedMinutes, String sourceType, String sourceNote,
                                Boolean recommended, Integer recommendWeight,
                                LocalDateTime createdAt, LocalDateTime updatedAt,
                                LocalDateTime deletedAt, Integer version) {
        Topic topic = new Topic();
        topic.id = new TopicId(id);
        topic.title = title;
        topic.summary = summary;
        topic.coverUrl = coverUrl;
        topic.status = status;
        topic.sortOrder = sortOrder == null ? 0 : sortOrder;
        topic.articleCount = articleCount == null ? 0 : articleCount;
        topic.intro = intro == null ? "" : intro;
        topic.difficulty = difficulty == null ? "INTERMEDIATE" : difficulty;
        topic.estimatedMinutes = estimatedMinutes == null ? 0 : estimatedMinutes;
        topic.sourceType = sourceType == null ? "ORIGINAL" : sourceType;
        topic.sourceNote = sourceNote == null ? "" : sourceNote;
        topic.recommended = recommended == null || recommended;
        topic.recommendWeight = recommendWeight == null ? 0 : recommendWeight;
        topic.createdAt = createdAt;
        topic.updatedAt = updatedAt;
        topic.deletedAt = deletedAt;
        topic.version = version;
        return topic;
    }

    public void update(String title, String summary, String coverUrl,
                       Integer sortOrder, String status) {
        this.title = title;
        this.summary = summary;
        this.coverUrl = coverUrl;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    public boolean isPublished() {
        return "PUBLISHED".equals(status) && deletedAt == null;
    }

    public TopicId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getStatus() {
        return status;
    }

    public Integer getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public Integer getArticleCount() {
        return articleCount == null ? 0 : articleCount;
    }

    public String getIntro() {
        return intro == null ? "" : intro;
    }

    public String getDifficulty() {
        return difficulty == null ? "INTERMEDIATE" : difficulty;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes == null ? 0 : estimatedMinutes;
    }

    public String getSourceType() {
        return sourceType == null ? "ORIGINAL" : sourceType;
    }

    public String getSourceNote() {
        return sourceNote == null ? "" : sourceNote;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public Integer getRecommendWeight() {
        return recommendWeight == null ? 0 : recommendWeight;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
