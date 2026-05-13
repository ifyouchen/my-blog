package com.myblog.interfaces.rest.dto.response;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章推荐分组响应.
 * <p>
 * 表示一个推荐分组（如"相关推荐"、"同作者推荐"、"热门推荐"等），包含分组内的文章列表.
 *
 * @author my-blog
 * @since 1.0.0
 */
public class ArticleRecommendationSectionResponse {

    /** 分组唯一标识（如 related、same_author、hot）. */
    private String key;

    /** 分组显示标题（如"相关推荐"、"同作者文章"）. */
    private String title;

    /** 分组内的文章列表. */
    private List<ArticleResponse> items = new ArrayList<ArticleResponse>();

    /**
     * 获取分组标识.
     *
     * @return 分组key
     */
    public String getKey() {
        return key;
    }

    /**
     * 设置分组标识.
     *
     * @param key 分组key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 获取分组显示标题.
     *
     * @return 显示标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置分组显示标题.
     *
     * @param title 显示标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 获取分组内文章列表.
     *
     * @return 文章列表
     */
    public List<ArticleResponse> getItems() {
        return items;
    }

    /**
     * 设置分组内文章列表.
     *
     * @param items 文章列表
     */
    public void setItems(List<ArticleResponse> items) {
        this.items = items == null ? new ArrayList<ArticleResponse>() : items;
    }
}
