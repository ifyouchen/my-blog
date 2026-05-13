package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ColumnId;
import com.myblog.domain.model.valueobject.UserId;

import java.time.LocalDateTime;

/**
 * 专栏聚合根。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class Column {

    /**
     * 专栏 ID
     */
    private ColumnId id;

    /**
     * 专栏作者用户 ID
     */
    private UserId authorId;

    /**
     * 专栏标题
     */
    private String title;

    /**
     * 专栏简介摘要
     */
    private String summary;

    /**
     * 专栏封面图片地址
     */
    private String coverUrl;

    /**
     * 专栏状态（PUBLISHED / DRAFT 等）
     */
    private String status;

    /**
     * 排序权重，值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 订阅人数
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
     * 专栏创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 专栏最后更新时间
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

    private Column() {
    }

    /**
     * 创建专栏聚合根。
     *
     * @param id        专栏 ID
     * @param authorId  作者用户 ID
     * @param title     专栏标题
     * @param summary   专栏摘要
     * @param coverUrl  封面图片地址
     * @param sortOrder 排序权重
     * @return 专栏聚合根
     */
    public static Column create(Long id, UserId authorId, String title, String summary,
                                String coverUrl, Integer sortOrder) {
        Column column = new Column();
        column.id = new ColumnId(id);
        column.authorId = authorId;
        column.title = title;
        column.summary = summary;
        column.coverUrl = coverUrl;
        column.status = "PUBLISHED";
        column.sortOrder = sortOrder == null ? 0 : sortOrder;
        column.subscriberCount = 0;
        column.articleCount = 0;
        column.intro = "";
        column.difficulty = "INTERMEDIATE";
        column.estimatedMinutes = 0;
        column.sourceType = "ORIGINAL";
        column.sourceNote = "";
        column.recommended = false;
        column.recommendWeight = 0;
        column.createdAt = LocalDateTime.now();
        column.updatedAt = column.createdAt;
        column.deletedAt = null;
        column.version = 0;
        return column;
    }

    /**
     * 从持久化数据还原专栏聚合根（简化版，缺省扩展字段）。
     *
     * @param id              专栏 ID
     * @param authorId        作者用户 ID
     * @param title           专栏标题
     * @param summary         专栏摘要
     * @param coverUrl        封面图片地址
     * @param status          专栏状态
     * @param sortOrder       排序权重
     * @param subscriberCount 订阅人数
     * @param articleCount    文章数量
     * @param createdAt       创建时间
     * @param updatedAt       更新时间
     * @param deletedAt       删除时间
     * @param version         乐观锁版本号
     * @return 专栏聚合根
     */
    public static Column restore(Long id, UserId authorId, String title, String summary,
                                 String coverUrl, String status, Integer sortOrder,
                                 Integer subscriberCount, Integer articleCount,
                                 LocalDateTime createdAt, LocalDateTime updatedAt,
                                 LocalDateTime deletedAt, Integer version) {
        return restore(id, authorId, title, summary, coverUrl, status, sortOrder,
            subscriberCount, articleCount, "", "INTERMEDIATE", 0, "ORIGINAL", "",
            false, 0, createdAt, updatedAt, deletedAt, version);
    }

    /**
     * 从持久化数据还原专栏聚合根（完整版）。
     *
     * @param id               专栏 ID
     * @param authorId         作者用户 ID
     * @param title            专栏标题
     * @param summary          专栏摘要
     * @param coverUrl         封面图片地址
     * @param status           专栏状态
     * @param sortOrder        排序权重
     * @param subscriberCount  订阅人数
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
     * @return 专栏聚合根
     */
    public static Column restore(Long id, UserId authorId, String title, String summary,
                                 String coverUrl, String status, Integer sortOrder,
                                 Integer subscriberCount, Integer articleCount,
                                 String intro, String difficulty, Integer estimatedMinutes,
                                 String sourceType, String sourceNote, Boolean recommended,
                                 Integer recommendWeight, LocalDateTime createdAt,
                                 LocalDateTime updatedAt, LocalDateTime deletedAt, Integer version) {
        Column column = new Column();
        column.id = new ColumnId(id);
        column.authorId = authorId;
        column.title = title;
        column.summary = summary;
        column.coverUrl = coverUrl;
        column.status = status;
        column.sortOrder = sortOrder == null ? 0 : sortOrder;
        column.subscriberCount = subscriberCount == null ? 0 : subscriberCount;
        column.articleCount = articleCount == null ? 0 : articleCount;
        column.intro = intro == null ? "" : intro;
        column.difficulty = difficulty == null ? "INTERMEDIATE" : difficulty;
        column.estimatedMinutes = estimatedMinutes == null ? 0 : estimatedMinutes;
        column.sourceType = sourceType == null ? "ORIGINAL" : sourceType;
        column.sourceNote = sourceNote == null ? "" : sourceNote;
        column.recommended = recommended != null && recommended;
        column.recommendWeight = recommendWeight == null ? 0 : recommendWeight;
        column.createdAt = createdAt;
        column.updatedAt = updatedAt;
        column.deletedAt = deletedAt;
        column.version = version;
        return column;
    }

    /**
     * 更新专栏基本信息。
     *
     * @param title     新标题
     * @param summary   新摘要
     * @param coverUrl  新封面图片地址
     * @param sortOrder 新排序权重
     * @param status    新状态
     */
    public void update(String title, String summary, String coverUrl, Integer sortOrder, String status) {
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

    /**
     * 软删除专栏。
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = this.deletedAt;
    }

    /**
     * 增加订阅人数。
     */
    public void increaseSubscriberCount() {
        this.subscriberCount = getSubscriberCount() + 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 减少订阅人数，最低为 0。
     */
    public void decreaseSubscriberCount() {
        this.subscriberCount = Math.max(0, getSubscriberCount() - 1);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断专栏是否已发布且未删除。
     *
     * @return 已发布且未删除返回 true，否则返回 false
     */
    public boolean isPublished() {
        return "PUBLISHED".equals(status) && deletedAt == null;
    }

    /**
     * 获取专栏 ID。
     *
     * @return 专栏 ID
     */
    public ColumnId getId() {
        return id;
    }

    /**
     * 获取专栏作者用户 ID。
     *
     * @return 作者用户 ID
     */
    public UserId getAuthorId() {
        return authorId;
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
     * 获取专栏简介摘要。
     *
     * @return 专栏摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 获取专栏封面图片地址。
     *
     * @return 封面图片地址
     */
    public String getCoverUrl() {
        return coverUrl;
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
     * 获取排序权重。
     *
     * @return 排序权重
     */
    public Integer getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    /**
     * 获取订阅人数。
     *
     * @return 订阅人数
     */
    public Integer getSubscriberCount() {
        return subscriberCount == null ? 0 : subscriberCount;
    }

    /**
     * 获取专栏包含的文章数量。
     *
     * @return 文章数量
     */
    public Integer getArticleCount() {
        return articleCount == null ? 0 : articleCount;
    }

    /**
     * 获取专栏详细介绍。
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
     * 判断专栏是否推荐展示。
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
     * 获取专栏创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取专栏最后更新时间。
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
