package com.myblog.application.command;

import java.util.List;

/**
 * 创建文章命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CreateArticleCommand {

    private Long authorId;
    private String title;
    private String summary;
    private String content;
    private String coverUrl;
    private String category;
    private List<String> tags;
    private String status;

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
     * 获取文章标题。
     *
     * @return 文章标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置文章标题。
     *
     * @param title 文章标题
     */
    public void setTitle(String title) {
        this.title = title;
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
     * 设置文章摘要。
     *
     * @param summary 文章摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
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
     * 设置文章正文。
     *
     * @param content 文章正文
     */
    public void setContent(String content) {
        this.content = content;
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
     * 设置封面地址。
     *
     * @param coverUrl 封面地址
     */
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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
     * 设置文章分类。
     *
     * @param category 文章分类
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 获取文章标签。
     *
     * @return 文章标签
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * 设置文章标签。
     *
     * @param tags 文章标签
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * 获取文章状态。
     *
     * @return 文章状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置文章状态。
     *
     * @param status 文章状态
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
