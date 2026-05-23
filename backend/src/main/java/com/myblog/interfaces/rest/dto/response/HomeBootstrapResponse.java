package com.myblog.interfaces.rest.dto.response;

import com.myblog.application.dto.CategoryDTO;
import com.myblog.application.dto.TagDTO;
import com.myblog.application.dto.TopicDTO;
import com.myblog.application.service.HomeStatsAppService.HomeStats;

import java.util.List;
import java.util.Map;

/**
 * 首页启动数据响应.
 * <p>
 * 首页加载时一次性返回的所有聚合数据，包括统计信息、推荐内容、热门标签等.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class HomeBootstrapResponse {

    /** 站点统计数据（文章数、用户数等）. */
    private HomeStats stats;

    /** 分类列表. */
    private List<CategoryDTO> categories;

    /** 分类大类映射（groupName → 分类列表）. */
    private Map<String, List<CategoryDTO>> categoryGroups;

    /** 推荐专栏列表. */
    private List<ColumnResponse> recommendedColumns;

    /** 作者排行榜列表. */
    private List<AuthorRankingResponse> authorRankings;

    /** 精选文章列表. */
    private List<ArticleResponse> featuredArticles;

    /** 本周热门文章列表. */
    private List<ArticleResponse> weeklyArticles;

    /** 热门专题列表. */
    private List<TopicDTO> hotTopics;

    /** 今日焦点文章. */
    private ArticleResponse todayFocus;

    /** 学习专题列表. */
    private List<TopicDTO> learningTopics;

    /** 热门标签列表. */
    private List<TagDTO> hotTags;

    /** 热门关键词列表. */
    private List<String> hotKeywords;

    /**
     * 获取站点统计数据.
     *
     * @return 统计数据
     */
    public HomeStats getStats() {
        return stats;
    }

    /**
     * 设置站点统计数据.
     *
     * @param stats 统计数据
     */
    public void setStats(HomeStats stats) {
        this.stats = stats;
    }

    /**
     * 获取分类列表.
     *
     * @return 分类列表
     */
    public List<CategoryDTO> getCategories() {
        return categories;
    }

    /**
     * 设置分类列表.
     *
     * @param categories 分类列表
     */
    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories;
    }

    /**
     * 获取分类大类映射.
     *
     * @return 分类大类映射
     */
    public Map<String, List<CategoryDTO>> getCategoryGroups() {
        return categoryGroups;
    }

    /**
     * 设置分类大类映射.
     *
     * @param categoryGroups 分类大类映射
     */
    public void setCategoryGroups(Map<String, List<CategoryDTO>> categoryGroups) {
        this.categoryGroups = categoryGroups;
    }

    /**
     * 获取推荐专栏列表.
     *
     * @return 专栏列表
     */
    public List<ColumnResponse> getRecommendedColumns() {
        return recommendedColumns;
    }

    /**
     * 设置推荐专栏列表.
     *
     * @param recommendedColumns 专栏列表
     */
    public void setRecommendedColumns(List<ColumnResponse> recommendedColumns) {
        this.recommendedColumns = recommendedColumns;
    }

    /**
     * 获取作者排行榜列表.
     *
     * @return 作者排行榜
     */
    public List<AuthorRankingResponse> getAuthorRankings() {
        return authorRankings;
    }

    /**
     * 设置作者排行榜列表.
     *
     * @param authorRankings 作者排行榜
     */
    public void setAuthorRankings(List<AuthorRankingResponse> authorRankings) {
        this.authorRankings = authorRankings;
    }

    /**
     * 获取精选文章列表.
     *
     * @return 文章列表
     */
    public List<ArticleResponse> getFeaturedArticles() {
        return featuredArticles;
    }

    /**
     * 设置精选文章列表.
     *
     * @param featuredArticles 文章列表
     */
    public void setFeaturedArticles(List<ArticleResponse> featuredArticles) {
        this.featuredArticles = featuredArticles;
    }

    /**
     * 获取本周热门文章列表.
     *
     * @return 文章列表
     */
    public List<ArticleResponse> getWeeklyArticles() {
        return weeklyArticles;
    }

    /**
     * 设置本周热门文章列表.
     *
     * @param weeklyArticles 文章列表
     */
    public void setWeeklyArticles(List<ArticleResponse> weeklyArticles) {
        this.weeklyArticles = weeklyArticles;
    }

    /**
     * 获取热门专题列表.
     *
     * @return 专题列表
     */
    public List<TopicDTO> getHotTopics() {
        return hotTopics;
    }

    /**
     * 设置热门专题列表.
     *
     * @param hotTopics 专题列表
     */
    public void setHotTopics(List<TopicDTO> hotTopics) {
        this.hotTopics = hotTopics;
    }

    /**
     * 获取今日焦点文章.
     *
     * @return 今日焦点文章
     */
    public ArticleResponse getTodayFocus() {
        return todayFocus;
    }

    /**
     * 设置今日焦点文章.
     *
     * @param todayFocus 今日焦点文章
     */
    public void setTodayFocus(ArticleResponse todayFocus) {
        this.todayFocus = todayFocus;
    }

    /**
     * 获取学习专题列表.
     *
     * @return 专题列表
     */
    public List<TopicDTO> getLearningTopics() {
        return learningTopics;
    }

    /**
     * 设置学习专题列表.
     *
     * @param learningTopics 专题列表
     */
    public void setLearningTopics(List<TopicDTO> learningTopics) {
        this.learningTopics = learningTopics;
    }

    /**
     * 获取热门标签列表.
     *
     * @return 标签列表
     */
    public List<TagDTO> getHotTags() {
        return hotTags;
    }

    /**
     * 设置热门标签列表.
     *
     * @param hotTags 标签列表
     */
    public void setHotTags(List<TagDTO> hotTags) {
        this.hotTags = hotTags;
    }

    /**
     * 获取热门关键词列表.
     *
     * @return 关键词列表
     */
    public List<String> getHotKeywords() {
        return hotKeywords;
    }

    /**
     * 设置热门关键词列表.
     *
     * @param hotKeywords 关键词列表
     */
    public void setHotKeywords(List<String> hotKeywords) {
        this.hotKeywords = hotKeywords;
    }
}
