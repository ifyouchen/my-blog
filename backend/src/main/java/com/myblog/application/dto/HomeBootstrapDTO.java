package com.myblog.application.dto;

import com.myblog.application.service.HomeStatsAppService.HomeStats;

import java.util.List;

/**
 * 首页启动数据。
 *
 * @author Codex
 * @since 1.0.0
 */
public class HomeBootstrapDTO {

    private HomeStats stats;
    private List<CategoryDTO> categories;
    private List<ColumnDTO> recommendedColumns;
    private List<AuthorRankingDTO> authorRankings;
    private List<ArticleDTO> featuredArticles;
    /** 首页侧边栏热门专题 */
    private List<TopicDTO> hotTopics;

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

    public List<ColumnDTO> getRecommendedColumns() {
        return recommendedColumns;
    }

    public void setRecommendedColumns(List<ColumnDTO> recommendedColumns) {
        this.recommendedColumns = recommendedColumns;
    }

    public List<AuthorRankingDTO> getAuthorRankings() {
        return authorRankings;
    }

    public void setAuthorRankings(List<AuthorRankingDTO> authorRankings) {
        this.authorRankings = authorRankings;
    }

    public List<ArticleDTO> getFeaturedArticles() {
        return featuredArticles;
    }

    public void setFeaturedArticles(List<ArticleDTO> featuredArticles) {
        this.featuredArticles = featuredArticles;
    }

    public List<TopicDTO> getHotTopics() {
        return hotTopics;
    }

    public void setHotTopics(List<TopicDTO> hotTopics) {
        this.hotTopics = hotTopics;
    }
}
