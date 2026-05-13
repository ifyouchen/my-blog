package com.myblog.application.dto;

import java.util.List;

/**
 * 搜索启动数据。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class SearchBootstrapDTO {

    /** 分类列表 */
    private List<CategoryDTO> categories;
    /** 标签列表 */
    private List<TagDTO> tags;
    /** 热门搜索词 */
    private List<String> hotKeywords;
    /** 最近搜索词 */
    private List<String> recentKeywords;
    /** 推荐专题列表 */
    private List<TopicDTO> recommendedTopics;
    /** 推荐专栏列表 */
    private List<ColumnDTO> recommendedColumns;

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
     * 获取标签列表。
     *
     * @return 标签列表
     */
    public List<TagDTO> getTags() {
        return tags;
    }

    /**
     * 设置标签列表。
     *
     * @param tags 标签列表
     */
    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
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

    /**
     * 获取最近搜索词。
     *
     * @return 最近搜索词
     */
    public List<String> getRecentKeywords() {
        return recentKeywords;
    }

    /**
     * 设置最近搜索词。
     *
     * @param recentKeywords 最近搜索词
     */
    public void setRecentKeywords(List<String> recentKeywords) {
        this.recentKeywords = recentKeywords;
    }

    /**
     * 获取推荐专题列表。
     *
     * @return 推荐专题列表
     */
    public List<TopicDTO> getRecommendedTopics() {
        return recommendedTopics;
    }

    /**
     * 设置推荐专题列表。
     *
     * @param recommendedTopics 推荐专题列表
     */
    public void setRecommendedTopics(List<TopicDTO> recommendedTopics) {
        this.recommendedTopics = recommendedTopics;
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
}
