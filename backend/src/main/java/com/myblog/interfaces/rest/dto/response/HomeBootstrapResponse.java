package com.myblog.interfaces.rest.dto.response;

import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.dto.TopicDTO;
import com.myblog.application.service.HomeStatsAppService.HomeStats;

import java.util.List;

/**
 * 首页启动数据响应。
 *
 * @author Codex
 * @since 1.0.0
 */
public class HomeBootstrapResponse {

    private HomeStats stats;
    private List<CategoryDTO> categories;
    private List<ColumnResponse> recommendedColumns;
    private List<AuthorRankingResponse> authorRankings;
    private List<ArticleResponse> featuredArticles;
    private List<TopicDTO> hotTopics;
    private ArticleResponse todayFocus;
    private List<TopicDTO> learningTopics;
    private List<TagDTO> hotTags;
    private List<String> hotKeywords;

    public HomeStats getStats() {
        return stats;
    }

    public void setStats(HomeStats stats) {
        this.stats = stats;
    }

    public List<CategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories;
    }

    public List<ColumnResponse> getRecommendedColumns() {
        return recommendedColumns;
    }

    public void setRecommendedColumns(List<ColumnResponse> recommendedColumns) {
        this.recommendedColumns = recommendedColumns;
    }

    public List<AuthorRankingResponse> getAuthorRankings() {
        return authorRankings;
    }

    public void setAuthorRankings(List<AuthorRankingResponse> authorRankings) {
        this.authorRankings = authorRankings;
    }

    public List<ArticleResponse> getFeaturedArticles() {
        return featuredArticles;
    }

    public void setFeaturedArticles(List<ArticleResponse> featuredArticles) {
        this.featuredArticles = featuredArticles;
    }

    public List<TopicDTO> getHotTopics() {
        return hotTopics;
    }

    public void setHotTopics(List<TopicDTO> hotTopics) {
        this.hotTopics = hotTopics;
    }

    public ArticleResponse getTodayFocus() {
        return todayFocus;
    }

    public void setTodayFocus(ArticleResponse todayFocus) {
        this.todayFocus = todayFocus;
    }

    public List<TopicDTO> getLearningTopics() {
        return learningTopics;
    }

    public void setLearningTopics(List<TopicDTO> learningTopics) {
        this.learningTopics = learningTopics;
    }

    public List<TagDTO> getHotTags() {
        return hotTags;
    }

    public void setHotTags(List<TagDTO> hotTags) {
        this.hotTags = hotTags;
    }

    public List<String> getHotKeywords() {
        return hotKeywords;
    }

    public void setHotKeywords(List<String> hotKeywords) {
        this.hotKeywords = hotKeywords;
    }
}
