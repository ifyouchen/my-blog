package com.myblog.application.dto;

import com.myblog.shared.result.PageResult;

import java.util.List;

/**
 * 统一知识搜索结果。
 *
 * @author Codex
 * @since 1.0.0
 */
public class UnifiedSearchResultDTO {

    private String keyword;
    private PageResult<ArticleDTO> articles;
    private PageResult<TopicDTO> topics;
    private PageResult<ColumnDTO> columns;
    private List<TagDTO> tags;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public PageResult<ArticleDTO> getArticles() {
        return articles;
    }

    public void setArticles(PageResult<ArticleDTO> articles) {
        this.articles = articles;
    }

    public PageResult<TopicDTO> getTopics() {
        return topics;
    }

    public void setTopics(PageResult<TopicDTO> topics) {
        this.topics = topics;
    }

    public PageResult<ColumnDTO> getColumns() {
        return columns;
    }

    public void setColumns(PageResult<ColumnDTO> columns) {
        this.columns = columns;
    }

    public List<TagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
    }
}
