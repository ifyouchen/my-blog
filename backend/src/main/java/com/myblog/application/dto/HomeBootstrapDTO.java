package com.myblog.application.dto;

import com.myblog.application.service.HomeStatsAppService.HomeStats;

import java.util.List;
import java.util.Map;

/**
 * 首页启动数据。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class HomeBootstrapDTO {

    /** 首页统计数据 */
    private HomeStats stats;
    /** 分类列表 */
    private List<CategoryDTO> categories;
    /** 分类大类映射（groupName → 分类列表） */
    private Map<String, List<CategoryDTO>> categoryGroups;
    /** 推荐专栏列表 */
    private List<ColumnDTO> recommendedColumns;
    /** 作者排行榜 */
    private List<AuthorRankingDTO> authorRankings;
    /** 编辑精选文章 */
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

    /**
     * 获取首页统计数据。
     *
     * @return 首页统计数据
     */
    public HomeStats getStats() {
        return stats;
    }

    /**
     * 设置首页统计数据。
     *
     * @param stats 首页统计数据
     */
    public void setStats(HomeStats stats) {
        this.stats = stats;
    }

    /**
     * 获取分类列表。
     *
     * @return 分类列表
     */
    public List<CategoryDTO> getCategories() {
        return categories;
    }

    /**
     * 设置分类列表。
     *
     * @param categories 分类列表
     */
    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories;
    }

    /**
     * 获取分类大类映射。
     *
     * @return 分类大类映射
     */
    public Map<String, List<CategoryDTO>> getCategoryGroups() {
        return categoryGroups;
    }

    /**
     * 设置分类大类映射。
     *
     * @param categoryGroups 分类大类映射
     */
    public void setCategoryGroups(Map<String, List<CategoryDTO>> categoryGroups) {
        this.categoryGroups = categoryGroups;
    }

    /**
     * 获取推荐专栏列表。
     *
     * @return 推荐专栏列表
     */
    public List<ColumnDTO> getRecommendedColumns() {
        return recommendedColumns;
    }

    /**
     * 设置推荐专栏列表。
     *
     * @param recommendedColumns 推荐专栏列表
     */
    public void setRecommendedColumns(List<ColumnDTO> recommendedColumns) {
        this.recommendedColumns = recommendedColumns;
    }

    /**
     * 获取作者排行榜。
     *
     * @return 作者排行榜
     */
    public List<AuthorRankingDTO> getAuthorRankings() {
        return authorRankings;
    }

    /**
     * 设置作者排行榜。
     *
     * @param authorRankings 作者排行榜
     */
    public void setAuthorRankings(List<AuthorRankingDTO> authorRankings) {
        this.authorRankings = authorRankings;
    }

    /**
     * 获取编辑精选文章。
     *
     * @return 编辑精选文章
     */
    public List<ArticleDTO> getFeaturedArticles() {
        return featuredArticles;
    }

    /**
     * 设置编辑精选文章。
     *
     * @param featuredArticles 编辑精选文章
     */
    public void setFeaturedArticles(List<ArticleDTO> featuredArticles) {
        this.featuredArticles = featuredArticles;
    }

    /**
     * 获取本周必读文章。
     *
     * @return 本周必读文章
     */
    public List<ArticleDTO> getWeeklyArticles() {
        return weeklyArticles;
    }

    /**
     * 设置本周必读文章。
     *
     * @param weeklyArticles 本周必读文章
     */
    public void setWeeklyArticles(List<ArticleDTO> weeklyArticles) {
        this.weeklyArticles = weeklyArticles;
    }

    /**
     * 获取热门专题。
     *
     * @return 热门专题
     */
    public List<TopicDTO> getHotTopics() {
        return hotTopics;
    }

    /**
     * 设置热门专题。
     *
     * @param hotTopics 热门专题
     */
    public void setHotTopics(List<TopicDTO> hotTopics) {
        this.hotTopics = hotTopics;
    }

    /**
     * 获取今日焦点文章。
     *
     * @return 今日焦点文章
     */
    public ArticleDTO getTodayFocus() {
        return todayFocus;
    }

    /**
     * 设置今日焦点文章。
     *
     * @param todayFocus 今日焦点文章
     */
    public void setTodayFocus(ArticleDTO todayFocus) {
        this.todayFocus = todayFocus;
    }

    /**
     * 获取学习路径专题。
     *
     * @return 学习路径专题
     */
    public List<TopicDTO> getLearningTopics() {
        return learningTopics;
    }

    /**
     * 设置学习路径专题。
     *
     * @param learningTopics 学习路径专题
     */
    public void setLearningTopics(List<TopicDTO> learningTopics) {
        this.learningTopics = learningTopics;
    }

    /**
     * 获取热门标签。
     *
     * @return 热门标签
     */
    public List<TagDTO> getHotTags() {
        return hotTags;
    }

    /**
     * 设置热门标签。
     *
     * @param hotTags 热门标签
     */
    public void setHotTags(List<TagDTO> hotTags) {
        this.hotTags = hotTags;
    }

    /**
     * 获取热门搜索词。
     *
     * @return 热门搜索词
     */
    public List<String> getHotKeywords() {
        return hotKeywords;
    }

    /**
     * 设置热门搜索词。
     *
     * @param hotKeywords 热门搜索词
     */
    public void setHotKeywords(List<String> hotKeywords) {
        this.hotKeywords = hotKeywords;
    }
}
