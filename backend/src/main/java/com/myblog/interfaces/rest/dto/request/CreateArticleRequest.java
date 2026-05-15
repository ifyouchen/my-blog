package com.myblog.interfaces.rest.dto.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 创建文章请求.
 * <p>
 * 作者创建新文章时的请求参数，支持草稿保存和定时发布.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class CreateArticleRequest {

    /** 文章标题. */
    @Size(max = 120, message = "标题长度不能超过{max}个字符")
    private String title;

    /** 文章摘要. */
    @Size(max = 300, message = "摘要长度不能超过{max}个字符")
    private String summary;

    /** 文章正文内容（Markdown格式）. */
    private String content;

    /** 封面图片URL. */
    @Size(max = 500, message = "封面地址长度不能超过{max}个字符")
    private String coverUrl;

    /** 文章分类名称. */
    @Size(max = 50, message = "分类长度不能超过{max}个字符")
    private String category;

    /** 文章标签列表. */
    private List<String> tags;

    /** 文章初始状态（如 DRAFT、PUBLISHED）. */
    private String status;

    /** URL友好的文章标识符（slug）. */
    @Size(max = 255, message = "Slug长度不能超过{max}个字符")
    private String slug;

    /** SEO标题. */
    @Size(max = 255, message = "SEO标题长度不能超过{max}个字符")
    private String seoTitle;

    /** SEO描述. */
    @Size(max = 500, message = "SEO描述长度不能超过{max}个字符")
    private String seoDescription;

    /** 是否需要积分解锁. */
    private Boolean needUnlock;

    /** 解锁所需积分. */
    @Min(value = 0, message = "解锁积分不能小于{value}")
    @Max(value = 1000000, message = "解锁积分不能超过{value}")
    private Integer unlockPointPrice;

    /** 定时发布时间（ISO格式），为空则立即发布. */
    private String scheduledPublishAt;

    /**
     * 获取文章标题.
     *
     * @return 文章标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置文章标题.
     *
     * @param title 文章标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取文章摘要.
     *
     * @return 文章摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置文章摘要.
     *
     * @param summary 文章摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取文章正文.
     *
     * @return 文章正文
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置文章正文.
     *
     * @param content 文章正文
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取封面URL.
     *
     * @return 封面URL
     */
    public String getCoverUrl() {
        return coverUrl;
    }

    /**
     * 设置封面URL.
     *
     * @param coverUrl 封面URL
     */
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    /**
     * 获取文章分类.
     *
     * @return 分类名称
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置文章分类.
     *
     * @param category 分类名称
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 获取文章标签列表.
     *
     * @return 标签列表
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * 设置文章标签列表.
     *
     * @param tags 标签列表
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * 获取文章状态.
     *
     * @return 文章状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置文章状态.
     *
     * @param status 文章状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取文章Slug.
     *
     * @return Slug标识符
     */
    public String getSlug() {
        return slug;
    }

    /**
     * 设置文章Slug.
     *
     * @param slug Slug标识符
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /**
     * 获取SEO标题.
     *
     * @return SEO标题
     */
    public String getSeoTitle() {
        return seoTitle;
    }

    /**
     * 设置SEO标题.
     *
     * @param seoTitle SEO标题
     */
    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    /**
     * 获取SEO描述.
     *
     * @return SEO描述
     */
    public String getSeoDescription() {
        return seoDescription;
    }

    /**
     * 设置SEO描述.
     *
     * @param seoDescription SEO描述
     */
    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    /**
     * 获取是否需要积分解锁.
     *
     * @return 是否需要积分解锁
     */
    public Boolean getNeedUnlock() {
        return needUnlock;
    }

    /**
     * 设置是否需要积分解锁.
     *
     * @param needUnlock 是否需要积分解锁
     */
    public void setNeedUnlock(Boolean needUnlock) {
        this.needUnlock = needUnlock;
    }

    /**
     * 获取解锁所需积分.
     *
     * @return 解锁积分
     */
    public Integer getUnlockPointPrice() {
        return unlockPointPrice;
    }

    /**
     * 设置解锁所需积分.
     *
     * @param unlockPointPrice 解锁积分
     */
    public void setUnlockPointPrice(Integer unlockPointPrice) {
        this.unlockPointPrice = unlockPointPrice;
    }

    /**
     * 获取定时发布时间.
     *
     * @return 定时发布时间（ISO格式）
     */
    public String getScheduledPublishAt() {
        return scheduledPublishAt;
    }

    /**
     * 设置定时发布时间.
     *
     * @param scheduledPublishAt 定时发布时间（ISO格式）
     */
    public void setScheduledPublishAt(String scheduledPublishAt) {
        this.scheduledPublishAt = scheduledPublishAt;
    }
}
