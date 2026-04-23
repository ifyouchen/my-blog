package com.myblog.application.dto;

/**
 * 专栏应用数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ColumnDTO {

    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private int subscriberCount;
    private int articleCount;
    private boolean subscribed;
    private UserDTO author;

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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public int getSubscriberCount() {
        return subscriberCount;
    }

    public void setSubscriberCount(int subscriberCount) {
        this.subscriberCount = subscriberCount;
    }

    public int getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public UserDTO getAuthor() {
        return author;
    }

    public void setAuthor(UserDTO author) {
        this.author = author;
    }
}
