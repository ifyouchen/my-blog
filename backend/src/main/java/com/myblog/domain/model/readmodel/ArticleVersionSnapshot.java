package com.myblog.domain.model.readmodel;

import java.time.LocalDateTime;

/**
 * 文章版本快照。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleVersionSnapshot {

    private Long id;
    private Long articleId;
    private Integer versionNo;
    private String title;
    private String content;
    private String summary;
    private Long savedBy;
    private LocalDateTime createdAt;

    /**
     * 获取版本 ID。
     *
     * @return 版本 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置版本 ID。
     *
     * @param id 版本 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置文章 ID。
     *
     * @param articleId 文章 ID
     */
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    /**
     * 获取版本号。
     *
     * @return 版本号
     */
    public Integer getVersionNo() {
        return versionNo;
    }

    /**
     * 设置版本号。
     *
     * @param versionNo 版本号
     */
    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
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
     * 获取保存用户 ID。
     *
     * @return 保存用户 ID
     */
    public Long getSavedBy() {
        return savedBy;
    }

    /**
     * 设置保存用户 ID。
     *
     * @param savedBy 保存用户 ID
     */
    public void setSavedBy(Long savedBy) {
        this.savedBy = savedBy;
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
