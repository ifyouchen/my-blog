package com.myblog.interfaces.rest.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建文章请求。
 *
 * @author Codex
 * @since 1.0.0
 */
public class CreateArticleRequest {

    @Size(max = 120, message = "标题长度不能超过{max}个字符")
    private String title;

    @Size(max = 300, message = "摘要长度不能超过{max}个字符")
    private String summary;

    private String content;

    @Size(max = 500, message = "封面地址长度不能超过{max}个字符")
    private String coverUrl;

    @Size(max = 50, message = "分类长度不能超过{max}个字符")
    private String category;

    private List<String> tags;

    private String status;

    @Size(max = 255, message = "Slug长度不能超过{max}个字符")
    private String slug;

    @Size(max = 255, message = "SEO标题长度不能超过{max}个字符")
    private String seoTitle;

    @Size(max = 500, message = "SEO描述长度不能超过{max}个字符")
    private String seoDescription;

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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }
}
