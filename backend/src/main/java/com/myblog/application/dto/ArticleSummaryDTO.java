package com.myblog.application.dto;

/**
 * 轻量文章摘要数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleSummaryDTO {

    private Long id;
    private String title;
    private String slug;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
