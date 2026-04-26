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
    private List<String> tags;
    private ArticleStatus status;
    private int viewCount;
    private int likeCount;
    private int favoriteCount;
    private int commentCount;
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
                                 String coverUrl, String category, List<String> tags, ArticleStatus status) {
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
        article.createdAt = LocalDateTime.now();
        article.updatedAt = article.createdAt;
        article.version = 0;
        if (requiresCompleteContent(article.status)) {
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
                                  String coverUrl, String category, List<String> tags, ArticleStatus status,
                                  int viewCount, int likeCount, int favoriteCount, int commentCount,
                                  LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
                                  Integer version) {
        Article article = new Article();
        article.id = new ArticleId(id);
        article.authorId = authorId;
        article.title = title;
        article.summary = summary;
        article.content = content;
        article.coverUrl = coverUrl;
        article.category = category;
        article.tags = tags == null ? new ArrayList<String>() : new ArrayList<String>(tags);
        article.status = status;
        article.viewCount = viewCount;
        article.likeCount = likeCount;
        article.favoriteCount = favoriteCount;
        article.commentCount = commentCount;
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
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 下架文章。
     */
    public void offline() {
        if (ArticleStatus.DELETED.equals(status)) {
            throw new DomainException(ErrorCode.CONFLICT, "已删除文章不能下架");
        }
        this.status = ArticleStatus.OFFLINE;
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
                              String category, List<String> tags) {
        this.title = normalizeText(title);
        this.summary = summary == null ? "" : summary.trim();
        this.content = normalizeText(content);
        this.coverUrl = coverUrl;
        this.category = normalizeText(category);
        this.tags = tags == null ? new ArrayList<String>() : new ArrayList<String>(tags);
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
        return ArticleStatus.PUBLISHED.equals(status) || ArticleStatus.OFFLINE.equals(status);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 删除文章。
     */
    public void delete() {
        this.status = ArticleStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新文章状态。
     */
    public void updateStatus(ArticleStatus status) {
        this.status = status;
        if (ArticleStatus.PUBLISHED.equals(status) && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
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
