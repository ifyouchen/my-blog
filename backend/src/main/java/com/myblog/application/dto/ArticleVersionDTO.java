package com.myblog.application.dto;

/**
 * 文章版本历史数据传输对象。
 *
 * @author my-blog
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

    /**
     * 获取版本号。
     *
     * @return 版本号
     */
    public Integer getVersionNo() {
        return versionNo;
    }

    /**
     * 设置版本号。
     *
     * @param versionNo 版本号
     */
    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    /**
     * 获取保存时间。
     *
     * @return 保存时间
     */
    public String getSavedAt() {
        return savedAt;
    }

    /**
     * 设置保存时间。
     *
     * @param savedAt 保存时间
     */
    public void setSavedAt(String savedAt) {
        this.savedAt = savedAt;
    }

    /**
     * 获取版本标题。
     *
     * @return 版本标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置版本标题。
     *
     * @param title 版本标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取版本摘要。
     *
     * @return 版本摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置版本摘要。
     *
     * @param summary 版本摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取版本正文。
     *
     * @return 版本正文
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置版本正文。
     *
     * @param content 版本正文
     */
    public void setContent(String content) {
        this.content = content;
    }
}

