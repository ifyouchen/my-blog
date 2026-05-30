package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.TopicId;

import java.time.LocalDateTime;

/**
 * 专题聚合根。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class Topic {

    /**
     * 专题 ID
     */
    private TopicId id;

    /**
     * 专题标题
     */
    private String title;

    /**
     * 专题简介摘要
     */
    private String summary;

    /**
     * 专题封面图片地址
     */
    private String coverUrl;

    /**
     * 专题状态（PUBLISHED / DRAFT 等）
     */
    private String status;

    /**
     * 排序权重，值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 专题包含的文章数量
     */
    private Integer articleCount;

    /**
     * 专题详细介绍
     */
    private String intro;

    /**
     * 难度等级（BEGINNER / INTERMEDIATE / ADVANCED 等）
     */
    private String difficulty;

    /**
     * 预计阅读完成所需分钟数
     */
    private Integer estimatedMinutes;

    /**
     * 内容来源类型（ORIGINAL 原创 / TRANSLATED 翻译等）
     */
    private String sourceType;

    /**
     * 内容来源备注
     */
    private String sourceNote;

    /**
     * 是否推荐展示
     */
    private boolean recommended;

    /**
     * 推荐权重，值越大越靠前
     */
    private Integer recommendWeight;

    /**
     * 专题创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 专题最后更新时间
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

    private Topic() {
    }

    /**
     * 创建专题聚合根。
     *
     * @param id        专题 ID
     * @param title     专题标题
     * @param summary   专题摘要
     * @param coverUrl  封面图片地址
     * @param sortOrder 排序权重
     * @return 专题聚合根
     */
    public static Topic create(Long id, String title, String summary,
                               String coverUrl, Integer sortOrder) {
        return create(id, title, summary, coverUrl, sortOrder, null);
    }

    public static Topic create(Long id, String title, String summary,
                               String coverUrl, Integer sortOrder, String difficulty) {
        Topic topic = new Topic();
        topic.id = new TopicId(id);
        topic.title = title;
        topic.summary = summary;
        topic.coverUrl = coverUrl;
        topic.status = "PUBLISHED";
        topic.sortOrder = sortOrder == null ? 0 : sortOrder;
        topic.articleCount = 0;
        topic.intro = "";
        topic.difficulty = difficulty == null ? "INTERMEDIATE" : difficulty;
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

    /**
     * 从持久化数据还原专题聚合根（简化版，缺省扩展字段）。
     *
     * @param id           专题 ID
     * @param title        专题标题
     * @param summary      专题摘要
     * @param coverUrl     封面图片地址
     * @param status       专题状态
     * @param sortOrder    排序权重
     * @param articleCount 文章数量
     * @param createdAt    创建时间
     * @param updatedAt    更新时间
     * @param deletedAt    删除时间
     * @param version      乐观锁版本号
     * @return 专题聚合根
     */
    public static Topic restore(Long id, String title, String summary,
                                String coverUrl, String status, Integer sortOrder,
                                Integer articleCount,
                                LocalDateTime createdAt, LocalDateTime updatedAt,
                                LocalDateTime deletedAt, Integer version) {
        return restore(id, title, summary, coverUrl, status, sortOrder, articleCount,
            "", "INTERMEDIATE", 0, "ORIGINAL", "", true, 0,
            createdAt, updatedAt, deletedAt, version);
    }

    /**
     * 从持久化数据还原专题聚合根（完整版）。
     *
     * @param id               专题 ID
     * @param title            专题标题
     * @param summary          专题摘要
     * @param coverUrl         封面图片地址
     * @param status           专题状态
     * @param sortOrder        排序权重
     * @param articleCount     文章数量
     * @param intro            详细介绍
     * @param difficulty       难度等级
     * @param estimatedMinutes 预计阅读分钟数
     * @param sourceType       内容来源类型
     * @param sourceNote       内容来源备注
     * @param recommended      是否推荐
     * @param recommendWeight  推荐权重
     * @param createdAt        创建时间
     * @param updatedAt        更新时间
     * @param deletedAt        删除时间
     * @param version          乐观锁版本号
     * @return 专题聚合根
     */
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

    /**
     * 更新专题基本信息。
     *
     * @param title     新标题
     * @param summary   新摘要
     * @param coverUrl  新封面图片地址
     * @param sortOrder 新排序权重
     * @param status    新状态
     */
    public void update(String title, String summary, String coverUrl,
                       Integer sortOrder, String status) {
        update(title, summary, coverUrl, sortOrder, status, null);
    }

    public void update(String title, String summary, String coverUrl,
                       Integer sortOrder, String status, String difficulty) {
        this.title = title;
        this.summary = summary;
        this.coverUrl = coverUrl;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        if (status != null) {
            this.status = status;
        }
        if (difficulty != null) {
            this.difficulty = difficulty;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 软删除专题。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    /**
     * 判断专题是否已发布且未删除。
     *
     * @return 已发布且未删除返回 true，否则返回 false
     */
    public boolean isPublished() {
        return "PUBLISHED".equals(status) && deletedAt == null;
    }

    /**
     * 获取专题 ID。
     *
     * @return 专题 ID
     */
    public TopicId getId() {
        return id;
    }

    /**
     * 获取专题标题。
     *
     * @return 专题标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取专题简介摘要。
     *
     * @return 专题摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 获取专题封面图片地址。
     *
     * @return 封面图片地址
     */
    public String getCoverUrl() {
        return coverUrl;
    }

    /**
     * 获取专题状态。
     *
     * @return 专题状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 获取排序权重。
     *
     * @return 排序权重
     */
    public Integer getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    /**
     * 获取专题包含的文章数量。
     *
     * @return 文章数量
     */
    public Integer getArticleCount() {
        return articleCount == null ? 0 : articleCount;
    }

    /**
     * 获取专题详细介绍。
     *
     * @return 详细介绍
     */
    public String getIntro() {
        return intro == null ? "" : intro;
    }

    /**
     * 获取难度等级。
     *
     * @return 难度等级
     */
    public String getDifficulty() {
        return difficulty == null ? "INTERMEDIATE" : difficulty;
    }

    /**
     * 获取预计阅读完成所需分钟数。
     *
     * @return 预计分钟数
     */
    public Integer getEstimatedMinutes() {
        return estimatedMinutes == null ? 0 : estimatedMinutes;
    }

    /**
     * 获取内容来源类型。
     *
     * @return 内容来源类型
     */
    public String getSourceType() {
        return sourceType == null ? "ORIGINAL" : sourceType;
    }

    /**
     * 获取内容来源备注。
     *
     * @return 内容来源备注
     */
    public String getSourceNote() {
        return sourceNote == null ? "" : sourceNote;
    }

    /**
     * 判断专题是否推荐展示。
     *
     * @return 是否推荐
     */
    public boolean isRecommended() {
        return recommended;
    }

    /**
     * 获取推荐权重。
     *
     * @return 推荐权重
     */
    public Integer getRecommendWeight() {
        return recommendWeight == null ? 0 : recommendWeight;
    }

    /**
     * 获取专题创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取专题最后更新时间。
     *
     * @return 最后更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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
     * 获取乐观锁版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}
