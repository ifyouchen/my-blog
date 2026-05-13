package com.myblog.application.dto;

import com.myblog.shared.result.PageResult;

import java.util.List;

/**
 * 统一知识搜索结果。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class UnifiedSearchResultDTO {

    /** 搜索关键词 */
    private String keyword;
    /** 文章搜索结果（分页） */
    private PageResult<ArticleDTO> articles;
    /** 专题搜索结果（分页） */
    private PageResult<TopicDTO> topics;
    /** 专栏搜索结果（分页） */
    private PageResult<ColumnDTO> columns;
    /** 匹配的标签列表 */
    private List<TagDTO> tags;

    /**
     * 获取搜索关键词。
     *
     * @return 搜索关键词
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 设置搜索关键词。
     *
     * @param keyword 搜索关键词
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * 获取文章搜索结果。
     *
     * @return 文章搜索结果
     */
    public PageResult<ArticleDTO> getArticles() {
        return articles;
    }

    /**
     * 设置文章搜索结果。
     *
     * @param articles 文章搜索结果
     */
    public void setArticles(PageResult<ArticleDTO> articles) {
        this.articles = articles;
    }

    /**
     * 获取专题搜索结果。
     *
     * @return 专题搜索结果
     */
    public PageResult<TopicDTO> getTopics() {
        return topics;
    }

    /**
     * 设置专题搜索结果。
     *
     * @param topics 专题搜索结果
     */
    public void setTopics(PageResult<TopicDTO> topics) {
        this.topics = topics;
    }

    /**
     * 获取专栏搜索结果。
     *
     * @return 专栏搜索结果
     */
    public PageResult<ColumnDTO> getColumns() {
        return columns;
    }

    /**
     * 设置专栏搜索结果。
     *
     * @param columns 专栏搜索结果
     */
    public void setColumns(PageResult<ColumnDTO> columns) {
        this.columns = columns;
    }

    /**
     * 获取匹配的标签列表。
     *
     * @return 匹配的标签列表
     */
    public List<TagDTO> getTags() {
        return tags;
    }

    /**
     * 设置匹配的标签列表。
     *
     * @param tags 匹配的标签列表
     */
    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
    }
}
