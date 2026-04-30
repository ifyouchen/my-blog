package com.myblog.application.dto;

/**
 * 文章版本历史 DTO。
 *
 * @author Codex
 * @since 1.0.0
 */
public class ArticleVersionDTO {

    /** 版本号 */
    private Integer versionNo;
    /** 保存时间（格式化字符串） */
    private String savedAt;
    /** 版本标题（列表用） */
    private String title;
    /** 版本摘要（列表用） */
    private String summary;
    /** 版本正文（详情用，列表时为空） */
    private String content;

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(String savedAt) {
        this.savedAt = savedAt;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

