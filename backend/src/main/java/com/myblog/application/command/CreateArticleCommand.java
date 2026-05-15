package com.myblog.application.command;

import java.util.List;

/**
 * 创建文章命令。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CreateArticleCommand {

    /**
     * 作者用户 ID
     */
    private Long authorId;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章正文（Markdown 格式）
     */
    private String content;

    /**
     * 封面图片地址
     */
    private String coverUrl;

    /**
     * 文章分类名称
     */
    private String category;

    /**
     * 文章标签列表
     */
    private List<String> tags;

    /**
     * 文章状态（DRAFT / PUBLISHED / SCHEDULED 等）
     */
    private String status;

    /**
     * URL 友好的别名（slug），用于 SEO 路由
     */
    private String slug;

    /**
     * SEO 标题，为空时默认使用文章标题
     */
    private String seoTitle;

    /**
     * SEO 描述，用于搜索引擎摘要展示
     */
    private String seoDescription;

    /**
     * 是否需要积分解锁
     */
    private boolean needUnlock;

    /**
     * 解锁所需积分
     */
    private int unlockPointPrice;

    /**
     * 定时发布时间（ISO-8601 字符串），仅 status=SCHEDULED 时有效
     */
    private String scheduledPublishAt;

    /**
     * 获取作者用户 ID。
     *
     * @return 作者用户 ID
     */
    public Long getAuthorId() {
        return authorId;
    }

    /**
     * 设置作者用户 ID。
     *
     * @param authorId 作者用户 ID
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
     * 获取文章分类名称。
     *
     * @return 文章分类名称
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置文章分类名称。
     *
     * @param category 文章分类名称
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 获取文章标签列表。
     *
     * @return 文章标签列表
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * 设置文章标签列表。
     *
     * @param tags 文章标签列表
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

    /**
     * 获取 URL 别名（slug）。
     *
     * @return URL 别名
     */
    public String getSlug() {
        return slug;
    }

    /**
     * 设置 URL 别名（slug）。
     *
     * @param slug URL 别名
     */
    public void setSlug(String slug) {
        this.slug = slug;
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
     * 设置 SEO 标题。
     *
     * @param seoTitle SEO 标题
     */
    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
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
     * 设置 SEO 描述。
     *
     * @param seoDescription SEO 描述
     */
    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    /**
     * 获取是否需要积分解锁。
     *
     * @return 是否需要积分解锁
     */
    public boolean isNeedUnlock() {
        return needUnlock;
    }

    /**
     * 设置是否需要积分解锁。
     *
     * @param needUnlock 是否需要积分解锁
     */
    public void setNeedUnlock(boolean needUnlock) {
        this.needUnlock = needUnlock;
    }

    /**
     * 获取解锁所需积分。
     *
     * @return 解锁积分
     */
    public int getUnlockPointPrice() {
        return unlockPointPrice;
    }

    /**
     * 设置解锁所需积分。
     *
     * @param unlockPointPrice 解锁积分
     */
    public void setUnlockPointPrice(int unlockPointPrice) {
        this.unlockPointPrice = unlockPointPrice;
    }

    /**
     * 获取定时发布时间字符串。
     *
     * @return 定时发布时间（ISO-8601 格式）
     */
    public String getScheduledPublishAt() {
        return scheduledPublishAt;
    }

    /**
     * 设置定时发布时间字符串。
     *
     * @param scheduledPublishAt 定时发布时间（ISO-8601 格式）
     */
    public void setScheduledPublishAt(String scheduledPublishAt) {
        this.scheduledPublishAt = scheduledPublishAt;
    }
}
