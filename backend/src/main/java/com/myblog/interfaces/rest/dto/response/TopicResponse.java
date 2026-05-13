package com.myblog.interfaces.rest.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 专题响应.
 * <p>
 * 专题详情信息，包含基本信息、难度等级、学习进度等.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class TopicResponse {

    /** 专题ID. */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 专题标题. */
    private String title;

    /** 专题摘要. */
    private String summary;

    /** 封面图片URL. */
    private String coverUrl;

    /** 包含文章数. */
    private int articleCount;

    /** 专题介绍. */
    private String intro;

    /** 难度等级（如 beginner、intermediate、advanced）. */
    private String difficulty;

    /** 预估阅读时长（分钟）. */
    private Integer estimatedMinutes;

    /** 来源类型. */
    private String sourceType;

    /** 来源备注说明. */
    private String sourceNote;

    /** 是否为推荐专题. */
    private boolean recommended;

    /** 推荐权重，值越大越靠前. */
    private Integer recommendWeight;

    /** 当前用户的学习进度. */
    private Object progress;

    /** 专题大纲/目录结构. */
    private Object outline;

    /** 下一篇待读文章. */
    private ArticleResponse nextArticle;

    /**
     * 获取专题ID.
     *
     * @return 专题ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置专题ID.
     *
     * @param id 专题ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取专题标题.
     *
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置专题标题.
     *
     * @param title 标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取专题摘要.
     *
     * @return 摘要
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 设置专题摘要.
     *
     * @param summary 摘要
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 获取封面图片URL.
     *
     * @return 封面URL
     */
    public String getCoverUrl() {
        return coverUrl;
    }

    /**
     * 设置封面图片URL.
     *
     * @param coverUrl 封面URL
     */
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    /**
     * 获取包含文章数.
     *
     * @return 文章数
     */
    public int getArticleCount() {
        return articleCount;
    }

    /**
     * 设置包含文章数.
     *
     * @param articleCount 文章数
     */
    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
    }

    /**
     * 获取专题介绍.
     *
     * @return 介绍文本
     */
    public String getIntro() {
        return intro;
    }

    /**
     * 设置专题介绍.
     *
     * @param intro 介绍文本
     */
    public void setIntro(String intro) {
        this.intro = intro;
    }

    /**
     * 获取难度等级.
     *
     * @return 难度等级
     */
    public String getDifficulty() {
        return difficulty;
    }

    /**
     * 设置难度等级.
     *
     * @param difficulty 难度等级
     */
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * 获取预估阅读时长.
     *
     * @return 预估时长（分钟）
     */
    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    /**
     * 设置预估阅读时长.
     *
     * @param estimatedMinutes 预估时长（分钟）
     */
    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    /**
     * 获取来源类型.
     *
     * @return 来源类型
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * 设置来源类型.
     *
     * @param sourceType 来源类型
     */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 获取来源备注.
     *
     * @return 来源备注
     */
    public String getSourceNote() {
        return sourceNote;
    }

    /**
     * 设置来源备注.
     *
     * @param sourceNote 来源备注
     */
    public void setSourceNote(String sourceNote) {
        this.sourceNote = sourceNote;
    }

    /**
     * 是否为推荐专题.
     *
     * @return 是否推荐
     */
    public boolean isRecommended() {
        return recommended;
    }

    /**
     * 设置是否推荐.
     *
     * @param recommended 是否推荐
     */
    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    /**
     * 获取推荐权重.
     *
     * @return 推荐权重
     */
    public Integer getRecommendWeight() {
        return recommendWeight;
    }

    /**
     * 设置推荐权重.
     *
     * @param recommendWeight 推荐权重
     */
    public void setRecommendWeight(Integer recommendWeight) {
        this.recommendWeight = recommendWeight;
    }

    /**
     * 获取学习进度.
     *
     * @return 进度对象
     */
    public Object getProgress() {
        return progress;
    }

    /**
     * 设置学习进度.
     *
     * @param progress 进度对象
     */
    public void setProgress(Object progress) {
        this.progress = progress;
    }

    /**
     * 获取大纲结构.
     *
     * @return 大纲对象
     */
    public Object getOutline() {
        return outline;
    }

    /**
     * 设置大纲结构.
     *
     * @param outline 大纲对象
     */
    public void setOutline(Object outline) {
        this.outline = outline;
    }

    /**
     * 获取下一篇待读文章.
     *
     * @return 文章信息
     */
    public ArticleResponse getNextArticle() {
        return nextArticle;
    }

    /**
     * 设置下一篇待读文章.
     *
     * @param nextArticle 文章信息
     */
    public void setNextArticle(ArticleResponse nextArticle) {
        this.nextArticle = nextArticle;
    }
}
