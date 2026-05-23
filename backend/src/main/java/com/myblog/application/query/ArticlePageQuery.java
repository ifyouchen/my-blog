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
    public static final String SORT_RECOMMEND = "recommend";

    private int page;
    private int pageSize;
    private String keyword;
    private String category;
    private String categoryGroup;
    private String tag;
    private String sort;
    private String authorKeyword;
    private String dateFrom;
    private String dateTo;
    private boolean followingOnly;
    private Long currentUserId;

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
     * 获取分类大类筛选条件。
     *
     * @return 分类大类
     */
    public String getCategoryGroup() {
        return categoryGroup;
    }

    /**
     * 设置分类大类筛选条件。
     *
     * @param categoryGroup 分类大类
     */
    public void setCategoryGroup(String categoryGroup) {
        this.categoryGroup = categoryGroup;
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
     * 获取作者关键字筛选条件。
     *
     * @return 作者关键字
     */
    public String getAuthorKeyword() {
        return authorKeyword;
    }

    /**
     * 设置作者关键字筛选条件。
     *
     * @param authorKeyword 作者关键字
     */
    public void setAuthorKeyword(String authorKeyword) {
        this.authorKeyword = authorKeyword;
    }

    /**
     * 获取起始日期筛选条件。
     *
     * @return 起始日期
     */
    public String getDateFrom() {
        return dateFrom;
    }

    /**
     * 设置起始日期筛选条件。
     *
     * @param dateFrom 起始日期
     */
    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    /**
     * 获取结束日期筛选条件。
     *
     * @return 结束日期
     */
    public String getDateTo() {
        return dateTo;
    }

    /**
     * 设置结束日期筛选条件。
     *
     * @param dateTo 结束日期
     */
    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    /**
     * 是否仅看已关注作者。
     *
     * @return 是否仅看已关注作者
     */
    public boolean isFollowingOnly() {
        return followingOnly;
    }

    /**
     * 设置是否仅看已关注作者。
     *
     * @param followingOnly 是否仅看已关注作者
     */
    public void setFollowingOnly(boolean followingOnly) {
        this.followingOnly = followingOnly;
    }

    /**
     * 获取当前用户ID。
     *
     * @return 当前用户ID
     */
    public Long getCurrentUserId() {
        return currentUserId;
    }

    /**
     * 设置当前用户ID。
     *
     * @param currentUserId 当前用户ID
     */
    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
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
        if (SORT_RECOMMEND.equals(sort)) {
            return SORT_RECOMMEND;
        }
        return SORT_LATEST;
    }
}
