package com.myblog.application.query;

/**
 * 文章分页查询。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticlePageQuery {

    public static final String SORT_LATEST = "latest";
    public static final String SORT_HOT = "hot";
    public static final String SORT_FEATURED = "featured";

    private int page;
    private int pageSize;
    private String keyword;
    private String category;
    private String tag;
    private String sort;

    /**
     * 创建文章分页查询。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     * @param sort 排序方式
     */
    public ArticlePageQuery(int page, int pageSize, String keyword, String category, String tag, String sort) {
        this.page = page <= 0 ? 1 : page;
        this.pageSize = pageSize <= 0 ? 10 : pageSize;
        this.keyword = keyword;
        this.category = category;
        this.tag = tag;
        this.sort = normalizeSort(sort);
    }

    /**
     * 获取页码。
     *
     * @return 页码
     */
    public int getPage() {
        return page;
    }

    /**
     * 获取每页数量。
     *
     * @return 每页数量
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 获取搜索关键字。
     *
     * @return 搜索关键字
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 获取分类筛选条件。
     *
     * @return 分类筛选条件
     */
    public String getCategory() {
        return category;
    }

    /**
     * 获取标签筛选条件。
     *
     * @return 标签筛选条件
     */
    public String getTag() {
        return tag;
    }

    /**
     * 获取排序方式。
     *
     * @return 排序方式
     */
    public String getSort() {
        return sort;
    }

    /**
     * 规范化排序方式。
     *
     * @param sort 原始排序方式
     * @return 规范化后的排序方式
     */
    private String normalizeSort(String sort) {
        if (SORT_HOT.equals(sort)) {
            return SORT_HOT;
        }
        if (SORT_FEATURED.equals(sort)) {
            return SORT_FEATURED;
        }
        return SORT_LATEST;
    }
}
