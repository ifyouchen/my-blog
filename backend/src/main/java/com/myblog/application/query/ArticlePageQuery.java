package com.myblog.application.query;

/**
 * 文章分页查询。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticlePageQuery {

    private int page;
    private int pageSize;
    private String keyword;
    private String category;
    private String tag;

    /**
     * 创建文章分页查询。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param keyword 关键字
     * @param category 分类
     * @param tag 标签
     */
    public ArticlePageQuery(int page, int pageSize, String keyword, String category, String tag) {
        this.page = page <= 0 ? 1 : page;
        this.pageSize = pageSize <= 0 ? 10 : pageSize;
        this.keyword = keyword;
        this.category = category;
        this.tag = tag;
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
}
