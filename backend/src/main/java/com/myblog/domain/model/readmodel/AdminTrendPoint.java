package com.myblog.domain.model.readmodel;

/**
 * 管理后台每日新增趋势点。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdminTrendPoint {

    private String date;
    private long newUsers;
    private long newArticles;
    private long newComments;

    /**
     * 获取日期。
     *
     * @return 日期
     */
    public String getDate() {
        return date;
    }

    /**
     * 设置日期。
     *
     * @param date 日期
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * 获取新增用户数。
     *
     * @return 新增用户数
     */
    public long getNewUsers() {
        return newUsers;
    }

    /**
     * 设置新增用户数。
     *
     * @param newUsers 新增用户数
     */
    public void setNewUsers(long newUsers) {
        this.newUsers = newUsers;
    }

    /**
     * 获取新增文章数。
     *
     * @return 新增文章数
     */
    public long getNewArticles() {
        return newArticles;
    }

    /**
     * 设置新增文章数。
     *
     * @param newArticles 新增文章数
     */
    public void setNewArticles(long newArticles) {
        this.newArticles = newArticles;
    }

    /**
     * 获取新增评论数。
     *
     * @return 新增评论数
     */
    public long getNewComments() {
        return newComments;
    }

    /**
     * 设置新增评论数。
     *
     * @param newComments 新增评论数
     */
    public void setNewComments(long newComments) {
        this.newComments = newComments;
    }
}
