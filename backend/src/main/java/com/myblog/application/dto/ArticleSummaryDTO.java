package com.myblog.application.dto;

/**
 * 轻量文章摘要数据。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleSummaryDTO {

    /** 文章 ID */
    private Long id;
    /** 文章标题 */
    private String title;
    /** URL 别名 */
    private String slug;

    /**
     * 获取文章 ID。
     *
     * @return 文章 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置文章 ID。
     *
     * @param id 文章 ID
     */
    public void setId(Long id) {
        this.id = id;
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
     * 获取 URL 别名。
     *
     * @return URL 别名
     */
    public String getSlug() {
        return slug;
    }

    /**
     * 设置 URL 别名。
     *
     * @param slug URL 别名
     */
    public void setSlug(String slug) {
        this.slug = slug;
    }
}
