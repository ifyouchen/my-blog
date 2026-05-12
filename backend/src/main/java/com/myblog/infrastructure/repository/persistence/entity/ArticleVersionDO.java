package com.myblog.infrastructure.repository.persistence.entity;

import java.time.LocalDateTime;

/**
 * 文章版本历史数据对象。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleVersionDO {

    /**
     * 版本记录 ID
     */
    private Long id;

    /**
     * 所属文章 ID
     */
    private Long articleId;

    /**
     * 版本号，从 1 开始递增
     */
    private Integer versionNo;

    /**
     * 该版本的文章标题
     */
    private String title;

    /**
     * 该版本的文章正文内容
     */
    private String content;

    /**
     * 该版本的文章摘要
     */
    private String summary;

    /**
     * 保存该版本的用户 ID
     */
    private Long savedBy;

    /**
     * 版本创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 获取版本记录 ID。
     *
     * @return 版本记录 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置版本记录 ID。
     *
     * @param id 版本记录 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取所属文章 ID。
     *
     * @return 文章 ID
     */
    public Long getArticleId() {
        return articleId;
    }

    /**
     * 设置所属文章 ID。
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
     * 获取该版本的文章标题。
     *
     * @return 文章标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置该版本的文章标题。
     *
     * @param title 文章标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取该版本的文章正文内容。
     *
     * @return 正文内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置该版本的文章正文内容。
     *
     * @param content 正文内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取该版本的文章摘要。
     *
     * @return 文章摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置该版本的文章摘要。
     *
     * @param summary 文章摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取保存该版本的用户 ID。
     *
     * @return 用户 ID
     */
    public Long getSavedBy() {
        return savedBy;
    }

    /**
     * 设置保存该版本的用户 ID。
     *
     * @param savedBy 用户 ID
     */
    public void setSavedBy(Long savedBy) {
        this.savedBy = savedBy;
    }

    /**
     * 获取版本创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置版本创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

