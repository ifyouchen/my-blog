package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 管理后台趋势数据点 DO（按日期聚合的新增统计）。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdminTrendPointDO {

    /**
     * 统计日期（格式：yyyy-MM-dd）
     */
    private String date;

    /**
     * 当日新增用户数
     */
    private long newUsers;

    /**
     * 当日新增文章数
     */
    private long newArticles;

    /**
     * 当日新增评论数
     */
    private long newComments;

    /**
     * 获取统计日期。
     *
     * @return 日期字符串（格式：yyyy-MM-dd）
     */
    public String getDate() {
        return date;
    }

    /**
     * 设置统计日期。
     *
     * @param date 日期字符串
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * 获取当日新增用户数。
     *
     * @return 新增用户数
     */
    public long getNewUsers() {
        return newUsers;
    }

    /**
     * 设置当日新增用户数。
     *
     * @param newUsers 新增用户数
     */
    public void setNewUsers(long newUsers) {
        this.newUsers = newUsers;
    }

    /**
     * 获取当日新增文章数。
     *
     * @return 新增文章数
     */
    public long getNewArticles() {
        return newArticles;
    }

    /**
     * 设置当日新增文章数。
     *
     * @param newArticles 新增文章数
     */
    public void setNewArticles(long newArticles) {
        this.newArticles = newArticles;
    }

    /**
     * 获取当日新增评论数。
     *
     * @return 新增评论数
     */
    public long getNewComments() {
        return newComments;
    }

    /**
     * 设置当日新增评论数。
     *
     * @param newComments 新增评论数
     */
    public void setNewComments(long newComments) {
        this.newComments = newComments;
    }
}

