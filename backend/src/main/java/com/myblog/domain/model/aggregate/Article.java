package com.myblog.domain.model.aggregate;

import com.myblog.domain.model.valueobject.ArticleId;
import com.myblog.domain.model.valueobject.UserId;
import com.myblog.shared.enums.ArticleStatus;
import com.myblog.shared.exception.DomainException;
import com.myblog.shared.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文章聚合根。
 *
 * @author Codex
 * @since 1.0.0
 */
public class Article {

    private ArticleId id;
    private UserId authorId;
    private String title;
    private String summary;
    private String content;
    private String coverUrl;
    private String category;
    private String offlineReason;
    private List<String> tags;
    private ArticleStatus status;
    private int viewCount;
    private int likeCount;
    private int favoriteCount;
    private int commentCount;
    private boolean warnFlag;
    private boolean featured;
    private LocalDateTime featuredAt;
    /** 精选权重（0-1000000），值越大在精选列表和推荐中排越靠前 */
    private int featureWeight;
    private String slug;
    private String seoTitle;
    private String seoDescription;
    private LocalDateTime scheduledPublishAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;

    private Article() {
    }

    /**
     * 创建文章聚合根。
     *
     * @param id 文章 ID
     * @param authorId 作者 ID
     * @param title 标题
     * @param summary 摘要
     * @param content 正文
     * @param coverUrl 封面地址
     * @param category 分类
     * @param tags 标签列表
     * @param status 文章状态
     * @return 文章聚合根
     */
    public static Article create(Long id, UserId authorId, String title, String summary, String content,
                                 String coverUrl, String category, List<String> tags, ArticleStatus status,
                                 String slug, String seoTitle, String seoDescription,
                                 LocalDateTime scheduledPublishAt) {
        Article article = new Article();
        article.id = new ArticleId(id);
        article.authorId = authorId;
        article.title = normalizeText(title);
        article.summary = summary == null ? "" : summary.trim();
        article.content = normalizeText(content);
        article.coverUrl = coverUrl;
        article.category = normalizeText(category);
        article.tags = tags == null ? new ArrayList<String>() : new ArrayList<String>(tags);
        article.status = status == null ? ArticleStatus.DRAFT : status;
        article.viewCount = 0;
        article.likeCount = 0;
        article.favoriteCount = 0;
        article.commentCount = 0;
        article.featured = false;
        article.featuredAt = null;
        article.featureWeight = 0;
        article.slug = normalizeNullableText(slug);
        article.seoTitle = normalizeNullableText(seoTitle);
        article.seoDescription = normalizeNullableText(seoDescription);
        article.scheduledPublishAt = null;
        article.createdAt = LocalDateTime.now();
        article.updatedAt = article.createdAt;
        article.version = 0;
        if (ArticleStatus.SCHEDULED.equals(article.status)) {
            article.schedulePublish(scheduledPublishAt);
        } else if (requiresCompleteContent(article.status)) {
            article.validatePublishable();
        }
        if (ArticleStatus.PUBLISHED.equals(article.status)) {
            article.publishedAt = article.createdAt;
        }
        return article;
    }

    /**
     * 从持久化数据还原文章聚合根。
     *
     * @param id 文章 ID
     * @param authorId 作者 ID
     * @param title 标题
     * @param summary 摘要
     * @param content 正文
     * @param coverUrl 封面地址
     * @param category 分类
     * @param tags 标签列表
     * @param status 文章状态
     * @param viewCount 阅读数
     * @param likeCount 点赞数
     * @param favoriteCount 收藏数
     * @param commentCount 评论数
     * @param publishedAt 发布时间
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     * @return 文章聚合根
     */
    public static Article restore(Long id, UserId authorId, String title, String summary, String content,
                                  String coverUrl, String category, String offlineReason, List<String> tags, ArticleStatus status,
                                  int viewCount, int likeCount, int favoriteCount, int commentCount,
                                  boolean warnFlag, boolean featured, LocalDateTime featuredAt, int featureWeight,
                                  String slug, String seoTitle, String seoDescription,
                                  LocalDateTime scheduledPublishAt, LocalDateTime publishedAt,
                                  LocalDateTime createdAt, LocalDateTime updatedAt,
                                  Integer version) {
        Article article = new Article();
        article.id = new ArticleId(id);
        article.authorId = authorId;
        article.title = title;
        article.summary = summary;
        article.content = content;
        article.coverUrl = coverUrl;
        article.category = category;
        article.offlineReason = offlineReason;
        article.tags = tags == null ? new ArrayList<String>() : new ArrayList<String>(tags);
        article.status = status;
        article.viewCount = viewCount;
        article.likeCount = likeCount;
        article.favoriteCount = favoriteCount;
        article.commentCount = commentCount;
        article.warnFlag = warnFlag;
        article.featured = featured;
        article.featuredAt = featuredAt;
        article.featureWeight = Math.max(0, Math.min(1000000, featureWeight));
        article.slug = slug;
        article.seoTitle = seoTitle;
        article.seoDescription = seoDescription;
        article.scheduledPublishAt = scheduledPublishAt;
        article.publishedAt = publishedAt;
        article.createdAt = createdAt;
        article.updatedAt = updatedAt;
        article.version = version == null ? 0 : version;
        return article;
    }

    /**
     * 增加文章阅读数。
     */
    public void increaseViewCount() {
        this.viewCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加评论数。
     */
    public void increaseCommentCount() {
        this.commentCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 减少评论数。
     */
    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加收藏数。
     */
    public void increaseFavoriteCount() {
        this.favoriteCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 减少收藏数。
     */
    public void decreaseFavoriteCount() {
        if (this.favoriteCount > 0) {
            this.favoriteCount--;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加点赞数。
     */
    public void increaseLikeCount() {
        this.likeCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 减少点赞数。
     */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 发布文章。
     */
    public void publish() {
        if (ArticleStatus.DELETED.equals(status)) {
            throw new DomainException(ErrorCode.CONFLICT, "已删除文章不能发布");
        }
        validatePublishable();
        this.status = ArticleStatus.PUBLISHED;
        this.offlineReason = null;
        this.scheduledPublishAt = null;
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 保存为草稿。
     */
    public void saveDraft() {
        if (ArticleStatus.DELETED.equals(status)) {
            throw new DomainException(ErrorCode.CONFLICT, "已删除文章不能保存为草稿");
        }
        this.status = ArticleStatus.DRAFT;
        this.offlineReason = null;
        this.scheduledPublishAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设为定时发布。
     *
     * @param publishAt 计划发布时间
     */
    public void schedulePublish(LocalDateTime publishAt) {
        if (ArticleStatus.DELETED.equals(status)) {
            throw new DomainException(ErrorCode.CONFLICT, "已删除文章不能定时发布");
        }
        if (publishAt == null) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "请选择定时发布时间");
        }
        if (!publishAt.isAfter(LocalDateTime.now())) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "定时发布时间必须晚于当前时间");
        }
        validatePublishable();
        this.status = ArticleStatus.SCHEDULED;
        this.offlineReason = null;
        this.scheduledPublishAt = publishAt;
        this.publishedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 下架文章。
     */
    public void offline() {
        offline(null);
    }

    /**
     * 下架文章。
     *
     * @param reason 下架原因
     */
    public void offline(String reason) {
        if (ArticleStatus.DELETED.equals(status)) {
            throw new DomainException(ErrorCode.CONFLICT, "已删除文章不能下架");
        }
        this.status = ArticleStatus.OFFLINE;
        this.offlineReason = normalizeNullableText(reason);
        this.scheduledPublishAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新文章内容。
     *
     * @param title 标题
     * @param summary 摘要
     * @param content 正文
     * @param coverUrl 封面地址
     * @param category 分类
     * @param tags 标签列表
     */
    public void updateContent(String title, String summary, String content, String coverUrl,
                              String category, List<String> tags,
                              String slug, String seoTitle, String seoDescription) {
        this.title = normalizeText(title);
        this.summary = summary == null ? "" : summary.trim();
        this.content = normalizeText(content);
        this.coverUrl = coverUrl;
        this.category = normalizeText(category);
        this.tags = tags == null ? new ArrayList<String>() : new ArrayList<String>(tags);
        this.slug = normalizeNullableText(slug);
        this.seoTitle = normalizeNullableText(seoTitle);
        this.seoDescription = normalizeNullableText(seoDescription);
        this.updatedAt = LocalDateTime.now();
    }

    private void validatePublishable() {
        if (title == null || title.trim().isEmpty()) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "文章标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "文章正文不能为空");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "文章分类不能为空");
        }
    }

    private static boolean requiresCompleteContent(ArticleStatus status) {
        return ArticleStatus.PUBLISHED.equals(status)
            || ArticleStatus.SCHEDULED.equals(status)
            || ArticleStatus.OFFLINE.equals(status);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 设为精选（带权重）。
     *
     * @param weight 精选权重 0-1000000，值越大排序越靠前
     */
    public void feature(int weight) {
        this.featured = true;
        if (this.featuredAt == null) {
            this.featuredAt = LocalDateTime.now();
        }
        this.featureWeight = Math.max(0, Math.min(1000000, weight));
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设为精选（默认权重 500）。
     */
    public void feature() {
        feature(500);
    }

    /**
     * 取消精选。
     */
    public void unfeature() {
        this.featured = false;
        this.featuredAt = null;
        this.featureWeight = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 删除文章。
     */
    public void delete() {
        this.status = ArticleStatus.DELETED;
        this.scheduledPublishAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新文章状态。
     */
    public void updateStatus(ArticleStatus status) {
        if (ArticleStatus.SCHEDULED.equals(status)) {
            throw new DomainException(ErrorCode.PARAM_ERROR, "定时发布需要指定发布时间");
        }
        this.status = status;
        if (ArticleStatus.PUBLISHED.equals(status) && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
        if (!ArticleStatus.OFFLINE.equals(status)) {
            this.offlineReason = null;
        }
        this.scheduledPublishAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public ArticleId getId() {
        return id;
    }

    /**
     * 获取作者 ID。
     *
     * @return 作者 ID
     */
    public UserId getAuthorId() {
        return authorId;
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
     * 获取文章摘要。
     *
     * @return 文章摘要
     */
    public String getSummary() {
        return summary;
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
     * 获取封面地址。
     *
     * @return 封面地址
     */
    public String getCoverUrl() {
        return coverUrl;
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
     * 获取下架原因。
     *
     * @return 下架原因
     */
    public String getOfflineReason() {
        return offlineReason;
    }

    /**
     * 获取文章标签。
     *
     * @return 不可变文章标签列表
     */
    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    /**
     * 获取文章状态。
     *
     * @return 文章状态
     */
    public ArticleStatus getStatus() {
        return status;
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
     * 获取点赞数。
     *
     * @return 点赞数
     */
    public int getLikeCount() {
        return likeCount;
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
     * 获取评论数。
     *
     * @return 评论数
     */
    public int getCommentCount() {
        return commentCount;
    }

    /**
     * 是否精选。
     *
     * @return 是否精选
     */

    /**
     * 获取敏感词警告标记。
     *
     * @return 敏感词警告标记
     */
    public boolean isWarnFlag() {
        return warnFlag;
    }

    /**
     * 标记为敏感词警告。
     */
    public void markWarnFlag() {
        this.warnFlag = true;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    /**
     * 更新敏感词警告标记。
     *
     * @param warnFlag 是否命中过 WARN 级别敏感词
     */
    public void updateWarnFlag(boolean warnFlag) {
        this.warnFlag = warnFlag;
        this.updatedAt = java.time.LocalDateTime.now();
    }

    /**
     * 判断文章是否已设为精选。
     *
     * @return 是否已精选
     */
    public boolean isFeatured() {
        return featured;
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
     * 获取精选权重。
     *
     * @return 精选权重 0-1000000
     */
    public int getFeatureWeight() {
        return featureWeight;
    }

    /**
     * 获取 URL Slug。
     *
     * @return URL Slug
     */
    public String getSlug() {
        return slug;
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
     * 获取 SEO 描述。
     *
     * @return SEO 描述
     */
    public String getSeoDescription() {
        return seoDescription;
    }

    /**
     * 获取计划发布时间。
     *
     * @return 计划发布时间
     */
    public LocalDateTime getScheduledPublishAt() {
        return scheduledPublishAt;
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
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
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
     * 获取版本号。
     *
     * @return 版本号
     */
    public Integer getVersion() {
        return version;
    }
}
