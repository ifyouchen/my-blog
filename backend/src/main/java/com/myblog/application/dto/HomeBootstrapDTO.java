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
}
