package com.myblog.infrastructure.repository.persistence.entity;

/**
 * 管理后台趋势数据点 DO（按日期聚合的新增统计）。
 *
 * @author Codex
 * @since 1.0.0
 */
public class AdminTrendPointDO {

    private String date;
    private long newUsers;
    private long newArticles;
    private long newComments;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(long newUsers) {
        this.newUsers = newUsers;
    }

    public long getNewArticles() {
        return newArticles;
    }

    public void setNewArticles(long newArticles) {
        this.newArticles = newArticles;
    }

    public long getNewComments() {
        return newComments;
    }

    public void setNewComments(long newComments) {
        this.newComments = newComments;
    }
}

