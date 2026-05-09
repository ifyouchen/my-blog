package com.myblog.application.query;

import java.util.Objects;

/**
 * 首页推荐文章缓存键。
 *
 * @author Codex
 * @since 1.0.0
 */
public class RecommendArticleCacheKey {

    private final String category;
    private final int page;
    private final int pageSize;

    private RecommendArticleCacheKey(String category, int page, int pageSize) {
        this.category = normalizeCategory(category);
        this.page = Math.max(page, 1);
        this.pageSize = Math.max(pageSize, 1);
    }

    public static RecommendArticleCacheKey of(String category, int page, int pageSize) {
        return new RecommendArticleCacheKey(category, page, pageSize);
    }

    public String getCategory() {
        return category;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    private static String normalizeCategory(String category) {
        if (category == null) {
            return "";
        }
        return category.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecommendArticleCacheKey)) {
            return false;
        }
        RecommendArticleCacheKey that = (RecommendArticleCacheKey) o;
        return page == that.page
            && pageSize == that.pageSize
            && Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, page, pageSize);
    }

    @Override
    public String toString() {
        return "RecommendArticleCacheKey{"
            + "category='" + category + '\''
            + ", page=" + page
            + ", pageSize=" + pageSize
            + '}';
    }
}
