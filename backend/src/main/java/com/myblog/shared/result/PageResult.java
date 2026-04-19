package com.myblog.shared.result;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应数据。
 *
 * @param <T> 列表元素类型
 * @author Codex
 * @since 1.0.0
 */
public class PageResult<T> {

    private List<T> items;
    private int page;
    private int pageSize;
    private long total;

    /**
     * 创建空分页响应。
     */
    public PageResult() {
    }

    /**
     * 创建分页响应。
     *
     * @param items 当前页数据
     * @param page 页码
     * @param pageSize 每页数量
     * @param total 总数
     */
    public PageResult(List<T> items, int page, int pageSize, long total) {
        this.items = items == null ? Collections.<T>emptyList() : items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    /**
     * 获取当前页数据。
     *
     * @return 当前页数据
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * 设置当前页数据。
     *
     * @param items 当前页数据
     */
    public void setItems(List<T> items) {
        this.items = items;
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
     * 设置页码。
     *
     * @param page 页码
     */
    public void setPage(int page) {
        this.page = page;
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
     * 设置每页数量。
     *
     * @param pageSize 每页数量
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 获取总数。
     *
     * @return 总数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置总数。
     *
     * @param total 总数
     */
    public void setTotal(long total) {
        this.total = total;
    }
}
