package com.myblog.application.dto;

/**
 * 公告数据传输对象。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class AnnouncementDTO {

    /** 公告 ID */
    private Long id;
    /** 公告标题 */
    private String title;
    /** 公告内容 */
    private String content;
    /** 目标受众 */
    private String target;
    /** 是否已发布 */
    private Boolean published;
    /** 发布时间 */
    private String publishedAt;
    /** 过期时间 */
    private String expiresAt;
    /** 创建时间 */
    private String createdAt;
    /** 更新时间 */
    private String updatedAt;

    /**
     * 获取公告 ID。
     *
     * @return 公告 ID
     */
    public Long getId() { return id; }

    /**
     * 设置公告 ID。
     *
     * @param id 公告 ID
     */
    public void setId(Long id) { this.id = id; }

    /**
     * 获取公告标题。
     *
     * @return 公告标题
     */
    public String getTitle() { return title; }

    /**
     * 设置公告标题。
     *
     * @param title 公告标题
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * 获取公告内容。
     *
     * @return 公告内容
     */
    public String getContent() { return content; }

    /**
     * 设置公告内容。
     *
     * @param content 公告内容
     */
    public void setContent(String content) { this.content = content; }

    /**
     * 获取目标受众。
     *
     * @return 目标受众
     */
    public String getTarget() { return target; }

    /**
     * 设置目标受众。
     *
     * @param target 目标受众
     */
    public void setTarget(String target) { this.target = target; }

    /**
     * 获取是否已发布。
     *
     * @return 是否已发布
     */
    public Boolean getPublished() { return published; }

    /**
     * 设置是否已发布。
     *
     * @param published 是否已发布
     */
    public void setPublished(Boolean published) { this.published = published; }

    /**
     * 获取发布时间。
     *
     * @return 发布时间
     */
    public String getPublishedAt() { return publishedAt; }

    /**
     * 设置发布时间。
     *
     * @param publishedAt 发布时间
     */
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    /**
     * 获取过期时间。
     *
     * @return 过期时间
     */
    public String getExpiresAt() { return expiresAt; }

    /**
     * 设置过期时间。
     *
     * @param expiresAt 过期时间
     */
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public String getCreatedAt() { return createdAt; }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public String getUpdatedAt() { return updatedAt; }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}

