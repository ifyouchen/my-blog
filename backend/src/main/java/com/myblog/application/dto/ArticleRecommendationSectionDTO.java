package com.myblog.application.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章推荐分组应用数据。
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleRecommendationSectionDTO {

    /** 分组唯一标识 */
    private String key;
    /** 分组标题 */
    private String title;
    /** 分组内文章列表 */
    private List<ArticleDTO> items = new ArrayList<ArticleDTO>();

    /**
     * 默认构造函数。
     */
    public ArticleRecommendationSectionDTO() {
    }

    /**
     * 使用分组信息构造。
     *
     * @param key   分组唯一标识
     * @param title 分组标题
     * @param items 分组内文章列表
     */
    public ArticleRecommendationSectionDTO(String key, String title, List<ArticleDTO> items) {
        this.key = key;
        this.title = title;
        this.items = items == null ? new ArrayList<ArticleDTO>() : items;
    }

    /**
     * 获取分组唯一标识。
     *
     * @return 分组唯一标识
     */
    public String getKey() {
        return key;
    }

    /**
     * 设置分组唯一标识。
     *
     * @param key 分组唯一标识
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 获取分组标题。
     *
     * @return 分组标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置分组标题。
     *
     * @param title 分组标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取分组内文章列表。
     *
     * @return 分组内文章列表
     */
    public List<ArticleDTO> getItems() {
        return items;
    }

    /**
     * 设置分组内文章列表。
     *
     * @param items 分组内文章列表
     */
    public void setItems(List<ArticleDTO> items) {
        this.items = items == null ? new ArrayList<ArticleDTO>() : items;
    }
}
