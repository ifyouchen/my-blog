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
    /** 本周必读：算法挑选，不受管理员精选权重影响 */
    private List<ArticleDTO> weeklyArticles;
    /** 首页侧边栏热门专题 */
    private List<TopicDTO> hotTopics;
    /** 今日焦点：默认取编辑精选第一篇 */
    private ArticleDTO todayFocus;
    /** 学习路径专题：强调专题资产入口 */
    private List<TopicDTO> learningTopics;
    /** 知识地图热门标签 */
    private List<TagDTO> hotTags;
    /** 热门搜索词 */
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

    public List<ArticleDTO> getWeeklyArticles() {
        return weeklyArticles;
    }

    public void setWeeklyArticles(List<ArticleDTO> weeklyArticles) {
        this.weeklyArticles = weeklyArticles;
    }

    public List<TopicDTO> getHotTopics() {
        return hotTopics;
    }

    public void setHotTopics(List<TopicDTO> hotTopics) {
        this.hotTopics = hotTopics;
    }

    public ArticleDTO getTodayFocus() {
        return todayFocus;
    }

    public void setTodayFocus(ArticleDTO todayFocus) {
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
