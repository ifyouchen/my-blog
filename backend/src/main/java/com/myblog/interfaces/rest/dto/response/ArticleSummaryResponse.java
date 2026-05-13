package com.myblog.interfaces.rest.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 轻量文章摘要响应.
 * <p>
 * 仅包含文章基本信息的精简响应，用于排行榜、列表嵌入等场景.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleSummaryResponse {

    /** 文章ID. */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 文章标题. */
    private String title;

    /** 文章Slug（URL友好标识符）. */
    private String slug;

    /**
     * 获取文章ID.
     *
     * @return 文章ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置文章ID.
     *
     * @param id 文章ID
     */
    public void setId(Long id) {
        this.id = id;
    }

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
}
