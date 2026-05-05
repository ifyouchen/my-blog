package com.myblog.interfaces.rest.dto.response;

import com.myblog.application.dto.CategoryDTO;
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
}
