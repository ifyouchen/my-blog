package com.myblog.application.query;

import java.util.Objects;

/**
 * 已发布文章分页缓存键。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticlePageCacheKey {

    private final int page;
    private final int pageSize;
    private final String keyword;
    private final String category;
    private final String categoryGroup;
    private final String tag;
    private final String sort;
    private final String authorKeyword;
    private final String dateFrom;
    private final String dateTo;
    private final boolean fulltext;

    private ArticlePageCacheKey(ArticlePageQuery query, boolean fulltext) {
        this.page = Math.max(query.getPage(), 1);
        this.pageSize = Math.max(query.getPageSize(), 1);
        this.keyword = normalize(query.getKeyword());
        this.category = normalize(query.getCategory());
        this.categoryGroup = normalize(query.getCategoryGroup());
        this.tag = normalize(query.getTag());
        this.sort = normalize(query.getSort());
        this.authorKeyword = normalize(query.getAuthorKeyword());
        this.dateFrom = normalize(query.getDateFrom());
        this.dateTo = normalize(query.getDateTo());
        this.fulltext = fulltext;
    }

    /**
     * 创建文章分页缓存键。
     *
     * @param query 文章分页查询
     * @param fulltext 是否使用全文索引
     * @return 缓存键
     */
    public static ArticlePageCacheKey of(ArticlePageQuery query, boolean fulltext) {
        return new ArticlePageCacheKey(query, fulltext);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArticlePageCacheKey)) {
            return false;
        }
        ArticlePageCacheKey that = (ArticlePageCacheKey) o;
        return page == that.page
            && pageSize == that.pageSize
            && fulltext == that.fulltext
            && Objects.equals(keyword, that.keyword)
            && Objects.equals(category, that.category)
            && Objects.equals(categoryGroup, that.categoryGroup)
            && Objects.equals(tag, that.tag)
            && Objects.equals(sort, that.sort)
            && Objects.equals(authorKeyword, that.authorKeyword)
            && Objects.equals(dateFrom, that.dateFrom)
            && Objects.equals(dateTo, that.dateTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, pageSize, keyword, category, categoryGroup, tag, sort, authorKeyword, dateFrom, dateTo, fulltext);
    }

    @Override
    public String toString() {
        return page + "|" + pageSize + "|" + keyword + "|" + category + "|" + categoryGroup + "|" + tag + "|" + sort + "|" + authorKeyword + "|" + dateFrom + "|" + dateTo + "|" + fulltext;
    }
}
